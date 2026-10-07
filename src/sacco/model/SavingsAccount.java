package sacco.model;

import sacco.exception.InsufficientFundsException;

/**
 * An ordinary member savings account.
 *
 * Topic 7 - Inheritance in Java: extends Account.
 * Topic 8 - Using Interfaces: also implements InterestBearing, which a single
 *           superclass could never have given it.
 */
public class SavingsAccount extends Account implements InterestBearing {

    private static final long serialVersionUID = 1L;

    /** The SACCO's standard savings rate: 7% per year. */
    public static final double STANDARD_RATE = 0.07;

    /** Members must leave this much in the account to stay registered. */
    private final double minimumBalance;

    private final double annualRate;

    public SavingsAccount(Member owner, double openingBalance) {
        this(owner, openingBalance, 20_000.0, STANDARD_RATE);
    }

    /**
     * Topic 7 - super(...) passes the shared fields up to Account's constructor
     * before this constructor sets its own fields.
     */
    public SavingsAccount(Member owner, double openingBalance, double minimumBalance, double annualRate) {
        super(owner, openingBalance);
        if (minimumBalance < 0) {
            throw new IllegalArgumentException("Minimum balance cannot be negative");
        }
        // Topic 8 - REGULATORY_FLOOR is a constant inherited from the interface
        if (annualRate < REGULATORY_FLOOR) {
            throw new IllegalArgumentException(
                    "Rate is below the regulatory floor of " + REGULATORY_FLOOR);
        }
        this.minimumBalance = minimumBalance;
        this.annualRate = annualRate;
    }

    public double getMinimumBalance() {
        return minimumBalance;
    }

    /* ---------- Topic 8: the abstract methods from Account ---------- */

    @Override
    public String accountType() {
        return "Savings";
    }

    /** Everything above the minimum balance may be taken out. */
    @Override
    public double withdrawableBalance() {
        double available = balance - minimumBalance;
        return available > 0 ? round(available) : 0.0;
    }

    /* ---------- Topic 7: Method Overriding ---------- */

    /**
     * Topic 7 - Method Overriding and super.
     * We add our own rule (never breach the minimum balance), then hand off to the
     * inherited implementation with super.withdraw rather than duplicating it.
     * Removing the super call would silently stop the balance from ever changing.
     */
    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        ensureOpen();
        if (balance - amount < minimumBalance) {
            throw new InsufficientFundsException(getAccountNumber(), amount, withdrawableBalance());
        }
        super.withdraw(amount);
    }

    /* ---------- Topic 8: the InterestBearing contract ---------- */

    @Override
    public double annualRate() {
        return annualRate;
    }

    @Override
    public double monthlyInterest() {
        return round(balance * annualRate / 12.0);
    }

    /** Savings EARN interest, so it is added to the balance. */
    @Override
    public double applyMonthlyInterest() {
        ensureOpen();
        double interest = monthlyInterest();
        balance = round(balance + interest);
        return interest;
    }

    @Override
    public String summary() {
        // Topic 7 - calling the superclass version and extending its output
        return super.summary() + String.format("  min=%,.0f rate=%.2f%%",
                minimumBalance, annualRate * 100);
    }
}
