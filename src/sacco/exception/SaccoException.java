package sacco.exception;

/**
 * Root of the application's own exception hierarchy.
 *
 * Topic 10 - Defining and Throwing Exceptions.
 * This extends Exception (not RuntimeException), so it is a CHECKED exception:
 * the compiler forces every caller to either catch it or declare it with throws.
 */
public class SaccoException extends Exception {

    private static final long serialVersionUID = 1L;

    public SaccoException(String message) {
        super(message);
    }

    /** Wrapping constructor - keeps the original cause for printStackTrace(). */
    public SaccoException(String message, Throwable cause) {
        super(message, cause);
    }
}
