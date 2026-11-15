package piems.domain;

import java.util.List;

/**
 * Leaf category node in the category composite hierarchy.
 *
 * <p>Represents a concrete classification for income or expense transactions
 * according to Decision D-08 and Requirement FR-05.</p>
 */
public class Category extends CategoryComponent {

    /**
     * Constructs a leaf Category with full attributes.
     *
     * @param id       unique identifier, or null if unpersisted
     * @param name     category display name
     * @param kind     category kind ("INCOME" or "EXPENSE")
     * @param archived true if archived
     */
    public Category(Long id, String name, String kind, boolean archived) {
        super(id, name, kind, archived);
    }

    /**
     * Constructs an active leaf Category.
     *
     * @param id   unique identifier, or null if unpersisted
     * @param name category display name
     * @param kind category kind ("INCOME" or "EXPENSE")
     */
    public Category(Long id, String name, String kind) {
        super(id, name, kind);
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
        return List.of(this);
    }

    @Override
    public int childCount() {
        return 0;
    }

    @Override
    public boolean isGroup() {
        return false;
    }

    @Override
    public CategoryComponent copyWithId(Long id) {
        Category copy = new Category(id, name(), kind(), isArchived());
        if (isInUse()) {
            copy.markInUse();
        }
        if (parent() != null) {
            copy.setParent(parent());
        }
        return copy;
    }
}
