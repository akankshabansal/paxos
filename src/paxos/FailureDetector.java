package paxos;

import java.util.Properties;

public class FailureDetector extends Process {
    Leader forLeader;
    BallotNumber lastActiveBallot_number;

    public FailureDetector(Env env, ProcessId me, Leader leader, BallotNumber lastActiveBallot_number) {
        this.env = env;
        this.me = me;
        this.forLeader = leader;
        this.lastActiveBallot_number = lastActiveBallot_number;
        setLogger();
        loadProp();
        env.addProc(me, this);
    }

    Properties loadProp() {
        super.loadProp();
        return prop;
    }

    public void body() {
        logger.log(messageLevel, "Here I am: " + me);

        while (!forLeader.stop_request()) {
            Leader p = (Leader) env.procs.get(lastActiveBallot_number.leader_id);
            try {
                sendMessage(p.heartbeat.me, new PingMessage(me));
            } catch (NullPointerException e) {
                //e.printStackTrace();
                sendMessage(forLeader.me, new LeaderTimeoutMessage(me, lastActiveBallot_number));
                break;
            }
            PaxosMessage msg = getNextMessage(forLeader.failureDetectionTimeout);
            if (!(msg instanceof PongMessage) || msg == null ) {
                sendMessage(forLeader.me, new LeaderTimeoutMessage(me, lastActiveBallot_number));
                break;
            }
        }
    }
}
