package piems.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import piems.domain.Account;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.PersistenceException;
import piems.domain.Transaction;
import piems.repository.AccountRepository;
import piems.repository.CategoryRepository;
import piems.repository.TransactionRecord;
import piems.repository.TransactionRepository;
import piems.service.ImportResult.SkippedRow;

/**
 * Parses and ingests RFC 4180 CSV bank and ledger statements (FR-17, D-30).
 *
 * <p>Enforces standard 7-column schema, executes duplicate reconciliation via multiset
 * matching, splits transfers into atomic two-leg pairs, and captures malformed or
 * invalid rows with diagnostic line numbers.</p>
 */
public class CsvImporter {

    private static final List<String> EXPECTED_HEADER = List.of(
            "date", "kind", "account", "to_account", "category", "amount", "note"
    );
    private static final String FORMULA_PREFIXES = "=+-@\t\r'";

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetMonitor budgetMonitor;

    private record ParsedRow(int lineNumber, List<String> fields, String rawText) {}
    private record RegularSig(LocalDate date, String kind, long accountId, Long categoryId, long minor, String note) {}
    private record TransferSig(LocalDate date, long fromAccId, long toAccId, long minor, String note) {}

    /**
     * Constructs a CsvImporter without a budget monitor.
     *
     * @param accountRepo     account persistence repository
     * @param categoryRepo    category persistence repository
     * @param transactionRepo transaction persistence repository
     */
    public CsvImporter(
            AccountRepository accountRepo,
            CategoryRepository categoryRepo,
            TransactionRepository transactionRepo
    ) {
        this(accountRepo, categoryRepo, transactionRepo, null);
    }

    /**
     * Constructs a CsvImporter with full dependencies.
     *
     * @param accountRepo     account persistence repository
     * @param categoryRepo    category persistence repository
     * @param transactionRepo transaction persistence repository
     * @param budgetMonitor   optional budget monitor for alert notifications
     */
    public CsvImporter(
            AccountRepository accountRepo,
            CategoryRepository categoryRepo,
            TransactionRepository transactionRepo,
            BudgetMonitor budgetMonitor
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepo, "accountRepo cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepo, "categoryRepo cannot be null");
        this.transactionRepository = Objects.requireNonNull(transactionRepo, "transactionRepo cannot be null");
        this.budgetMonitor = budgetMonitor;
    }

    /**
     * Imports transactions from CSV string content (FR-17).
     *
     * @param csvContent raw CSV text
     * @return ImportResult containing imported counts and skipped row diagnostics
     * @throws ImportException if the file is empty or the header is invalid
     */
    public ImportResult importCsv(String csvContent) throws ImportException {
        if (csvContent == null || csvContent.isBlank()) {
            throw new ImportException("CSV content is empty");
        }
        List<ParsedRow> rows = tokenizeCsv(csvContent);
        if (rows.isEmpty()) {
            throw new ImportException("CSV content contains no records");
        }
        validateHeader(rows.get(0));
        try {
            return processDataRows(rows.subList(1, rows.size()));
        } catch (PersistenceException e) {
            throw new ImportException("Persistence error while importing CSV: " + e.getMessage(), e);
        }
    }

    private void validateHeader(ParsedRow headerRow) throws ImportException {
        List<String> headers = headerRow.fields().stream().map(String::trim).map(String::toLowerCase).toList();
        if (!headers.equals(EXPECTED_HEADER)) {
            throw new ImportException(
                    "Invalid CSV header: expected '" + String.join(",", EXPECTED_HEADER)
                            + "' but found: " + String.join(",", headers)
            );
        }
    }

    private ImportResult processDataRows(List<ParsedRow> dataRows) throws PersistenceException {
        Map<RegularSig, Integer> regularPool = new HashMap<>();
        Map<TransferSig, Integer> transferPool = new HashMap<>();
        populateExistingPools(regularPool, transferPool);

        int importedCount = 0;
        int duplicateCount = 0;
        List<SkippedRow> skipped = new ArrayList<>();

        for (ParsedRow row : dataRows) {
            if (row.fields().size() != 7) {
                skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Expected 7 columns"));
                continue;
            }
            int result = processSingleRow(row, regularPool, transferPool, skipped);
            if (result == 1) {
                importedCount++;
            } else if (result == 2) {
                duplicateCount++;
            }
        }
        return new ImportResult(importedCount, duplicateCount, skipped);
    }

