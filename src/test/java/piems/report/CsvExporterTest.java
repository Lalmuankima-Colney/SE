package piems.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Money;

/**
 * Unit tests for {@link CsvExporter} and {@link TextExporter} using the Template Method pattern (D-10, FR-16).
 */
class CsvExporterTest {

    @Test
    @DisplayName("CsvExporter.export formats ReportResult using Template Method (header, rows, footer)")
    void exportFormatsReportResultAsCsv() {
        ReportResult report = new ReportResult(
                "Monthly Summary",
                List.of("Month", "Income", "Expense", "Net"),
                List.of(
                        List.of("2026-01", "50000.00", "20000.00", "30000.00"),
                        List.of("2026-02", "50000.00", "15000.00", "35000.00")
                ),
                Map.of()
        );

        CsvExporter exporter = new CsvExporter();
        String csv = exporter.export(report);

        assertThat(csv)
                .startsWith("Month,Income,Expense,Net\n")
                .contains("2026-01,50000.00,20000.00,30000.00\n")
                .contains("2026-02,50000.00,15000.00,35000.00\n");
    }

    @Test
    @DisplayName("CsvExporter.exportTransactions outputs standard 7-column header and escaped rows")
    void exportTransactionsFormatsStandardCsv() {
        TransactionExportRow row1 = new TransactionExportRow(
                LocalDate.of(2026, 1, 15),
                "EXPENSE",
                "Bank Account",
                "",
                "Groceries",
                Money.of("250.00"),
                "Weekly \"milk\", eggs & bread"
        );
        TransactionExportRow row2 = new TransactionExportRow(
                LocalDate.of(2026, 1, 16),
                "TRANSFER",
                "Bank Account",
                "Cash Wallet",
                "",
                Money.of("500.00"),
                "ATM cash"
        );

        CsvExporter exporter = new CsvExporter();
        String csv = exporter.exportTransactions(List.of(row1, row2));

        assertThat(csv)
                .startsWith("date,kind,account,to_account,category,amount,note\n")
                .contains("2026-01-15,EXPENSE,Bank Account,,Groceries,250.00,\"Weekly \"\"milk\"\", eggs & bread\"\n")
                .contains("2026-01-16,TRANSFER,Bank Account,Cash Wallet,,500.00,ATM cash\n");
    }

    @Test
    @DisplayName("escapeCsvField handles quotes, commas, newlines per RFC 4180")
    void escapeCsvFieldFollowsRfc4180() {
        assertThat(CsvExporter.escapeCsvField("simple")).isEqualTo("simple");
        assertThat(CsvExporter.escapeCsvField("hello, world")).isEqualTo("\"hello, world\"");
        assertThat(CsvExporter.escapeCsvField("quote \"here\"")).isEqualTo("\"quote \"\"here\"\"\"");
        assertThat(CsvExporter.escapeCsvField("line1\nline2")).isEqualTo("\"line1\nline2\"");
        assertThat(CsvExporter.escapeCsvField(null)).isEmpty();
    }

    @Test
    @DisplayName("TextExporter formats ReportResult as aligned ASCII table with borders")
    void textExporterFormatsAlignedTable() {
        ReportResult report = new ReportResult(
                "Category Breakdown",
                List.of("Category", "Spent", "Share"),
                List.of(
                        List.of("Food", "300.00", "60.0%"),
                        List.of("Transport", "200.00", "40.0%")
                ),
                Map.of()
        );

        TextExporter exporter = new TextExporter();
        String text = exporter.export(report);

        assertThat(text)
                .contains("Category Breakdown")
                .contains("Category")
                .contains("Spent")
                .contains("Share")
                .contains("Food")
                .contains("300.00")
                .contains("-+-");
    }

    @Test
    @DisplayName("TextExporter formats TransactionExportRow list as aligned ASCII table")
    void textExporterFormatsTransactions() {
        TransactionExportRow row = new TransactionExportRow(
                LocalDate.of(2026, 1, 15),
                "EXPENSE",
                "Bank",
                "",
                "Food",
                Money.of("250.00"),
                "Groceries"
        );

        TextExporter exporter = new TextExporter();
        String text = exporter.exportTransactions(List.of(row));

        assertThat(text)
                .contains("Transactions")
                .contains("Date")
                .contains("Bank")
                .contains("Food")
                .contains("250.00")
                .contains("Groceries");
    }

    @Test
    @DisplayName("Exporter rejects null ReportResult with NullPointerException")
    void exporterRejectsNullReport() {
        CsvExporter exporter = new CsvExporter();
        assertThatThrownBy(() -> exporter.export(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ReportResult cannot be null");
    }
}
