package sacco.app;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import sacco.exception.AccountNotFoundException;
import sacco.exception.DuplicateMemberException;
import sacco.exception.InsufficientFundsException;
import sacco.exception.InvalidAmountException;
import sacco.exception.SaccoException;
import sacco.io.LedgerStore;
import sacco.io.MemberImporter;
import sacco.io.ReceiptPrinter;
import sacco.io.SaccoState;
import sacco.io.StateStore;
import sacco.model.Account;
import sacco.model.LoanAccount;
import sacco.model.LoanApplication;
import sacco.model.LoanStatus;
import sacco.model.Member;
import sacco.model.MemberRole;
import sacco.model.SavingsAccount;
import sacco.model.Transaction;
import sacco.service.AccountService;
import sacco.service.LoanService;
import sacco.service.MemberService;
import sacco.service.Repository;

// Topic 9 - Static Imports. These pull in individual STATIC MEMBERS, not types, so
// format(x) and CURRENCY can be written below without the Money. prefix.
import static sacco.util.Money.CURRENCY;
import static sacco.util.Money.format;

/**
 * Village SACCO - Savings and Credit Co-operative management system.
 *
 * COS 3103 Application Development by Java.
 * This is the entry point; see COVERAGE.md for where each course topic is demonstrated.
 *
 * Topic 2 - A First Java Program / Compiling and Interpreting Applications.
 * Compile:  javac -d bin $(find src -name "*.java")
 * Run:      java -cp bin sacco.app.SaccoApp
 */
public class SaccoApp {

    /* Topic 5 - Class Data: file locations, one copy for the whole program. */
    private static final String DATA_DIR = "data";
    private static final String RECEIPTS_DIR = DATA_DIR + File.separator + "receipts";
    private static final String LEDGER_FILE = DATA_DIR + File.separator + "ledger.dat";
    private static final String STATE_FILE = DATA_DIR + File.separator + "sacco.ser";
    private static final String IMPORT_FILE = DATA_DIR + File.separator + "members.csv";

    /* Topic 5 - Instance Data: the collaborators this app is built from. */
    private final ConsoleMenu console = new ConsoleMenu();
    private MemberService memberService = new MemberService();
    private AccountService accountService = new AccountService();
    private LoanService loanService;

    private final ReceiptPrinter receiptPrinter = new ReceiptPrinter(RECEIPTS_DIR);
    private final LedgerStore ledgerStore = new LedgerStore(LEDGER_FILE);
    private final StateStore stateStore = new StateStore(STATE_FILE);

    /** The signed-in officer, used to check who may approve loans. */
    private Member currentOfficer;

    public SaccoApp() {
        this.loanService = new LoanService(accountService);
        this.loanService.setMemberService(memberService);
    }

    /**
     * Topic 2 / Topic 4 - the entry point. The JVM looks for exactly this signature:
     * public (callable from outside), static (no object exists yet), void, and a
     * String[] of the command-line arguments.
     */
    public static void main(String[] args) {
        SaccoApp app = new SaccoApp(); // Topic 5 - Creating an Object

        // Topic 4 - Arrays: args.length, and an enhanced for over the arguments
        boolean seedDemoData = false;
        for (String arg : args) {
            if ("--demo".equalsIgnoreCase(arg)) {
                seedDemoData = true;
            } else if ("--help".equalsIgnoreCase(arg)) {
                System.out.println("Usage: java -cp bin sacco.app.SaccoApp [--demo]");
                System.out.println("  --demo   start with sample members, accounts and loans");
                return;
            }
        }

        try {
            app.run(seedDemoData);
        } catch (RuntimeException unexpected) {
            /*
             * Topic 10 - a last-resort catch so the user never sees a bare stack trace.
             * printStackTrace() goes to System.err and is kept because, for a bug we
             * did not anticipate, the stack trace is the only useful information.
             */
            System.err.println("The application stopped because of an unexpected error:");
            System.err.println("  " + unexpected.getMessage());
            unexpected.printStackTrace();
        } finally {
            app.console.close(); // Topic 10 - finally always runs
        }
    }

    /* ================= main loop ================= */

