package piems.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.Transaction;

/**
 * Strategy generating a category-wise expenditure breakdown report.
 *
 * <p>Implements Requirement FR-14 and Decisions D-10, D-36 (Q-014). Uses the
 * Hare-Niemeyer Largest Remainder Method to guarantee percentage shares sum to
 * exactly 100.0%. Subcategories roll up through the Composite tree.</p>
 */
public class CategoryBreakdownReport implements ReportStrategy {

    private static final String STRATEGY_NAME = "Category Breakdown";

    @Override
    public String name() {
        return STRATEGY_NAME;
    }

    @Override
    public ReportResult generate(ReportRequest request, List<Transaction> transactions) {
        Objects.requireNonNull(request, "Request cannot be null");
        Objects.requireNonNull(transactions, "Transactions cannot be null");

        Map<String, Long> totals = aggregateExpenses(request, transactions);
        long totalExpense = totals.values().stream().mapToLong(Long::longValue).sum();

        if (totalExpense == 0L) {
            return buildEmptyResult(request);
        }

        List<CategoryShare> shares = computeSharesLargestRemainder(totals, totalExpense);
        return buildResult(request, shares, totalExpense);
    }

    private Map<String, Long> aggregateExpenses(ReportRequest request, List<Transaction> transactions) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            if (tx != null && request.matchesMonth(tx.date()) && "EXPENSE".equals(tx.kind())) {
                String label = resolveCategoryLabel(tx);
                totals.merge(label, tx.amount().minorUnits(), Long::sum);
            }
        }
        return totals;
    }

    private String resolveCategoryLabel(Transaction tx) {
        CategoryComponent comp = tx.category();
        if (comp != null) {
            return comp.path();
        }
        return "Uncategorized";
    }

    private List<CategoryShare> computeSharesLargestRemainder(Map<String, Long> totals, long totalExpense) {
        List<CategoryShare> list = new ArrayList<>();
        long allocatedTenthsSum = 0L;

        for (Map.Entry<String, Long> entry : totals.entrySet()) {
            long product = entry.getValue() * 1000L;
            long baseTenths = product / totalExpense;
            long rem = product % totalExpense;
            list.add(new CategoryShare(entry.getKey(), entry.getValue(), baseTenths, rem));
            allocatedTenthsSum += baseTenths;
        }

        distributeRemainders(list, (int) (1000L - allocatedTenthsSum));
        list.sort((a, b) -> Long.compare(b.amountMinor, a.amountMinor));
        return list;
    }

    private void distributeRemainders(List<CategoryShare> list, int extraTenths) {
        List<CategoryShare> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> {
            int cmp = Long.compare(b.remainder, a.remainder);
            if (cmp != 0) {
                return cmp;
            }
            return a.category.compareTo(b.category);
        });

        for (int i = 0; i < extraTenths && i < sorted.size(); i++) {
            sorted.get(i).tenths++;
        }
    }

    private ReportResult buildResult(ReportRequest req, List<CategoryShare> shares, long total) {
        List<String> headers = List.of("Category", "Amount", "Share (%)");
        List<List<String>> rows = new ArrayList<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalExpenseMinor", total);

        for (CategoryShare share : shares) {
            String pct = (share.tenths / 10L) + "." + (share.tenths % 10L) + "%";
            rows.add(List.of(share.category, Money.ofMinor(share.amountMinor).toString(), pct));
            data.put(share.category, pct);
        }

        return new ReportResult("Category Breakdown - " + req.month(), headers, rows, data);
    }

    private ReportResult buildEmptyResult(ReportRequest req) {
        List<String> headers = List.of("Category", "Amount", "Share (%)");
        return new ReportResult(
                "Category Breakdown - " + req.month(),
                headers,
                Collections.emptyList(),
                Map.of("totalExpenseMinor", 0L));
    }

    private static final class CategoryShare {
        private final String category;
        private final long amountMinor;
        private long tenths;
        private final long remainder;

        CategoryShare(String category, long amountMinor, long tenths, long remainder) {
            this.category = category;
            this.amountMinor = amountMinor;
            this.tenths = tenths;
            this.remainder = remainder;
        }
    }
}
