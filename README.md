# Village SACCO — Savings and Credit Co-operative management system

A console application for a village savings-and-credit group: it registers members,
runs their savings accounts, queues loan applications for the committee, charges and
pays interest, and keeps everything on disk in three different file formats.

Written for **COS 3103 — Application Development by Java**. It is built so that every
topic in the course outline appears in code that has a real reason to exist, rather
than in isolated demo snippets. `COVERAGE.md` maps all 12 topics to the exact file and
method that demonstrates each one.

No external libraries — only the JDK.

---

## Requirements

- A JDK, version 7 or later (developed and tested on Temurin 17)
- Eclipse, if you want the IDE workflow the course uses

Check what you have:

```sh
javac -version
java -version
```

If `javac` is missing you have a JRE but not a JDK. On macOS:

```sh
brew install --cask temurin        # asks for your password
```

## Running it

### From the command line

```sh
./build.sh compile      # javac → bin/
./build.sh run          # java  → starts the app
./build.sh demo         # compile and run with sample data
./build.sh clean        # delete bin/
```

Or the two commands by hand, which is what `build.sh` wraps:

```sh
javac -d bin $(find src -name "*.java")
java -cp bin sacco.app.SaccoApp --demo
```

`--demo` seeds four members, four savings accounts and three loan applications, so
there is something to look at immediately. Without it, the app starts empty unless it
finds a saved state file.

### From Eclipse

1. **File → Import → General → Existing Projects into Workspace**
2. Choose this folder as the root directory, then **Finish**
3. Right-click `SaccoApp.java` → **Run As → Java Application**
4. For the sample data: **Run → Run Configurations → Arguments** → put `--demo` in
   *Program arguments*

The project ships `.project` and `.classpath`, so Eclipse recognises it as a Java
project with `src/` as source and `bin/` as output.

## Project layout

```
javaApp/
├── build.sh                  compile/run without Eclipse
├── COVERAGE.md               syllabus topic → code map
├── README.md
├── .project / .classpath     Eclipse project metadata
├── src/
│   └── sacco/
│       ├── app/              SaccoApp (main), ConsoleMenu
│       ├── model/            Member, Account, SavingsAccount, LoanAccount,
│       │                     Transaction, LoanApplication, Entity, 3 enums,
│       │                     InterestBearing
│       ├── service/          Repository<T>, MemberService, AccountService,
│       │                     LoanService
│       ├── io/               ReceiptPrinter, LedgerStore, StateStore,
│       │                     MemberImporter, SaccoState
│       ├── exception/        SaccoException + 4 subclasses
│       └── util/             Money, Validator
├── bin/                      compiled .class files (mirrors the packages)
└── data/                     created at runtime
    ├── sacco.ser             serialized object state
    ├── ledger.dat            binary transaction ledger
    ├── members.csv           optional bulk-import file
    └── receipts/             one .txt per receipt, plus saved statements
```

The six packages split the application the way the course describes: the UI knows
about the services, the services know about the model, and the model knows about
nothing but itself.

## What it does

**Members** — register with a validated national ID (14 alphanumeric characters) and
phone number; four roles, each with its own loan ceiling; listings sorted by name or
by join date; grouping by role.

**Accounts** — savings accounts with a minimum balance and a 7% annual rate; deposits,
withdrawals, transfers with rollback if the second leg fails; printed statements; a
month-end interest run; account closure.

**Loans** — applications are risk-scored and queued. The committee reviews the
**lowest-risk** application first; approved loans are then paid out in **strictly the
order they were approved**. Disbursement opens a loan account at 18% a year, which
accrues interest *against* the member and is paid down by repayments.

**Files** — three formats, deliberately, because they are good for different things:

| File | Format | Written by | Good for |
|---|---|---|---|
| `receipts/*.txt` | character stream | `PrintWriter` | humans; any program can read it |
| `ledger.dat` | binary primitives | `DataOutputStream` | compact, exact, fast |
| `sacco.ser` | Java serialization | `ObjectOutputStream` | saves the whole object graph in one call |

`ledger.dat` starts with a magic number and a version, so the app refuses to misread a
file that is not its own, and recovers what it can from a truncated one.

## A guided walkthrough

About ten minutes, and it touches every course topic. Start with `./build.sh demo`.

1. **Accounts → 6 (List all accounts)** — four savings accounts. One loop printing
   both account types correctly is *polymorphism*; each line comes from an overridden
   `summary()`.
2. **Loans → 2 (View the review queue)** — three applications. Note they come back
   in **risk order**, not the order they were submitted: that is the `PriorityQueue`
   using `LoanApplication.compareTo`.
3. **Loans → 3 (Review the next application)** — Grace's small, low-risk request is
   offered first. Approve it. It moves to the FIFO disbursement queue.
4. **Loans → 4 (Disburse)** — a `LoanAccount` is created. List the accounts again:
   the new row shows a loan, with a term and an instalment, from the same loop as
   before.
5. **Accounts → 7 (Run month-end interest)** — one call to `applyMonthlyInterest()`
   per account. Savings balances go **up**; the loan balance goes **up too** — because
   interest is being charged against it. Same method name, opposite meaning, decided
   by the object.
6. **Accounts → 3 (Withdraw)**, pick the loan account — refused outright, because
   `LoanAccount.withdraw` overrides the inherited behaviour and throws.
7. **Accounts → 3 (Withdraw)**, pick a savings account, ask for far too much — the
   error tells you exactly how short you are, because
   `InsufficientFundsException` carries the figures and not just a message.
8. **Accounts → 5 (Print a statement)** — built with a `StringBuilder`, then saved
   through a `PrintWriter`. Open `data/receipts/` to see the file.
9. **Files → 1 (Save everything)** — writes both `sacco.ser` and `ledger.dat`.
10. Quit, restart, and accept "Saved data was found. Load it?" — one `readObject()`
    call restores every member, account and transaction with all their references
    intact. New member IDs carry on from where they left off rather than restarting
    at `MBR-0001`.
11. **Files → 4, then 3** — writes a sample CSV and imports it. The file contains one
    deliberately broken row; the import reports it and keeps going.
12. **Reports → 4 (Unique surnames)** — a `TreeSet`: sorted, de-duplicated, for free.

## Suggested breakpoints

For Topic 3's debugging session, these four are the ones worth stepping through:

| File | Method | What to watch |
|---|---|---|
| `service/AccountService.java` | `runMonthEndInterest` | Step **into** `applyMonthlyInterest()` and watch the debugger land in `SavingsAccount` or `LoanAccount` depending on the object — polymorphic dispatch, visible |
| `service/LoanService.java` | `takeNextForReview` | Inspect `reviewQueue` before the `poll()`. The internal array is a heap, not a sorted list — only element 0 is guaranteed |
| `model/SavingsAccount.java` | `withdraw` | Step over `super.withdraw(amount)` and watch `balance` change in the inherited method |
| `io/LedgerStore.java` | `load` | Watch each `readUTF`/`readDouble` consume the stream in the same order `save` wrote it |

## Known limitations

Deliberate, so the code stays on the syllabus:

- **Money is stored as `double`.** Correct for a teaching project and keeps the focus
  on the course topics, but production financial code should use `BigDecimal`, since
  binary floating point cannot represent every decimal fraction exactly. Every
  arithmetic result is rounded to two decimal places to stop the error accumulating.
- **Single user, no concurrency.** The ID counters are `synchronized` and the report
  buffer in `summaryReport` is a `StringBuffer`, but nothing else is thread-safe.
- **No database.** Persistence is flat files, which is what Topic 11 covers.
- **Interest is a flat monthly calculation**, not a proper amortisation schedule.
