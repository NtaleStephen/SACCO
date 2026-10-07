package sacco.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * One immutable line in the SACCO ledger.
 *
 * Topic 5 - Encapsulation taken to its conclusion: every field is private and final,
 * there are no setters, so a posted transaction can never be altered. That is what
 * you want in an accounting record.
 *
 * Topic 8 - Comparable: transactions sort newest-last by default.
 */
public class Transaction extends Entity implements Comparable<Transaction> {

    private static final long serialVersionUID = 1L;

    private static int txnCounter = 0;

    private final String accountNumber;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter;
    private final long timestamp;
    private final String narration;

    public Transaction(String accountNumber, TransactionType type, double amount,
                       double balanceAfter, String narration) {
        super(nextTxnId());
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.narration = narration == null ? "" : narration;
        this.timestamp = System.currentTimeMillis();
    }

    /** Used when rebuilding a transaction read back from the binary ledger file. */
    public Transaction(String id, String accountNumber, TransactionType type, double amount,
                       double balanceAfter, long timestamp, String narration) {
        super(id);
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
        this.narration = narration == null ? "" : narration;
    }

    private static synchronized String nextTxnId() {
        txnCounter++;
        return String.format("TXN-%06d", txnCounter);
    }

    public static void restoreCounter(int value) {
        if (value > txnCounter) {
            txnCounter = value;
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getNarration() {
        return narration;
    }

    /** Topic 6 - Format Strings, here for dates rather than numbers. */
    public String getFormattedDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }

    /** The signed effect on the balance, from the enum's own sign. */
    public double signedAmount() {
        return type.getSign() * amount;
    }

    @Override
    public String summary() {
        return String.format("%-11s %-20s %-4s %,14.2f %,16.2f  %s",
                getId(), getFormattedDate(), type.getCode(), amount, balanceAfter, narration);
    }

    @Override
    public String toString() {
        return getId() + " " + type.getCode() + " " + amount;
    }

    /**
     * Topic 8 - Comparable: oldest first. Long.compare avoids the overflow bug you
     * get from writing `(int)(this.timestamp - other.timestamp)`.
     */
    @Override
    public int compareTo(Transaction other) {
        if (this.timestamp != other.timestamp) {
            return this.timestamp < other.timestamp ? -1 : 1;
        }
        return this.getId().compareTo(other.getId());
    }
}
