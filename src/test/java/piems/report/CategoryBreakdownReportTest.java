package piems.report;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.BankAccount;
import piems.domain.Category;
import piems.domain.CategoryGroup;
import piems.domain.Expense;
import piems.domain.Income;
import piems.domain.Money;
import piems.domain.Transaction;
import piems.domain.Transfer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit and integration tests for {@link CategoryBreakdownReport}.
 *
 * <p>Verifies Requirement FR-14, Decision D-10, D-36, and Q-014:
 * multi-category share calculation (TC-FR14-01), Hare-Niemeyer Largest Remainder
 * rounding guaranteeing exactly 100.0% sum (TC-FR14-02), and exclusion of transfers and income.</p>
 */
class CategoryBreakdownReportTest {

    private final CategoryBreakdownReport report = new CategoryBreakdownReport();
    private final Account account = new BankAccount(1L, "Primary Bank", Money.ofMinor(1000000L));

    @Test
    @DisplayName("TC-FR14-01: Category shares sum to exactly 100.0% on standard data")
    void sharesSumToOneHundredPercent() {
        YearMonth month = YearMonth.of(2026, 1);
        ReportRequest request = new ReportRequest(month);

        Category food = new Category(1L, "Food", "EXPENSE");
        Category rent = new Category(2L, "Rent", "EXPENSE");
        Category utils = new Category(3L, "Utilities", "EXPENSE");
        Category fun = new Category(4L, "Entertainment", "EXPENSE");

        Transaction t1 = new Expense(1L, LocalDate.of(2026, 1, 2), Money.ofMinor(40000L), "Groceries", account, food);
        Transaction t2 = new Expense(2L, LocalDate.of(2026, 1, 5), Money.ofMinor(30000L), "Flat Rent", account, rent);
        Transaction t3 = new Expense(3L, LocalDate.of(2026, 1, 10), Money.ofMinor(20000L), "Electric", account, utils);
        Transaction t4 = new Expense(4L, LocalDate.of(2026, 1, 15), Money.ofMinor(10000L), "Cinema", account, fun);

        ReportResult result = report.generate(request, List.of(t1, t2, t3, t4));

        assertThat(result.headers()).containsExactly("Category", "Amount", "Share (%)");
        assertThat(result.rows()).containsExactly(
                List.of("Food", "400.00", "40.0%"),
                List.of("Rent", "300.00", "30.0%"),
                List.of("Utilities", "200.00", "20.0%"),
                List.of("Entertainment", "100.00", "10.0%")
        );

        double totalShare = 40.0 + 30.0 + 20.0 + 10.0;
        assertThat(totalShare).isEqualTo(100.0);
    }

    @Test
    @DisplayName("TC-FR14-02: Repeating fractions sum to exactly 100.0% using Largest Remainder Method")
    void repeatingFractionsSumToExactlyOneHundredPercent() {
        YearMonth month = YearMonth.of(2026, 2);
        ReportRequest request = new ReportRequest(month);

        Category catA = new Category(1L, "Category A", "EXPENSE");
        Category catB = new Category(2L, "Category B", "EXPENSE");
        Category catC = new Category(3L, "Category C", "EXPENSE");

        // Three equal ₹10.00 expenses -> 10/30 = 33.333...%
        Transaction t1 = new Expense(1L, LocalDate.of(2026, 2, 1), Money.ofMinor(1000L), "A", account, catA);
        Transaction t2 = new Expense(2L, LocalDate.of(2026, 2, 2), Money.ofMinor(1000L), "B", account, catB);
        Transaction t3 = new Expense(3L, LocalDate.of(2026, 2, 3), Money.ofMinor(1000L), "C", account, catC);

        ReportResult result = report.generate(request, List.of(t1, t2, t3));

        List<List<String>> rows = result.rows();
        assertThat(rows).hasSize(3);

        int sumTenths = 0;
        for (List<String> row : rows) {
            String pctStr = row.get(2).replace("%", "");
            sumTenths += Math.round(Double.parseDouble(pctStr) * 10);
        }
        assertThat(sumTenths).isEqualTo(1000);
        // Largest remainder method allocates the surplus 0.1% to Category A
        assertThat(rows.get(0).get(2)).isEqualTo("33.4%");
        assertThat(rows.get(1).get(2)).isEqualTo("33.3%");
        assertThat(rows.get(2).get(2)).isEqualTo("33.3%");
    }

    @Test
    @DisplayName("Transfers and income are strictly excluded from category expenditure breakdown")
    void transfersAndIncomeExcludedFromCategoryBreakdown() {
        YearMonth month = YearMonth.of(2026, 6);
        ReportRequest request = new ReportRequest(month);

        Category food = new Category(1L, "Food", "EXPENSE");
        Transaction exp = new Expense(1L, LocalDate.of(2026, 6, 2), Money.ofMinor(5000L), "Lunch", account, food);
        Transaction inc = new Income(2L, LocalDate.of(2026, 6, 3), Money.ofMinor(100000L), "Paycheck", account, 100L);
        Transaction transfer = new Transfer(3L, LocalDate.of(2026, 6, 4), Money.ofMinor(50000L), "Transfer", account, "uuid");

        ReportResult result = report.generate(request, List.of(exp, inc, transfer));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0)).containsExactly("Food", "50.00", "100.0%");
    }

    @Test
    @DisplayName("Subcategories roll up with hierarchical path in breakdown")
    void subcategoriesRollUpThroughCompositeTree() {
        YearMonth month = YearMonth.of(2026, 7);
        ReportRequest request = new ReportRequest(month);

        CategoryGroup housing = new CategoryGroup(10L, "Housing", "EXPENSE");
        Category rent = new Category(11L, "Rent", "EXPENSE");
        housing.add(rent);

        Transaction exp = new Expense(1L, LocalDate.of(2026, 7, 5), Money.ofMinor(120000L), "Rent", account, rent);
        ReportResult result = report.generate(request, List.of(exp));

        assertThat(result.rows().get(0).get(0)).isEqualTo("Housing > Rent");
        assertThat(result.rows().get(0).get(2)).isEqualTo("100.0%");
    }

    @Test
    @DisplayName("Empty month produces empty breakdown rows with zero total")
    void emptyMonthProducesEmptyBreakdown() {
        ReportRequest request = new ReportRequest(YearMonth.of(2026, 8));
        ReportResult result = report.generate(request, List.of());

        assertThat(result.rows()).isEmpty();
        assertThat(result.getData("totalExpenseMinor")).isEqualTo(0L);
    }
}