    private void run(boolean seedDemoData) {
        console.banner("Village SACCO Management System");
        console.println("  COS 3103 - Application Development by Java");
        console.printf("  Currency: %s%n", CURRENCY); // static import in use

        loadOrSeed(seedDemoData);

        /*
         * Topic 4 - Loop Statements and Switch Statements.
         * A do-while because the menu must always be shown at least once, and a switch
         * on the choice because a chain of else-if would be far harder to read.
         */
        int choice;
        do {
            console.showMenu("Main Menu", new String[] {
                    "Members",
                    "Accounts and transactions",
                    "Loans",
                    "Reports",
                    "Files and data"
            });
            console.printf("  Signed in as: %s%n", currentOfficer == null
                    ? "(nobody)" : currentOfficer.toString());
            choice = console.readInt("Choice", 0, 5);

            switch (choice) {
                case 1:
                    membersMenu();
                    break;
                case 2:
                    accountsMenu();
                    break;
                case 3:
                    loansMenu();
                    break;
                case 4:
                    reportsMenu();
                    break;
                case 5:
                    filesMenu();
                    break;
                case 0:
                    shutdown();
                    break;
                default:
                    // Unreachable, because readInt already bounded the value - but a
                    // default branch documents that and survives future edits.
                    console.warn("Unknown option.");
                    break;
            }
        } while (choice != 0);
    }

    private void shutdown() {
        console.banner("Closing");
        if (console.confirm("Save all data before exiting?")) {
            saveEverything();
        }
        console.println("  Goodbye.");
    }

    /* ================= members ================= */

    private void membersMenu() {
        int choice;
        do {
            console.showMenu("Members", new String[] {
                    "Register a new member",
                    "List members (by name)",
                    "List members (newest first)",
                    "Find a member",
                    "Change a member's role",
                    "Sign in as an officer",
                    "Members grouped by role"
            });
            choice = console.readInt("Choice", 0, 7);

            switch (choice) {
                case 1: registerMember();     break;
                case 2: listMembers(memberService.sortedByName(), "Members by name"); break;
                case 3: listMembers(memberService.sortedByJoinDate(), "Members, newest first"); break;
                case 4: findMember();         break;
                case 5: changeRole();         break;
                case 6: signIn();             break;
                case 7: membersByRole();      break;
                default: break;
            }
        } while (choice != 0);
    }

    private void registerMember() {
        console.banner("Register a member");
        console.println("  National ID must be 14 alphanumeric characters, e.g. CF94051234ABCD");
        console.println("  Phone must be 10 digits starting 070/074/075/076/077/078/039");

        String nationalId = console.readRequired("National ID");
        String fullName = console.readRequired("Full name");
        String phone = console.readRequired("Phone");
        MemberRole role = console.readRole();

        /*
         * Topic 10 - Catching Exceptions, with the branches ordered from most to least
         * specific. DuplicateMemberException is one of OUR checked exceptions;
         * IllegalArgumentException comes from the constructor's validation and is
         * unchecked, so catching it is a choice, made here because a clerk mistyping
         * a phone number should not end the program.
         */
        try {
            Member member = memberService.register(nationalId, fullName, phone, role);
            console.success("Registered " + member);
            console.printf("          Loan ceiling: %s%n", format(member.loanCeiling()));

            if (currentOfficer == null && role.canApproveLoans()) {
                currentOfficer = member;
                console.success("Signed in as " + member.getFullName()
                        + " (first officer registered)");
            }
        } catch (DuplicateMemberException dme) {
            console.error(dme.getMessage());
        } catch (IllegalArgumentException iae) {
            console.error("Could not register: " + iae.getMessage());
        }
    }

    /** Topic 4 - Enhanced for Loop; Topic 8 - polymorphic use of summary(). */
    private void listMembers(List<Member> members, String title) {
        console.banner(title);
        if (members.isEmpty()) {
            console.warn("No members registered yet.");
            return;
        }
        console.printf("  %-9s %-24s %-11s %-12s %s%n",
                "ID", "NAME", "PHONE", "ROLE", "ACCOUNTS");
        console.rule();
        for (Member member : members) {
            console.println("  " + member.summary());
        }
        console.rule();
        console.printf("  %d member(s). Unique surnames: %s%n",
                members.size(), memberService.uniqueSurnames());
    }

    private void findMember() {
        console.banner("Find a member");
        String query = console.readRequired("Member ID or National ID");

        // Try both lookups; the Map-backed one first because it is the cheap one
        Member member = memberService.findById(query.toUpperCase());
        if (member == null) {
            member = memberService.findByNationalId(query);
        }
        if (member == null) {
            console.warn("No member matches '" + query + "'.");
            return;
        }

        console.rule();
        console.printf("  %-14s %s%n", "Member ID", member.getId());
        console.printf("  %-14s %s%n", "Name", member.getFullName());
        console.printf("  %-14s %s%n", "National ID", member.getNationalId());
        console.printf("  %-14s %s%n", "Phone", member.getPhone());
        console.printf("  %-14s %s%n", "Role", member.getRole().getLabel());
        console.printf("  %-14s %s%n", "Can approve", member.getRole().canApproveLoans() ? "yes" : "no");
        console.printf("  %-14s %s%n", "Loan ceiling", format(member.loanCeiling()));
        console.rule();

        List<Account> owned = accountService.findByOwner(member);
        if (owned.isEmpty()) {
            console.println("  No accounts.");
        } else {
            double total = 0.0;
            for (Account account : owned) {
                console.println("  " + account.summary());
                // Topic 7 - Casting guarded by instanceof: only savings count as wealth
                if (account instanceof SavingsAccount) {
                    total += account.getBalance();
                }
            }
            console.printf("  Total savings: %s%n", format(total));
        }
    }

