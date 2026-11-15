package piems.domain;

import java.util.List;
import java.util.Objects;

/**
 * Abstract component in the category composite hierarchy.
 *
 * <p>Enforces unified handling of leaf categories and category groups according
 * to Decision D-08, D-26, and Requirement FR-05.</p>
 */
public abstract class CategoryComponent {

    private final Long id;
    private final String name;
    private final String kind;
    private boolean archived;
    private boolean inUse;
    private CategoryGroup parent;

    /**
     * Constructs a CategoryComponent with full attributes.
     *
     * @param id       unique identifier, or null if unpersisted
     * @param name     category display name
     * @param kind     category kind ("INCOME" or "EXPENSE")
     * @param archived true if archived
     */
    public CategoryComponent(Long id, String name, String kind, boolean archived) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }
        if (kind == null || kind.trim().isEmpty()) {
            throw new IllegalArgumentException("Category kind cannot be null or empty");
        }
        String upperKind = kind.trim().toUpperCase();
        if (!upperKind.equals("INCOME") && !upperKind.equals("EXPENSE")) {
            throw new IllegalArgumentException("Category kind must be INCOME or EXPENSE: " + kind);
        }
        this.id = id;
        this.name = name.trim();
        this.kind = upperKind;
        this.archived = archived;
        this.inUse = false;
    }

    /**
     * Constructs an active CategoryComponent.
     *
     * @param id   unique identifier, or null if unpersisted
     * @param name category display name
     * @param kind category kind ("INCOME" or "EXPENSE")
     */
    public CategoryComponent(Long id, String name, String kind) {
        this(id, name, kind, false);
    }

    /**
     * Returns the unique category identifier.
     *
     * @return ID, or null if unpersisted
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the display name of this category component.
     *
     * @return non-empty name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the uppercase kind ("INCOME" or "EXPENSE").
     *
     * @return kind string
     */
    public String kind() {
        return kind;
    }

    /**
     * Returns whether this category component has been soft-deleted/archived.
     *
     * @return true if archived
     */
    public boolean isArchived() {
        return archived;
    }

    /**
     * Archives this category component, preventing assignment to new transactions.
     */
    public void archive() {
        this.archived = true;
    }

    /**
     * Returns whether this category is currently referenced by transactions.
     *
     * @return true if in use
     */
    public boolean isInUse() {
        return inUse;
    }

    /**
     * Marks this category as actively referenced by one or more transactions.
     */
    public void markInUse() {
        this.inUse = true;
    }

    /**
     * Clears the in-use reference flag (e.g. after reassigning transactions).
     */
    public void clearInUse() {
        this.inUse = false;
    }

    /**
     * Returns the parent group of this component, if any.
     *
     * @return parent CategoryGroup, or null if at root
     */
    public CategoryGroup parent() {
        return parent;
    }

    /**
     * Sets the parent group for this component.
     *
     * @param parent parent group
     */
    void setParent(CategoryGroup parent) {
        this.parent = parent;
    }

    /**
     * Deletes this category from the active ledger.
     *
     * @throws ValidationException if this category is in use by transactions
     */
    public void delete() throws ValidationException {
        if (inUse) {
            throw new ValidationException(
                    "Category '" + name + "' is in use and cannot be deleted; reassign or archive instead");
        }
    }

    /**
     * Computes the hierarchical breadcrumb path of this category component.
     *
     * @return non-empty breadcrumb path (e.g. "Expenses > Food > Groceries")
     */
    public abstract String path();

    /**
     * Polymorphically retrieves all leaf categories within this component.
     *
     * @return unmodifiable list of leaf Category instances
     */
    public abstract List<Category> leaves();

    /**
     * Returns the count of direct child components.
     *
     * @return integer child count (0 for leaves)
     */
    public abstract int childCount();

    /**
     * Returns whether this component is a composite group.
     *
     * @return true if a CategoryGroup, false if a leaf Category
     */
    public boolean isGroup() {
        return false;
    }

    /**
     * Returns an unmodifiable list of direct child components (empty for leaves).
     *
     * @return direct children list
     */
    public List<CategoryComponent> children() {
        return List.of();
    }

    /**
     * Adds a child category component to this group. Leaf categories throw UnsupportedOperationException.
     *
     * @param child child component to add
     */
    public void add(CategoryComponent child) {
        throw new UnsupportedOperationException("Cannot add child to leaf category: " + name());
    }

    /**
     * Creates a copy of this category component with the specified identifier.
     *
     * @param id assigned identifier
     * @return new category component copy with the given id
     */
    public abstract CategoryComponent copyWithId(Long id);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CategoryComponent that)) {
            return false;
        }
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(name, that.name) && Objects.equals(kind, that.kind);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(name, kind);
    }
}
