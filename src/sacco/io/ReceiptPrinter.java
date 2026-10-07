package sacco.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import sacco.model.Account;
import sacco.model.Transaction;

/**
 * Topic 11 - PrintWriter Class, and Bytes vs. Characters.
 *
 * A receipt is human-readable text, so a CHARACTER stream is the right family.
 * The chain built below is the standard one worth knowing:
 *
 *     FileOutputStream      - a byte stream; writes raw bytes to the file
 *       -> OutputStreamWriter - the BRIDGE; converts characters to bytes using a
 *                               named charset (Topic 11: "Converting Byte Streams to
 *                               Character Streams", in the writing direction)
 *            -> PrintWriter   - adds println/printf and never throws IOException
 *
 * Naming the charset ("UTF-8") matters: the no-argument constructor silently uses
 * the platform default, so a file written in one place can be unreadable in another.
 */
public class ReceiptPrinter {

    /** Topic 11 - File Object. A File is a PATH; it is not the file's contents. */
    private final File directory;

    public ReceiptPrinter(String directoryPath) {
        this.directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs(); // create the folder tree if it is missing
        }
    }

    /**
     * Writes one receipt and returns the File it created.
     * Topic 10 - Declaring Exceptions: IOException is checked, so it is declared.
     */
    public File printReceipt(Account account, Transaction txn) throws IOException {
        File target = new File(directory, txn.getId() + ".txt");

        /*
         * Topic 11 - Closing Streams, and Topic 10 - The finally Block.
         *
         * The writer is declared OUTSIDE the try so finally can still see it, and the
         * null check matters because the constructor itself can fail. This is the
         * pattern the course teaches; Java 7's try-with-resources (used in
         * LedgerStore) is the shorter modern equivalent.
         */
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(target), "UTF-8"));

            writer.println("=========================================");
            writer.println("        VILLAGE SACCO - RECEIPT          ");
            writer.println("=========================================");

            // Topic 6 - printf / Format Strings
            writer.printf("Receipt No : %s%n", txn.getId());
            writer.printf("Date       : %s%n", txn.getFormattedDate());
            writer.printf("Account    : %s (%s)%n", account.getAccountNumber(), account.accountType());
            writer.printf("Member     : %s%n", account.getOwner().getFullName());
            writer.printf("Member ID  : %s%n", account.getOwner().getId());
            writer.println("-----------------------------------------");
            writer.printf("Type       : %s%n", txn.getType());
            writer.printf("Amount     : %,15.2f%n", txn.getAmount());
            writer.printf("Balance    : %,15.2f%n", txn.getBalanceAfter());
            writer.println("-----------------------------------------");
            writer.printf("Note       : %s%n", txn.getNarration());
            writer.println();
            writer.println("Served by the Village SACCO system");
            writer.println("Keep this receipt. Thank you.");
            writer.println("=========================================");

            /*
             * PrintWriter swallows IOException rather than throwing it - so the ONLY
             * way to find out a write actually failed is to ask it.
             */
            if (writer.checkError()) {
                throw new IOException("Failed while writing receipt " + target.getPath());
            }
        } catch (UnsupportedEncodingException uee) {
            // Every JVM is required to support UTF-8, so this should be unreachable
            throw new IOException("UTF-8 is unavailable on this JVM", uee);
        } finally {
            // finally runs whether we returned normally, threw, or broke out early
            if (writer != null) {
                writer.close(); // flushes the buffer; skipping this can leave a 0-byte file
            }
        }
        return target;
    }

    /** Writes a pre-built report (e.g. a statement) to a named text file. */
    public File writeReport(String fileName, String content) throws IOException {
        File target = new File(directory, fileName);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(target), "UTF-8"));
            writer.print(content);
            if (writer.checkError()) {
                throw new IOException("Failed while writing " + target.getPath());
            }
        } catch (UnsupportedEncodingException uee) {
            throw new IOException("UTF-8 is unavailable on this JVM", uee);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return target;
    }

    public File getDirectory() {
        return directory;
    }
}
