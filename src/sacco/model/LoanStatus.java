package sacco.model;

/**
 * Topic 6 - Enumerated Types. The life cycle of a loan application.
 */
public enum LoanStatus {

    PENDING("Awaiting review"),
    APPROVED("Approved, not yet paid out"),
    REJECTED("Declined by the committee"),
    DISBURSED("Money paid to the member");

    private final String description;

    private LoanStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /** Only a pending application can still be decided on. */
    public boolean isOpen() {
        return this == PENDING;
    }
}
