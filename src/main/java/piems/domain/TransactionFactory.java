package piems.domain;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Factory for validating and instantiating concrete Transaction instances.
 *
 * <p>Enforces Requirement FR-19 and Decision D-07, validating amount boundaries
 * (0.00 rejected, 0.01 accepted, 999,999,999.99 accepted, 1,000,000,000.00 rejected,
 * 0.001 rejected) and constructing appropriate subtypes without {@code instanceof} checks.</p>
 */
public final class TransactionFactory {

    private TransactionFactory() {
        // Utility factory class
    }

    /**
     * Creates an Income transaction directly from validated domain objects.
     *
     * @param id         unique transaction ID, or null
     * @param date       transaction date
     * @param amount     positive Money amount
     * @param note       optional note
     * @param account    destination account
     * @param categoryId optional income category ID
     * @return concrete Income instance
     */
    public static Income createIncome(
            Long id, LocalDate date, Money amount, String note, Account account, Long categoryId) {
        return new Income(id, date, amount, note, account, categoryId);
    }

    /**
     * Creates an Income transaction with associated category component.
     *
     * @param id       unique transaction ID, or null
     * @param date     transaction date
     * @param amount   positive Money amount
     * @param note     optional note
     * @param account  destination account
     * @param category associated category component
     * @return concrete Income instance
     * @throws ValidationException if the category is archived
     */
    public static Income createIncome(
            Long id, LocalDate date, Money amount, String note, Account account, CategoryComponent category)
            throws ValidationException {
        validateCategory(category);
        return new Income(id, date, amount, note, account, category);
    }

    /**
     * Creates an Expense transaction directly from validated domain objects.
     *
     * @param id         unique transaction ID, or null
     * @param date       transaction date
     * @param amount     positive Money amount
     * @param note       optional note
     * @param account    source account
     * @param categoryId optional expense category ID
     * @return concrete Expense instance
     */
    public static Expense createExpense(
            Long id, LocalDate date, Money amount, String note, Account account, Long categoryId) {
        return new Expense(id, date, amount, note, account, categoryId);
    }

    /**
     * Creates an Expense transaction with associated category component.
     *
     * @param id       unique transaction ID, or null
     * @param date     transaction date
     * @param amount   positive Money amount
     * @param note     optional note
     * @param account  source account
     * @param category associated category component
     * @return concrete Expense instance
     * @throws ValidationException if the category is archived
     */
    public static Expense createExpense(
            Long id, LocalDate date, Money amount, String note, Account account, CategoryComponent category)
            throws ValidationException {
        validateCategory(category);
        return new Expense(id, date, amount, note, account, category);
    }

    /**
     * Creates a Transfer transaction directly from validated domain objects.
     *
     * @param id             unique transaction ID, or null
     * @param date           transaction date
     * @param amount         positive Money amount
     * @param note           optional note
     * @param fromAccount    source account debited
     * @param toAccount      destination account credited
     * @param transferLinkId UUID linking legs, or null
     * @return concrete Transfer instance
     */
    public static Transfer createTransfer(
            Long id,
            LocalDate date,
            Money amount,
            String note,
            Account fromAccount,
            Account toAccount,
            String transferLinkId
    ) {
        return new Transfer(id, date, amount, note, fromAccount, toAccount, transferLinkId);
    }

    /**
     * Validates inputs and creates the matching Transaction subtype.
     *
     * @param kind        transaction kind ("INCOME", "EXPENSE", "TRANSFER")
     * @param date        transaction date
     * @param amount      monetary amount
     * @param note        optional note
     * @param fromAccount source or primary account
     * @param toAccount   destination account (required for transfer)
     * @param categoryId  optional category ID
     * @return concrete Transaction subtype
     * @throws ValidationException if inputs fail validation
     */
    public static Transaction create(
            String kind,
            LocalDate date,
            Money amount,
            String note,
            Account fromAccount,
            Account toAccount,
            Long categoryId
    ) throws ValidationException {
        validateCommonInputs(kind, date, amount, fromAccount);
        String upperKind = kind.trim().toUpperCase();
        return switch (upperKind) {
            case "INCOME" -> new Income(null, date, amount, note, fromAccount, categoryId);
            case "EXPENSE" -> new Expense(null, date, amount, note, fromAccount, categoryId);
            case "TRANSFER" -> createTransferWithValidation(date, amount, note, fromAccount, toAccount);
            default -> throw new ValidationException("Unknown transaction kind: " + kind);
        };
    }

