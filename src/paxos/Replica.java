package paxos;

import java.lang.*;
import java.util.*;
import java.util.logging.Level;

public class Replica extends Process {
    ProcessId[] leaders;
    int slot_num = 1;
    List<Account> accountList;
    Map<Integer /* slot number */, Command> proposals = new HashMap<Integer, Command>();
    Map<Integer /* slot number */, Command> decisions = new HashMap<Integer, Command>();
    Map<Integer /* slot number */, Set<Command>> readOnlyFlags = new HashMap<Integer, Set<Command>>();
    Set<String> excludedLeaders = new HashSet<String>();

    public Replica(Env env, ProcessId me, ProcessId[] leaders) {
        this.env = env;
        this.me = me;
        this.leaders = leaders;
        setLogger();
        loadProp();
        env.addProc(me, this);
    }

    Properties loadProp() {
        super.loadProp();
        String accountListStr = prop.getProperty("AccountsList");
        accountList = new ArrayList<Account>();
        String[] strArr = accountListStr.split(",");
        for (int i = 0; i < strArr.length; i++) {
            accountList.add(new Account(strArr[i], i));
        }
        return prop;
    }

    void propose(Command c) {
        /* COMMENT THIS IF BLOCK TO DISABLE SPECIAL TREATMENT OF READ ONLY*/
        if(c.op.opType == Operation.OperationType.Inquiry) {
            for (ProcessId ldr : leaders) {
                if(excludedLeaders.contains(ldr.name)) continue;
                if(!stop_request())
                    sendMessage(ldr, new ReadOnlyMessage(me, c));
            }
            return;
        }
        if (!decisions.containsValue(c)) {
            for (int s = 1; ; s++) {
                if (!proposals.containsKey(s) && (!decisions.containsKey(s) || decisions.get(s).op == null)) {
                    proposals.put(s, c);
//                    for(int i = leaders.length -1; i >= 0; i-- ) {
//                      ProcessId ldr = leaders[i];
                    for (ProcessId ldr : leaders) {
                        if(excludedLeaders.contains(ldr.name)) continue;
                        if(!stop_request())
                            sendMessage(ldr, new ProposeMessage(me, s, c));
                    }
                    break;
                }
            }
        }
    }


    boolean perform(Command c) {
        sendReadOnlyBefore(c);
        if(c.op == null) return false; // FOR COMMANDS HAVING ONLY READ ONLY BUT NO ACTUAL CMD
        for (int s = 1; s < slot_num; s++) {
            if (c.cmdEquals(decisions.get(s))) {
                slot_num++;
//                logger.log(messageLevel,"increasing slot no for same cmd "+slot_num);
                return true;
            }
        }
//        logger.log(messageLevel,"calling read only "+slot_num);
        String[] operationArgs = c.op.operationArgs.split(Env.TX_MSG_SEPARATOR);
        try {
            Account account = accountList.get(Integer.parseInt(operationArgs[0]));
            int amt = operationArgs.length > 1 ? Integer.parseInt(operationArgs[1]) : 0;
            String output;
            switch (c.op.opType) {
                case Deposit:
                    account.credit(amt);
                    output = account.toString();
                    break;
                case Inquiry:
                    account.getBalance();
                    output = account.toString();
                    break;
                case Transfer:
                    Account toAccount = accountList.get(Integer.parseInt(operationArgs[2]));
                    account.transfer(toAccount, amt);
                    output = account.toString() + " : "+ toAccount.toString();
                    break;
                case Withdraw:
                    account.debit(amt);
                    output = account.toString();
                    break;
                default:
                    output = "INVALID OPERATION TYPE";
                    break;
            }
            logger.log(messageLevel, "PERFORM " + c + " for slot :" + (slot_num) + " OUTPUT :" + output);
            sendMessage(c.client,new ResponseMessage(me,c,account));
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Error in the input msg ");
            sendMessage(c.client,new ResponseMessage(me,c,null));
        }
//        sendReadOnlyAfter();
        slot_num++;
        return true;
    }

//    public void sendReadOnlyAfter() {
//        sendReadOnly(slot_num);
//    }

    public void sendReadOnlyBefore(Command cmd) {
        if(cmd.readOnlySets != null) {
            Set<Command> readOnlySets = cmd.readOnlySets;
            for(Iterator<Command> i = readOnlySets.iterator(); i.hasNext();) {
                Command c = i.next();
                String[] operationArgs = c.op.operationArgs.split(Env.TX_MSG_SEPARATOR);
                Account account = accountList.get(Integer.parseInt(operationArgs[0]));
                if(c.op.opType == Operation.OperationType.Inquiry){
                    logger.log(messageLevel, "PERFORM R/O " + c + " after slot :" + (slot_num) + " OUTPUT :" + account);
                    sendMessage(c.client, new ResponseMessage(me, c, account));
                } else logger.log(Level.SEVERE, "Not a Read only command - " + c);
            }
        }
    }

//    public void sendReadOnly(int slot_number) {
//        Set<Command> readOnlySet = readOnlyFlags.get(slot_number);
//        if(readOnlySet != null && !readOnlySet.isEmpty()) {
//            for(Iterator<Command> i = readOnlySet.iterator(); i.hasNext();) {
//                Command c = i.next();
//                String[] operationArgs = c.op.operationArgs.split(Env.TX_MSG_SEPARATOR);
//                Account account = accountList.get(Integer.parseInt(operationArgs[0]));
//                if(c.op.opType == Operation.OperationType.Inquiry){
//                    logger.log(messageLevel, "PERFORM R/O " + c + " after slot :" + (slot_number) + " OUTPUT :" + account);
//                    sendMessage(c.client,new ResponseMessage(me,c,account));
//                    readOnlySet.remove(c);
//                } else logger.log(Level.SEVERE, "Not a Read only command - " + c);
//            }
//        }
//    }

    public void body() {
        logger.log(messageLevel, "Here I am: " + me);
        while (!stop_request()) {
            PaxosMessage msg = getNextMessage();

            if (msg instanceof RequestMessage) {
                RequestMessage m = (RequestMessage) msg;
                propose(m.command);
            } else if (msg instanceof DecisionMessage) {
                DecisionMessage m = (DecisionMessage) msg;
                decisions.put(m.slot_number, m.command);

//                if (m.readOnlyMessages != null) {
//                    Set<Command> readOnlyCommands = new HashSet<Command>();
//                    for(ReadOnlyMessage r: m.readOnlyMessages) readOnlyCommands.add(r.command);
//                    readOnlyFlags.put(m.slot_number, readOnlyCommands);
//                }

                while (!stop_request()) {
                    Command c = decisions.get(slot_num);
                    if (c == null) {
                        break;
                    }
                    Command c2 = proposals.get(slot_num);
                    if (c.op!= null && c2 != null && !c2.equals(c)) {
                        propose(c2);
                    }
//                    logger.log(messageLevel, "CALLING PERFORM " + c + " for slot :" + (slot_num) +" de"+decisions);
                    if(!perform(c)) break;
                }
            } else if (msg instanceof ReadOnlyDecisionMessage) {
                ReadOnlyDecisionMessage m = (ReadOnlyDecisionMessage)msg;
                Set<Command> currentReadOnlyBag = readOnlyFlags.get(m.slot_number);
                if(currentReadOnlyBag == null) currentReadOnlyBag = new HashSet<Command>();
                currentReadOnlyBag.add(m.command);
                readOnlyFlags.put(m.slot_number, currentReadOnlyBag);
//                sendReadOnlyBefore();
            } else {
                logger.log(Level.SEVERE, "paxos.Replica: unknown msg type");
            }
        }
    }
}