    private int processSingleRow(
            ParsedRow row,
            Map<RegularSig, Integer> regPool,
            Map<TransferSig, Integer> txPool,
            List<SkippedRow> skipped
    ) throws PersistenceException {
        String kind = row.fields().get(1).trim().toUpperCase();
        if ("TRANSFER".equals(kind)) {
            return processTransferRow(row, txPool, skipped);
        } else if ("INCOME".equals(kind) || "EXPENSE".equals(kind)) {
            return processRegularRow(row, kind, regPool, skipped);
        } else {
            skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Unknown transaction kind: " + kind));
            return 0;
        }
    }

    private int processTransferRow(
            ParsedRow row,
            Map<TransferSig, Integer> txPool,
            List<SkippedRow> skipped
    ) throws PersistenceException {
        ParsedTransfer pt = parseTransferFields(row, skipped);
        if (pt == null) {
            return 0;
        }
        TransferSig sig = new TransferSig(
                pt.date, pt.fromAccount.id(), pt.toAccount.id(), pt.amount.minorUnits(), pt.note
        );
        if (consumeFromPool(txPool, sig)) {
            return 2;
        }
        if (!canDebit(pt.fromAccount, pt.amount)) {
            skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Insufficient funds for transfer"));
            return 0;
        }
        executeTransferLegs(pt);
        return 1;
    }

    private void executeTransferLegs(ParsedTransfer pt) throws PersistenceException {
        String linkId = UUID.randomUUID().toString();
        TransactionRecord debit = new TransactionRecord(
                null, "TRANSFER", pt.date, pt.amount, pt.note, pt.fromAccount.id(), linkId, null, null
        );
        TransactionRecord credit = new TransactionRecord(
                null, "TRANSFER", pt.date, pt.amount, pt.note, pt.toAccount.id(), linkId, null, null
        );
        transactionRepository.executeTransfer(debit, credit);
    }

    private record ParsedTransfer(LocalDate date, Account fromAccount, Account toAccount, Money amount, String note) {}

    private ParsedTransfer parseTransferFields(ParsedRow row, List<SkippedRow> skipped) throws PersistenceException {
        LocalDate date = parseDate(row.fields().get(0), row, skipped);
        Money amount = parseAmount(row.fields().get(5), row, skipped);
        Account from = findAccount(unescapeFormula(row.fields().get(2).trim()));
        Account to = findAccount(unescapeFormula(row.fields().get(3).trim()));
        if (date == null || amount == null) {
            return null;
        }
        if (from == null) {
            skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Unknown source account"));
            return null;
        }
        if (to == null) {
            skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Unknown destination account"));
            return null;
        }
        if (from.id().equals(to.id())) {
            skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Source and destination accounts identical"));
            return null;
        }
        return new ParsedTransfer(date, from, to, amount, normalizeNote(row.fields().get(6)));
    }

    private int processRegularRow(
            ParsedRow row,
            String kind,
            Map<RegularSig, Integer> regPool,
            List<SkippedRow> skipped
    ) throws PersistenceException {
        LocalDate date = parseDate(row.fields().get(0), row, skipped);
        Money amount = parseAmount(row.fields().get(5), row, skipped);
        Account account = findAccount(unescapeFormula(row.fields().get(2).trim()));
        if (date == null || amount == null) {
            return 0;
        }
        if (account == null) {
            skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Unknown account"));
            return 0;
        }
        Long categoryId = resolveCategory(row, skipped);
        if (categoryId == Long.MIN_VALUE) {
            return 0;
        }
        String note = normalizeNote(row.fields().get(6));
        RegularSig sig = new RegularSig(date, kind, account.id(), categoryId, amount.minorUnits(), note);
        if (consumeFromPool(regPool, sig)) {
            return 2;
        }
        return persistRegularTransaction(row, kind, date, amount, note, account, categoryId, skipped);
    }

    private int persistRegularTransaction(
            ParsedRow row, String kind, LocalDate date, Money amount, String note,
            Account account, Long categoryId, List<SkippedRow> skipped
    ) throws PersistenceException {
        if ("EXPENSE".equals(kind) && !canDebit(account, amount)) {
            skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Insufficient funds for expense"));
            return 0;
        }
        TransactionRecord record = new TransactionRecord(
                null, kind, date, amount, note, account.id(), null, categoryId, null
        );
        transactionRepository.save(record);
        if ("EXPENSE".equals(kind) && budgetMonitor != null && categoryId != null) {
            checkBudgetAlert(categoryId, date);
        }
        return 1;
    }

