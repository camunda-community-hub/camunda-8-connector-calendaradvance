package io.camunda.connector.calendaradvance.timemachine;


import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.calendaradvance.toolbox.CalendarAdvanceError;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * This class contains slot contains given at the configuration
 */
public class SlotContainer {


    public static final String SLOT_24_7 = "24/7";
    /**
     * the minus namo
     * when we advance in revert with a 24/7 calendar, on dau 9 the "next date" is Day 8 23:59. But doing that, the day 8 will have 1 days -1.
     * To avoid that, the "next date" is Day 8 23:59:59 9999999.
     * But when we search the period on day 8, we must manage this situation and assume the end of the day is not 23:59:50 but midnight, so the period calculation is correctly the full day
     */
    public final static LocalTime MIDNIGHT_MINUS = LocalTime.MIDNIGHT.minusNanos(1);
    private static final DateTimeFormatter formatterSpecificDay = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final List<String> defaultListSlots = List.of("Monday=09:00:00-18:00:00",
            "Tuesday=09:00:00-18:00:00",
            "Wednesday=09:00:00-18:00:00",
            "Thursday=09:00:00-18:00:00",
            "Friday=09:00:00-18:00:00");
    public static final String SPECIFIC_DAY_PREFIX = "DAY_";
    private final List<Period> specificPeriods = new ArrayList<>();
    private Map<DayOfWeek, List<Period>> daysPeriods = new HashMap<>();

