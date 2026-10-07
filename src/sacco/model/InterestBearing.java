package sacco.model;

/**
 * Topic 8 - Interfaces.
 *
 * An interface is a contract with no state and (here) no implementations. Any class
 * that implements it promises to supply these methods. Because Java allows only one
 * superclass but many interfaces, this is how SavingsAccount can be both "an Account"
 * and "something that earns interest" at the same time.
 */
public interface InterestBearing {

    /**
     * Fields in an interface are implicitly public static final - constants.
     * The regulator's minimum annual rate for member savings.
     */
    double REGULATORY_FLOOR = 0.005; // 0.5% per year

    /** The annual rate as a fraction, e.g. 0.07 for 7%. */
    double annualRate();

    /**
     * Works out one month's interest WITHOUT applying it.
     * A pure calculation, so it can be shown to the member before they commit.
     */
    double monthlyInterest();

    /**
     * Adds (or charges) one month's interest and returns the amount moved.
     * This one changes the object's state.
     */
    double applyMonthlyInterest();
}
