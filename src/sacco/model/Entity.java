package sacco.model;

import java.io.Serializable;

/**
 * Base class for everything the SACCO stores.
 *
 * Topic 8 - Abstract Classes. Entity cannot be instantiated; it only exists to be
 * extended. summary() is abstract, so every subclass is FORCED to describe itself.
 *
 * Topic 11 - implements Serializable so whole object graphs can be written to disk
 * with ObjectOutputStream.
 *
 * Topic 8 - bounded generics: Repository<T extends Entity> relies on getId() being
 * available on every stored object, which is exactly what this class guarantees.
 */
public abstract class Entity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Topic 5 - Encapsulation. private + final: set once in the constructor,
     * readable through a getter, changeable by nobody.
     */
    private final String id;

    /**
     * Topic 5 - Access Modifiers. protected, so only subclasses can call it.
     * Topic 7 - subclasses reach this with super(...).
     */
    protected Entity(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id is required");
        }
        this.id = id.trim();
    }

    public String getId() {
        return id;
    }

    /** Topic 8 - Abstract method: no body here, mandatory in subclasses. */
    public abstract String summary();

    /**
     * Topic 7 - The Object Class / Method Overriding.
     * Topic 6 - toString: called automatically by println and string concatenation.
     */
    @Override
    public String toString() {
        // getClass() is inherited from Object - Topic 7
        return getClass().getSimpleName() + "{" + id + "}";
    }

    /**
     * Topic 6 - Comparing and Identifying Objects.
     * == compares references (are these the same object?).
     * equals compares state (do these two objects mean the same thing?).
     * Two Entity objects are equal when they are the same kind with the same id.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true; // same reference - identical object
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        // Topic 7 - Casting: safe now that the class check above has passed
        Entity that = (Entity) other;
        return id.equals(that.id);
    }

    /**
     * Topic 6 / Topic 12 - hashCode MUST be overridden alongside equals, otherwise
     * HashSet and HashMap will store duplicates that equals() says are the same.
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