    /**
     * Give the list of slot
     * A slot is <Day>=<period>+
     * For example:
     * "Monday=08:00:00-12:00:00, 14:00:00-18:00:00",
     * or
     * "Day_2026/07/14=08:00:00-12:00:00"
     *
     * @param listSlots list slot to load in the object
     */
    public void setSlots(List<String> listSlots) {
        if (listSlots == null || listSlots.isEmpty()) {
            listSlots = defaultListSlots;
        }

        // Special use case
        if (listSlots.size() == 1 && SLOT_24_7.equals(listSlots.get(0))) {
            daysPeriods = new HashMap<>();
            daysPeriods.put(DayOfWeek.MONDAY, List.of(Period.getPeriod(DayOfWeek.MONDAY, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)));
            daysPeriods.put(DayOfWeek.TUESDAY, List.of(Period.getPeriod(DayOfWeek.TUESDAY, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)));
            daysPeriods.put(DayOfWeek.WEDNESDAY, List.of(Period.getPeriod(DayOfWeek.WEDNESDAY, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)));
            daysPeriods.put(DayOfWeek.THURSDAY, List.of(Period.getPeriod(DayOfWeek.THURSDAY, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)));
            daysPeriods.put(DayOfWeek.FRIDAY, List.of(Period.getPeriod(DayOfWeek.FRIDAY, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)));
            daysPeriods.put(DayOfWeek.SATURDAY, List.of(Period.getPeriod(DayOfWeek.SATURDAY, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)));
            daysPeriods.put(DayOfWeek.SUNDAY, List.of(Period.getPeriod(DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)));
            return;
        }

        for (String slot : listSlots) {
            String[] parts = slot.split("=");
            if (parts.length < 2) {
                throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_PERIOD, "A period must follow <day>=<value>. Period [{}] don't follow this pattern");
            }
            String day = parts[0];
            String[] periods = parts[1].split(",");

            for (String onePeriod : periods) {
                String[] slice = onePeriod.split("-");
                if (slice.length != 2) {
                    throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_PERIOD, "A period must follow <day>=<period>[,*] . <period> is <localtime>-<localtime> like 09:00-12:30. Period does not contains a - ");

                }
                Period period = new Period();
                period.startTime = parseLocalTimeWithException(slice[0], slot);
                period.endTime = parseLocalTimeWithException(slice[1], slot);
                if (day.toUpperCase().startsWith(SPECIFIC_DAY_PREFIX)) {
                    period.specificDay = parseLocalDateWithException(day.substring(SPECIFIC_DAY_PREFIX.length()), slot);
                    specificPeriods.add(period);
                } else {
                    period.dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
                    daysPeriods
                            .computeIfAbsent(period.dayOfWeek, k -> new ArrayList<>())
                            .add(period);
                }
            }
        }
    }


    public LocalDateTime advanceNextDay(LocalDateTime cursor, boolean advance) {
        if (advance)
            return cursor.toLocalDate().plusDays(1).atStartOfDay();
        else
            return cursor.toLocalDate().minusDays(1).atTime(MIDNIGHT_MINUS);
    }

    /**
     * Here the calculation to find a next period from the reference date.
     * The period is adapted to the reference date: for example, if the reference date start at 10:13, and a period 9:00-12:00 is found the period returned
     * is 10:13-1200 if advance, or 09:00-1°:13 is ! advance
     *
     * @param referenceDate search at this moment
     * @param advance       if true, the next period in the calendar are found, else we search backward
     * @return an advanceResult; which contain the period adjusted, the orginal period, the new reference date
     */
    public AdvanceResult getNextPeriod(LocalDateTime referenceDate, boolean advance, boolean useHoliday, List<String> countriesCode) {

        LocalDateTime cursor = referenceDate;

        for (int i = 0; i < 100; i++) {
            DayOfWeek currentDay = cursor.getDayOfWeek();

            final LocalDateTime finalCursor = cursor;
            // Specific?
            List<Period> listMatchPeriod = specificPeriods.stream()
                    .filter(p -> p.specificDay != null)
                    .filter(p -> p.specificDay.getDayOfWeek().equals(currentDay))
                    .toList();

            // It maybe a specificDay upgrade, but according to the time, maybe  no more period is found
            boolean foundSpecificDay = ! listMatchPeriod.isEmpty();
            listMatchPeriod = listMatchPeriod.stream()
                    .filter(p -> matchPeriod(finalCursor, advance, p))
                    .toList();

            // holiday ?
            if (! foundSpecificDay && useHoliday && HolidayContainer.getInstance().isHoliday(cursor.toLocalDate(), countriesCode)) {
                // we have to advance
                cursor = advanceNextDay(cursor, advance);
                continue;
            }

            // Get all periods in this day
            if (!foundSpecificDay) {
                listMatchPeriod = daysPeriods
                        .getOrDefault(currentDay, List.of())
                        .stream()
                        .filter(p -> matchPeriod(finalCursor, advance, p)).toList();
                // To find the correct period, we must ORDER it according the advance : if there is P1, P2, P3, we must choose the first period in the day
            }
            // Now sort the list of periods
            Optional<Period> periodDay = listMatchPeriod.stream()
                    .sorted(advance
                            ? Comparator.comparing((Period p) -> p.startTime)
                            : Comparator.comparing((Period p) -> p.startTime).reversed())
                    .findFirst();


            if (periodDay.isPresent()) {
                return AdvanceResult.getResult(cursor, advance, periodDay.get());
            }
            cursor = advanceNextDay(cursor, advance);
        }

        // not found after x iterations
        AdvanceResult result = new AdvanceResult();
        result.foundPeriod = false;
        return result;
    }



    /* ******************************************************************** */
    /*                                                                      */
    /*  Private method                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    private LocalTime parseLocalTimeWithException(String text, String slot) throws ConnectorException{
        try {
            return LocalTime.parse(text.trim());
        } catch (DateTimeException e) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_PERIOD, "Can't parse LocalTime with text[" + text + ") in slot [" + slot + "]");
        }
    }
    private LocalDate parseLocalDateWithException(String text, String slot) throws ConnectorException{
        try {
            return LocalDate.parse(text.trim(), formatterSpecificDay);
        } catch (DateTimeException e) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_PERIOD, "Can't parse LocalDate with text[" + text + ") in slot [" + slot + "]");
        }
    }
    /**
     * note: if referenceTime= 00:00, it is not possible to kwno if this is 0:00 or 23:59:00
     */
    private boolean matchPeriod(LocalDateTime reference, boolean advance, Period period) {
        LocalTime referenceTime = reference.toLocalTime();

        // the LocalTime.MIDNIGHT convention for endTime : then this is the end of the day
        /**
         * The reference is strictly INSIDE the period: GOOD
         *  START   R     END
         */
        if (period.startTime.isBefore(referenceTime) && (period.endTime.equals(LocalTime.MIDNIGHT) || period.endTime.isAfter(referenceTime)))
            return true;

        if (advance) {
            /**  the reference is AT THE BEGINNING of the period: GOOD
             *      Advance >>>>>
             *      START
             *      R
             *
             * or   The period is AFTER the reference : good
             *      Advance >>>>>
             *         R         START
             */
            return referenceTime.equals(period.startTime) || referenceTime.isBefore(period.startTime);

        } else {
            // Manage spacial case
            // referenceTime ==00:00 : period can match
            // period.endTime = 00:00 ==> Meaning actually 23:60 so this period match
            if (LocalTime.MIDNIGHT.equals(referenceTime))
                return false;
            if (LocalTime.MIDNIGHT.equals(period.endTime))
                return true;

            /*   <<< forward
             *  the reference is AT THE END of the period: GOOD
             *
             *      END
             *      R
             *
             * or   The period is BEFORE the reference : good
             *         END        R
             * or endTime=MIDNIGHT && reference==MIDNIGHT
             */

            return referenceTime.isAfter(period.endTime) || referenceTime.equals(period.endTime)
                    || (referenceTime.equals(LocalTime.MIDNIGHT) && period.endTime.equals(LocalTime.MIDNIGHT));

        }

    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  Period                                                              */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * The period can reference a day of week (Monday, Thursday) or especially a date (July 14, 2026)
     * The class can be used to instantiate a real period during the calculation, then it will onboard the "dateOfPeriod" variable
     */
    public static class Period {
        public DayOfWeek dayOfWeek;
        public LocalDate specificDay;
        public LocalTime startTime;
        public LocalTime endTime;

        /**
         * A period can be instantiate to show a date in particular
         */
        public LocalDate dateOfPeriod;

        public static Period getPeriod(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
            Period period = new Period();
            period.dayOfWeek = dayOfWeek;
            period.startTime = startTime;
            period.endTime = endTime;
            return period;
        }

        public Period setDateOfPeriod(LocalDate dateOfPeriod) {
            this.dateOfPeriod = dateOfPeriod;
            return this;
        }

        public Period cloneForRealPeriod(LocalDate localDate) {
            Period clone = new Period();
            clone.dayOfWeek = this.dayOfWeek;
            clone.specificDay = this.specificDay;
            clone.startTime = this.startTime;
            clone.endTime = this.endTime;
            clone.dateOfPeriod = localDate;
            return clone;
        }

        public String toString() {
            return (dayOfWeek == null ? "" : dayOfWeek.name()) + " "
                    + (specificDay == null ? "" : specificDay.format(DateTimeFormatter.ISO_LOCAL_DATE) + " ")
                    + (dateOfPeriod == null ? "" : "(" + dateOfPeriod + ") ")
                    + (startTime == null ? "" : startTime.toString()) + "-"
                    + (endTime == null ? "" : endTime.toString())
                    + " (" + (startTime != null && endTime != null ? getMinutes() : "") + " mn)";
        }

        public long getMinutes() {
            if (endTime == null || startTime == null)
                return 0;
            // the MIDNIGHT convention : the end is at the end of the day
            if (LocalTime.MIDNIGHT.equals(endTime) || MIDNIGHT_MINUS.equals(endTime)) {
                // we can get the difference between 23:59 and then we add 1
                return Duration.between(startTime, LocalTime.of(23, 59)).toMinutes() + 1;
            }
            return Duration.between(startTime, endTime).toMinutes();
        }
    }


    /* ******************************************************************** */
    /*                                                                      */
    /*  Advance Result                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    public static class AdvanceResult {
        public Period referencePeriod;
        public Period period;
        public LocalDateTime newDate;
        public boolean foundPeriod;
        public LocalDate periodDate;

        public static AdvanceResult noPeriod() {
            AdvanceResult advanceResult = new AdvanceResult();
            advanceResult.foundPeriod = false;
            return advanceResult;
        }


        /**
         * Return different value according the date and the reference period. Example, referencePeriod is August 9, 09:00-12:00 and reference is 10:12
         * Note: the referenceDate is INSIDE the period on this method
         * <p>
         * IF ADVANCE
         * - period = referenceDate - referencePeriod.endTime   ( 10:12-12:00)
         * - nextDate = referencePeriod.endTime  : so August 9, 12:00 (so to search the next period, this period is out)
         * <p>
         * Case of full day period  (00:00 - 00:00) because there is not a 23:60
         * - Period: referenceDate - referencePeriod.endTime (10:12 - 00:00) : SAME
         * - nextDate: 00:00 ON THE NEXT DAY
         * <p>
         * IF BACKWARD
         * - period = referencePeriod.startTime : referenceDate   ( 09:00 - 10:12)
         * - nextDate = referencePeriod.startTime  : so August 9, 09:00 (so to search the next period, this period is out)
         * <p>
         * Case of full day period  (00:00 - 00:00) because there is not a 23:60
         * - period: referencePeriod.startTime - referenceDAte
         * but here there is a special case: the reference Date may be 23:59:59.999999 so the period is adapted again to 00-00
         * - nextDay: referencePeriod.startTime SAME: Which is 00:00. Next round on the same day will find nothing
         * <p>
         * other field like referencePeriod and dayOfWeek are evident
         *
         * @param referenceDate   referenceDate to calculate the new period
         * @param advance         direction
         * @param referencePeriod referencePeriod to calculate from
         * @return the advanceResult information
         */
        public static AdvanceResult getResult(LocalDateTime referenceDate, boolean advance, Period referencePeriod) {
            AdvanceResult advanceResult = new AdvanceResult();
            advanceResult.referencePeriod = referencePeriod;
            advanceResult.foundPeriod = true;
            advanceResult.period = new Period();
            advanceResult.period.dayOfWeek = referenceDate.getDayOfWeek();
            advanceResult.periodDate = referenceDate.toLocalDate();
            if (advance) {
                advanceResult.period.startTime = referenceDate.toLocalTime().isBefore(referencePeriod.startTime) ? referencePeriod.startTime : referenceDate.toLocalTime();
                advanceResult.period.endTime = referencePeriod.endTime;
                // The MIDNIGHT convention : then the day is NEXT DAY
                advanceResult.newDate = advanceResult.period.endTime.atDate(referenceDate.toLocalDate());
                if (referencePeriod.endTime.equals(LocalTime.MIDNIGHT)) {
                    // 00:00 next day
                    advanceResult.newDate = advanceResult.newDate.plusDays(1);
                }
            } else {
                advanceResult.period.startTime = referencePeriod.startTime;

                // End Date is MIDNIGHT? Special case here, MIDNIGHT is of the day, so the end of the period is the referenceDate
                if (LocalTime.MIDNIGHT.equals(referencePeriod.endTime) || MIDNIGHT_MINUS.equals(referencePeriod.endTime)) {
                    advanceResult.period.endTime = referenceDate.toLocalTime();
                } else {
                    advanceResult.period.endTime = referenceDate.toLocalTime().isAfter(referencePeriod.endTime) ? referencePeriod.endTime : referenceDate.toLocalTime();
                }
                // If the end period is the marker, then actually the end of the period is MIDNIGHT
                if (MIDNIGHT_MINUS.equals(advanceResult.period.endTime)) {
                    advanceResult.period.endTime = LocalTime.MIDNIGHT;
                }
                advanceResult.newDate = advanceResult.period.startTime.atDate(referenceDate.toLocalDate());
            }
            return advanceResult;
        }
    }

}
