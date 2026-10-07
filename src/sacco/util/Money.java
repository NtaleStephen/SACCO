package sacco.util;

import sacco.exception.InvalidAmountException;

/**
 * Currency helpers.
 *
 * Topic 9 - this class exists to be reached by a STATIC IMPORT:
 *     import static sacco.util.Money.format;
 * so that callers can write format(x) instead of Money.format(x).
 *
 * Topic 5 - everything here is class data / class methods (static), because a
 * "Money" has no per-object state worth keeping. The private constructor stops
 * anyone creating a pointless instance.
 */
public final class Money {

    /** Class data (static) - one copy shared by the whole program. */
    public static final String CURRENCY = "UGX";

    /** Smallest amount the SACCO will process. */
    public static final double MINIMUM_TRANSACTION = 500.0;

    private Money() {
        // utility class - never instantiated
    }

    /**
     * Topic 6 - Format Strings.
     * %,.2f = group thousands with commas, two decimal places.
     */
    public static String format(double amount) {
        return String.format("%s %,.2f", CURRENCY, amount);
    }

    /** Right-aligned in a fixed width so report columns line up. */
    public static String formatAligned(double amount, int width) {
        return String.format("%" + width + "s", format(amount));
    }

    /**
     * Topic 6 - The Primitive-Type Wrapper Classes.
     * Double.parseDouble turns text into a primitive double and throws
     * NumberFormatException (a RuntimeException) when the text is not a number.
     */
    public static double parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new InvalidAmountException("No amount was entered");
        }
        try {
            // String methods - Topic 4 (Strings)
            double value = Double.parseDouble(text.trim().replace(",", ""));
            return round(value);
        } catch (NumberFormatException nfe) {
            // Re-throwing as our own type, keeping the cause - Topic 10
            throw new InvalidAmountException("'" + text + "' is not a valid amount");
        }
    }

    /** Rounds to whole cents so repeated interest does not drift. */
    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
