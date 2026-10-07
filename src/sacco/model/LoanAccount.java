package sacco.model;

import sacco.exception.InsufficientFundsException;
import sacco.util.Validator;

/**
 * Money the SACCO has lent out. The balance here is what the member still OWES,
 * so it behaves almost the opposite way round to a savings account - which is
 * exactly why polymorphism is useful: callers do not have to care.
 *
 * Topic 7 - extends Account.
 * Topic 8 - implements InterestBearing, but charges interest instead of paying it.
 */
public class LoanAccount extends Account implements InterestBearing {

    private static final long serialVersionUID = 1L;

    public static final double STANDARD_RATE = 0.18; // 18% per year

    private final double principal;
    private final double annualRate;
    private final int termMonths;
    private int paymentsMade = 0;

    public LoanAccount(Member owner, double principal, int termMonths) {
        this(owner, principal, termMonths, STANDARD_RATE);
    }

    public LoanAccount(Member owner, double principal, int termMonths, double annualRate) {
        // The opening balance of a loan is the full amount owed
        super(owner, principal);
        Validator.requirePositiveAmount(principal);
        if (termMonths <= 0 || termMonths > 60) {
            throw new IllegalArgumentException("Loan term must be 1-60 months (got " + termMonths + ")");
        }
        if (annualRate < REGULATORY_FLOOR) {
            throw new IllegalArgumentException("Rate is below the regulatory floor");
        }
        this.principal = principal;
        this.termMonths = termMonths;
        this.annualRate = annualRate;
    }

    public double getPrincipal() {
        return principal;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public int getPaymentsMade() {
        return paymentsMade;
    }

    /** What the member still owes. */
    public double getOutstanding() {
        return balance;
    }

    public boolean isCleared() {
        return balance <= 0.0;
    }

    /** Equal monthly instalment, before interest compounding. */
    public double monthlyInstalment() {
        return round((principal + (principal * annualRate * termMonths / 12.0)) / termMonths);
    }

    /* ---------- Topic 8: abstract methods from Account ---------- */

    @Override
    public String accountType() {
        return "Loan";
    }

    /** Nothing can be withdrawn from a loan - it is a debt, not savings. */
    @Override
    public double withdrawableBalance() {
        return 0.0;
    }

    /* ---------- Topic 7: Method Overriding ---------- */

    /**
     * Topic 10 - Errors and RuntimeExceptions.
     * Withdrawing from a loan account makes no sense at all, so this is refused with
     * an unchecked UnsupportedOperationException rather than a checked exception.
     * Note it still honours the inherited `throws` clause - an override may throw
     * FEWER checked exceptions than the method it overrides, never more.
     */
    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        throw new UnsupportedOperationException(
                "You cannot withdraw from loan account " + getAccountNumber() + "; use repay()");
    }

    /**
     * Topic 7 - Method Overriding. On a loan, "depositing" means repaying, so the
     * inherited meaning is replaced entirely and super.deposit is NOT called.
     */
    @Override
    public void deposit(double amount) {
        repay(amount);
    }

    /** Pay down the debt. Overpayment is rejected rather than silently credited. */
    public void repay(double amount) {
        ensureOpen();
        Validator.requirePositiveAmount(amount);
        if (amount > balance) {
            throw new IllegalArgumentException(String.format(
                    "Overpayment: %,.2f offered but only %,.2f is outstanding", amount, balance));
        }
        balance = round(balance - amount);
        paymentsMade++;
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

    /**
     * A loan is CHARGED interest, so the outstanding balance goes UP.
     * Same method name as SavingsAccount, opposite effect - this is polymorphism
     * doing real work rather than a textbook demonstration.
     */
    @Override
    public double applyMonthlyInterest() {
        ensureOpen();
        if (isCleared()) {
            return 0.0;
        }
        double charge = monthlyInterest();
        balance = round(balance + charge);
        return charge;
    }

    @Override
    public String summary() {
        return super.summary() + String.format("  term=%dm paid=%d/%d rate=%.2f%%",
                termMonths, paymentsMade, termMonths, annualRate * 100);
    }
}
