package piems.domain;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and composite tests for {@link CategoryComponent}, {@link Category}, and {@link CategoryGroup}.
 *
 * <p>Verifies Requirement FR-05 and Decision D-08: hierarchical breadcrumb paths,
 * polymorphic leaf discovery, deletion prevention for in-use categories (TC-FR05-01),
 * soft-delete archiving (TC-FR05-02), and zero type checks in composite traversal.</p>
 */
public class CategoryTreeTest {

    @Test
    void pathOfNestedCategory() {
        CategoryGroup expenses = new CategoryGroup(1L, "Expenses", "EXPENSE");
        CategoryGroup food = new CategoryGroup(2L, "Food", "EXPENSE");
        Category groceries = new Category(3L, "Groceries", "EXPENSE");

        food.add(groceries);
        expenses.add(food);

        assertEquals("Expenses > Food > Groceries", groceries.path());
        assertEquals("Expenses > Food", food.path());
        assertEquals("Expenses", expenses.path());

        Category standaloneSalary = new Category(4L, "Salary", "INCOME");
        assertEquals("Salary", standaloneSalary.path());
    }

    @Test
    void leavesOfGroup() {
        CategoryGroup expenses = new CategoryGroup(1L, "Expenses", "EXPENSE");
        CategoryGroup food = new CategoryGroup(2L, "Food", "EXPENSE");
        Category groceries = new Category(3L, "Groceries", "EXPENSE");
        Category dining = new Category(4L, "Dining", "EXPENSE");
        food.add(groceries);
        food.add(dining);

        Category rent = new Category(5L, "Rent", "EXPENSE");
        expenses.add(food);
        expenses.add(rent);

        List<Category> leaves = expenses.leaves();
        assertEquals(3, leaves.size());
        assertTrue(leaves.contains(groceries));
        assertTrue(leaves.contains(dining));
        assertTrue(leaves.contains(rent));

        assertEquals(List.of(rent), rent.leaves());
        assertEquals(0, rent.childCount());
        assertEquals(2, expenses.childCount());
    }

    @Test
    void rejectsDeletingCategoryInUse() {
        // TC-FR05-01: Category with transactions -> Delete rejected (reassign or archive offered)
        Category category = new Category(10L, "Groceries", "EXPENSE");
        category.markInUse();
        assertTrue(category.isInUse());

        ValidationException ex = assertThrows(ValidationException.class, category::delete);
        assertTrue(ex.getMessage().contains("is in use and cannot be deleted"));
        assertTrue(ex.getMessage().contains("reassign or archive instead"));
    }

    @Test
    void archivesInsteadOfDeleting() throws ValidationException {
        // TC-FR05-02: Category with transactions -> archive category and attempt new expense -> rejected
        Category category = new Category(10L, "Deprecated Gym", "EXPENSE");
        category.markInUse();

        category.archive();
        assertTrue(category.isArchived());

        Account account = new CashAccount(1L, "Cash", Money.of("100.00"));
        ValidationException ex = assertThrows(ValidationException.class, () ->
                TransactionFactory.createExpense(
                        1L, LocalDate.of(2026, 11, 14), Money.of("50.00"), "Membership", account, category));
        assertTrue(ex.getMessage().contains("Cannot assign archived category"));
    }

    @Test
    void reassignAllowsDeletion() {
        // TC-FR05-01 workflow: reassign transactions then delete
        Category oldCategory = new Category(10L, "Old Category", "EXPENSE");
        oldCategory.markInUse();

        // Simulate reassigning transactions away from oldCategory
        oldCategory.clearInUse();
        assertFalse(oldCategory.isInUse());

        assertDoesNotThrow(oldCategory::delete);
    }

    @Test
    void compositeHasNoTypeChecks() {
        CategoryGroup root = new CategoryGroup(1L, "Income", "INCOME");
        CategoryGroup active = new CategoryGroup(2L, "Active", "INCOME");
        Category salary = new Category(3L, "Salary", "INCOME");
        active.add(salary);
        root.add(active);

        List<CategoryComponent> components = List.of(root, active, salary);
        for (CategoryComponent comp : components) {
            assertNotNull(comp.path());
            assertNotNull(comp.leaves());
        }
    }

    @Test
    void groupRejectsMismatchedKindOrSelfAddition() {
        CategoryGroup expenses = new CategoryGroup(1L, "Expenses", "EXPENSE");
        Category salary = new Category(2L, "Salary", "INCOME");

        assertThrows(IllegalArgumentException.class, () -> expenses.add(salary));
        assertThrows(IllegalArgumentException.class, () -> expenses.add(expenses));
        assertThrows(NullPointerException.class, () -> expenses.add(null));
    }

    @Test
    void rejectsNullOrBlankNameAndKind() {
        assertThrows(IllegalArgumentException.class, () -> new Category(1L, null, "EXPENSE"));
        assertThrows(IllegalArgumentException.class, () -> new Category(1L, "   ", "EXPENSE"));
        assertThrows(IllegalArgumentException.class, () -> new Category(1L, "Food", null));
        assertThrows(IllegalArgumentException.class, () -> new Category(1L, "Food", "INVALID_KIND"));
    }

    @Test
    void removeChildUpdatesParentReference() {
        CategoryGroup group = new CategoryGroup(1L, "Expenses", "EXPENSE");
        Category child = new Category(2L, "Fuel", "EXPENSE");

        group.add(child);
        assertEquals(group, child.parent());
        assertEquals(1, group.childCount());

        group.remove(child);
        assertNull(child.parent());
        assertEquals(0, group.childCount());
    }

    @Test
    void equalsAndHashCodeContract() {
        Category c1 = new Category(10L, "Food", "EXPENSE");
        Category c2 = new Category(10L, "Food", "EXPENSE");
        Category c3 = new Category(11L, "Food", "EXPENSE");

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1, c3);
        assertNotEquals(c1, null);

        Category unpersisted1 = new Category(null, "Transport", "EXPENSE");
        Category unpersisted2 = new Category(null, "Transport", "EXPENSE");
        assertEquals(unpersisted1, unpersisted2);
    }

    @Test
    void categoryGroupRemoveIgnoresNullChild() {
        // TC-FR05-05
        CategoryGroup group = new CategoryGroup(1L, "Expenses", "EXPENSE");
        assertDoesNotThrow(() -> group.remove(null));
        assertEquals(0, group.childCount());
    }

    @Test
    void categoryGroupRemoveIgnoresNonMemberChild() {
        // TC-FR05-06
        CategoryGroup group = new CategoryGroup(1L, "Expenses", "EXPENSE");
        Category outsideChild = new Category(2L, "Dining", "EXPENSE");
        assertDoesNotThrow(() -> group.remove(outsideChild));
        assertEquals(0, group.childCount());
    }
}