    private void changeRole() {
        Member member = pickMember("Change whose role?");
        if (member == null) {
            return;
        }
        console.printf("  %s is currently a %s.%n", member.getFullName(), member.getRole().getLabel());
        MemberRole role = console.readRole();
        member.setRole(role); // Topic 5 - the guarded setter does the validating
        console.success(member.getFullName() + " is now a " + role.getLabel());
    }

    private void signIn() {
        List<Member> committee = memberService.committeeMembers();
        if (committee.isEmpty()) {
            console.warn("No member holds a role that can approve loans.");
            return;
        }
        Member officer = pickFrom("Sign in as", committee);
        if (officer != null) {
            currentOfficer = officer;
            console.success("Signed in as " + officer);
        }
    }

    /** Topic 12 - iterating a Map's entrySet, the cheap way to get key AND value. */
    private void membersByRole() {
        console.banner("Members by role");
        Map<MemberRole, List<Member>> grouped = memberService.groupByRole();

        for (Map.Entry<MemberRole, List<Member>> entry : grouped.entrySet()) {
            MemberRole role = entry.getKey();
            List<Member> inRole = entry.getValue();

            console.printf("%n  %s (%d) - ceiling %s%n",
                    role.getLabel(), inRole.size(), format(role.getLoanCeiling()));
            if (inRole.isEmpty()) {
                console.println("      (none)");
            } else {
                for (Member member : inRole) {
                    console.printf("      %-9s %s%n", member.getId(), member.getFullName());
                }
            }
        }
    }

    /* ================= accounts ================= */

    private void accountsMenu() {
        int choice;
        do {
            console.showMenu("Accounts and transactions", new String[] {
                    "Open a savings account",
                    "Deposit",
                    "Withdraw",
                    "Transfer between accounts",
                    "Print a statement",
                    "List all accounts",
                    "Run month-end interest",
                    "Close an empty account"
            });
            choice = console.readInt("Choice", 0, 8);

            switch (choice) {
                case 1: openSavings();      break;
                case 2: doDeposit();        break;
                case 3: doWithdraw();       break;
                case 4: doTransfer();       break;
                case 5: showStatement();    break;
                case 6: listAccounts();     break;
                case 7: runInterest();      break;
                case 8: closeAccount();     break;
                default: break;
            }
        } while (choice != 0);
    }

    private void openSavings() {
        Member member = pickMember("Open an account for whom?");
        if (member == null) {
            return;
        }
        double opening = console.readAmount("Opening deposit");
        try {
            SavingsAccount account = accountService.openSavings(member, opening);
            console.success("Opened " + account.getAccountNumber()
                    + " with " + format(account.getBalance()));
            console.printf("          Minimum balance %s, rate %.2f%%%n",
                    format(account.getMinimumBalance()), account.annualRate() * 100);
        } catch (InvalidAmountException | IllegalArgumentException problem) {
            // Topic 10 - multi-catch: two unrelated types, one identical response
            console.error(problem.getMessage());
        }
    }

    private void doDeposit() {
        Account account = pickAccount("Deposit into which account?");
        if (account == null) {
            return;
        }
        double amount = console.readAmount("Amount");
        String note = console.readLine("Narration (optional)");

        try {
            Transaction txn = accountService.deposit(account.getAccountNumber(), amount, note);
            console.success(String.format("%s posted. New balance %s",
                    txn.getType(), format(account.getBalance())));
            offerReceipt(account, txn);
        } catch (AccountNotFoundException anf) {
            console.error(anf.getMessage());
        } catch (InvalidAmountException | IllegalArgumentException problem) {
            console.error(problem.getMessage());
        }
    }

    private void doWithdraw() {
        Account account = pickAccount("Withdraw from which account?");
        if (account == null) {
            return;
        }
        console.printf("  Balance %s, withdrawable %s%n",
                format(account.getBalance()), format(account.withdrawableBalance()));

        double amount = console.readAmount("Amount");
        String note = console.readLine("Narration (optional)");

        try {
            Transaction txn = accountService.withdraw(account.getAccountNumber(), amount, note);
            console.success("Withdrawal posted. New balance " + format(account.getBalance()));
            offerReceipt(account, txn);

        } catch (InsufficientFundsException ife) {
            /*
             * Topic 10 - Exception Methods. A custom exception can carry DATA, not just
             * a message, which is what lets us tell the member exactly how short they
             * are rather than just "failed".
             */
            console.error(ife.getMessage());
            console.printf("          Short by %s%n", format(ife.getShortfall()));

        } catch (AccountNotFoundException anf) {
            console.error(anf.getMessage());

        } catch (UnsupportedOperationException uoe) {
            // LoanAccount.withdraw refuses outright - Topic 7's override in action
            console.error(uoe.getMessage());
        }
    }