    private Long resolveCategory(ParsedRow row, List<SkippedRow> skipped) throws PersistenceException {
        String catName = unescapeFormula(row.fields().get(4).trim());
        if (catName.isEmpty()) {
            return null;
        }
        CategoryComponent cat = findCategory(catName);
        if (cat == null) {
            skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Unknown category: " + catName));
            return Long.MIN_VALUE;
        }
        return cat.id();
    }

    private void checkBudgetAlert(Long categoryId, LocalDate date) {
        try {
            CategoryComponent cat = categoryRepository.findById(categoryId).orElse(null);
            if (cat != null) {
                java.time.YearMonth month = java.time.YearMonth.from(date);
                java.util.Optional<piems.domain.Budget> budgetOpt = budgetMonitor.getBudget(cat, month);
                if (budgetOpt.isPresent()) {
                    Money totalSpent = calculateTotalCategoryExpense(categoryId, month);
                    budgetMonitor.check(budgetOpt.get(), totalSpent, date);
                }
            }
        } catch (Exception ignored) {
            // Decision D-09: Observer alerts never abort recording
        }
    }

    private Money calculateTotalCategoryExpense(
            long categoryId, java.time.YearMonth month
    ) throws PersistenceException {
        long sumMinor = 0;
        for (TransactionRecord tx : transactionRepository.findAll()) {
            if ("EXPENSE".equalsIgnoreCase(tx.kind())
                    && tx.categoryId() != null
                    && tx.categoryId() == categoryId
                    && java.time.YearMonth.from(tx.date()).equals(month)) {
                sumMinor += tx.amount().minorUnits();
            }
        }
        return Money.ofMinor(sumMinor);
    }

    private boolean canDebit(Account account, Money amount) throws PersistenceException {
        if (account.isArchived()) {
            return false;
        }
        if ("CREDIT_CARD".equals(account.type())) {
            long debt = computeCreditCardDebtMinor(account.id());
            long limit = account.creditLimit() != null ? account.creditLimit().minorUnits() : 0;
            return amount.minorUnits() <= (limit - debt);
        }
        Money balance = accountRepository.getDerivedBalance(account.id());
        return amount.minorUnits() <= balance.minorUnits();
    }

    private long computeCreditCardDebtMinor(long accountId) throws PersistenceException {
        List<TransactionRecord> txs = transactionRepository.findByAccountId(accountId);
        long debt = 0;
        for (TransactionRecord tx : txs) {
            if ("EXPENSE".equalsIgnoreCase(tx.kind())) {
                debt += tx.amount().minorUnits();
            } else if ("INCOME".equalsIgnoreCase(tx.kind())) {
                debt -= tx.amount().minorUnits();
            }
        }
        return Math.max(0, debt);
    }

    private void populateExistingPools(
            Map<RegularSig, Integer> regPool,
            Map<TransferSig, Integer> txPool
    ) throws PersistenceException {
        List<TransactionRecord> all = transactionRepository.findAll();
        Map<String, List<TransactionRecord>> transferGroups = new HashMap<>();
        for (TransactionRecord r : all) {
            if ("TRANSFER".equalsIgnoreCase(r.kind()) && r.transferLinkId() != null) {
                transferGroups.computeIfAbsent(r.transferLinkId(), k -> new ArrayList<>()).add(r);
            } else {
                addToPool(regPool, new RegularSig(
                        r.date(), r.kind().toUpperCase(), r.accountId(), r.categoryId(),
                        r.amount().minorUnits(), normalizeNote(r.note())
                ));
            }
        }
        populateTransferPool(txPool, transferGroups);
    }

    private void populateTransferPool(
            Map<TransferSig, Integer> txPool,
            Map<String, List<TransactionRecord>> transferGroups
    ) {
        for (List<TransactionRecord> pair : transferGroups.values()) {
            if (pair.size() >= 2) {
                pair.sort((a, b) -> Long.compare(a.id(), b.id()));
                TransactionRecord debit = pair.get(0);
                TransactionRecord credit = pair.get(1);
                addToPool(txPool, new TransferSig(
                        debit.date(), debit.accountId(), credit.accountId(),
                        debit.amount().minorUnits(), normalizeNote(debit.note())
                ));
            }
        }
    }

    private static <T> void addToPool(Map<T, Integer> pool, T item) {
        pool.put(item, pool.getOrDefault(item, 0) + 1);
    }

    private static <T> boolean consumeFromPool(Map<T, Integer> pool, T item) {
        Integer count = pool.get(item);
        if (count == null || count <= 0) {
            return false;
        }
        if (count == 1) {
            pool.remove(item);
        } else {
            pool.put(item, count - 1);
        }
        return true;
    }

