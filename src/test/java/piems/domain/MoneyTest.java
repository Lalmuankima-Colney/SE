package piems.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Money} value object.
 *
 * <p>Verifies exact integer minor unit arithmetic, currency validation,
 * formatting, parsing, non-negative invariants, and equality semantics.</p>
 */
class MoneyTest {

    private static final Currency INR = Currency.getInstance("INR");
    private static final Currency USD = Currency.getInstance("USD");

    /**
     * TC-NFR01-01: Verifies that summing 1,000 entries of 0.10 yields exactly 100.00
     * without any floating-point accumulation drift.
     */
    @Test
    @DisplayName("TC-NFR01-01: Summing 1,000 entries of 0.10 equals exactly 100.00")
    void sumsThousandTenCentEntriesToExactlyOneHundred() {
        Money sum = Money.zero(INR);
        Money tenPaise = Money.of("0.10", INR);

        for (int i = 0; i < 1000; i++) {
            sum = sum.add(tenPaise);
        }

        Money expected = Money.of("100.00", INR);
        assertEquals(expected, sum);
        assertEquals(10000L, sum.minorUnits());
    }

    /**
     * TC-FR19-01: Verifies that decimal strings with more decimal places
     * than permitted by the currency are rejected.
     */
    @Test
    @DisplayName("TC-FR19-01: Rejects inputs with more than permitted decimal places")
    void rejectsTooManyDecimals() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("0.001", INR));
        assertThrows(IllegalArgumentException.class, () -> Money.of("10.125", INR));
    }

    /**
     * Q-003 / D-31: Verifies that negative money amounts are rejected.
     * Debt is tracked as a separate variable.
     */
    @Test
    @DisplayName("Q-003/D-31: Rejects negative amounts on construction and parsing")
    void rejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> Money.ofMinor(-1L, INR));
        assertThrows(IllegalArgumentException.class, () -> Money.of("-0.01", INR));
        assertThrows(IllegalArgumentException.class, () -> Money.of("-50.00", INR));
    }

    /**
     * Q-003 / D-31: Verifies that subtraction resulting in a negative value is rejected.
     */
    @Test
    @DisplayName("Q-003/D-31: Rejects subtraction resulting in negative money")
    void rejectsSubtractionBelowZero() {
        Money smaller = Money.of("10.00", INR);
        Money larger = Money.of("20.00", INR);

        assertThrows(IllegalArgumentException.class, () -> smaller.subtract(larger));
    }

    /**
     * Verifies parsing valid whole and fractional decimal amounts.
     */
    @Test
    @DisplayName("Parses valid decimal string representations")
    void parsesValidDecimalStrings() {
        assertEquals(0L, Money.of("0", INR).minorUnits());
        assertEquals(0L, Money.of("0.00", INR).minorUnits());
        assertEquals(500L, Money.of("5", INR).minorUnits());
        assertEquals(550L, Money.of("5.5", INR).minorUnits());
        assertEquals(550L, Money.of("5.50", INR).minorUnits());
        assertEquals(100000L, Money.of("1000.00", INR).minorUnits());
    }

    /**
     * Verifies that invalid string formats throw IllegalArgumentException.
     */
    @Test
    @DisplayName("Rejects malformed and null string inputs")
    void rejectsInvalidStringFormats() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(null, INR));
        assertThrows(IllegalArgumentException.class, () -> Money.of("", INR));
        assertThrows(IllegalArgumentException.class, () -> Money.of("   ", INR));
        assertThrows(IllegalArgumentException.class, () -> Money.of("abc", INR));
        assertThrows(IllegalArgumentException.class, () -> Money.of("12.34.56", INR));
    }

    /**
     * Verifies that arithmetic overflow throws ArithmeticException.
     */
    @Test
    @DisplayName("Rejects arithmetic overflow on addition")
    void rejectsArithmeticOverflow() {
        Money maxMoney = Money.ofMinor(Long.MAX_VALUE, INR);
        Money onePaise = Money.ofMinor(1L, INR);

        assertThrows(ArithmeticException.class, () -> maxMoney.add(onePaise));
    }

    /**
     * Verifies that operations mixing different currencies throw IllegalArgumentException.
     */
    @Test
    @DisplayName("Rejects operations between mismatched currencies")
    void rejectsCurrencyMismatch() {
        Money inrMoney = Money.of("10.00", INR);
        Money usdMoney = Money.of("10.00", USD);

        assertThrows(IllegalArgumentException.class, () -> inrMoney.add(usdMoney));
        assertThrows(IllegalArgumentException.class, () -> inrMoney.subtract(usdMoney));
        assertThrows(IllegalArgumentException.class, () -> inrMoney.compareTo(usdMoney));
    }

    /**
     * Verifies value object equality and hash code contracts.
     */
    @Test
    @DisplayName("Satisfies equals and hashCode contracts")
    void satisfiesEqualsAndHashCodeContract() {
        Money m1 = Money.of("50.00", INR);
        Money m2 = Money.of("50.00", INR);
        Money m3 = Money.ofMinor(5000L, INR);
        Money diffAmount = Money.of("60.00", INR);
        Money diffCurrency = Money.of("50.00", USD);

        assertEquals(m1, m1);
        assertEquals(m1, m2);
        assertEquals(m2, m3);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertNotEquals(m1, diffAmount);
        assertNotEquals(m1, diffCurrency);
        assertNotEquals(null, m1);
        assertNotEquals("50.00", m1);
    }

    /**
     * Verifies Comparable ordering contract.
     */
    @Test
    @DisplayName("Satisfies Comparable ordering contract")
    void satisfiesComparableContract() {
        Money m10 = Money.of("10.00", INR);
        Money m20 = Money.of("20.00", INR);
        Money m20Duplicate = Money.of("20.00", INR);

        assertTrue(m10.compareTo(m20) < 0);
        assertTrue(m20.compareTo(m10) > 0);
        assertEquals(0, m20.compareTo(m20Duplicate));
    }

    /**
     * Verifies string formatting output.
     */
    @Test
    @DisplayName("Formats decimal and currency string representations")
    void formatsToStringAndFormattedString() {
        Money m = Money.of("123.45", INR);

        assertEquals("123.45", m.toString());
        assertEquals("₹123.45", m.toFormattedString());
        assertEquals(INR, m.currency());
        assertEquals(12345L, m.minorUnits());
    }

    /**
     * Verifies default currency factory methods and predicates.
     */
    @Test
    @DisplayName("Tests default currency factories and predicates")
    void testsDefaultCurrencyFactoriesAndPredicates() {
        Money defZero = Money.zero();
        assertTrue(defZero.isZero());
        assertEquals(0L, defZero.minorUnits());
        assertEquals(INR, defZero.currency());

        Money defMinor = Money.ofMinor(500L);
        assertTrue(defMinor.isPositive());
        assertEquals(500L, defMinor.minorUnits());

        Money defString = Money.of("10.00");
        assertEquals(1000L, defString.minorUnits());

        assertThrows(IllegalArgumentException.class, () -> Money.of("10.00", null));
        assertThrows(IllegalArgumentException.class, () -> defString.add(null));
    }
}
