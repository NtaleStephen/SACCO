package sacco.exception;

/**
 * Thrown when an amount is negative, zero or not a number.
 *
 * Topic 10 - Errors and RuntimeExceptions.
 * This one extends RuntimeException deliberately: passing a negative amount is a
 * PROGRAMMING bug, not a business outcome the caller should be forced to handle.
 * Contrast it with InsufficientFundsException, which is checked because running out
 * of money is a normal thing that happens to a real member.
 */
public class InvalidAmountException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidAmountException(String message) {
        super(message);
    }
}
