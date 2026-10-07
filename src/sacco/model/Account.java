package sacco.model;

import sacco.exception.InsufficientFundsException;
import sacco.util.Validator;

/**
 * Topic 8 - Abstract Classes.
 *
 * Account holds everything every account has (a number, an owner, a balance) and the
 * logic that is identical everywhere (deposit). What differs between a savings account
 * and a loan account is left abstract, so each subclass must supply it.
 *
 * Topic 7 - this is the superclass that SavingsAccount and LoanAccount extend, and the
 * type that polymorphic code is written against.
 */
public abstract class Account extends Entity {

    private static final long serialVersionUID = 1L;

    /** Topic 5 - Class Data: shared counter for generating account numbers. */
    private static int accountCounter = 0;

    /* Topic 5 - Instance Data. */
    private final Member owner;

    /**
     * Topic 5 - Access Modifiers. protected means subclasses may read it directly,
     * but the mutation path still goes through the guarded methods below.
     */
    protected double balance;

    private boolean closed = false;

    /**
     * Topic 7 - super(...). The subclass constructor calls this, which calls
     * Entity's constructor. Construction always runs top-down: Entity, then Account,
     * then SavingsAccount.
     */
    protected Account(Member owner, double openingBalance) {
        super(nextAccountNumber());
        if (owner == null) {
            throw new IllegalArgumentException("An account must have an owner");
        }
        if (openingBalance < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative");
        }
        this.owner = owner;
        this.balance = openingBalance;

        // Topic 9 - Package Scope: attachAccount is package-private in Member and we
        // are in the same package, so this compiles here and nowhere else.
        owner.attachAccount(getId());
    }

    private static synchronized String nextAccountNumber() {
        accountCounter++;
        return String.format("ACC-%05d", accountCounter);
    }

    public static void restoreCounter(int value) {
        if (value > accountCounter) {
            accountCounter = value;
        }
    }

    public static int getAccountCount() {
        return accountCounter;
    }

    /* ---------- Topic 8: what every subclass must decide for itself ---------- */

    /** Human-readable kind of account, e.g. "Savings". */
    public abstract String accountType();

    /**
     * How much of the balance may actually be taken out right now. A savings account
     * must keep a minimum balance; a loan account allows nothing.
     */
    public abstract double withdrawableBalance();

    /* ---------- Shared behaviour ---------- */

    public String getAccountNumber() {
        return getId();
    }

    public Member getOwner() {
        return owner;
    }

    public double getBalance() {
        return balance;
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * Topic 6 - Parameter Passing.
     * `amount` is a primitive double, so this method receives a COPY. Changing
     * `amount` inside here would not affect the caller's variable. By contrast the
     * `owner` field is a reference, so a method that mutates the Member object it
     * points at WOULD be visible to the caller. That is the whole of Java's
     * pass-by-value rule: the value copied is either the number or the reference.
     */
    public void deposit(double amount) {
        ensureOpen();
        Validator.requirePositiveAmount(amount); // may throw InvalidAmountException
        balance = round(balance + amount);
    }

    /**
     * Topic 10 - Declaring Exceptions.
     * `throws InsufficientFundsException` is part of the method's signature: every
     * caller now has to deal with the possibility, decided at compile time.
     */
    public void withdraw(double amount) throws InsufficientFundsException {
        ensureOpen();
        Validator.requirePositiveAmount(amount);

        double available = withdrawableBalance(); // Topic 7 - polymorphic call
        if (amount > available) {
            throw new InsufficientFundsException(getAccountNumber(), amount, available);
        }
        balance = round(balance - amount);
    }

    /** Topic 10 - throwing an unchecked exception for an illegal state. */
    protected void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Account " + getAccountNumber() + " is closed");
        }
    }

    /**
     * Topic 6 - Destroying Objects. Java has no destructor and no delete; the garbage
     * collector reclaims an object once nothing references it. So "closing" is a
     * business flag, and the real cleanup happens when the service drops its reference.
     */
    public void close() {
        if (balance != 0) {
            throw new IllegalStateException(String.format(
                    "Cannot close %s while it holds %,.2f", getAccountNumber(), balance));
        }
        this.closed = true;
        owner.detachAccount(getId()); // package scope again
    }

    protected static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Topic 8 - the abstract method inherited from Entity. */
    @Override
    public String summary() {
        return String.format("%-10s %-10s %-22s %,15.2f %s",
                getAccountNumber(), accountType(), owner.getFullName(), balance,
                closed ? "[CLOSED]" : "");
    }

    @Override
    public String toString() {
        return accountType() + " " + getAccountNumber() + " (" + owner.getFullName() + ")";
    }
}
