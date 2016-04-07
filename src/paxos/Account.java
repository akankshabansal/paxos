package paxos;

/**
 * Created with IntelliJ IDEA.
 * User: bansal
 * Date: 05/11/13
 * Time: 5:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class Account {
    int accountNo;
    int balance;

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public int debit(int by) {
        return (this.balance = balance - by);
    }

    public int credit(int by) {
        return (this.balance = balance + by);
    }

    public boolean transfer(Account to, int amt) {
        to.credit(amt);
        debit(amt);
        return true;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountNo=" + accountNo +
                ", balance=" + balance +
                '}';
    }

    public Account(String str, int accountNo) {
        this.accountNo = accountNo;
        this.balance = Integer.parseInt(str.split(Env.TX_MSG_SEPARATOR)[1]);
    }

}
