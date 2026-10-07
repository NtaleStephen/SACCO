package sacco.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import sacco.exception.DuplicateMemberException;
import sacco.model.Member;
import sacco.model.MemberRole;

/**
 * Membership register.
 *
 * Topic 12 - uses three of the four collection families for three different reasons:
 *   Map  (inside Repository)  -> look a member up by id instantly
 *   Set  (HashSet)            -> enforce "one membership per national ID"
 *   List (ArrayList)          -> ordered results for reports
 */
public class MemberService implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Topic 8 - the generic Repository, bound to Member here. */
    private final Repository<Member> members = new Repository<Member>("Member");

    /**
     * Topic 12 - The Set Interface / Set Implementation Classes.
     * A HashSet is the right tool: we never need these in order, we only ever ask
     * "have I seen this one before?", and HashSet answers that in constant time.
     * A List would turn registration into an O(n) scan.
     */
    private final Set<String> registeredNationalIds = new HashSet<String>();

    /**
     * Topic 10 - Declaring Exceptions: the duplicate case is checked, because a clerk
     * typing an already-registered ID is a normal event the UI must handle.
     */
    public Member register(String nationalId, String fullName, String phone, MemberRole role)
            throws DuplicateMemberException {

        String normalised = nationalId == null ? "" : nationalId.trim().toUpperCase();

        // The Set earns its place right here
        if (registeredNationalIds.contains(normalised)) {
            throw new DuplicateMemberException(normalised);
        }

        // Member's constructor validates and may throw IllegalArgumentException
        Member member = new Member(normalised, fullName, phone, role);

        members.add(member);
        registeredNationalIds.add(member.getNationalId());
        return member;
    }

    /** Re-registers a member loaded from disk, keeping the ID set in step. */
    public void reinstate(Member member) {
        if (members.add(member)) {
            registeredNationalIds.add(member.getNationalId());
        }
    }

    public Member findById(String memberId) {
        return members.findById(memberId);
    }

    /**
     * Topic 6 - Comparing and Identifying Objects.
     * equals() is used, never ==: two Member objects read back from two different
     * files can describe the same person and so be equal, while == would say no
     * because they sit at different addresses in memory.
     */
    public Member findByNationalId(String nationalId) {
        if (nationalId == null) {
            return null;
        }
        String target = nationalId.trim().toUpperCase();
        // Topic 4 - Enhanced for Loop over a collection
        for (Member member : members.values()) {
            if (member.getNationalId().equals(target)) {
                return member;
            }
        }
        return null;
    }

    public List<Member> findAll() {
        return members.findAll();
    }

    public int count() {
        return members.size();
    }

    /**
     * Topic 8 - Comparable in action. Collections.sort uses Member.compareTo,
     * so the ordering rule lives with the class rather than being repeated here.
     */
    public List<Member> sortedByName() {
        List<Member> all = members.findAll();
        Collections.sort(all);
        return all;
    }

    /**
     * Topic 8 - a Comparator, for when you need an ordering that is NOT the natural
     * one. Comparable gives a class one ordering; Comparator gives you as many extra
     * orderings as you like without touching the class.
     *
     * Written as a named inner class rather than a lambda to stay within the Java 7
     * syntax this course covers.
     */
    public List<Member> sortedByJoinDate() {
        List<Member> all = members.findAll();
        Collections.sort(all, new Comparator<Member>() {
            @Override
            public int compare(Member a, Member b) {
                // newest first
                if (a.getJoinedAt() != b.getJoinedAt()) {
                    return a.getJoinedAt() > b.getJoinedAt() ? -1 : 1;
                }
                return a.getId().compareTo(b.getId());
            }
        });
        return all;
    }

    /**
     * Topic 12 - The Set Interface: a TreeSet keeps its contents sorted at all times
     * and silently drops duplicates. Handy for a de-duplicated alphabetical roll call.
     */
    public Set<String> uniqueSurnames() {
        Set<String> surnames = new TreeSet<String>(); // sorted, no duplicates
        for (Member member : members.values()) {
            // Topic 4 - Strings and Arrays: split returns a String[].
            // Names here follow the local convention of surname FIRST
            // ("Nakato Grace"), so the family name is element 0, not the last one.
            String[] parts = member.getFullName().trim().split("\\s+");
            surnames.add(parts[0].toUpperCase());
        }
        return surnames;
    }

    /**
     * Topic 12 - The Map Interface, grouping values under keys.
     * An EnumMap is the specialist Map for enum keys: internally just an array
     * indexed by ordinal, so it is faster and smaller than a HashMap and iterates
     * in the order the constants were declared.
     */
    public Map<MemberRole, List<Member>> groupByRole() {
        Map<MemberRole, List<Member>> grouped = new EnumMap<MemberRole, List<Member>>(MemberRole.class);

        // Pre-seed every role so empty roles still show up in the report
        for (MemberRole role : MemberRole.values()) {
            grouped.put(role, new ArrayList<Member>());
        }
        for (Member member : members.values()) {
            grouped.get(member.getRole()).add(member);
        }
        return grouped;
    }

    /** Members allowed to sign off on loans. */
    public List<Member> committeeMembers() {
        List<Member> committee = new ArrayList<Member>();
        for (Member member : members.values()) {
            if (member.getRole().canApproveLoans()) { // Topic 4 - switch, inside the enum
                committee.add(member);
            }
        }
        Collections.sort(committee);
        return committee;
    }

    public Set<String> getRegisteredNationalIds() {
        return Collections.unmodifiableSet(registeredNationalIds);
    }
}