    private void doTransfer() {
        Account from = pickAccount("Transfer FROM which account?");
        if (from == null) {
            return;
        }
        Account to = pickAccount("Transfer TO which account?");
        if (to == null) {
            return;
        }
        double amount = console.readAmount("Amount");

        try {
            accountService.transfer(from.getAccountNumber(), to.getAccountNumber(), amount);
            console.success("Transferred " + format(amount));
            console.printf("          %s now %s%n", from.getAccountNumber(), format(from.getBalance()));
            console.printf("          %s now %s%n", to.getAccountNumber(), format(to.getBalance()));

        } catch (SaccoException problem) {
            /*
             * Topic 7 / Topic 10 - Polymorphism applied to exceptions. Both
             * AccountNotFoundException and InsufficientFundsException extend
             * SaccoException, so one catch block handles either. This is the whole
             * reason for having a common base exception class.
             */
            console.error(problem.getMessage());

        } catch (IllegalArgumentException iae) {
            console.error(iae.getMessage());
        }
    }

    private void showStatement() {
        Account account = pickAccount("Statement for which account?");
        if (account == null) {
            return;
        }
        try {
            String statement = accountService.statement(account.getAccountNumber());
            console.blank();
            console.println(statement);

            if (console.confirm("Save this statement to a file?")) {
                File saved = receiptPrinter.writeReport(
                        "statement-" + account.getAccountNumber() + ".txt", statement);
                console.success("Written to " + saved.getPath());
            }
        } catch (AccountNotFoundException anf) {
            console.error(anf.getMessage());
        } catch (IOException ioe) {
            console.error("Could not write the file: " + ioe.getMessage());
        }
    }

    /**
     * Topic 7 - Polymorphism. One loop, one variable of type Account, and each object
     * prints itself correctly because summary() was overridden differently in
     * SavingsAccount and LoanAccount. No instanceof, no casting, no switch on type.
     */
    private void listAccounts() {
        console.banner("All accounts");
        List<Account> accounts = accountService.findAll();
        if (accounts.isEmpty()) {
            console.warn("No accounts yet.");
            return;
        }
        console.printf("  %-10s %-10s %-22s %15s%n", "ACCOUNT", "TYPE", "OWNER", "BALANCE");
        console.rule();
        for (Account account : accounts) {
            console.println("  " + account.summary());
        }
        console.rule();
        console.printf("  Savings held      : %s%n", format(accountService.totalSavings()));
        console.printf("  Loans outstanding : %s%n", format(accountService.totalLoansOutstanding()));
    }

    private void runInterest() {
        console.banner("Month-end interest run");
        if (!console.confirm("Apply one month of interest to every open account?")) {
            return;
        }
        Map<String, Double> movements = accountService.runMonthEndInterest();
        if (movements.isEmpty()) {
            console.warn("No account earned or was charged interest.");
            return;
        }
        console.printf("  %-12s %18s%n", "ACCOUNT", "INTEREST");
        console.rule();
        double total = 0.0;
        for (Map.Entry<String, Double> entry : movements.entrySet()) {
            // Topic 6 - Wrapper Classes: the Map holds Double objects, and
            // auto-unboxing turns entry.getValue() back into a primitive double here
            double amount = entry.getValue();
            console.printf("  %-12s %18s%n", entry.getKey(), format(amount));
            total += amount;
        }
        console.rule();
        console.printf("  %d account(s), %s moved%n", movements.size(), format(total));
    }

    private void closeAccount() {
        Account account = pickAccount("Close which account?");
        if (account == null) {
            return;
        }
        try {
            accountService.closeAccount(account.getAccountNumber());
            console.success(account.getAccountNumber() + " closed and removed.");
        } catch (AccountNotFoundException anf) {
            console.error(anf.getMessage());
        } catch (IllegalStateException ise) {
            // Account.close() refuses while the balance is non-zero
            console.error(ise.getMessage());
        }
    }

    private void offerReceipt(Account account, Transaction txn) {
        if (!console.confirm("Print a receipt?")) {
            return;
        }
        try {
            File receipt = receiptPrinter.printReceipt(account, txn);
            console.success("Receipt written to " + receipt.getPath());
        } catch (IOException ioe) {
            console.error("Receipt failed: " + ioe.getMessage());
        }
    }

    /* ================= loans ================= */

