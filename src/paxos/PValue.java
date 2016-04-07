package paxos;

public class PValue {
	BallotNumber ballot_number;
	int slot_number;
	Command command;

	public PValue(BallotNumber ballot_number, int slot_number, Command command){
		this.ballot_number = ballot_number;
		this.slot_number = slot_number;
		this.command = command;
	}

	public String toString(){
		return "PV(" + ballot_number + ", " + slot_number + ", " + command + ")";
	}

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof PValue)) return false;

        PValue pval = (PValue) o;
        if(this.ballot_number.equals(pval.ballot_number) && this.slot_number == pval.slot_number)
            return true;
        else
            return false;
    }

    @Override
    public int hashCode() {
        return (31*(this.ballot_number.hashCode()) + 31*slot_number);
    }
}
