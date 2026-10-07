package sacco.model;

import sacco.util.Validator;

/**
 * A request for a loan, waiting for the committee.
 *
 * Topic 12 - The Queue Interface. These objects are held in a PriorityQueue, and the
 * compareTo below is what decides who gets reviewed first: the longest-standing
 * members with the smallest requests, since small loans to loyal members are the
 * lowest-risk thing a village SACCO can do.
 */
public class LoanApplication extends Entity implements Comparable<LoanApplication> {

    private static final long serialVersionUID = 1L;

    private static int applicationCounter = 0;

    private final String memberId;
    private final String memberName;
    private final double amount;
    private final int termMonths;
    private final String purpose;
    private final long submittedAt;
    private final int senioritySeconds;

    private LoanStatus status = LoanStatus.PENDING;
    private String decisionNote = "";

    public LoanApplication(Member applicant, double amount, int termMonths, String purpose) {
        super(nextApplicationId());
        if (applicant == null) {
            throw new IllegalArgumentException("An application needs an applicant");
        }
        Validator.requirePositiveAmount(amount);
        if (termMonths <= 0 || termMonths > 60) {
            throw new IllegalArgumentException("Term must be 1-60 months");
        }
        // Topic 6 - using the enum's per-constant data to enforce a business rule
        if (amount > applicant.loanCeiling()) {
            throw new IllegalArgumentException(String.format(
                    "A %s may borrow at most %,.2f; %,.2f was requested",
                    applicant.getRole().getLabel(), applicant.loanCeiling(), amount));
        }
        this.memberId = applicant.getId();
        this.memberName = applicant.getFullName();
        this.amount = amount;
        this.termMonths = termMonths;
        this.purpose = purpose == null ? "unspecified" : purpose;
        this.submittedAt = System.currentTimeMillis();
        // How long the applicant has been a member, in seconds
        this.senioritySeconds = (int) ((submittedAt - applicant.getJoinedAt()) / 1000L);
    }

    private static synchronized String nextApplicationId() {
        applicationCounter++;
        return String.format("APP-%04d", applicationCounter);
    }

    public static void restoreCounter(int value) {
        if (value > applicationCounter) {
            applicationCounter = value;
        }
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public double getAmount() {
        return amount;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public String getPurpose() {
        return purpose;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    /** Topic 5 - Encapsulation: status only ever moves through these two methods. */
    public void approve(String note) {
        requirePending();
        this.status = LoanStatus.APPROVED;
        this.decisionNote = note == null ? "Approved" : note;
    }

    public void reject(String reason) {
        requirePending();
        this.status = LoanStatus.REJECTED;
        this.decisionNote = reason == null ? "Rejected" : reason;
    }

    public void markDisbursed(String accountNumber) {
        if (status != LoanStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only an approved application can be disbursed (status is " + status + ")");
        }
        this.status = LoanStatus.DISBURSED;
        this.decisionNote = "Disbursed to " + accountNumber;
    }

    private void requirePending() {
        if (!status.isOpen()) {
            throw new IllegalStateException(
                    "Application " + getId() + " has already been decided: " + status);
        }
    }

    /**
     * A crude risk score used in the report. Bigger = riskier.
     * Topic 4 - Expressions in Java: arithmetic and a ternary in one calculation.
     */
    public int riskScore() {
        int sizeRisk = (int) (amount / 100_000.0);
        int termRisk = termMonths / 3;
        int loyaltyDiscount = senioritySeconds > 60 ? 5 : 0;
        int score = sizeRisk + termRisk - loyaltyDiscount;
        return score < 0 ? 0 : score;
    }

    @Override
    public String summary() {
        return String.format("%-9s %-22s %,14.2f %4dm  risk=%-4d %-10s %s",
                getId(), memberName, amount, termMonths, riskScore(), status, purpose);
    }

    @Override
    public String toString() {
        return getId() + " " + memberName + " " + String.format("%,.2f", amount);
    }

    /**
     * Topic 8 - Comparable, and Topic 12 - what makes a PriorityQueue useful.
     *
     * Natural order = review order. Lowest risk score first; ties broken by who
     * applied earlier, so nobody can be leapfrogged indefinitely.
     */
    @Override
    public int compareTo(LoanApplication other) {
        int byRisk = this.riskScore() - other.riskScore();
        if (byRisk != 0) {
            return byRisk;
        }
        if (this.submittedAt != other.submittedAt) {
            return this.submittedAt < other.submittedAt ? -1 : 1;
        }
        return this.getId().compareTo(other.getId());
    }
}
