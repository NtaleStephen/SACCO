# Syllabus coverage map

**COS 3103 — Application Development by Java**
Every topic and sub-topic from the course outline, mapped to the file that demonstrates it.

Open any file in Eclipse and use `Ctrl+O` (Mac: `Cmd+O`) to jump to a member by name.

---

## 1. Introduction to Java Programming — 1 hr

Course administration only (objectives, overview, workbook, references). Nothing to
implement; see `README.md` for the project's own overview and how to get started.

## 2. Getting Started with Java — 1 hr

| Sub-topic | Where |
|---|---|
| What is Java? / How to get Java | `README.md` → "Requirements" |
| A First Java Program | `src/sacco/app/SaccoApp.java` → `main(String[] args)` |
| Compiling and Interpreting Applications | `build.sh` — the header comment explains `javac` vs `java` |
| The JSDK Directory Structure | `README.md` → "Project layout", and `build.sh` → `-d bin` |

## 3. Eclipse — 2 hrs

| Sub-topic | Where |
|---|---|
| Setting up a Project / Importing Existing Java Code | `.project` and `.classpath` — *File → Import → Existing Projects into Workspace* |
| Creating a New Java Application | `src/` uses Eclipse's standard `src` → `bin` layout |
| Running a Java Application | Run configuration: main class `sacco.app.SaccoApp`; see `README.md` |
| Debugging a Java Application | `README.md` → "Suggested breakpoints" lists four places worth stepping through |
| Editors, Views, and Perspectives | Tool usage, not code |

## 4. Language Fundamentals — 2 hrs

| Sub-topic | Where |
|---|---|
| A Java Program | `SaccoApp.main` — the `public static void main(String[])` signature |
| **If Statements** | `util/Validator.java` → `requirePositiveAmount`, `isValidPhone` |
| **Switch Statements** | `SaccoApp.run` (switch on `int`), `ConsoleMenu.confirm` (switch on `String`, Java 7+), `MemberRole.canApproveLoans` (switch on an `enum`) |
| **Loop Statements** | `do-while`: every `*Menu()` in `SaccoApp`, and `ConsoleMenu.readRequired`<br>`while`: `MemberImporter.importFrom` (`while ((line = reader.readLine()) != null)`)<br>`for`: `LedgerStore.load`, `ConsoleMenu.banner`<br>reverse `for`: `SaccoApp.describeFiles` |
| Syntax Details | Throughout — braces, semicolons, Javadoc comments |
| **Primitive Datatypes** | `double balance` (`model/Account.java`), `long timestamp` (`model/Transaction.java`), `char c` (`Validator.isValidNationalId`), `int`, `boolean` |
| Variables | Local, instance and `static` — contrasted in `model/Member.java` |
| **Expressions in Java** | `LoanApplication.riskScore` (arithmetic + ternary), `Validator.requirePositiveAmount` (`||`) |
| **Strings** | `Validator` (`trim`, `isEmpty`, `charAt`, `length`, `substring`, `equalsIgnoreCase`), `MemberImporter` (`split`, `startsWith`) |
| **Arrays** | `Validator.VALID_PHONE_PREFIXES`, the `String[]` menus in `SaccoApp`, `MemberRole.values()`, `String[] fields = line.split(",")` |
| **Enhanced for Loop** | `Validator.isValidPhone`, `SaccoApp.listAccounts`, `MemberService.uniqueSurnames` |

## 5. Objects and Classes — 2 hrs

