package sacco.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Topic 11 - Reading and Writing Objects (Java object serialization).
 *
 * ObjectOutputStream.writeObject() saves an entire object graph in one call, and
 * ObjectInputStream.readObject() rebuilds it. Compare the three storage styles in
 * this package, which is the real lesson of Topic 11:
 *
 *   ReceiptPrinter -> character stream, human-readable, any program can read it
 *   LedgerStore    -> byte stream of primitives, compact and fast, needs a reader
 *                     that knows the field order
 *   StateStore     -> object stream, saves structure and references for free, but the
 *                     file is only readable by Java classes of a matching version
 *
 * Serialization's catch: readObject() returns Object, so the result must be cast, and
 * it can throw ClassNotFoundException if the class is missing from the classpath.
 */
public class StateStore {

    private final File file;

    public StateStore(String path) {
        this.file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Saves to a temporary file first, then renames. If the JVM dies halfway through,
     * the previous good save is still intact instead of half-overwritten.
     */
    public void save(SaccoState state) throws IOException {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");

        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(temp)));

            out.writeObject(state); // one call; the whole graph follows
            out.flush();
        } finally {
            // Topic 10 - The finally Block; Topic 11 - Closing Streams
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                    // A failure to close must not mask a failure to write
                }
            }
        }

        if (file.exists() && !file.delete()) {
            throw new IOException("Could not replace " + file.getPath());
        }
        if (!temp.renameTo(file)) {
            throw new IOException("Could not rename " + temp.getName() + " to " + file.getName());
        }
    }

    /**
     * Loads saved state, or returns null when there is nothing to load.
     *
     * Topic 10 - Catching Exceptions with multi-catch (Java 7): one block handles two
     * unrelated exception types that need the same treatment, without resorting to
     * `catch (Exception e)` which would also swallow bugs we want to see.
     */
    public SaccoState load() throws IOException {
        if (!file.exists() || file.length() == 0) {
            return null;
        }

        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(file)));

            // Topic 7 - Casting. readObject() is declared to return Object, so this
            // downcast is unavoidable; it throws ClassCastException if the file holds
            // something else. Generics cannot help here - the type is not in the file.
            Object raw = in.readObject();
            if (!(raw instanceof SaccoState)) {
                throw new IOException("Save file does not contain SACCO state");
            }
            return (SaccoState) raw;

        } catch (InvalidClassException ice) {
            // The classes changed since this file was written (serialVersionUID clash)
            throw new IOException("Save file was written by an incompatible version", ice);
        } catch (ClassNotFoundException | ClassCastException problem) {
            // Topic 10 - multi-catch; `problem` is the nearest common supertype
            throw new IOException("Save file could not be understood", problem);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // nothing useful to do
                }
            }
        }
    }

    public boolean hasSavedState() {
        return file.exists() && file.length() > 0;
    }

    public String describe() {
        if (!file.exists()) {
            return file.getPath() + " (no save yet)";
        }
        return String.format("%s (%,d bytes)", file.getAbsolutePath(), file.length());
    }
}
