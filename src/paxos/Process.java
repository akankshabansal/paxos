package paxos;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.*;

public abstract class Process extends Thread {

    Logger logger;
    ProcessId me;
    Queue<PaxosMessage> inbox = new Queue<PaxosMessage>();
    Env env;
    Properties prop = new Properties();
    int delay;

    public boolean assign_stop_request = false;
    public boolean scheduledToCountSend = true;
    public int messagesToCount = 0;
    public ProcessId countMessagesOf;

    public Level messageLevel = Level.FINER;
    String my_name = "";
    Map<String,Integer> sentCount = new HashMap<String, Integer>();
    Map<String,Integer> rcvdCount = new HashMap<String, Integer>();

//    int leaseTime;

    public boolean stop_request(ProcessId whoGotKilled){
        try {
            Thread.sleep(this.delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(assign_stop_request) {
            env.removeProc(whoGotKilled);
            logger.log(Level.SEVERE, whoGotKilled+" is getting killed. Bbye.");
        }
        return assign_stop_request;
    }

    public boolean stop_request() {
        return stop_request(me);
    }

    abstract void body();

    public void run() {
        body();
        env.removeProc(me);
    }

    Properties loadProp() {
        try {
            prop.load(new FileInputStream("config.properties"));
            if (prop.getProperty(me.name) != null) {
                delay = Integer.parseInt(prop.getProperty(me.name));
            } else {
                delay = Integer.parseInt(prop.getProperty("delay"));
            }
            messageLevel = "TRUE".equalsIgnoreCase(prop.getProperty("printMessages")) ? Level.CONFIG : Level.FINER;
            //Schedule KILL
            String killSchedule = prop.getProperty(this.me+"_KILL");
            if(killSchedule != null) {
                String[] pidOptionSplit = killSchedule.split(Env.CLIENT_MSG_SEPARATOR,3);
                ProcessId pidToCount = null;
                for(ProcessId pp : env.procs.keySet())
                    if(pp.toString().equals(pidOptionSplit[1]))
                        pidToCount = pp;
                scheduledToCountSend = ("SEND").equals(pidOptionSplit[0]);
                countMessagesOf = pidToCount;
                messagesToCount = Integer.parseInt(pidOptionSplit[2]);
            }
//            leaseTime = Integer.parseInt(prop.getProperty("leaseTime"));

        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
        return prop;
    }

    PaxosMessage getNextMessage() {
        return inbox.bdequeue();
    }

    PaxosMessage getNextMessage(int timeout) {
        return inbox.bdequeue(timeout);
    }

    void sendMessage(ProcessId dst, PaxosMessage msg) {
        incrementSendMessages(dst);
        this.logger.log(messageLevel, my_name + "SENT >>" + dst + ">> : " + msg);
        env.sendMessage(dst, msg);
    }

    private void incrementSendMessages(ProcessId dst) {
        if(sentCount.get(dst.toString()) == null)
            sentCount.put(dst.toString(),1);
        else
            sentCount.put(dst.toString(), sentCount.get(dst.toString())+1);
        if(messagesToCount > 0 && scheduledToCountSend == true){
            if((countMessagesOf == null && getTotalSentMessages() == messagesToCount) ||
                (countMessagesOf != null && sentCount.get(countMessagesOf.toString()) != null && sentCount.get(countMessagesOf.toString()) == messagesToCount)){
                    assign_stop_request = true;
                    this.logger.log(Level.SEVERE,me +" is going to get killed.");
            }
        }
    }

    void deliver(PaxosMessage msg) {
        incrementRcvdMessages(msg.src_name);
        inbox.enqueue(msg);
        this.logger.log(messageLevel,my_name+ "RCVD <<" + msg.src_name + "<< : " + msg);
    }

    public Integer getTotalSentMessages(){
        int total = 0;
        for(String s: sentCount.keySet())
            total += sentCount.get(s);
        return total;
    }

    public Integer getTotalRcvdMessages(){
        int total = 0;
        for(String s: rcvdCount.keySet())
            total += rcvdCount.get(s);
        return total;
    }
    private void incrementRcvdMessages(String src_name) {
        if(rcvdCount.get(src_name) == null)
            rcvdCount.put(src_name, 1);
        else
            rcvdCount.put(src_name, rcvdCount.get(src_name)+1);
        if(messagesToCount > 0 && scheduledToCountSend == false){
            if((countMessagesOf == null && getTotalRcvdMessages() == messagesToCount) ||
                (countMessagesOf != null && rcvdCount.get(countMessagesOf) == messagesToCount)){
                assign_stop_request = true;
                this.logger.log(Level.SEVERE,me +" is going to get killed.");
            }
        }
    }

    public void setLogger() {
        String loggerName = me.toString();
        if(this instanceof Scout)
            loggerName = ((Scout)this).leader.me.toString();
        else if(this instanceof Commander)
            loggerName = ((Commander)this).leader.me.toString();
        logger = Logger.getLogger(loggerName);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.FINER);
        Handler consoleHandler = null;
        for (Handler handler : logger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                consoleHandler = handler;
                break;
            }
        }
        if (consoleHandler == null) {
            //there was no console handler found, create a new one
            consoleHandler = new ConsoleHandler();
            logger.addHandler(consoleHandler);
        }
        consoleHandler.setLevel(Level.CONFIG);
        if(!(this instanceof Scout || this instanceof Commander)) {
            try {
                boolean clean = Boolean.parseBoolean(prop.getProperty("clean"));
                FileHandler fileHandler = new FileHandler("log/Log" + loggerName + ".log", !clean);
                fileHandler.setLevel(Level.FINER);
                logger.addHandler(fileHandler);
                SimpleFormatter formatter = new SimpleFormatter();
                fileHandler.setFormatter(formatter);

            } catch (SecurityException e) {
                logger.log(Level.SEVERE, e.getMessage());
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
    }

}