    /**
     * Validates inputs and creates the matching Transaction subtype with CategoryComponent.
     *
     * @param kind        transaction kind ("INCOME", "EXPENSE", "TRANSFER")
     * @param date        transaction date
     * @param amount      monetary amount
     * @param note        optional note
     * @param fromAccount source or primary account
     * @param toAccount   destination account (required for transfer)
     * @param category    associated category component
     * @return concrete Transaction subtype
     * @throws ValidationException if inputs fail validation or category is archived
     */
    public static Transaction createWithCategory(
            String kind,
            LocalDate date,
            Money amount,
            String note,
            Account fromAccount,
            Account toAccount,
            CategoryComponent category
    ) throws ValidationException {
        validateCategory(category);
        validateCommonInputs(kind, date, amount, fromAccount);
        String upperKind = kind.trim().toUpperCase();
        return switch (upperKind) {
            case "INCOME" -> new Income(null, date, amount, note, fromAccount, category);
            case "EXPENSE" -> new Expense(null, date, amount, note, fromAccount, category);
            case "TRANSFER" -> createTransferWithValidation(date, amount, note, fromAccount, toAccount);
            default -> throw new ValidationException("Unknown transaction kind: " + kind);
        };
    }

    /**
     * Parses raw string inputs with CategoryComponent, applies boundary validation, and constructs a Transaction.
     *
     * @param kind        transaction kind ("INCOME", "EXPENSE", "TRANSFER")
     * @param dateStr     ISO date string (YYYY-MM-DD)
     * @param amountStr   decimal amount string (e.g., "100.50")
     * @param note        optional note
     * @param fromAccount source or primary account
     * @param toAccount   destination account (for transfer)
     * @param category    associated category component
     * @return concrete Transaction subtype
     * @throws ValidationException if validation or parsing fails
     */
    public static Transaction createFromStringWithCategory(
            String kind,
            String dateStr,
            String amountStr,
            String note,
            Account fromAccount,
            Account toAccount,
            CategoryComponent category
    ) throws ValidationException {
        LocalDate date = parseDate(dateStr);
        Money amount = parseAndValidateAmount(amountStr);
        return createWithCategory(kind, date, amount, note, fromAccount, toAccount, category);
    }

    /**
     * Parses raw string inputs, applies boundary validation, and constructs a Transaction.
     *
     * @param kind        transaction kind ("INCOME", "EXPENSE", "TRANSFER")
     * @param dateStr     ISO date string (YYYY-MM-DD)
     * @param amountStr   decimal amount string (e.g., "100.50")
     * @param note        optional note
     * @param fromAccount source or primary account
     * @param toAccount   destination account (for transfer)
     * @param categoryId  optional category ID
     * @return concrete Transaction subtype
     * @throws ValidationException if validation or parsing fails
     */
    public static Transaction createFromString(
            String kind,
            String dateStr,
            String amountStr,
            String note,
            Account fromAccount,
            Account toAccount,
            Long categoryId
    ) throws ValidationException {
        LocalDate date = parseDate(dateStr);
        Money amount = parseAndValidateAmount(amountStr);
        return create(kind, date, amount, note, fromAccount, toAccount, categoryId);
    }

    private static void validateCommonInputs(
            String kind, LocalDate date, Money amount, Account fromAccount) throws ValidationException {
        if (kind == null || kind.isBlank()) {
            throw new ValidationException("Transaction kind cannot be null or blank");
        }
        if (date == null) {
            throw new ValidationException("Transaction date cannot be null");
        }
        if (amount == null) {
            throw new ValidationException("Transaction amount cannot be null");
        }
        if (amount.minorUnits() <= 0) {
            throw new ValidationException("Transaction amount must be strictly positive: " + amount);
        }
        if (amount.minorUnits() > Transaction.MAX_AMOUNT_MINOR) {
            throw new ValidationException("Transaction amount exceeds ceiling of 999,999,999.99: " + amount);
        }
        if (fromAccount == null) {
            throw new ValidationException("Primary account cannot be null");
        }
    }

    private static Transfer createTransferWithValidation(
            LocalDate date, Money amount, String note, Account fromAccount, Account toAccount)
            throws ValidationException {
        if (toAccount == null) {
            throw new ValidationException("Destination account cannot be null for transfer");
        }
        if (fromAccount.equals(toAccount)) {
            throw new ValidationException("Cannot transfer funds to the same account");
        }
        return new Transfer(null, date, amount, note, fromAccount, toAccount, null);
    }

    private static LocalDate parseDate(String dateStr) throws ValidationException {
        if (dateStr == null || dateStr.isBlank()) {
            throw new ValidationException("Date string cannot be null or blank");
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format (expected YYYY-MM-DD): " + dateStr, e);
        }
    }

    private static Money parseAndValidateAmount(String amountStr) throws ValidationException {
        if (amountStr == null || amountStr.isBlank()) {
            throw new ValidationException("Amount string cannot be null or blank");
        }
        try {
            return Money.of(amountStr.trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid monetary amount: " + amountStr + " (" + e.getMessage() + ")", e);
        }
    }

    private static void validateCategory(CategoryComponent category) throws ValidationException {
        if (category != null && category.isArchived()) {
            throw new ValidationException("Cannot assign archived category: " + category.name());
        }
    }
}
