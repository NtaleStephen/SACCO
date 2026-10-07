package sacco.exception;

/**
 * Thrown when a national ID is already registered.
 * Topic 10 - Defining and Throwing Exceptions.
 */
public class DuplicateMemberException extends SaccoException {

    private static final long serialVersionUID = 1L;

    public DuplicateMemberException(String nationalId) {
        super("A member with national ID '" + nationalId + "' is already registered");
    }
}
