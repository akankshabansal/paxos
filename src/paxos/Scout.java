package paxos;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class Scout extends Process {

    Leader leader;
	ProcessId[] acceptors;
	BallotNumber ballot_number;
    int maxPostProposal = -1;
    Command readOnlyCommand;

    public Scout(Env env, ProcessId me, Leader leader,
                 ProcessId[] acceptors, BallotNumber ballot_number, Integer maxPostProposal, Command c){
        this(env, me, leader, acceptors, ballot_number);
        this.maxPostProposal = maxPostProposal;
        this.readOnlyCommand = c;
    }

    public Scout(Env env, ProcessId me, Leader leader,
        ProcessId[] acceptors, BallotNumber ballot_number){
		this.env = env;
		this.me = me;
		this.acceptors = acceptors;
		this.leader = leader;
		this.ballot_number = ballot_number;
        this.my_name = "[["+me.toString()+"]]";
        setLogger();
        loadProp();
        env.addProc(me, this);
//        this.leader.leaseEndTime = System.currentTimeMillis() + leaseTime;
//        logger.log(Level.FINER, "LET:"+this.leader.leaseEndTime+"=="+leaseTime+"+"+System.currentTimeMillis());
	}

	public void body(){
//		P1aMessage m1 = new P1aMessage(me, ballot_number, leader.leaseEndTime);
        P1aMessage m1 = new P1aMessage(me, ballot_number, maxPostProposal, readOnlyCommand);
        Set<ProcessId> waitfor = new HashSet<ProcessId>();
		for (ProcessId a: acceptors) {
            if(!(!leader.stop_request(me) && !stop_request())) break;
            sendMessage(a, m1);
			waitfor.add(a);
		}

		Set<PValue> pvalues = new HashSet<PValue>();
//        HashMap<Integer, Set<Command>> readOnlyFlagsAll = new HashMap<Integer, Set<Command>>();
		while (2 * waitfor.size() >= acceptors.length && !leader.stop_request(me) && !stop_request()) {
			PaxosMessage msg = getNextMessage();

			if (msg instanceof P1bMessage) {
				P1bMessage m = (P1bMessage) msg;

				int cmp = ballot_number.compareTo(m.ballot_number);
				if (cmp != 0) {
					sendMessage(leader.me, new PreemptedMessage(me, m.ballot_number));
					return;
				}
				if (waitfor.contains(m.src)) {
					waitfor.remove(m.src);
					pvalues.addAll(m.accepted);
//                    for(Integer i: m.readOnlyFlags.keySet()) {
//                        Set<Command> commands = m.readOnlyFlags.get(i);
//                        if(commands != null && !commands.isEmpty()) {
//
//                        }
//                    }
				}
			}
			else {
                logger.log(Level.SEVERE, "paxos.Scout: unexpected msg");
			}
		}

        if(!leader.stop_request(me) && !stop_request())
		    sendMessage(leader.me, new AdoptedMessage(me, ballot_number, pvalues));
	}
}
