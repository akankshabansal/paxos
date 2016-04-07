package paxos;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Acceptor extends Process {
    BallotNumber ballot_number = null;
	Set<PValue> accepted = new HashSet<PValue>();
//    Map<Integer, Set<Command>> readOnlyFlags = new HashMap<Integer, Set<Command>>();
//    long leaseEndTime;
//    ProcessId leaser;

	public Acceptor(Env env, ProcessId me){
		this.env = env;
		this.me = me;
        setLogger();
        loadProp();
        env.addProc(me, this);
	}

	public void body(){
        logger.log(messageLevel, "Here I am: " + me);
		while (!stop_request()) {
			PaxosMessage msg = getNextMessage();

			if (msg instanceof P1aMessage) {
				P1aMessage m = (P1aMessage) msg;
//                long currentTime = System.currentTimeMillis();
//                logger.log(messageLevel, "BN:"+ballot_number+"leaser:"+leaser+"B:"+m+"leaseEnd:"+leaseEndTime+"T="+currentTime);
//                if(ballot_number != null && leaseEndTime > currentTime &&
//                        !m.ballot_number.leader_id.equals(leaser)) {
//                    logger.log(messageLevel, "Already in lease with "+leaser+" for "+leaseEndTime+" Current: "
//                    + currentTime + "Ignoring : " + m.ballot_number);
//                } else
                if (ballot_number == null ||
						ballot_number.compareTo(m.ballot_number) <= 0) {
					ballot_number = m.ballot_number;
                    if(m.readOnlyCommand != null) {
                        boolean found = false;
                        for(Iterator<PValue> i = accepted.iterator(); i.hasNext();) {
                            PValue p = i.next();
                            if(p.ballot_number.equals(m.ballot_number) && p.slot_number == m.maxPostProposal) {
                                found = true;
                                if(p.command.readOnlySets == null) p.command.readOnlySets = new HashSet<Command>();
                                for(Command readOnlyFromLeader : m.readOnlyCommand.readOnlySets)
                                    p.command.readOnlySets.add(readOnlyFromLeader);
                            }
                        }
//                        logger.log(Level.FINER, found + "--"+m.maxPostProposal+"--"+accepted);
                        if(found == false && m.maxPostProposal != -1) {
                            Set<Command> r = new HashSet<Command>();
                            for(Command readOnlyFromLeader : m.readOnlyCommand.readOnlySets)
                                r.add(readOnlyFromLeader);
                            accepted.add(new PValue(m.ballot_number,m.maxPostProposal, new Command(r)));
                        }
//                        logger.log(Level.FINER, found + "++"+m.maxPostProposal+"++"+accepted);
                    }
//                    Set<Command> currentFlags = readOnlyFlags.get(m.maxPostProposal);
//                    if(currentFlags == null) currentFlags = new HashSet<Command>();
//                    currentFlags.add(m.readOnlyCommand);
//                    readOnlyFlags.put(m.maxPostProposal, currentFlags);
//                    leaseEndTime = m.leaseEndTime;
//                    leaser = m.ballot_number.leader_id;
				}
				sendMessage(m.src, new P1bMessage(me, ballot_number, new HashSet<PValue>(accepted)));
			}
			else if (msg instanceof P2aMessage) {
				P2aMessage m = (P2aMessage) msg;
//                long currentTime = System.currentTimeMillis();
//                if(ballot_number != null && leaseEndTime > currentTime &&
//                        m.ballot_number.leader_id != leaser) {
//                    logger.log(messageLevel, "Already in lease with "+leaser+" for "+leaseEndTime+" Current: "
//                            + currentTime + "Ignoring : " + m.ballot_number);
//                } else
                if (ballot_number == null ||
						ballot_number.compareTo(m.ballot_number) <= 0) {
					ballot_number = m.ballot_number;
                    boolean found = false;
                    for(Iterator<PValue> i = accepted.iterator(); i.hasNext();){
                        PValue p = i.next();
                        if(p.ballot_number.equals(m.ballot_number) && p.slot_number == m.slot_number) {
                            found = true;
                            p.command.updateWith(m.command);
                        }
                    }
                    if(found == false) {
                        accepted.add(new PValue(m.ballot_number,m.slot_number,m.command));
                    }
				}
				sendMessage(m.src, new P2bMessage(me, ballot_number, m.slot_number));
			}
		}
	}
}
