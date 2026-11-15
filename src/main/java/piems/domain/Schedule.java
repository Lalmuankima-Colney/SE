package piems.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Schedule definition and deterministic due date calculation for recurring transactions (FR-11).
 *
 * <p>Supports daily, weekly, and monthly frequencies. Implements month-end date clamping
 * for monthly schedules starting on the 29th, 30th, or 31st according to Decision D-29 and Q-010.</p>
 */
public final class Schedule {

    /**
     * Recurrence frequency for transaction scheduling.
     */
    public enum Frequency {
        /** Generates every calendar day. */
        DAILY,

        /** Generates every seven days. */
        WEEKLY,

        /** Generates once each calendar month with month-end clamping. */
        MONTHLY
    }

    private final Frequency frequency;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int targetDayOfMonth;

    /**
     * Constructs a Schedule with an optional termination end date.
     *
     * @param frequency recurrence frequency
     * @param startDate inclusive first generation start date
     * @param endDate   optional inclusive termination date, or null if indefinite
     * @throws IllegalArgumentException if endDate is before startDate
     * @throws NullPointerException     if frequency or startDate is null
     */
    public Schedule(Frequency frequency, LocalDate startDate, LocalDate endDate) {
        this.frequency = Objects.requireNonNull(frequency, "frequency cannot be null");
        this.startDate = Objects.requireNonNull(startDate, "startDate cannot be null");
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate cannot be before startDate");
        }
        this.endDate = endDate;
        this.targetDayOfMonth = startDate.getDayOfMonth();
    }

    /**
     * Constructs an indefinite Schedule starting at the given date.
     *
     * @param frequency recurrence frequency
     * @param startDate inclusive first generation start date
     */
    public Schedule(Frequency frequency, LocalDate startDate) {
        this(frequency, startDate, null);
    }

    /**
     * Computes all due dates strictly after {@code fromAfter} up to {@code until}.
     *
     * @param fromAfter lower date boundary (exclusive), or null to start from startDate
     * @param until     upper date boundary (inclusive)
     * @return unmodifiable list of chronological due dates
     * @throws NullPointerException if until is null
     */
    public List<LocalDate> computeDueDates(LocalDate fromAfter, LocalDate until) {
        Objects.requireNonNull(until, "until cannot be null");
        if (until.isBefore(startDate)) {
            return List.of();
        }
        LocalDate effectiveEnd = (endDate != null && endDate.isBefore(until)) ? endDate : until;
        if (fromAfter != null && !fromAfter.isBefore(effectiveEnd)) {
            return List.of();
        }

        return switch (frequency) {
            case DAILY -> computeDailyDueDates(fromAfter, effectiveEnd);
            case WEEKLY -> computeWeeklyDueDates(fromAfter, effectiveEnd);
            case MONTHLY -> computeMonthlyDueDates(fromAfter, effectiveEnd);
        };
    }

    private List<LocalDate> computeDailyDueDates(LocalDate fromAfter, LocalDate effectiveEnd) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate curr = (fromAfter != null && fromAfter.isAfter(startDate))
                ? fromAfter.plusDays(1)
                : startDate;
        while (!curr.isAfter(effectiveEnd)) {
            dates.add(curr);
            curr = curr.plusDays(1);
        }
        return Collections.unmodifiableList(dates);
    }

    private List<LocalDate> computeWeeklyDueDates(LocalDate fromAfter, LocalDate effectiveEnd) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate curr = startDate;
        while (fromAfter != null && !curr.isAfter(fromAfter)) {
            curr = curr.plusWeeks(1);
        }
        while (!curr.isAfter(effectiveEnd)) {
            dates.add(curr);
            curr = curr.plusWeeks(1);
        }
        return Collections.unmodifiableList(dates);
    }

    private List<LocalDate> computeMonthlyDueDates(LocalDate fromAfter, LocalDate effectiveEnd) {
        List<LocalDate> dates = new ArrayList<>();
        YearMonth currYm = YearMonth.from(startDate);
        while (true) {
            int day = Math.min(targetDayOfMonth, currYm.lengthOfMonth());
            LocalDate dueDate = currYm.atDay(day);
            if (dueDate.isAfter(effectiveEnd)) {
                break;
            }
            if (fromAfter == null || dueDate.isAfter(fromAfter)) {
                if (!dueDate.isBefore(startDate)) {
                    dates.add(dueDate);
                }
            }
            currYm = currYm.plusMonths(1);
        }
        return Collections.unmodifiableList(dates);
    }

    /**
     * Returns the recurrence frequency.
     *
     * @return frequency enum
     */
    public Frequency getFrequency() {
        return frequency;
    }

    /**
     * Returns the schedule start date.
     *
     * @return inclusive start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Returns the schedule termination end date.
     *
     * @return optional end date, or null if indefinite
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Returns the target day of month for monthly schedules.
     *
     * @return day of month (1 to 31)
     */
    public int getTargetDayOfMonth() {
        return targetDayOfMonth;
    }
}
