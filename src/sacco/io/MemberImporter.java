package sacco.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import sacco.exception.DuplicateMemberException;
import sacco.model.Member;
import sacco.model.MemberRole;
import sacco.service.MemberService;

/**
 * Bulk-registers members from a CSV file.
 *
 * Topic 11 - Converting Byte Streams to Character Streams. This is the read-side
 * bridge, and the chain is worth memorising:
 *
 *     FileInputStream       - gives us BYTES; it has no idea what a character is
 *       -> InputStreamReader - the BRIDGE; decodes bytes into chars with a charset
 *            -> BufferedReader - adds readLine() and reads in big chunks
 *
 * Without the InputStreamReader there is no way to get from bytes to text, because
 * "which bytes make which character" is a decision only a charset can answer. A
 * two-byte UTF-8 character read one byte at a time is just two meaningless numbers.
 */
public class MemberImporter {

    private final MemberService memberService;

    /** Lines that could not be imported, with the reason - reported back to the user. */
    private final List<String> errors = new ArrayList<String>();

    private int imported = 0;
    private int skipped = 0;

    public MemberImporter(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * Expected format, one member per line:
     *     nationalId,fullName,phone,role
     * Lines starting with # are comments.
     */
    public List<Member> importFrom(String path) throws IOException {
        File source = new File(path);
        List<Member> added = new ArrayList<Member>();

        errors.clear();
        imported = 0;
        skipped = 0;

        BufferedReader reader = null;
        try {
            // The three-layer bridge described above
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(source), "UTF-8"));

            String line;
            int lineNumber = 0;

            // Topic 4 - a while loop whose condition is also an assignment.
            // readLine() returns null at end-of-file; that null IS the loop's exit.
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue; // blank line or comment
                }

                // Topic 4 - Arrays and Strings: split produces a String[]
                String[] fields = trimmed.split(",");
                if (fields.length < 3) {
                    errors.add("Line " + lineNumber + ": expected at least 3 fields, got "
                            + fields.length);
                    skipped++;
                    continue;
                }

                try {
                    MemberRole role = MemberRole.MEMBER;
                    if (fields.length >= 4 && !fields[3].trim().isEmpty()) {
                        // valueOf is case-sensitive, hence the toUpperCase
                        role = MemberRole.valueOf(fields[3].trim().toUpperCase());
                    }
                    Member member = memberService.register(
                            fields[0].trim(), fields[1].trim(), fields[2].trim(), role);
                    added.add(member);
                    imported++;

                } catch (DuplicateMemberException dme) {
                    // Topic 10 - Exception Methods: getMessage() is where the detail is
                    errors.add("Line " + lineNumber + ": " + dme.getMessage());
                    skipped++;
                } catch (IllegalArgumentException iae) {
                    // Covers both bad validation AND MemberRole.valueOf on a bad role
                    errors.add("Line " + lineNumber + ": " + iae.getMessage());
                    skipped++;
                }
                /*
                 * Note what is NOT caught here: IOException. A bad LINE should be
                 * skipped, but a broken FILE must abort the whole import, so that
                 * exception is left to propagate out of the method.
                 */
            }
        } catch (FileNotFoundException fnf) {
            // Topic 10 - re-throwing with a clearer message, keeping the original cause
            throw new IOException("No import file at " + source.getAbsolutePath(), fnf);
        } finally {
            if (reader != null) {
                reader.close(); // closes the whole chain down to the FileInputStream
            }
        }
        return added;
    }

    /** Writes a sample CSV so the import can be demonstrated without hand-made files. */
    public static File writeSampleFile(String path) throws IOException {
        ReceiptPrinter writer = new ReceiptPrinter(new File(path).getParent());
        StringBuilder csv = new StringBuilder();
        csv.append("# nationalId,fullName,phone,role\n");
        csv.append("CF94051234ABCD,Nakato Grace,0701234567,MEMBER\n");
        csv.append("CM88120456EFGH,Okello Samuel,0772345678,TREASURER\n");
        csv.append("CF91070789IJKL,Atim Betty,0756789012,SECRETARY\n");
        csv.append("CM85030321MNOP,Mugisha Peter,0789012345,MEMBER\n");
        csv.append("BAD-ID,Rejected Row,0700000000,MEMBER\n");
        return writer.writeReport(new File(path).getName(), csv.toString());
    }

    public int getImported() {
        return imported;
    }

    public int getSkipped() {
        return skipped;
    }

    public List<String> getErrors() {
        return new ArrayList<String>(errors);
    }
}