    private Account findAccount(String name) throws PersistenceException {
        for (Account a : accountRepository.findAll()) {
            if (a.name().equalsIgnoreCase(name)) {
                return a;
            }
        }
        return null;
    }

    private CategoryComponent findCategory(String name) throws PersistenceException {
        for (CategoryComponent c : categoryRepository.findAll()) {
            if (c.name().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

    private LocalDate parseDate(String raw, ParsedRow row, List<SkippedRow> skipped) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Invalid date: " + raw));
            return null;
        }
    }

    private Money parseAmount(String raw, ParsedRow row, List<SkippedRow> skipped) {
        try {
            Money m = Money.of(raw.trim());
            if (m.minorUnits() <= 0 || m.minorUnits() > Transaction.MAX_AMOUNT_MINOR) {
                skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Amount out of range: " + raw));
                return null;
            }
            return m;
        } catch (Exception e) {
            skipped.add(new SkippedRow(row.lineNumber(), row.rawText(), "Invalid amount format: " + raw));
            return null;
        }
    }

    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return "";
        }
        return unescapeFormula(note.trim());
    }

    /**
     * Reverses formula neutralization applied during export (D-44, P-304).
     *
     * <p>If the field begins with a neutralizing apostrophe followed by one of
     * '=+-@\t\r'', the leading apostrophe is stripped to restore original content.</p>
     *
     * @param text exported text value
     * @return original unescaped text
     */
    public static String unescapeFormula(String text) {
        if (text == null || text.length() < 2) {
            return text == null ? "" : text;
        }
        if (text.charAt(0) == '\'' && FORMULA_PREFIXES.indexOf(text.charAt(1)) >= 0) {
            return text.substring(1);
        }
        return text;
    }

    private static class TokenState {
        private int currentLine = 1;
        private int recordLine = 1;
        private final StringBuilder field = new StringBuilder();
        private final List<String> fields = new ArrayList<>();
        private final StringBuilder raw = new StringBuilder();
        private boolean inQuotes = false;
    }

    private List<ParsedRow> tokenizeCsv(String csv) {
        List<ParsedRow> rows = new ArrayList<>();
        String text = csv.startsWith("\uFEFF") ? csv.substring(1) : csv;
        TokenState state = new TokenState();
        int len = text.length();
        int i = 0;
        while (i < len) {
            i = processChar(text, i, len, state, rows);
        }
        finishRow(state.fields, state.field, state.raw, state.recordLine, rows);
        return rows;
    }

    private int processChar(String text, int i, int len, TokenState s, List<ParsedRow> rows) {
        char c = text.charAt(i);
        s.raw.append(c);
        if (c == '"') {
            return handleQuote(text, i, len, s);
        } else if (c == ',' && !s.inQuotes) {
            s.fields.add(s.field.toString());
            s.field.setLength(0);
        } else if ((c == '\n' || c == '\r') && !s.inQuotes) {
            return handleNewline(text, i, len, s, rows, c);
        } else {
            if (c == '\n') {
                s.currentLine++;
            }
            s.field.append(c);
        }
        return i + 1;
    }

    private int handleQuote(String text, int i, int len, TokenState s) {
        if (s.inQuotes && i + 1 < len && text.charAt(i + 1) == '"') {
            s.field.append('"');
            s.raw.append('"');
            return i + 2;
        }
        s.inQuotes = !s.inQuotes;
        return i + 1;
    }

    private int handleNewline(String text, int i, int len, TokenState s, List<ParsedRow> rows, char c) {
        int next = i + 1;
        if (c == '\r' && next < len && text.charAt(next) == '\n') {
            s.raw.append('\n');
            next++;
        }
        finishRow(s.fields, s.field, s.raw, s.recordLine, rows);
        s.currentLine++;
        s.recordLine = s.currentLine;
        return next;
    }

    private void finishRow(
            List<String> fields, StringBuilder field, StringBuilder raw, int recordLine, List<ParsedRow> rows
    ) {
        fields.add(field.toString());
        field.setLength(0);
        String trimmedRaw = raw.toString().trim();
        raw.setLength(0);
        if (!trimmedRaw.isEmpty() || !fields.isEmpty() && !(fields.size() == 1 && fields.get(0).isEmpty())) {
            rows.add(new ParsedRow(recordLine, new ArrayList<>(fields), trimmedRaw));
        }
        fields.clear();
    }
}
