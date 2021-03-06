package paxos;

import java.util.*;

public class Commander extends Process {
    Leader leader;
    ProcessId[] acceptors, replicas;
    BallotNumber ballot_number;
    int slot_number;
    Command command;

    public Commander(Env env, ProcessId me, Leader leader, ProcessId[] acceptors,
                     ProcessId[] replicas, BallotNumber ballot_number, int slot_number, Command command) {
        this.env = env;
        this.me = me;
        this.acceptors = acceptors;
        this.replicas = replicas;
        this.leader = leader;
        this.ballot_number = ballot_number;
        this.slot_number = slot_number;
        this.command = command;
        this.my_name = "[["+me.toString()+"]]";
        loadProp();
        setLogger();
        env.addProc(me, this);
    }

    public void body() {
        P2aMessage m2 = new P2aMessage(me, ballot_number, slot_number, command);
        Set<ProcessId> waitfor = new HashSet<ProcessId>();
        for (ProcessId a : acceptors) {
            if(!(!leader.stop_request(me) && !stop_request())) break;
            sendMessage(a, m2);
            waitfor.add(a);
        }

        while (2 * waitfor.size() >= acceptors.length && !leader.stop_request(me) && !stop_request()) {
            PaxosMessage msg = getNextMessage();

            if (msg instanceof P2bMessage) {
                P2bMessage m = (P2bMessage) msg;

                if (ballot_number.equals(m.ballot_number)) {
                    if (waitfor.contains(m.src)) {
                        waitfor.remove(m.src);
                    }
                } else {
                    sendMessage(leader.me, new PreemptedMessage(me, m.ballot_number));
                    return;
                }
            }
        }
        if(!(!leader.stop_request(me) && !stop_request())) return;
        leader.decisionsTaken.add(slot_number);

        for (ProcessId r : replicas) {
            if(!(!leader.stop_request(me) && !stop_request())) break;
            sendMessage(r, new DecisionMessage(me, slot_number, command));
        }
    }
}
