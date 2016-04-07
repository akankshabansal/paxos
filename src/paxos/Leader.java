package paxos;

import java.util.*;
import java.util.logging.Level;

public class Leader extends Process {
    ProcessId[] acceptors;
    ProcessId[] replicas;
    BallotNumber ballot_number;

    boolean active = false;
    Heartbeat heartbeat;
    FailureDetector failureDetector;
    int failureDetectionTimeout;
    boolean failureDetection;
    Map<Integer, Command> proposals = new HashMap<Integer, Command>();
    //    public long leaseEndTime;
    public Map<Integer, Set<ReadOnlyMessage>> readOnlyMessagesFlag = new HashMap<Integer, Set<ReadOnlyMessage>>();
    public Set<Integer /*slot number*/> decisionsTaken = new HashSet<Integer>();

    public Leader(Env env, ProcessId me, ProcessId[] acceptors,
                  ProcessId[] replicas) {
        this.env = env;
        this.me = me;
        ballot_number = new BallotNumber(0, me);
        this.acceptors = acceptors;
        this.replicas = replicas;
        this.setLogger();
        loadProp();
        heartbeat = new Heartbeat(env, new ProcessId("heartbeat:" + me + ":"), this);
        failureDetectionTimeout = Integer.parseInt(prop.getProperty("failureDetectionTimeout"));
        env.addProc(me, this);
    }

    Properties loadProp() {
        super.loadProp();
        try {
            failureDetection = "TRUE".equalsIgnoreCase(prop.getProperty("failureDetection")) ? true : false;
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage());
        }
        return prop;
    }

    public void body() {
        logger.log(messageLevel, "Here I am: " + me);

        new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
                this, acceptors, ballot_number);
        while (!stop_request()) {
            //ToDo: CHECK AND RENEW YOUR LEASE (Handle from scout's end)
//            if(active == true && leaseEndTime < (System.currentTimeMillis()))
//                new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
//                        this, acceptors, ballot_number);

            PaxosMessage msg = getNextMessage();

            if (msg instanceof ProposeMessage) {
                ProposeMessage m = (ProposeMessage) msg;
                if (!proposals.containsKey(m.slot_number) || proposals.get(m.slot_number).op == null) {
                    //SLOT NUMBER IS NEW OR SLOT NUMBER's OP IS EMPTY
                    if (proposals.containsKey(m.slot_number) && proposals.get(m.slot_number).op == null) {
                        m.command.readOnlySets=proposals.get(m.slot_number).readOnlySets;
                    }
                    proposals.put(m.slot_number, m.command);
                    //Commanders should be created only when the leader is moving from not active to active
                    if (active) {
                        new Commander(env,
                                new ProcessId("commander:" + me + ":" + ballot_number + ":" + m.slot_number),
                                this, acceptors, replicas, ballot_number, m.slot_number, new Command(m.command));
                    }
                }
            } else if (msg instanceof AdoptedMessage) {
                AdoptedMessage m = (AdoptedMessage) msg;

                if (ballot_number.equals(m.ballot_number)) {
                    Map<Integer, BallotNumber> max = new HashMap<Integer, BallotNumber>();
                    for (PValue pv : m.accepted) {
                        BallotNumber bn = max.get(pv.slot_number);
                        if (bn == null || bn.compareTo(pv.ballot_number) < 0) {
                            if (pv.command.op == null && proposals.containsKey(pv.slot_number)) {
                                pv.command.updateWith(proposals.get(pv.slot_number));
                            }
                            max.put(pv.slot_number, pv.ballot_number);
                            proposals.put(pv.slot_number, pv.command);
                        }
                    }

                    for (int slot_no : proposals.keySet()) {
                        if (proposals.get(slot_no).op == null) {
                            for (ProcessId r : replicas) {
                                if (stop_request()) break;
                                sendMessage(r, new DecisionMessage(me, slot_no, new Command(proposals.get(slot_no))));
                            }
                        } else {
                            if (!active)
                                new Commander(env,
                                        new ProcessId("commander:" + me + ":" + ballot_number + ":" + slot_no),
                                        this, acceptors, replicas, ballot_number, slot_no, new Command(proposals.get(slot_no)));
                        }
                    }
                    active = true;
                }
            } else if (msg instanceof PreemptedMessage) {
                PreemptedMessage m = (PreemptedMessage) msg;
                if (ballot_number.compareTo(m.ballot_number) < 0) {
                    if (failureDetection) {
                        if(failureDetector == null) {
                            ProcessId activeLeader = m.ballot_number.leader_id;
                            BallotNumber lastActiveBallot_number = m.ballot_number;
                            failureDetector = new FailureDetector(env, new ProcessId("failureDetector:" + me + ":" + activeLeader), this, lastActiveBallot_number);
                            logger.log(messageLevel, "Created a FailureDetector for " + activeLeader);
                        } //JUST IGNORE IF FAILURE DETECTOR IS ALREADY RUNNING...ITS TIMEOUT WILL TAKE CARE
                    } else {
                        ballot_number = new BallotNumber(m.ballot_number.round + 1, me);
                        new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
                                this, acceptors, ballot_number);
                    }
                    active = false;
                }
            } else if (msg instanceof LeaderTimeoutMessage) {
                LeaderTimeoutMessage m = (LeaderTimeoutMessage) msg;
//                if (ballot_number.compareTo(m.lastActiveBallot_number) < 0) {
                ballot_number = new BallotNumber(m.lastActiveBallot_number.round + 1, me);
                new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
                        this, acceptors, ballot_number);
                active = false;
                failureDetector = null;