    private void loansMenu() {
        int choice;
        do {
            console.showMenu("Loans", new String[] {
                    "Apply for a loan",
                    "View the review queue",
                    "Review the next application",
                    "Disburse the next approved loan",
                    "Repay a loan",
                    "Applications by status"
            });
            console.printf("  Review queue: %d waiting | Approved, awaiting payout: %d%n",
                    loanService.pendingCount(), loanService.awaitingDisbursement());
            choice = console.readInt("Choice", 0, 6);

            switch (choice) {
                case 1: applyForLoan();   break;
                case 2: viewQueue();      break;
                case 3: reviewNext();     break;
                case 4: disburseNext();   break;
                case 5: repayLoan();      break;
                case 6: byStatus();       break;
                default: break;
            }
        } while (choice != 0);
    }

    private void applyForLoan() {
        Member applicant = pickMember("Who is applying?");
        if (applicant == null) {
            return;
        }
        console.printf("  %s may borrow up to %s%n",
                applicant.getFullName(), format(applicant.loanCeiling()));

        double amount = console.readAmount("Amount requested");
        int term = console.readInt("Term in months (1-60)", 1, 60);
        String purpose = console.readLine("Purpose");

        try {
            LoanApplication application = loanService.submit(applicant, amount, term, purpose);
            console.success("Submitted " + application.getId());
            console.printf("          Risk score %d; monthly instalment about %s%n",
                    application.riskScore(),
                    format((amount + amount * LoanAccount.STANDARD_RATE * term / 12.0) / term));
            console.printf("          %d application(s) now in the review queue%n",
                    loanService.pendingCount());

        } catch (InvalidAmountException | IllegalArgumentException problem) {
            console.error(problem.getMessage());
        }
    }

    /** Topic 12 - showing the PriorityQueue's true serving order. */
    private void viewQueue() {
        console.banner("Loan review queue");
        List<LoanApplication> queue = loanService.reviewOrder();
        if (queue.isEmpty()) {
            console.warn("The review queue is empty.");
        } else {
            console.println("  Served lowest-risk first (PriorityQueue order):");
            console.rule();
            int position = 1;
            for (LoanApplication application : queue) {
                console.printf("  %2d. %s%n", position++, application.summary());
            }
            console.rule();
        }

        List<LoanApplication> payouts = loanService.disbursementOrder();
        if (!payouts.isEmpty()) {
            console.println("  Approved, awaiting payout (LinkedList, strict FIFO):");
            console.rule();
            int position = 1;
            for (LoanApplication application : payouts) {
                console.printf("  %2d. %s%n", position++, application.summary());
            }
            console.rule();
        }
    }

    private void reviewNext() {
        if (currentOfficer == null) {
            console.warn("Sign in as an officer first (Members > Sign in).");
            return;
        }
        if (!currentOfficer.getRole().canApproveLoans()) {
            console.error(currentOfficer.getRole().getLabel() + " cannot approve loans.");
            return;
        }

        LoanApplication application = loanService.takeNextForReview(); // poll()
        if (application == null) {
            console.warn("Nothing waiting for review.");
            return;
        }

        console.banner("Review " + application.getId());
        console.printf("  %-14s %s%n", "Applicant", application.getMemberName());
        console.printf("  %-14s %s%n", "Amount", format(application.getAmount()));
        console.printf("  %-14s %d months%n", "Term", application.getTermMonths());
        console.printf("  %-14s %s%n", "Purpose", application.getPurpose());
        console.printf("  %-14s %d%n", "Risk score", application.riskScore());
        console.rule();

        int decision = console.readInt("1 = approve, 2 = reject, 3 = defer", 1, 3);
        try {
            switch (decision) {
                case 1:
                    loanService.approve(application, currentOfficer);
                    console.success("Approved. Queued for payout behind "
                            + (loanService.awaitingDisbursement() - 1) + " other(s).");
                    break;
                case 2:
                    String reason = console.readRequired("Reason for rejection");
                    loanService.reject(application, reason);
                    console.success("Rejected: " + reason);
                    break;
                case 3:
                default:
                    // Put it back - poll() already removed it from the queue
                    loanService.returnToQueue(application);
                    console.success("Deferred and returned to the queue.");
                    break;
            }
        } catch (IllegalStateException ise) {
            console.error(ise.getMessage());
            loanService.returnToQueue(application);
        }
    }

    private void disburseNext() {
        try {
            LoanAccount loan = loanService.disburseNext();
            if (loan == null) {
                console.warn("No approved loan is waiting for payout.");
                return;
            }
            console.success("Disbursed " + format(loan.getPrincipal())
                    + " to " + loan.getOwner().getFullName());
            console.printf("          Loan account %s, %d months, instalment %s%n",
                    loan.getAccountNumber(), loan.getTermMonths(),
                    format(loan.monthlyInstalment()));

        } catch (AccountNotFoundException anf) {
            console.error(anf.getMessage());
        }
    }

