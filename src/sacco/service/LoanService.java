package sacco.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import sacco.exception.AccountNotFoundException;
import sacco.model.LoanAccount;
import sacco.model.LoanApplication;
import sacco.model.LoanStatus;
import sacco.model.Member;

/**
 * The loan committee's workflow.
 *
 * Topic 12 - The Queue Interface and Queue Implementation Classes, with both of the
 * standard implementations used for the two jobs they are each good at:
 *
 *   PriorityQueue - the review queue. Does NOT hand items back in arrival order; it
 *                   hands back the SMALLEST according to LoanApplication.compareTo,
 *                   which here means the lowest-risk application. The committee sees
 *                   the safest lending first, however it was typed in.
 *
 *   LinkedList    - the disbursement queue. Strict first-in-first-out: approved
 *                   applications get paid in the order they were approved, because
 *                   reordering who gets their money first would be indefensible.
 *
 * Both are declared as `Queue<LoanApplication>`, so the rest of the class is written
 * against the INTERFACE and either implementation could be swapped in - Topic 8.
 */
public class LoanService implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Risk-ordered: poll() returns the lowest risk score. */
    private final Queue<LoanApplication> reviewQueue = new PriorityQueue<LoanApplication>();

    /** Plain FIFO: poll() returns whichever was approved first. */
    private final Queue<LoanApplication> disbursementQueue = new LinkedList<LoanApplication>();

    /** Everything ever submitted, for the audit trail. */
    private final Repository<LoanApplication> archive = new Repository<LoanApplication>("LoanApplication");

    private final AccountService accountService;

    public LoanService(AccountService accountService) {
        if (accountService == null) {
            throw new IllegalArgumentException("LoanService needs an AccountService");
        }
        this.accountService = accountService;
    }

    /* ---------- submitting ---------- */

    public LoanApplication submit(Member applicant, double amount, int termMonths, String purpose) {
        LoanApplication application = new LoanApplication(applicant, amount, termMonths, purpose);
        archive.add(application);

        // offer() is the Queue way to add. It returns false instead of throwing when a
        // bounded queue is full; add() would throw. Neither of these queues is bounded,
        // but offer() is the habit to be in.
        reviewQueue.offer(application);
        return application;
    }

    /* ---------- reviewing ---------- */

    public int pendingCount() {
        return reviewQueue.size();
    }

    /**
     * peek() looks at the head WITHOUT removing it, and returns null when empty.
     * (element() does the same but throws NoSuchElementException instead.)
     */
    public LoanApplication peekNext() {
        return reviewQueue.peek();
    }

    /**
     * poll() removes and returns the head, or null when empty.
     * (remove() throws NoSuchElementException instead - poll() is safer in a loop.)
     */
    public LoanApplication takeNextForReview() {
        return reviewQueue.poll();
    }

    /** Approve an application already taken off the review queue. */
    public void approve(LoanApplication application, Member approver) {
        if (application == null) {
            throw new IllegalArgumentException("No application to approve");
        }
        if (approver == null || !approver.getRole().canApproveLoans()) {
            throw new IllegalStateException(
                    "A " + (approver == null ? "null member" : approver.getRole().getLabel())
                            + " cannot approve loans");
        }
        application.approve("Approved by " + approver.getFullName());
        disbursementQueue.offer(application); // now waits its turn, strictly FIFO
    }

    public void reject(LoanApplication application, String reason) {
        if (application == null) {
            throw new IllegalArgumentException("No application to reject");
        }
        application.reject(reason);
    }

    /** Put an application back at the front of the queue - used when review is cancelled. */
    public void returnToQueue(LoanApplication application) {
        if (application != null && application.getStatus().isOpen()) {
            reviewQueue.offer(application);
        }
    }

    /* ---------- disbursing ---------- */

    public int awaitingDisbursement() {
        return disbursementQueue.size();
    }

    /**
     * Pays out the next approved loan, FIFO, and opens the LoanAccount that will
     * track the debt.
     */
    public LoanAccount disburseNext() throws AccountNotFoundException {
        LoanApplication application = disbursementQueue.poll();
        if (application == null) {
            return null; // nothing waiting
        }
        Member member = application.getMemberId() == null ? null : resolve(application);
        if (member == null) {
            throw new AccountNotFoundException("member " + application.getMemberId());
        }
        LoanAccount loan = accountService.openLoan(member, application.getAmount(),
                application.getTermMonths());
        application.markDisbursed(loan.getAccountNumber());
        return loan;
    }

    /** Set by the app so the service can turn a member id back into a Member. */
    private transient MemberService memberService;

    public void setMemberService(MemberService memberService) {
        this.memberService = memberService;
    }

    private Member resolve(LoanApplication application) {
        if (memberService == null) {
            throw new IllegalStateException("MemberService has not been wired into LoanService");
        }
        return memberService.findById(application.getMemberId());
    }

    /* ---------- reporting ---------- */

    /**
     * The review queue in the order it will actually be served.
     *
     * Iterating a PriorityQueue directly does NOT give sorted order - its iterator
     * walks the internal heap array, so only the head is guaranteed to be first.
     * Draining a copy with poll() is the way to see the true order, which is a
     * genuinely easy mistake to make with this collection.
     */
    public List<LoanApplication> reviewOrder() {
        Queue<LoanApplication> copy = new PriorityQueue<LoanApplication>(reviewQueue);
        List<LoanApplication> ordered = new ArrayList<LoanApplication>();
        while (!copy.isEmpty()) {
            ordered.add(copy.poll());
        }
        return ordered;
    }

    /** The disbursement queue, already in its serving order. */
    public List<LoanApplication> disbursementOrder() {
        return new ArrayList<LoanApplication>(disbursementQueue);
    }

    public List<LoanApplication> allApplications() {
        return archive.findAll();
    }

    public List<LoanApplication> byStatus(LoanStatus status) {
        List<LoanApplication> matches = new ArrayList<LoanApplication>();
        for (LoanApplication application : archive.values()) {
            if (application.getStatus() == status) { // enum constants compare safely with ==
                matches.add(application);
            }
        }
        return matches;
    }

    public double totalRequested() {
        double total = 0.0;
        for (LoanApplication application : archive.values()) {
            total += application.getAmount();
        }
        return total;
    }
}
