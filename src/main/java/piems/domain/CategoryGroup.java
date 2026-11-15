package piems.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Composite group node in the category composite hierarchy.
 *
 * <p>Aggregates child category components (groups and leaves) of the same kind,
 * delegating leaf discovery and path computations polymorphically without type checks
 * (Decision D-08, Requirement FR-05).</p>
 */
public class CategoryGroup extends CategoryComponent {

    private final List<CategoryComponent> children = new ArrayList<>();

    /**
     * Constructs a CategoryGroup with full attributes.
     *
     * @param id       unique identifier, or null if unpersisted
     * @param name     group display name
     * @param kind     group kind ("INCOME" or "EXPENSE")
     * @param archived true if archived
     */
    public CategoryGroup(Long id, String name, String kind, boolean archived) {
        super(id, name, kind, archived);
    }

    /**
     * Constructs an active CategoryGroup.
     *
     * @param id   unique identifier, or null if unpersisted
     * @param name group display name
     * @param kind group kind ("INCOME" or "EXPENSE")
     */
    public CategoryGroup(Long id, String name, String kind) {
        super(id, name, kind);
    }

    /**
     * Adds a child category component to this group.
     *
     * @param child child component to add
     */
    public void add(CategoryComponent child) {
        Objects.requireNonNull(child, "Child component cannot be null");
        if (child == this) {
            throw new IllegalArgumentException("Cannot add a category group to itself");
        }
        if (!kind().equalsIgnoreCase(child.kind())) {
            throw new IllegalArgumentException(
                    "Cannot add child of kind '" + child.kind() + "' to group of kind '" + kind() + "'");
        }
        child.setParent(this);
        children.add(child);
    }

    /**
     * Removes a child category component from this group.
     *
     * @param child child component to remove
     */
    public void remove(CategoryComponent child) {
        if (children.remove(child) && child != null) {
            child.setParent(null);
        }
    }

    /**
     * Returns an unmodifiable list of direct child components.
     *
     * @return direct children list
     */
    public List<CategoryComponent> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public int childCount() {
        return children.size();
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    @Override
    public CategoryComponent copyWithId(Long id) {
        CategoryGroup copy = new CategoryGroup(id, name(), kind(), isArchived());
        if (isInUse()) {
            copy.markInUse();
        }
        if (parent() != null) {
            copy.setParent(parent());
        }
        for (CategoryComponent child : children) {
            copy.add(child);
        }
        return copy;
    }

    @Override
    public String path() {
        if (parent() != null) {
            return parent().path() + " > " + name();
        }
        return name();
    }

    @Override
    public List<Category> leaves() {
        List<Category> allLeaves = new ArrayList<>();
        for (CategoryComponent child : children) {
            allLeaves.addAll(child.leaves());
        }
        return Collections.unmodifiableList(allLeaves);
    }
}