    private void repayLoan() {
        List<Account> loans = new java.util.ArrayList<Account>();
        for (Account account : accountService.findAll()) {
            if (account instanceof LoanAccount && !((LoanAccount) account).isCleared()) {
                loans.add(account);
            }
        }
        if (loans.isEmpty()) {
            console.warn("No loan currently has an outstanding balance.");
            return;
        }

        Account chosen = pickFromAccounts("Repay which loan?", loans);
        if (chosen == null) {
            return;
        }
        // Topic 7 - Casting. Safe because the list was filtered on instanceof above.
        LoanAccount loan = (LoanAccount) chosen;

        console.printf("  Outstanding %s; instalment %s%n",
                format(loan.getOutstanding()), format(loan.monthlyInstalment()));
        double amount = console.readAmount("Repayment");

        try {
            Transaction txn = accountService.deposit(loan.getAccountNumber(), amount, "Loan repayment");
            console.success("Repaid. Outstanding now " + format(loan.getOutstanding()));
            if (loan.isCleared()) {
                console.success("This loan is fully cleared.");
            }
            offerReceipt(loan, txn);

        } catch (AccountNotFoundException anf) {
            console.error(anf.getMessage());
        } catch (IllegalArgumentException iae) {
            // repay() rejects overpayment
            console.error(iae.getMessage());
        }
    }

    /** Topic 4 - Arrays + Enhanced for over an enum's values. */
    private void byStatus() {
        console.banner("Applications by status");
        for (LoanStatus status : LoanStatus.values()) {
            List<LoanApplication> matches = loanService.byStatus(status);
            console.printf("%n  %s - %s (%d)%n", status, status.getDescription(), matches.size());
            for (LoanApplication application : matches) {
                console.printf("      %s%n", application.summary());
            }
        }
        console.blank();
        console.printf("  Total ever requested: %s%n", format(loanService.totalRequested()));
    }

    /* ================= reports ================= */

    private void reportsMenu() {
        int choice;
        do {
            console.showMenu("Reports", new String[] {
                    "SACCO summary",
                    "Full transaction journal",
                    "Members sorted by name (Comparable)",
                    "Unique surnames (TreeSet)"
            });
            choice = console.readInt("Choice", 0, 4);

            switch (choice) {
                case 1: summaryReport();  break;
                case 2: journalReport();  break;
                case 3: sortedReport();   break;
                case 4: surnameReport();  break;
                default: break;
            }
        } while (choice != 0);
    }

    /**
     * Topic 6 - StringBuffer rather than StringBuilder, for contrast.
     *
     * The two have identical APIs; StringBuffer's methods are synchronized and
     * StringBuilder's are not. That makes StringBuffer safe to share between threads
     * and slightly slower. StringBuilder is the right default - this one is here to
     * show the difference, since a report buffer that might one day be handed to a
     * background printing thread is the one plausible excuse for StringBuffer.
     */
    private void summaryReport() {
        StringBuffer report = new StringBuffer(256);

        report.append(String.format("%n  SACCO SUMMARY%n"));
        report.append(String.format("  %s%n", "----------------------------------------"));
        report.append(String.format("  %-28s %s%n", "Members", memberService.count()));
        report.append(String.format("  %-28s %s%n", "Accounts", accountService.count()));
        report.append(String.format("  %-28s %s%n", "Savings held",
                format(accountService.totalSavings())));
        report.append(String.format("  %-28s %s%n", "Loans outstanding",
                format(accountService.totalLoansOutstanding())));
        report.append(String.format("  %-28s %s%n", "Transactions posted",
                accountService.allTransactions().size()));
        report.append(String.format("  %-28s %s%n", "Applications in queue",
                loanService.pendingCount()));
        report.append(String.format("  %-28s %s%n", "Awaiting disbursement",
                loanService.awaitingDisbursement()));
        report.append(String.format("  %-28s %s%n", "Committee members",
                memberService.committeeMembers().size()));

        double net = accountService.totalSavings() - accountService.totalLoansOutstanding();
        report.append(String.format("  %s%n", "----------------------------------------"));
        report.append(String.format("  %-28s %s%n", "Net position", format(net)));

        console.println(report.toString());
    }

    private void journalReport() {
        console.banner("Transaction journal");
        List<Transaction> all = accountService.allTransactions();
        if (all.isEmpty()) {
            console.warn("Nothing has been posted yet.");
            return;
        }
        console.printf("  %-11s %-20s %-4s %14s %16s  %s%n",
                "REF", "DATE", "TYP", "AMOUNT", "BALANCE", "NOTE");
        console.rule();
        for (Transaction txn : all) {
            console.println("  " + txn.summary());
        }
        console.rule();
        console.printf("  %d transaction(s)%n", all.size());
    }

