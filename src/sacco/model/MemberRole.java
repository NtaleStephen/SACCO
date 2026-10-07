package sacco.model;

/**
 * Topic 6 - Enumerated Types.
 *
 * A simple enum: a fixed, compile-time-checked set of values. Using an enum instead
 * of String constants means a typo like "TRESURER" is a compile error, not a bug
 * discovered in production.
 */
public enum MemberRole {

    /** Ordinary saving member. */
    MEMBER("Member", 1_000_000.0),

    /** Keeps the books; can approve small loans. */
    TREASURER("Treasurer", 3_000_000.0),

    /** Records minutes. */
    SECRETARY("Secretary", 2_000_000.0),

    /** Chairs the committee; highest loan ceiling. */
    CHAIRPERSON("Chairperson", 5_000_000.0);

    /*
     * An enum is a class: it can have fields, a constructor and methods.
     * The constructor is implicitly private and runs once per constant.
     */
    private final String label;
    private final double loanCeiling;

    private MemberRole(String label, double loanCeiling) {
        this.label = label;
        this.loanCeiling = loanCeiling;
    }

    public String getLabel() {
        return label;
    }

    public double getLoanCeiling() {
        return loanCeiling;
    }

    /** Topic 7 - overriding toString on an enum. */
    @Override
    public String toString() {
        return label;
    }

    /**
     * Topic 4 - Switch Statements, here switching on an enum.
     * Inside a switch on an enum you write the bare constant name, not MemberRole.X.
     */
    public boolean canApproveLoans() {
        switch (this) {
            case CHAIRPERSON:
            case TREASURER:
                return true;
            case SECRETARY:
            case MEMBER:
            default:
                return false;
        }
    }
}
