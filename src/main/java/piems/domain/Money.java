package piems.domain;

import java.util.Currency;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable value object representing a monetary amount as integer minor units.
 *
 * <p>Following Decision D-06 and D-31, money is stored strictly as non-negative
 * minor units (paise/cents). Floating-point arithmetic is prohibited to eliminate
 * rounding drift (NFR-01). Debt and negative account balances are tracked as
 * separate variables.</p>
 */
public final class Money implements Comparable<Money> {

    /** Default application currency (Indian Rupee, INR). */
    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("INR");

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+(\\.[0-9]+)?$");

    private final long minorUnits;
    private final Currency currency;

    private Money(long minorUnits, Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (minorUnits < 0) {
            throw new IllegalArgumentException(
                    "Money cannot be negative; debt is tracked as a separate variable: " + minorUnits);
        }
        this.minorUnits = minorUnits;
        this.currency = currency;
    }

    /**
     * Creates Money from integer minor units with a specified currency.
     *
     * @param minorUnits non-negative minor units
     * @param currency   currency instance
     * @return immutable Money instance
     */
    public static Money ofMinor(long minorUnits, Currency currency) {
        return new Money(minorUnits, currency);
    }

    /**
     * Creates Money from integer minor units using the default currency (INR).
     *
     * @param minorUnits non-negative minor units
     * @return immutable Money instance
     */
    public static Money ofMinor(long minorUnits) {
        return ofMinor(minorUnits, DEFAULT_CURRENCY);
    }

    /**
     * Creates a Money instance of value zero in the specified currency.
     *
     * @param currency currency instance
     * @return zero Money instance
     */
    public static Money zero(Currency currency) {
        return new Money(0L, currency);
    }

    /**
     * Creates a Money instance of value zero in the default currency (INR).
     *
     * @return zero Money instance
     */
    public static Money zero() {
        return zero(DEFAULT_CURRENCY);
    }

    /**
     * Parses a decimal string representation into Money with specified currency.
     *
     * @param amountStr decimal string (e.g., "100.50")
     * @param currency  currency instance
     * @return immutable Money instance
     */
    public static Money of(String amountStr, Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amountStr == null) {
            throw new IllegalArgumentException("Amount string cannot be null");
        }
        String trimmed = amountStr.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Amount string cannot be empty");
        }
        if (!NUMERIC_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid amount format: " + trimmed);
        }

        long minorUnits = parseToMinorUnits(trimmed, currency.getDefaultFractionDigits());
        return new Money(minorUnits, currency);
    }

    /**
     * Parses a decimal string representation into Money using default currency (INR).
     *
     * @param amountStr decimal string (e.g., "100.50")
     * @return immutable Money instance
     */
    public static Money of(String amountStr) {
        return of(amountStr, DEFAULT_CURRENCY);
    }

    private static long parseToMinorUnits(String trimmed, int fractionDigits) {
        String[] parts = trimmed.split("\\.");
        long whole = Long.parseLong(parts[0]);
        long factor = computeFactor(fractionDigits);

        if (parts.length == 1) {
            return Math.multiplyExact(whole, factor);
        }

        String fractionStr = parts[1];
        if (fractionStr.length() > fractionDigits) {
            throw new IllegalArgumentException("Exceeds max decimal places of " + fractionDigits + ": " + trimmed);
        }

        StringBuilder padded = new StringBuilder(fractionStr);
        while (padded.length() < fractionDigits) {
            padded.append('0');
        }
        long fraction = Long.parseLong(padded.toString());
        return Math.addExact(Math.multiplyExact(whole, factor), fraction);
    }

    private static long computeFactor(int fractionDigits) {
        long factor = 1L;
        for (int i = 0; i < fractionDigits; i++) {
            factor *= 10L;
        }
        return factor;
    }

    /**
     * Returns the monetary value in integer minor units (paise/cents).
     *
     * @return non-negative minor units
     */
    public long minorUnits() {
        return minorUnits;
    }

    /**
     * Returns the currency of this Money instance.
     *
     * @return currency instance
     */
    public Currency currency() {
        return currency;
    }

    /**
     * Adds another Money amount to this instance.
     *
     * @param other money to add
     * @return new Money instance with the sum
     */
    public Money add(Money other) {
        checkSameCurrency(other);
        long result = Math.addExact(this.minorUnits, other.minorUnits);
        return new Money(result, this.currency);
    }

    /**
     * Subtracts another Money amount from this instance.
     *
     * @param other money to subtract
     * @return new Money instance with difference
     */
    public Money subtract(Money other) {
        checkSameCurrency(other);
        if (this.minorUnits < other.minorUnits) {
            throw new IllegalArgumentException(
                    "Subtraction results in negative money; debt is tracked separately: " + this + " - " + other);
        }
        long result = Math.subtractExact(this.minorUnits, other.minorUnits);
        return new Money(result, this.currency);
    }

    /**
     * Returns whether this amount is strictly positive.
     *
     * @return true if minor units > 0
     */
    public boolean isPositive() {
        return minorUnits > 0;
    }

    /**
     * Returns whether this amount is zero.
     *
     * @return true if minor units == 0
     */
    public boolean isZero() {
        return minorUnits == 0;
    }

    @Override
    public int compareTo(Money other) {
        checkSameCurrency(other);
        return Long.compare(this.minorUnits, other.minorUnits);
    }

    private void checkSameCurrency(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot operate on null Money");
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Money other = (Money) o;
        return minorUnits == other.minorUnits && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minorUnits, currency);
    }

    @Override
    public String toString() {
        int fractionDigits = currency.getDefaultFractionDigits();
        long factor = computeFactor(fractionDigits);
        long whole = minorUnits / factor;
        long fraction = minorUnits % factor;
        String formatPattern = "%d.%0" + fractionDigits + "d";
        return String.format(formatPattern, whole, fraction);
    }

    /**
     * Formats the monetary amount with its currency symbol (e.g. ₹100.00).
     *
     * @return formatted currency string
     */
    public String toFormattedString() {
        return currency.getSymbol() + toString();
    }
}
