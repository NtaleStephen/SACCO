package sacco.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import sacco.exception.InvalidAmountException;
import sacco.model.MemberRole;
import sacco.util.Money;

/**
 * All keyboard input and screen output for the application.
 *
 * Topic 11 - System.in is an InputStream: a stream of BYTES. Wrapping it in an
 * InputStreamReader converts those bytes to characters, and BufferedReader then gives
 * us readLine(). Exactly the same bridge MemberImporter uses on a file - the only
 * difference is where the bytes come from, which is the point of streams.
 *
 * Topic 6 - every piece of output here goes through printf / format strings so the
 * columns line up.
 */
public class ConsoleMenu {

    private static final int WIDTH = 78;

    private final BufferedReader input;

    public ConsoleMenu() {
        this.input = new BufferedReader(new InputStreamReader(System.in));
    }

    /* ---------- output ---------- */

    /**
     * Topic 6 - StringBuilder, and Topic 4 - a loop building a repeated String.
     */
    public void banner(String title) {
        StringBuilder bar = new StringBuilder(WIDTH);
        for (int i = 0; i < WIDTH; i++) {
            bar.append('=');
        }
        System.out.println();
        System.out.println(bar);
        // %-Ns left-aligns in N columns; %n is the portable newline (prefer it to \n)
        System.out.printf("  %s%n", title.toUpperCase());
        System.out.println(bar);
    }

    public void rule() {
        StringBuilder bar = new StringBuilder(WIDTH);
        for (int i = 0; i < WIDTH; i++) {
            bar.append('-');
        }
        System.out.println(bar);
    }

    public void println(String message) {
        System.out.println(message);
    }

    public void blank() {
        System.out.println();
    }

    /** Topic 6 - varargs passed straight through to printf. */
    public void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    public void success(String message) {
        System.out.printf("  [OK]    %s%n", message);
    }

    public void warn(String message) {
        System.out.printf("  [!]     %s%n", message);
    }

    /** Errors go to System.err, which is a separate stream from System.out. */
    public void error(String message) {
        System.err.printf("  [ERROR] %s%n", message);
    }

    /**
     * Topic 4 - Arrays and the Enhanced for Loop.
     * The menu is data, not code: adding an entry means adding a String to the array.
     */
    public void showMenu(String title, String[] options) {
        banner(title);
        int number = 1;
        for (String option : options) {
            if (option == null || option.isEmpty()) {
                System.out.println();
            } else {
                System.out.printf("   %2d. %s%n", number, option);
            }
            number++;
        }
        System.out.printf("    0. %s%n", "Back / Exit");
        rule();
    }

    /* ---------- input ---------- */

    /**
     * Topic 10 - Catching Exceptions. readLine() throws IOException, which cannot
     * sensibly happen on a keyboard, so it is converted to an unchecked exception
     * rather than forcing every caller to wrap every prompt in try/catch.
     */
    public String readLine(String prompt) {
        System.out.printf("  %s: ", prompt);
        try {
            String line = input.readLine();
            return line == null ? "" : line.trim();
        } catch (IOException ioe) {
            throw new IllegalStateException("Console input failed", ioe);
        }
    }

    public String readRequired(String prompt) {
        // Topic 4 - a do-while loop: the body must run at least once, which is exactly
        // what "keep asking until they type something" needs.
        String value;
        do {
            value = readLine(prompt);
            if (value.isEmpty()) {
                warn("This field cannot be left blank.");
            }
        } while (value.isEmpty());
        return value;
    }

    /**
     * Topic 6 - The Primitive-Type Wrapper Classes.
     * Integer.parseInt is the bridge from text to the int primitive, and
     * NumberFormatException is how it reports failure.
     */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String raw = readLine(prompt);
            try {
                int value = Integer.parseInt(raw);
                if (value < min || value > max) {
                    warn("Please enter a number between " + min + " and " + max + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException nfe) {
                warn("'" + raw + "' is not a whole number.");
            }
        }
    }

    /** Keeps asking until a valid amount is typed. */
    public double readAmount(String prompt) {
        while (true) {
            String raw = readLine(prompt + " (" + Money.CURRENCY + ")");
            try {
                return Money.parse(raw);
            } catch (InvalidAmountException iae) {
                // Topic 10 - Exception Methods: getMessage() carries the explanation
                warn(iae.getMessage());
            }
        }
    }

    /** Topic 4 - Switch Statements on a String (legal from Java 7 onwards). */
    public boolean confirm(String prompt) {
        while (true) {
            String answer = readLine(prompt + " [y/n]").toLowerCase();
            switch (answer) {
                case "y":
                case "yes":
                    return true;
                case "n":
                case "no":
                    return false;
                default:
                    warn("Please answer y or n.");
                    break; // falls out of the switch, back round the while loop
            }
        }
    }

    /** Lets the user pick a role from the enum's own values. */
    public MemberRole readRole() {
        MemberRole[] roles = MemberRole.values(); // Topic 4 - Arrays
        blank();
        for (int i = 0; i < roles.length; i++) {
            System.out.printf("   %d. %-14s (loan ceiling %s)%n",
                    i + 1, roles[i].getLabel(), Money.format(roles[i].getLoanCeiling()));
        }
        int choice = readInt("Role", 1, roles.length);
        return roles[choice - 1];
    }

    /** Prints a numbered list and returns the chosen index, or -1 for cancel. */
    public int chooseFrom(String title, List<String> rows) {
        if (rows.isEmpty()) {
            warn("Nothing to choose from.");
            return -1;
        }
        blank();
        println("  " + title);
        rule();
        for (int i = 0; i < rows.size(); i++) {
            System.out.printf("   %2d. %s%n", i + 1, rows.get(i));
        }
        rule();
        int choice = readInt("Select (0 to cancel)", 0, rows.size());
        return choice == 0 ? -1 : choice - 1;
    }

    public void pause() {
        readLine("Press Enter to continue");
    }

    /** Topic 11 - Closing Streams, even for console input. */
    public void close() {
        try {
            input.close();
        } catch (IOException ignored) {
            // the program is ending anyway
        }
    }
}
