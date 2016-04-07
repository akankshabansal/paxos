package paxos;

import java.util.HashSet;
import java.util.Set;

public class Command {
    ProcessId client;
    int req_id;
    Operation op;
    Set<Command> readOnlySets;

    public Command(Set<Command> readOnlySets) {
        this.readOnlySets = readOnlySets;
    }

    public void updateWith(Command c) {
        this.client = c.client;
        this.req_id = c.req_id;
        this.op = c.op;
    }

    public Command(ProcessId client, int req_id, Operation op) {
        this.client = client;
        this.req_id = req_id;
        this.op = op;
    }

    public Command(Command c) {
        this.client = c.client;
        this.req_id = c.req_id;
        this.op = c.op;
        if (c.readOnlySets != null) {
            this.readOnlySets = new HashSet<Command>(c.readOnlySets);
        }
    }

    //Changed the equals function to take into consideration the read only set...
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Command)) return false;

        Command command = (Command) o;
        if (req_id != command.req_id) return false;

        if (client!= null ? !client.equals(command.client): command.client!= null) return false;
        if (op != null ? !op.equals(command.op) : command.op != null) return false;
        if (readOnlySets != null ? !readOnlySets.equals(command.readOnlySets) : command.readOnlySets != null)
            return false;
        return true;
    }

    public boolean cmdEquals(Command command) {
        if (req_id != command.req_id) return false;
        if (!client.equals(command.client)) return false;
        if (op != null ? !op.equals(command.op) : command.op != null) return false;
//        if (readOnlySets != null ? !readOnlySets.equals(command.readOnlySets) : command.readOnlySets != null)
//            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = client != null ? client.hashCode() : 0;
        result = 31 * result + req_id;
        result = 31 * result + (op != null ? op.hashCode() : 0);
        result = 31 * result + (readOnlySets != null ? readOnlySets.hashCode() : 0);
        return result;
    }

    //    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Command)) return false;
//
//        Command command = (Command) o;
//        if (req_id != command.req_id) return false;
//        if (!client.equals(command.client)) return false;
//        //if(op != null) {
//        if (!op.equals(command.op)) return false;
//        //}
////        else {
////            for()
////        }
//
//        return true;
//    }

    public String toString() {
        return "paxos.Command(" + client + "," + req_id + "," + op + ", RO:" + readOnlySets + ")";
    }
}

enum Commands {
    KILL("KILL <pid> // Kills the pid thread " + "\n KILL pid : SEND/RECV from : pid count"),
    SHOW("Shows all live pids"),
    HELP("Shows this stuff again"),
    TX("TX 1:Deposit$0$21 replica0:3000 ** TX 1:Inquiry$2//Make a Transaction"),
    DELAY("DELAY <pid> <time>"),
    EXC("EXC <replica_ID> leader0:leader1"),
    L2L("L2L <pid> <time> //Leader ping timeout"),
    FD("FD T/F");

    private String description;

    Commands(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
}