    /** Topic 8 - the generic static helper on Repository, reused for Transactions. */
    private void sortedReport() {
        console.banner("Sorted with Comparable");

        List<Member> byName = Repository.sorted(memberService.findAll());
        console.println("  Members in natural order (Member.compareTo - by name):");
        for (Member member : byName) {
            console.printf("      %-9s %s%n", member.getId(), member.getFullName());
        }

        List<Transaction> byTime = Repository.sorted(accountService.allTransactions());
        console.blank();
        console.printf("  Transactions in natural order (Transaction.compareTo - %d by time)%n",
                byTime.size());
        int shown = 0;
        for (Transaction txn : byTime) {
            if (shown++ >= 10) {
                console.printf("      ... and %d more%n", byTime.size() - 10);
                break;
            }
            console.printf("      %s %s %s%n", txn.getId(), txn.getFormattedDate(),
                    format(txn.getAmount()));
        }
    }

    private void surnameReport() {
        console.banner("Unique surnames");
        console.println("  A TreeSet: sorted automatically, duplicates dropped.");
        console.rule();
        for (String surname : memberService.uniqueSurnames()) {
            console.println("      " + surname);
        }
        console.rule();
        console.printf("  %d distinct surname(s) across %d member(s)%n",
                memberService.uniqueSurnames().size(), memberService.count());
    }

    /* ================= files ================= */

    private void filesMenu() {
        int choice;
        do {
            console.showMenu("Files and data", new String[] {
                    "Save everything",
                    "Reload saved state",
                    "Import members from CSV",
                    "Create a sample CSV",
                    "Where the files live"
            });
            choice = console.readInt("Choice", 0, 5);

            switch (choice) {
                case 1: saveEverything();   break;
                case 2: reloadState();      break;
                case 3: importMembers();    break;
                case 4: makeSampleCsv();    break;
                case 5: describeFiles();    break;
                default: break;
            }
        } while (choice != 0);
    }

    /** Topic 11 - all three storage styles driven from one place. */
    private void saveEverything() {
        try {
            stateStore.save(new SaccoState(memberService, accountService, loanService));
            console.success("Object state written to " + STATE_FILE);

            ledgerStore.save(accountService.allTransactions());
            console.success("Binary ledger written to " + LEDGER_FILE);

        } catch (IOException ioe) {
            console.error("Save failed: " + ioe.getMessage());
            // getCause() is how a wrapped exception reveals what really went wrong
            if (ioe.getCause() != null) {
                console.error("Caused by: " + ioe.getCause());
            }
        }
    }

    private void loadOrSeed(boolean seedDemoData) {
        if (stateStore.hasSavedState()) {
            console.blank();
            if (console.confirm("Saved data was found. Load it?")) {
                if (reloadState()) {
                    return;
                }
            }
        }
        if (seedDemoData || memberService.count() == 0) {
            seed();
        }
    }

    private boolean reloadState() {
        try {
            SaccoState state = stateStore.load();
            if (state == null) {
                console.warn("There is no saved state to load.");
                return false;
            }
            // Replace the live services with the restored ones
            this.memberService = state.getMemberService();
            this.accountService = state.getAccountService();
            this.loanService = state.getLoanService();
            state.restore(); // counters and the transient wiring

            console.success(String.format("Loaded %d member(s) and %d account(s)",
                    memberService.count(), accountService.count()));
            this.currentOfficer = null;

            // The binary ledger is independent; report whether it agrees
            List<Transaction> fromLedger = ledgerStore.load();
            console.printf("          Binary ledger holds %d transaction(s)%n", fromLedger.size());
            return true;

        } catch (IOException ioe) {
            console.error("Load failed: " + ioe.getMessage());
            return false;
        }
    }

    private void importMembers() {
        MemberImporter importer = new MemberImporter(memberService);
        String path = console.readLine("CSV path (Enter for " + IMPORT_FILE + ")");
        if (path.isEmpty()) {
            path = IMPORT_FILE;
        }
        try {
            List<Member> added = importer.importFrom(path);
            console.success(String.format("Imported %d, skipped %d",
                    importer.getImported(), importer.getSkipped()));
            for (Member member : added) {
                console.printf("          + %s%n", member);
            }
            if (!importer.getErrors().isEmpty()) {
                console.blank();
                console.warn("Rows that were skipped:");
                for (String problem : importer.getErrors()) {
                    console.printf("          - %s%n", problem);
                }
            }
        } catch (IOException ioe) {
            console.error(ioe.getMessage());
            console.println("  Tip: use option 4 to create a sample file first.");
        }
    }

