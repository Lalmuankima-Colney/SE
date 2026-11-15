package piems.report;

import java.util.List;
import piems.domain.Transaction;

/**
 * Strategy interface for generating financial reports (Decision D-10).
 *
 * <p>Decouples report generation algorithms from FinanceService and presentation layers,
 * satisfying Open/Closed Principle and Requirement NFR-09.</p>
 */
public interface ReportStrategy {

    /**
     * Returns the unique report strategy identifier or name.
     *
     * @return report name
     */
    String name();

    /**
     * Generates a report result from the request parameters and transaction collection.
     *
     * @param request      report parameters (date, range)
     * @param transactions collection of transactions to analyze
     * @return generated report result
     */
    ReportResult generate(ReportRequest request, List<Transaction> transactions);
}
