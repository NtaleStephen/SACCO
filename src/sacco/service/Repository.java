package sacco.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import sacco.model.Entity;

/**
 * Topic 8 - Generics.
 *
 * One storage class reused for Members, Accounts and LoanApplications. The type
 * parameter `T` is filled in at the point of use:
 *
 *     Repository<Member> members = new Repository<Member>("Member");
 *     Member m = members.findById("MBR-0001");   // no cast needed
 *
 * `T extends Entity` is a BOUNDED type parameter. It is what lets the code below
 * call item.getId() - without the bound, T could be anything and getId() would not
 * compile. It also makes Repository<String> a compile error.
 *
 * Before generics (Java 1.4) this held Object and every read needed a cast that could
 * fail at runtime. Generics move that failure to compile time.
 *
 * Topic 12 - The Map Interface. A LinkedHashMap gives O(1) lookup by id AND keeps
 * insertion order, so listings do not jump around between runs the way a HashMap's
 * would.
 */
public class Repository<T extends Entity> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, T> items = new LinkedHashMap<String, T>();
    private final String typeName;

    public Repository(String typeName) {
        this.typeName = typeName;
    }

    /** Returns false if the id was already present, rather than overwriting it. */
    public boolean add(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot store null in a " + typeName + " repository");
        }
        if (items.containsKey(item.getId())) {
            return false;
        }
        items.put(item.getId(), item);
        return true;
    }

    /**
     * The payoff of generics: this returns T, so the caller gets a Member or an
     * Account with no cast and no ClassCastException risk.
     */
    public T findById(String id) {
        return items.get(id); // null when absent
    }

    public boolean exists(String id) {
        return items.containsKey(id);
    }

    /** A copy, so callers cannot mutate the repository through the returned list. */
    public List<T> findAll() {
        return new ArrayList<T>(items.values());
    }

    /** The live values view, for read-only iteration in hot paths. */
    public Collection<T> values() {
        return Collections.unmodifiableCollection(items.values());
    }

    public T remove(String id) {
        return items.remove(id);
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public String getTypeName() {
        return typeName;
    }

    /**
     * Topic 8 - a GENERIC METHOD with its own type parameter <E>, separate from the
     * class's T. It sorts any list of Comparables, which is why it is static and why
     * it reads `<E extends Comparable<E>>`.
     */
    public static <E extends Comparable<E>> List<E> sorted(List<E> source) {
        List<E> copy = new ArrayList<E>(source);
        Collections.sort(copy); // uses each element's compareTo - Topic 8
        return copy;
    }

    @Override
    public String toString() {
        return typeName + "Repository[" + items.size() + "]";
    }
}
