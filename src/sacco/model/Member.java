package sacco.model;

import java.util.ArrayList;
import java.util.List;

import sacco.util.Validator;

/**
 * A person who has joined the SACCO.
 *
 * Topic 5 - Defining a Class, Instance Data, Constructors, Encapsulation.
 * Topic 7 - extends Entity, so it inherits getId/equals/hashCode.
 * Topic 8 - implements Comparable so members can be sorted without a Comparator.
 */
public class Member extends Entity implements Comparable<Member> {

    private static final long serialVersionUID = 1L;

    /**
     * Topic 5 - Class Data (static). ONE counter shared by every Member object,
     * used to hand out MBR-0001, MBR-0002, ... It is not part of any one member.
     */
    private static int memberCounter = 0;

    /* Topic 5 - Instance Data: every Member object gets its own copy of these. */
    private final String nationalId;
    private String fullName;
    private String phone;
    private MemberRole role;
    private final long joinedAt;

    /**
     * Topic 9 - Package Scope. No modifier at all means package-private: only other
     * classes in sacco.model can touch this list. Account's constructor calls
     * attachAccount() below; sacco.service and sacco.app cannot.
     */
    final List<String> accountNumbers = new ArrayList<String>();

    /**
     * Topic 5 - Constructors. The main constructor, which validates and assigns.
     */
    public Member(String nationalId, String fullName, String phone, MemberRole role) {
        // super(...) must be the very first statement - Topic 7
        super(nextMemberId());

        if (!Validator.isValidNationalId(nationalId)) {
            throw new IllegalArgumentException(
                    "National ID must be 14 alphanumeric characters (got '" + nationalId + "')");
        }
        if (!Validator.isValidPhone(phone)) {
            throw new IllegalArgumentException("Not a recognised phone number: " + phone);
        }
        this.nationalId = nationalId.toUpperCase();
        this.fullName = Validator.requireText(fullName, "Full name");
        this.phone = phone;
        this.role = role == null ? MemberRole.MEMBER : role;
        this.joinedAt = System.currentTimeMillis();
    }

    /**
     * Topic 5 - Constructor overloading. A convenience constructor that defaults the
     * role, then delegates to the main one with this(...).
     */
    public Member(String nationalId, String fullName, String phone) {
        this(nationalId, fullName, phone, MemberRole.MEMBER);
    }

    /**
     * Topic 5 - a static (class) method operating on class data.
     * Topic 6 - Format Strings: %04d pads the counter to four digits with zeros.
     */
    private static synchronized String nextMemberId() {
        memberCounter++;
        return String.format("MBR-%04d", memberCounter);
    }

    /** Lets the saved counter be restored after loading state from disk. */
    public static void restoreCounter(int value) {
        if (value > memberCounter) {
            memberCounter = value;
        }
    }

    public static int getMemberCount() {
        return memberCounter;
    }

    /* ---------- Topic 5: Encapsulation via getters and guarded setters ---------- */

    public String getNationalId() {
        return nationalId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = Validator.requireText(fullName, "Full name");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (!Validator.isValidPhone(phone)) {
            throw new IllegalArgumentException("Not a recognised phone number: " + phone);
        }
        this.phone = phone;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        this.role = role;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    /**
     * Returns a copy, not the live list. Without this an outside class could do
     * member.getAccountNumbers().clear() and silently corrupt the object -
     * the point of Topic 5's encapsulation.
     */
    public List<String> getAccountNumbers() {
        return new ArrayList<String>(accountNumbers);
    }

    /**
     * Topic 9 - Package Scope. Called by Account's constructor (same package) so a
     * member and their account are linked from a single place.
     */
    void attachAccount(String accountNumber) {
        if (!accountNumbers.contains(accountNumber)) {
            accountNumbers.add(accountNumber);
        }
    }

    /** Package-private counterpart, used when an account is closed. */
    void detachAccount(String accountNumber) {
        accountNumbers.remove(accountNumber);
    }

    /** The biggest loan this member's role permits. */
    public double loanCeiling() {
        return role.getLoanCeiling();
    }

    /* ---------- Topic 7 / 8: overriding inherited and interface methods ---------- */

    /** Topic 8 - the abstract method Entity declared; we are forced to write it. */
    @Override
    public String summary() {
        // Topic 6 - printf-style formatting through String.format
        return String.format("%-9s %-24s %-11s %-12s %d account(s)",
                getId(), fullName, phone, role.getLabel(), accountNumbers.size());
    }

    /** Topic 7 - Method Overriding: a more useful toString than Entity's. */
    @Override
    public String toString() {
        return getId() + " - " + fullName + " (" + role.getLabel() + ")";
    }

    /**
     * Topic 8 - Comparable. Defines the NATURAL ordering of members: by name, then
     * by id to break ties. Collections.sort(list) and TreeSet use this automatically.
     */
    @Override
    public int compareTo(Member other) {
        int byName = this.fullName.compareToIgnoreCase(other.fullName);
        if (byName != 0) {
            return byName;
        }
        return this.getId().compareTo(other.getId());
    }
}
