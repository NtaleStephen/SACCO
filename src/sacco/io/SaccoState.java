package sacco.io;

import java.io.Serializable;

import sacco.model.Account;
import sacco.model.LoanApplication;
import sacco.model.Member;
import sacco.model.Transaction;
import sacco.service.AccountService;
import sacco.service.LoanService;
import sacco.service.MemberService;

/**
 * Topic 11 - Reading and Writing Objects.
 *
 * A single Serializable container holding the whole application state, so one
 * writeObject() call saves everything. Java serialisation follows every reference it
 * finds, so writing this object also writes the services, their repositories, the
 * Maps and Sets inside them, and every Member, Account and Transaction - the entire
 * OBJECT GRAPH, with shared references preserved. An Account and its owning Member
 * come back pointing at each other exactly as they did before.
 *
 * The static ID counters are NOT part of any object's state, so they are copied in
 * here explicitly - otherwise a restart would start handing out MBR-0001 again and
 * overwrite real members.
 */
public class SaccoState implements Serializable {

    /**
     * Topic 11 - serialVersionUID is the version stamp of the class's shape. If a
     * field is added later and this number is unchanged, reading an old file throws
     * InvalidClassException. Letting the compiler generate it implicitly is the
     * classic mistake: the value then changes whenever the class is edited.
     */
    private static final long serialVersionUID = 1L;

    private final MemberService memberService;
    private final AccountService accountService;
    private final LoanService loanService;

    /* Saved separately because static fields are never serialised. */
    private final int memberCounter;
    private final int accountCounter;
    private final long savedAt;

    public SaccoState(MemberService memberService, AccountService accountService,
                      LoanService loanService) {
        this.memberService = memberService;
        this.accountService = accountService;
        this.loanService = loanService;
        this.memberCounter = Member.getMemberCount();
        this.accountCounter = Account.getAccountCount();
        this.savedAt = System.currentTimeMillis();
    }

    public MemberService getMemberService() {
        return memberService;
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public LoanService getLoanService() {
        return loanService;
    }

    public long getSavedAt() {
        return savedAt;
    }

    /**
     * Puts the static counters back after a load, and re-wires the one reference that
     * deliberately was not saved (LoanService.memberService is transient).
     */
    public void restore() {
        Member.restoreCounter(memberCounter);
        Account.restoreCounter(accountCounter);

        // Walk the restored data to push the transaction and application counters
        // past anything already issued.
        int highestTxn = 0;
        for (Transaction txn : accountService.allTransactions()) {
            highestTxn = Math.max(highestTxn, numericSuffix(txn.getId()));
        }
        Transaction.restoreCounter(highestTxn);

        int highestApp = 0;
        for (LoanApplication application : loanService.allApplications()) {
            highestApp = Math.max(highestApp, numericSuffix(application.getId()));
        }
        LoanApplication.restoreCounter(highestApp);

        loanService.setMemberService(memberService);
    }

    /** Pulls 42 out of "TXN-000042". Topic 4 - Strings and the Integer wrapper. */
    private static int numericSuffix(String id) {
        int dash = id.lastIndexOf('-');
        if (dash < 0 || dash == id.length() - 1) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(dash + 1));
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }
}
