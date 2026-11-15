package piems.report;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.Money;
import piems.domain.Transaction;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;
import piems.service.FinanceService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Requirement NFR-09 and Decision D-10 (TC-NFR09-01).
 *
 * <p>Proves that introducing a new {@link ReportStrategy} implementation
 * requires zero modifications to {@link FinanceService}, satisfying the
 * Open/Closed Principle.</p>
 */
class NewStrategyNeedsNoServiceChangeTest {

    @Test
    @DisplayName("TC-NFR09-01: Adding a new ReportStrategy requires zero changes to FinanceService")
    void addingNewStrategyNeedsNoFinanceServiceChange() throws Exception {
        InMemoryAccountRepository accountRepo = new InMemoryAccountRepository();
        InMemoryCategoryRepository categoryRepo = new InMemoryCategoryRepository();
        InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
        Clock clock = Clock.fixed(LocalDate.of(2026, 1, 15).atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));

        FinanceService service = new FinanceService(accountRepo, categoryRepo, txRepo, clock);
        Account account = service.createBankAccount("Checking", Money.ofMinor(100000L));
        service.recordIncome(account.id(), null, Money.ofMinor(50000L), LocalDate.of(2026, 1, 10), "Pay");

        // Create a new custom strategy that FinanceService has never seen before
        ReportStrategy customStrategy = new CustomTransactionCountStrategy();

        // 1. Direct polymorphic execution without modifying FinanceService
        ReportRequest request = new ReportRequest(YearMonth.of(2026, 1));
        ReportResult directResult = service.generateReport(customStrategy, request);
        assertThat(directResult.title()).isEqualTo("Custom Transaction Count");
        assertThat(directResult.getData("transactionCount")).isEqualTo(1);

        // 2. Named strategy registry execution without modifying FinanceService
        service.registerReportStrategy(customStrategy);
        ReportResult namedResult = service.generateReport("Custom Transaction Count", request);
        assertThat(namedResult.title()).isEqualTo("Custom Transaction Count");
        assertThat(namedResult.getData("transactionCount")).isEqualTo(1);
    }

    private static final class CustomTransactionCountStrategy implements ReportStrategy {

        @Override
        public String name() {
            return "Custom Transaction Count";
        }

        @Override
        public ReportResult generate(ReportRequest request, List<Transaction> transactions) {
            int count = 0;
            for (Transaction tx : transactions) {
                if (request.matchesMonth(tx.date())) {
                    count++;
                }
            }
            return new ReportResult(
                    "Custom Transaction Count",
                    List.of("Metric", "Value"),
                    List.of(List.of("Transactions", String.valueOf(count))),
                    Map.of("transactionCount", count)
            );
        }
    }
}
