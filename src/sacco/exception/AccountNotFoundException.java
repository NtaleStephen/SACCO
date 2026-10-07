package sacco.exception;

/**
 * Thrown when an account number does not exist in the repository.
 * Topic 10 - Defining and Throwing Exceptions.
 */
public class AccountNotFoundException extends SaccoException {

    private static final long serialVersionUID = 1L;

    private final String accountNumber;

    public AccountNotFoundException(String accountNumber) {
        super("No account exists with number '" + accountNumber + "'");
        this.accountNumber = accountNumber;
    }

    /** Custom exceptions may carry data, not just a message. */
    public String getAccountNumber() {
        return accountNumber;
    }
}
