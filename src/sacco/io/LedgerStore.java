package sacco.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sacco.model.Transaction;
import sacco.model.TransactionType;

/**
 * Topic 11 - Binary Input and Output, and Bytes vs. Characters.
 *
 * The ledger is machine-read-only, so it is stored as BINARY rather than text. The
 * chain here is the byte-stream counterpart of ReceiptPrinter's:
 *
 *     FileOutputStream        - raw bytes to disk
 *       -> BufferedOutputStream - batches them, so we are not hitting the disk per field
 *            -> DataOutputStream - writes Java PRIMITIVES in a defined binary layout
 *
 * DataOutputStream/DataInputStream are a matched pair. The read side must pull the
 * same types back in exactly the same ORDER they were written - there are no field
 * names in the file, only a sequence of bytes. Get the order wrong and you do not get
 * an error, you get nonsense, which is the main hazard of binary formats.
 *
 * Why this and not text? A double written with writeDouble is 8 bytes, exact, and
 * needs no parsing. Written as text it is longer, locale-dependent and can lose
 * precision on the round trip.
 */
public class LedgerStore {

    /** Lets us detect a file that is not ours, and refuse to misread it. */
    private static final int MAGIC = 0x53414343; // "SACC" as an int

    private static final int FORMAT_VERSION = 1;

    private final File file;

    public LedgerStore(String path) {
        this.file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Writes the whole ledger.
     *
     * Topic 11 - Closing Streams, done with Java 7's try-with-resources: the stream
     * declared in the parentheses is closed automatically at the end of the block,
     * whether it exits normally or by exception. Closing the outermost stream closes
     * the whole chain. Compare with ReceiptPrinter, which uses an explicit finally.
     */
    public void save(List<Transaction> transactions) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {

            // ---- header ----
            out.writeInt(MAGIC);
            out.writeInt(FORMAT_VERSION);
            out.writeInt(transactions.size());

            // ---- records ----
            for (Transaction txn : transactions) {
                // writeUTF writes a 2-byte length then the UTF-8 bytes, so readUTF
                // always knows where the String ends
                out.writeUTF(txn.getId());
                out.writeUTF(txn.getAccountNumber());
                out.writeUTF(txn.getType().name()); // the enum's constant name
                out.writeDouble(txn.getAmount());
                out.writeDouble(txn.getBalanceAfter());
                out.writeLong(txn.getTimestamp());
                out.writeUTF(txn.getNarration());
            }
            out.flush();
        }
        // no finally needed: try-with-resources closed the chain for us
    }

    /**
     * Reads the ledger back, in the same field order it was written.
     */
    public List<Transaction> load() throws IOException {
        List<Transaction> transactions = new ArrayList<Transaction>();

        if (!file.exists() || file.length() == 0) {
            return transactions; // nothing saved yet - not an error
        }

        DataInputStream in = null;
        try {
            in = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(file)));

            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IOException(file.getName() + " is not a SACCO ledger file");
            }
            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("Ledger format v" + version + " is not supported");
            }
            int count = in.readInt();

            // Topic 4 - a classic counted for loop; the header told us how many
            for (int i = 0; i < count; i++) {
                String id = in.readUTF();
                String accountNumber = in.readUTF();
                // Enum.valueOf turns the stored name back into the constant
                TransactionType type = TransactionType.valueOf(in.readUTF());
                double amount = in.readDouble();
                double balanceAfter = in.readDouble();
                long timestamp = in.readLong();
                String narration = in.readUTF();

                transactions.add(new Transaction(id, accountNumber, type, amount,
                        balanceAfter, timestamp, narration));
            }
        } catch (EOFException eof) {
            /*
             * Topic 10 - Catching Exceptions, and a real lesson about binary streams:
             * EOFException means the file ended mid-record - it was truncated. We keep
             * the records we managed to read rather than losing the lot.
             */
            System.err.println("Warning: ledger file is truncated; recovered "
                    + transactions.size() + " record(s)");
        } catch (IllegalArgumentException bad) {
            // valueOf() throws this for a transaction code the enum no longer has
            throw new IOException("Ledger contains an unknown transaction type", bad);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return transactions;
    }

    /** Topic 11 - File Object: querying the path without opening a stream. */
    public String describe() {
        if (!file.exists()) {
            return file.getPath() + " (not created yet)";
        }
        return String.format("%s (%,d bytes, %s)", file.getAbsolutePath(), file.length(),
                file.canWrite() ? "writable" : "read-only");
    }

    public File getFile() {
        return file;
    }
}
