package sacco.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sacco.exception.AccountNotFoundException;
import sacco.exception.InsufficientFundsException;
import sacco.model.Account;
import sacco.model.InterestBearing;
import sacco.model.LoanAccount;
import sacco.model.Member;
import sacco.model.SavingsAccount;
import sacco.model.Transaction;
import sacco.model.TransactionType;

/**
 * Accounts and the ledger.
 *
 * Topic 7 - Polymorphism is the whole design of this class: it is written against
 * Account, and savings and loan behaviour arrives through overridden methods.
 * Topic 12 - Map of account number -> its List of transactions.
 */
public class AccountService implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Repository<Account> accounts = new Repository<Account>("Account");

    /**
     * Topic 12 - The Map Interface with a List value: a one-to-many index.
     * HashMap is correct here because we only ever fetch a known account's history;
     * we never need the accounts in any particular order.
     */
    private final Map<String, List<Transaction>> ledger = new HashMap<String, List<Transaction>>();

    /* ---------- opening accounts ---------- */

    public SavingsAccount openSavings(Member owner, double openingBalance) {
        SavingsAccount account = new SavingsAccount(owner, openingBalance);
        register(account);
        if (openingBalance > 0) {
            post(account, TransactionType.DEPOSIT, openingBalance, "Opening balance");
        }
        return account;
    }

    public LoanAccount openLoan(Member owner, double principal, int termMonths) {
        LoanAccount loan = new LoanAccount(owner, principal, termMonths);
        register(loan);
        post(loan, TransactionType.LOAN_DISBURSEMENT, principal,
                termMonths + "-month loan disbursed");
        return loan;
    }

    /**
     * Topic 7 - the parameter type is the SUPERCLASS, so one method accepts both
     * kinds of account. This is the single most useful thing inheritance buys you.
     */
    private void register(Account account) {
        accounts.add(account);
        ledger.put(account.getAccountNumber(), new ArrayList<Transaction>());
    }

    /** Re-registers an account loaded from disk. */
    public void reinstate(Account account) {
        if (accounts.add(account) && !ledger.containsKey(account.getAccountNumber())) {
            ledger.put(account.getAccountNumber(), new ArrayList<Transaction>());
        }
    }

    /* ---------- lookups ---------- */

    /**
     * Topic 10 - Declaring Exceptions. Returning null here would push the problem
     * onto every caller and invite NullPointerException; throwing makes the failure
     * impossible to ignore and carries the bad account number with it.
     */
    public Account require(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.findById(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    public List<Account> findAll() {
        return accounts.findAll();
    }

    /** All accounts belonging to one member. */
    public List<Account> findByOwner(Member owner) {
        List<Account> owned = new ArrayList<Account>();
        for (Account account : accounts.values()) {
            // Topic 6 - equals, not ==: compares identity by id, not by memory address
            if (account.getOwner().equals(owner)) {
                owned.add(account);
            }
        }
        return owned;
    }

    public int count() {
        return accounts.size();
    }

    /* ---------- money movement ---------- */

    public Transaction deposit(String accountNumber, double amount, String narration)
            throws AccountNotFoundException {
        Account account = require(accountNumber);

        // Topic 7 - Polymorphism: on a SavingsAccount this credits the balance; on a
        // LoanAccount the override turns it into a repayment. Same line of code.
        account.deposit(amount);

        TransactionType type = (account instanceof LoanAccount)
                ? TransactionType.LOAN_REPAYMENT
                : TransactionType.DEPOSIT;
        return post(account, type, amount, narration);
    }

    public Transaction withdraw(String accountNumber, double amount, String narration)
            throws AccountNotFoundException, InsufficientFundsException {
        Account account = require(accountNumber);
        account.withdraw(amount); // SavingsAccount's override enforces the minimum balance
        return post(account, TransactionType.WITHDRAWAL, amount, narration);
    }

    /**
     * Topic 6 - Parameter Passing, demonstrated for real.
     *
     * `from` and `to` are references: the Account objects they point at are genuinely
     * modified, and the caller sees those changes. `amount` is a primitive copy -
     * reassigning it inside this method would change nothing outside. Java is always
     * pass-by-value; with objects, the value being copied is the reference.
     *
     * Topic 10 - a rollback in the catch block, so a failed second leg cannot leave
     * money destroyed.
     */
    public void transfer(String fromAccount, String toAccount, double amount)
            throws AccountNotFoundException, InsufficientFundsException {

        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException("Cannot transfer an account to itself");
        }
        Account source = require(fromAccount);
        Account target = require(toAccount);

        source.withdraw(amount);
        post(source, TransactionType.WITHDRAWAL, amount, "Transfer to " + toAccount);

        try {
            target.deposit(amount);
            post(target, TransactionType.DEPOSIT, amount, "Transfer from " + fromAccount);
        } catch (RuntimeException failure) {
            // Put the money back before letting the problem propagate
            source.deposit(amount);
            post(source, TransactionType.DEPOSIT, amount, "Reversal: transfer failed");
            throw failure;
        }
    }

    /**
     * Topic 7 - Polymorphism + Casting, and Topic 8 - Using Interfaces.
     *
     * Runs the month-end interest cycle over every account. `instanceof` asks whether
     * an object supports the interface, and the cast then lets us call its methods.
     * Savings accounts gain interest; loan accounts are charged it - decided by the
     * object, not by this loop.
     */
    public Map<String, Double> runMonthEndInterest() {
        Map<String, Double> movements = new HashMap<String, Double>();

        for (Account account : accounts.values()) {
            if (account.isClosed()) {
                continue;
            }
            // Topic 7 - Casting, guarded by instanceof so it can never fail
            if (account instanceof InterestBearing) {
                InterestBearing bearing = (InterestBearing) account;
                double movement = bearing.applyMonthlyInterest();

                if (movement != 0.0) {
                    TransactionType type = (account instanceof LoanAccount)
                            ? TransactionType.INTEREST_CHARGE
                            : TransactionType.INTEREST_CREDIT;
                    post(account, type, movement, String.format("Monthly interest @ %.2f%%",
                            bearing.annualRate() * 100));
                    movements.put(account.getAccountNumber(), movement);
                }
            }
        }
        return movements;
    }

    /**
     * Topic 6 - Destroying Objects.
     * Java has no destructor. Once this method removes the last reference the object
     * becomes unreachable and the garbage collector reclaims it whenever it next runs -
     * we cannot and need not say when. Setting `account = null` below is what actually
     * drops our local hold on it; finalize() is deprecated and must not be relied on.
     */
    public void closeAccount(String accountNumber) throws AccountNotFoundException {
        Account account = require(accountNumber);
        account.close();            // business-level close; throws if balance != 0
        accounts.remove(accountNumber);
        ledger.remove(accountNumber);
        account = null;             // last reference gone -> eligible for collection
    }

    /* ---------- the ledger ---------- */

    private Transaction post(Account account, TransactionType type, double amount, String narration) {
        Transaction txn = new Transaction(account.getAccountNumber(), type, amount,
                account.getBalance(), narration);
        List<Transaction> history = ledger.get(account.getAccountNumber());
        if (history == null) {
            history = new ArrayList<Transaction>();
            ledger.put(account.getAccountNumber(), history);
        }
        history.add(txn);
        return txn;
    }

    public void replay(Transaction txn) {
        List<Transaction> history = ledger.get(txn.getAccountNumber());
        if (history == null) {
            history = new ArrayList<Transaction>();
            ledger.put(txn.getAccountNumber(), history);
        }
        history.add(txn);
    }

    public List<Transaction> historyFor(String accountNumber) {
        List<Transaction> history = ledger.get(accountNumber);
        if (history == null) {
            return new ArrayList<Transaction>();
        }
        List<Transaction> copy = new ArrayList<Transaction>(history);
        Collections.sort(copy); // Topic 8 - Transaction.compareTo, oldest first
        return copy;
    }

    /** Every transaction in the SACCO, oldest first. */
    public List<Transaction> allTransactions() {
        List<Transaction> all = new ArrayList<Transaction>();
        // Topic 12 - iterating a Map's values()
        for (List<Transaction> history : ledger.values()) {
            all.addAll(history);
        }
        Collections.sort(all);
        return all;
    }

    /* ---------- reporting ---------- */

    /**
     * Topic 6 - StringBuilder and Format Strings.
     *
     * Built with StringBuilder rather than `report += line` in a loop: string
     * concatenation creates a brand new String object every single time round,
     * so a 500-line statement would allocate 500 throwaway Strings. StringBuilder
     * appends into one growable buffer.
     *
     * StringBuilder (not StringBuffer) is right here because this buffer never
     * leaves the method, so there is no thread to synchronise against.
     */
    public String statement(String accountNumber) throws AccountNotFoundException {
        Account account = require(accountNumber);
        List<Transaction> history = historyFor(accountNumber);

        StringBuilder out = new StringBuilder(512);

        border(out);
        row(out, " ACCOUNT STATEMENT");
        border(out);
        row(out, String.format(" Account : %s  (%s)",
                account.getAccountNumber(), account.accountType()));
        row(out, String.format(" Member  : %s  [%s]",
                account.getOwner().getFullName(), account.getOwner().getId()));
        row(out, String.format(" Balance : %,.2f", account.getBalance()));
        border(out);

        if (history.isEmpty()) {
            row(out, " No transactions yet.");
        } else {
            row(out, String.format(" %-11s %-19s %-3s %13s %15s %s",
                    "REF", "DATE", "TYP", "AMOUNT", "BALANCE", "NOTE"));
            double credits = 0.0;
            double debits = 0.0;

            for (Transaction txn : history) {
                row(out, String.format(" %-11s %-19s %-3s %,13.2f %,15.2f %s",
                        txn.getId(), txn.getFormattedDate(), txn.getType().getCode(),
                        txn.getAmount(), txn.getBalanceAfter(), txn.getNarration()));

                if (txn.getType().isCredit()) {
                    credits += txn.getAmount();
                } else {
                    debits += txn.getAmount();
                }
            }
            border(out);
            row(out, String.format(" %-35s %,13.2f", "Total credits", credits));
            row(out, String.format(" %-35s %,13.2f", "Total debits", debits));
        }
        border(out);
        return out.toString();
    }

    /** Inner width of the statement box, in characters. */
    private static final int BOX_WIDTH = 83;

    private static void border(StringBuilder out) {
        out.append('+');
        for (int i = 0; i < BOX_WIDTH; i++) {
            out.append('-');
        }
        out.append('+').append(System.lineSeparator());
    }

    /**
     * Writes one row padded - or truncated - to exactly BOX_WIDTH, so the right-hand
     * border always lines up however long the narration happens to be.
     */
    private static void row(StringBuilder out, String content) {
        String text = content.length() > BOX_WIDTH
                ? content.substring(0, BOX_WIDTH - 3) + "..."
                : content;
        out.append('|').append(text);
        for (int i = text.length(); i < BOX_WIDTH; i++) {
            out.append(' ');
        }
        out.append('|').append(System.lineSeparator());
    }

    /** Totals across the whole SACCO. */
    public double totalSavings() {
        double total = 0.0;
        for (Account account : accounts.values()) {
            if (account instanceof SavingsAccount) {
                total += account.getBalance();
            }
        }
        return total;
    }

    public double totalLoansOutstanding() {
        double total = 0.0;
        for (Account account : accounts.values()) {
            if (account instanceof LoanAccount) {
                total += account.getBalance();
            }
        }
        return total;
    }
}
