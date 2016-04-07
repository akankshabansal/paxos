package paxos;

public class Heartbeat extends Process {
    Leader forLeader;

    public Heartbeat(Env env, ProcessId me, Leader leader) {
        this.env = env;
        this.me = me;
        this.forLeader = leader;
        setLogger();
        loadProp();
        env.addProc(me, this);
        delay = Integer.parseInt(prop.getProperty("heartbeatDelay"));;
    }

    public void body() {
        while (!forLeader.stop_request() ) {
            logger.log(messageLevel, "Here I am: " + me);
            PaxosMessage msg = getNextMessage();
            stop_request();
            if (msg instanceof PingMessage) {
                PingMessage ping = (PingMessage) msg;
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {

                }
                sendMessage(ping.src, new PongMessage(me));
            }
        }
    }
}