    private void makeSampleCsv() {
        try {
            File sample = MemberImporter.writeSampleFile(IMPORT_FILE);
            console.success("Sample written to " + sample.getPath());
            console.println("  It deliberately contains one bad row, to show error handling.");
        } catch (IOException ioe) {
            console.error("Could not write the sample: " + ioe.getMessage());
        }
    }

    /** Topic 11 - File Object: inspecting paths without opening any stream. */
    private void describeFiles() {
        console.banner("Where the files live");
        File dataDir = new File(DATA_DIR);
        console.printf("  %-20s %s%n", "Working directory", new File(".").getAbsolutePath());
        console.printf("  %-20s %s%n", "Data directory",
                dataDir.getAbsolutePath() + (dataDir.exists() ? "" : "  (not created yet)"));
        console.printf("  %-20s %s%n", "Object state", stateStore.describe());
        console.printf("  %-20s %s%n", "Binary ledger", ledgerStore.describe());
        console.printf("  %-20s %s%n", "Receipts", receiptPrinter.getDirectory().getAbsolutePath());

        File[] receipts = receiptPrinter.getDirectory().listFiles();
        if (receipts != null && receipts.length > 0) {
            console.printf("  %d receipt file(s); most recent:%n", receipts.length);
            // Topic 4 - a reverse for loop over an array
            int shown = 0;
            for (int i = receipts.length - 1; i >= 0 && shown < 5; i--, shown++) {
                console.printf("      %-28s %,8d bytes%n",
                        receipts[i].getName(), receipts[i].length());
            }
        }
    }

    /* ================= pickers and seed data ================= */

    private Member pickMember(String title) {
        List<Member> members = memberService.sortedByName();
        if (members.isEmpty()) {
            console.warn("No members registered yet.");
            return null;
        }
        return pickFrom(title, members);
    }

    private Member pickFrom(String title, List<Member> members) {
        List<String> rows = new java.util.ArrayList<String>();
        for (Member member : members) {
            rows.add(member.summary());
        }
        int index = console.chooseFrom(title, rows);
        return index < 0 ? null : members.get(index);
    }

    private Account pickAccount(String title) {
        List<Account> accounts = accountService.findAll();
        if (accounts.isEmpty()) {
            console.warn("No accounts yet. Open one first.");
            return null;
        }
        return pickFromAccounts(title, accounts);
    }

    private Account pickFromAccounts(String title, List<Account> accounts) {
        List<String> rows = new java.util.ArrayList<String>();
        for (Account account : accounts) {
            rows.add(account.summary()); // Topic 7 - polymorphic summary()
        }
        int index = console.chooseFrom(title, rows);
        return index < 0 ? null : accounts.get(index);
    }

    /**
     * Sample data, so the app can be demonstrated without ten minutes of typing.
     * Topic 10 - the whole block is wrapped, because a failure to seed is not fatal.
     */
    private void seed() {
        console.blank();
        console.println("  Seeding sample data...");
        try {
            Member chair = memberService.register("CM80010001AAAA", "Okwi Joseph",
                    "0772000001", MemberRole.CHAIRPERSON);
            Member treasurer = memberService.register("CF83020002BBBB", "Nakalema Sarah",
                    "0701000002", MemberRole.TREASURER);
            Member grace = memberService.register("CF94051234CCCC", "Nakato Grace",
                    "0751000003", MemberRole.MEMBER);
            Member peter = memberService.register("CM85030321DDDD", "Mugisha Peter",
                    "0781000004", MemberRole.MEMBER);

            currentOfficer = chair;

            accountService.openSavings(chair, 800_000);
            accountService.openSavings(treasurer, 1_200_000);
            SavingsAccount graceSavings = accountService.openSavings(grace, 150_000);
            accountService.openSavings(peter, 45_000);

            accountService.deposit(graceSavings.getAccountNumber(), 60_000, "Market earnings");

            // Three applications, submitted in an order that is NOT the review order -
            // which is what makes the PriorityQueue visible in the demo.
            loanService.submit(peter, 900_000, 24, "Boda boda purchase");
            loanService.submit(grace, 120_000, 6, "Seed and fertiliser");
            loanService.submit(treasurer, 450_000, 12, "Shop stock");

            console.success(String.format(
                    "%d members, %d accounts, %d loan application(s) in the queue",
                    memberService.count(), accountService.count(), loanService.pendingCount()));
            console.printf("          Signed in as %s%n", chair);

        } catch (SaccoException problem) {
            // Topic 7 - one catch for the whole custom hierarchy
            console.error("Seeding stopped: " + problem.getMessage());
        } catch (RuntimeException problem) {
            console.error("Seeding stopped: " + problem.getMessage());
        }
    }
}