| Sub-topic | Where |
|---|---|
| **Defining a Class** | `model/Member.java` — the fullest example |
| **Creating an Object Instance** | `SaccoApp.main` → `new SaccoApp()`; `AccountService.openSavings` → `new SavingsAccount(...)` |
| **Instance Data and Class Data** | `model/Member.java` — instance fields (`fullName`, `phone`) vs the `static memberCounter` shared by all members |
| Methods | Throughout |
| **Constructors** | `model/Member.java` — overloaded, with the 3-arg one delegating via `this(...)`; `model/Account.java` — `protected`, called with `super(...)` |
| **Access Modifiers** | `public` (API), `private` (every field), `protected` (`Account.balance`, `Entity`'s constructor), package-private (`Member.attachAccount`) |
| **Encapsulation** | `model/Transaction.java` — every field `private final`, no setters, so a posted ledger entry can never be altered. Also `Member.getAccountNumbers()`, which returns a *copy* |

## 6. Using Java Objects — 3 hrs

| Sub-topic | Where |
|---|---|
| Printing to the Console | `app/ConsoleMenu.java` — `System.out` for normal output, `System.err` for errors |
| **printf / Format Strings** | `ConsoleMenu.printf`, `io/ReceiptPrinter.java`, `util/Money.format` (`%,.2f`), `Member.nextMemberId` (`%04d`) |
| **StringBuilder and StringBuffer** | `AccountService.statement` uses `StringBuilder`; `SaccoApp.summaryReport` uses `StringBuffer` — the comment on each explains why (synchronized vs not) |
| Methods and Messages | Throughout |
| **toString** | `Entity`, `Member`, `Account`, `Transaction`, `MemberRole` — each overrides it differently |
| **Parameter Passing** | `AccountService.transfer` — the comment walks through why objects appear to change and primitives do not. Also `Account.deposit` |
| **Comparing and Identifying Objects** | `Entity.equals` / `Entity.hashCode`, and `MemberService.findByNationalId` + `AccountService.findByOwner`, which use `equals` rather than `==` |
| **Destroying Objects** | `AccountService.closeAccount` and `Account.close` — no destructor in Java; dropping the last reference is what makes an object collectable |
| **The Primitive-Type Wrapper Classes** | `Money.parse` (`Double.parseDouble`), `ConsoleMenu.readInt` (`Integer.parseInt`), `SaccoState.numericSuffix`, and auto-unboxing in `SaccoApp.runInterest` (`Map<String, Double>` → `double`) |
| **Enumerated Types** | `model/MemberRole.java`, `model/TransactionType.java`, `model/LoanStatus.java` — all three carry per-constant fields and methods |

## 7. Inheritance in Java — 3 hrs

| Sub-topic | Where |
|---|---|
| **Inheritance / Inheritance in Java** | `Entity` → `Account` → `SavingsAccount`, a three-level chain |
| **Casting** | `AccountService.runMonthEndInterest` (`(InterestBearing) account`), `SaccoApp.repayLoan` (`(LoanAccount) chosen`), `StateStore.load` (the unavoidable downcast of `readObject()`) — each guarded by `instanceof` |
| **Method Overriding** | `SavingsAccount.withdraw` (adds a rule, then calls `super`), `LoanAccount.deposit` (replaces the meaning entirely), `LoanAccount.withdraw` (refuses) |
| **Polymorphism** | `AccountService` is written against `Account` throughout. `SaccoApp.listAccounts` is one loop with no `instanceof` — each object prints itself correctly. `applyMonthlyInterest` **adds** on savings and **charges** on loans from the same call site |
| **super** | `SavingsAccount.withdraw` → `super.withdraw`, `SavingsAccount.summary` → `super.summary`, every subclass constructor → `super(...)` |
| **The Object Class** | `Entity` overrides `toString`, `equals`, `hashCode`, and calls the inherited `getClass()` |

## 8. Advanced Inheritance and Generics — 3 hrs

| Sub-topic | Where |
|---|---|
| **Abstract Classes** | `model/Entity.java` (abstract `summary()`), `model/Account.java` (abstract `accountType()`, `withdrawableBalance()`) |
| **Interfaces** | `model/InterestBearing.java`, including an interface constant (`REGULATORY_FLOOR`) |
| **Using Interfaces** | `SavingsAccount` and `LoanAccount` both implement it — the thing single inheritance could not have given them. `AccountService.runMonthEndInterest` programs against the interface, not the classes |
| Collections | See Topic 12 |
| **Generics** | `service/Repository.java` — `Repository<T extends Entity>`, a bounded type parameter, reused for `Member`, `Account` and `LoanApplication`. The class comment explains what the bound buys and what pre-generics code had to do instead. Also the generic *method* `Repository.sorted(List<E>)` |
| **Comparable** | `Member.compareTo` (by name), `Transaction.compareTo` (by time), `LoanApplication.compareTo` (by risk — this one drives the `PriorityQueue`). `MemberService.sortedByJoinDate` adds a `Comparator` for contrast |

## 9. Packages — 3 hrs

| Sub-topic | Where |
|---|---|
| **Packages / Defining Packages** | Six of them: `sacco.app`, `sacco.model`, `sacco.service`, `sacco.io`, `sacco.exception`, `sacco.util` — each file opens with its `package` declaration |
| **The import Statement** | Every file; `SaccoApp.java` has the longest list |
| **Static Imports** | `SaccoApp.java` → `import static sacco.util.Money.format;` and `...Money.CURRENCY;`, used as bare `format(x)` and `CURRENCY` |
| **CLASSPATH and Import** | `build.sh` → `-cp bin`, with the comment on why `sacco.app.SaccoApp` is a class name and not a file path |
| **Package Scope** | `Member.attachAccount` / `Member.detachAccount` and the `Member.accountNumbers` field have **no** modifier. `Account`'s constructor can call them because it is in `sacco.model`; `AccountService` in `sacco.service` cannot |

## 10. Exception Handling — 3 hrs

| Sub-topic | Where |
|---|---|
| Exceptions Overview | `src/sacco/exception/` — a four-class hierarchy under `SaccoException` |
| **Catching Exceptions** | `SaccoApp.registerMember` (most-specific-first), `SaccoApp.doWithdraw` (three different types) |
| multi-catch (Java 7) | `StateStore.load`, `SaccoApp.openSavings`, `SaccoApp.applyForLoan` |
| **The finally Block** | `ReceiptPrinter.printReceipt`, `LedgerStore.load`, `StateStore.save`, `MemberImporter.importFrom`, `SaccoApp.main` |
| **Exception Methods** | `getMessage()` everywhere; `getCause()` in `SaccoApp.saveEverything`; `printStackTrace()` in `SaccoApp.main`; and `InsufficientFundsException.getShortfall()` — a custom exception carrying **data**, used in `SaccoApp.doWithdraw` |
| **Declaring Exceptions** | `AccountService.require` → `throws AccountNotFoundException`; `Account.withdraw` → `throws InsufficientFundsException` |
| **Defining and Throwing Exceptions** | All four classes in `src/sacco/exception/` |
| **Errors and RuntimeExceptions** | `InvalidAmountException extends RuntimeException` — its class comment explains why it is unchecked while `InsufficientFundsException` is checked. Also `UnsupportedOperationException` in `LoanAccount.withdraw` and `IllegalStateException` in `Account.close` |
| Catching the base type | `SaccoApp.doTransfer` catches `SaccoException` once and handles both subclasses — polymorphism applied to exceptions |

## 11. Input/Output Streams — 4 hrs

| Sub-topic | Where |
|---|---|
| **Overview of Streams** | `src/sacco/io/StateStore.java` — the class comment compares all three storage styles used in the project |
| **Bytes vs. Characters** | `ReceiptPrinter` (character) against `LedgerStore` (byte), with the reasoning in each header |
| **Converting Byte Streams to Character Streams** | `MemberImporter.importFrom` — `FileInputStream` → `InputStreamReader` → `BufferedReader`, with the comment on why the bridge is unavoidable. Same bridge on `System.in` in `ConsoleMenu`'s constructor |
| **File Object** | `ReceiptPrinter` (`mkdirs`, `new File(dir, name)`), `SaccoApp.describeFiles` (`exists`, `length`, `canWrite`, `listFiles`, `getAbsolutePath`) |
| **Binary Input and Output** | `io/LedgerStore.java` — `DataOutputStream`/`DataInputStream` with `writeUTF`/`writeDouble`/`writeLong`, a magic number and a format version |
| **PrintWriter Class** | `io/ReceiptPrinter.java`, including `checkError()` — the only way to learn that a `PrintWriter` write failed |
| **Reading and Writing Objects** | `io/StateStore.java` + `io/SaccoState.java` — one `writeObject` saves the whole object graph; `serialVersionUID` and `transient` both explained in context |
| **Closing Streams** | Explicit `finally` in `ReceiptPrinter`, `LedgerStore.load`, `StateStore`, `MemberImporter`; try-with-resources in `LedgerStore.save` for comparison |

## 12. Core Collection Classes — 4 hrs

| Sub-topic | Where | Why that one |
|---|---|---|
| **The Collections Framework** | Used across `sacco.service` | — |
| **The Set Interface** | `MemberService.registeredNationalIds`, `MemberService.uniqueSurnames` | — |
| **Set Implementation Classes** | `HashSet` → `registeredNationalIds`<br>`TreeSet` → `uniqueSurnames` | `HashSet` answers "seen this before?" in constant time and order is irrelevant. `TreeSet` keeps the surname roll call alphabetical for free |
| **The List Interface** | Return type of nearly every `findAll` / report method | — |
| **List Implementation Classes** | `ArrayList` → `Repository.findAll`, the ledger's per-account history<br>`LinkedList` → `LoanService.disbursementQueue` | `ArrayList` for indexed reads; `LinkedList` where only the two ends are touched |
| **The Queue Interface** | `LoanService.reviewQueue`, `LoanService.disbursementQueue` — both declared as `Queue<LoanApplication>` | Programming to the interface; either implementation could be swapped in |
| **Queue Implementation Classes** | `PriorityQueue` → review queue<br>`LinkedList` → disbursement queue | The committee should see the **lowest-risk** application first regardless of typing order (`PriorityQueue` + `LoanApplication.compareTo`). Payouts must be **strictly first-come-first-served** (`LinkedList`) |
| `offer` / `poll` / `peek` | `LoanService.submit`, `takeNextForReview`, `peekNext` | The null-returning forms, contrasted with the throwing `add`/`remove`/`element` in the comments |
| PriorityQueue iteration order | `LoanService.reviewOrder` | Iterating a `PriorityQueue` does **not** give sorted order — only the head is guaranteed. Draining a copy with `poll()` is the fix, and the comment says so |
| **The Map Interface** | `Repository.items`, `AccountService.ledger`, `MemberService.groupByRole` | — |
| **Map Implementation Classes** | `LinkedHashMap` → `Repository`<br>`HashMap` → the ledger, interest results<br>`EnumMap` → `groupByRole`<br>`TreeSet`-backed sorting elsewhere | `LinkedHashMap` keeps listings stable between runs; `HashMap` where order never matters; `EnumMap` because the keys are enum constants, making it an array lookup internally |
| Iterating a Map | `SaccoApp.membersByRole`, `SaccoApp.runInterest` — `entrySet()` | One pass for key *and* value, rather than `keySet()` plus a `get()` per entry |

## Practical Session — 30 hrs

The application itself. `README.md` → "A guided walkthrough" is a ten-minute script
that exercises every topic above in order, and the `--demo` flag seeds data so the
loan queue, polymorphic interest run and all three file formats can be shown without
typing anything in first.
