package sacco.util;

import sacco.exception.InvalidAmountException;

/**
 * Input rules for the SACCO.
 * Topic 4 - if statements, loops, arrays, primitive datatypes, String handling.
 */
public final class Validator {

    private Validator() {
    }

    /**
     * Topic 4 - Arrays. Valid local mobile-money prefixes.
     * Declared and initialised in one step with an array initialiser.
     */
    private static final String[] VALID_PHONE_PREFIXES = { "070", "074", "075", "076", "077", "078", "039" };

    /** Topic 10 - throwing a RuntimeException for a caller-side mistake. */
    public static void requirePositiveAmount(double amount) {
        // Topic 4 - If Statements, and the || operator (Expressions in Java)
        if (amount <= 0 || Double.isNaN(amount)) {
            throw new InvalidAmountException("Amount must be greater than zero (got " + amount + ")");
        }
        if (amount < Money.MINIMUM_TRANSACTION) {
            throw new InvalidAmountException(
                    "Minimum transaction is " + Money.format(Money.MINIMUM_TRANSACTION));
        }
    }

    /**
     * A Ugandan national ID is 14 alphanumeric characters.
     * Topic 4 - a classic for loop over a String, and char primitives.
     */
    public static boolean isValidNationalId(String nationalId) {
        if (nationalId == null || nationalId.length() != 14) {
            return false;
        }
        for (int i = 0; i < nationalId.length(); i++) {
            char c = nationalId.charAt(i);
            // Topic 4 - Expressions: combining boolean tests
            boolean alphanumeric = Character.isLetterOrDigit(c);
            if (!alphanumeric) {
                return false;
            }
        }
        return true;
    }

    /**
     * Topic 4 - Enhanced for Loop over an array.
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.length() != 10) {
            return false;
        }
        String prefix = phone.substring(0, 3);
        for (String valid : VALID_PHONE_PREFIXES) {
            if (valid.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Topic 4 - Strings: trimming, case handling, isEmpty. */
    public static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