//                }
            } else if (msg instanceof ReadOnlyMessage) {
                ReadOnlyMessage m = (ReadOnlyMessage) msg;
//                logger.log(Level.FINER, "active :"+active+" leaseEnd: "+leaseEndTime+" T="+System.currentTimeMillis());
//                if(active) {
//                    if(leaseEndTime > System.currentTimeMillis()) {
                //straight away tell the last decided slot to the replica
                if (active) {
                    //We need to add this read only cmd to the proposal set. we need to do this so that any cmd which comes after this scout
                    //has been spawned then we want the commander for this slot to actually include the read only cmd.
                    Integer maxProposal = getPostMaxProposal();
                    if(proposals.get(maxProposal) != null)
                        proposals.get(maxProposal).readOnlySets.add(m.command);
                    else {
                        Set<Command> roMessage = new HashSet<Command>(); roMessage.add(m.command);
                        proposals.put(maxProposal, new Command(roMessage));
                    }
                /*Changed the name of scout so that we can understand the scout has been created for which slot no...*/
                    new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number + ":" + maxProposal),
                            this, acceptors, ballot_number, maxProposal, new Command(proposals.get(maxProposal)));
                }
//                sendMessage(msg.src, new ReadOnlyDecisionMessage(me, getMaxDecisionSlot(), m.command));
                //tag the next slot message
//                        Set<ReadOnlyMessage> current = readOnlyMessagesFlag.get(1 + getMaxDecisionSlot());
//                        if(current == null) current = new HashSet<ReadOnlyMessage>(); else current.add(m);
//                        readOnlyMessagesFlag.put(getMaxDecisionSlot(), current);
//                    } else {
//                        //first renew
//                        logger.log(Level.SEVERE, "LEASE EXPIRED!! ..");
//                    }
//                }
            } else {
                System.err.println("paxos.Leader: unknown msg type");
            }
        }
    }

    private Integer getMaxDecisionSlot() {
        if (decisionsTaken == null || decisionsTaken.isEmpty()) return 0;
        return Collections.max(decisionsTaken);
    }

    private Integer getPostMaxProposal() {
        if (proposals.keySet().isEmpty()) return 1;
        int max = 1;
        for (Integer i : proposals.keySet()) {
            if (proposals.get(i).op != null && max < i) max = i;
        }
        return (proposals.get(max) == null || proposals.get(max).op == null) ? max : max + 1;
    }
}
