package piems.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import piems.domain.PiemsException;
import piems.domain.RecurringRule;

/**
 * Service orchestrating automatic generation of recurring transactions (FR-11, Scenario S4).
 *
 * <p>Uses constructor injection with an injected {@link Clock} to guarantee deterministic
 * date progression and prevent skipped or duplicate entries across downtime.</p>
 */
public final class RecurringGenerator {

    private final FinanceService financeService;
    private final Clock clock;

    /**
     * Constructs a RecurringGenerator with dependencies.
     *
     * @param financeService facade service for executing financial transactions
     * @param clock          clock provider for deterministic time retrieval
     */
    public RecurringGenerator(FinanceService financeService, Clock clock) {
        this.financeService = Objects.requireNonNull(financeService, "financeService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /**
     * Generates all pending transactions due on or before today for the given recurring rule.
     *
     * @param rule recurring rule to process
     * @return unmodifiable list of generated transaction views
     * @throws PiemsException if recording a transaction fails
     */
    public List<TransactionView> generateDue(RecurringRule rule) throws PiemsException {
        Objects.requireNonNull(rule, "rule cannot be null");
        LocalDate today = LocalDate.now(clock);
        List<LocalDate> dueDates = rule.dueDates(today);
        List<TransactionView> generated = new ArrayList<>();

        for (LocalDate dueDate : dueDates) {
            TransactionView view = recordSingleTransaction(rule, dueDate);
            rule.markGenerated(dueDate);
            generated.add(view);
        }
        return Collections.unmodifiableList(generated);
    }

    /**
     * Processes a list of recurring rules and generates all due transactions.
     *
     * @param rules list of recurring rules to evaluate
     * @return combined list of generated transaction views
     * @throws PiemsException if recording fails
     */
    public List<TransactionView> generateAll(List<RecurringRule> rules) throws PiemsException {
        Objects.requireNonNull(rules, "rules cannot be null");
        List<TransactionView> all = new ArrayList<>();
        for (RecurringRule rule : rules) {
            all.addAll(generateDue(rule));
        }
        return Collections.unmodifiableList(all);
    }

    private TransactionView recordSingleTransaction(RecurringRule rule, LocalDate dueDate)
            throws PiemsException {
        Long categoryId = (rule.getCategory() != null) ? rule.getCategory().id() : null;
        if ("EXPENSE".equalsIgnoreCase(rule.getTransactionType())) {
            return financeService.recordExpense(
                    rule.getAccount().id(), categoryId, rule.getAmount(), dueDate, rule.getDescription()
            );
        } else {
            return financeService.recordIncome(
                    rule.getAccount().id(), categoryId, rule.getAmount(), dueDate, rule.getDescription()
            );
        }
    }
}
