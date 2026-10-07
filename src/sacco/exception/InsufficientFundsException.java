package sacco.exception;

/**
 * Thrown when a withdrawal would push an account below what it is allowed to hold.
 * Topic 10 - Defining and Throwing Exceptions.
 */
public class InsufficientFundsException extends SaccoException {

    private static final long serialVersionUID = 1L;

    private final double requested;
    private final double available;

    public InsufficientFundsException(String accountNumber, double requested, double available) {
        // String.format - Topic 6 (Format Strings)
        super(String.format("Account %s: requested %,.2f but only %,.2f is withdrawable",
                accountNumber, requested, available));
        this.requested = requested;
        this.available = available;
    }

    public double getRequested() {
        return requested;
    }

    public double getAvailable() {
        return available;
    }

    /** How much the member is short by. */
    public double getShortfall() {
        return requested - available;
    }
}
