package io.camunda.connector.calendaradvance.timemachine;


import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * This class contains slot contains given at the configuration
 */
public class SlotContainer {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final List<String> defaultListSlots = List.of("Monday=09:00:00-18:00:00",
            "Tuesday=09:00:00-18:00:00",
            "Wednesday=09:00:00-18:00:00",
            "Thursday=09:00:00-18:00:00",
            "Friday=09:00:00-18:00:00");
    private final List<Period> specificPeriods = new ArrayList<>();
    private Map<DayOfWeek, List<Period>> daysPeriods = new HashMap<>();

    /**
     * Give the list of slot
     * A slot is <Day>=<period>+
     * For example:
     * "Monday=08:00:00-12:00:00,14:00:00-18:00:00",
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
        if (listSlots.size() == 1 && "24/7".equals(listSlots.get(0))) {
            daysPeriods = new HashMap<>();
            daysPeriods.put(DayOfWeek.MONDAY, List.of(Period.getPeriod(DayOfWeek.MONDAY, LocalTime.of(0, 0), LocalTime.of(23, 59))));
            daysPeriods.put(DayOfWeek.TUESDAY, List.of(Period.getPeriod(DayOfWeek.TUESDAY, LocalTime.of(0, 0), LocalTime.of(23, 59))));
            daysPeriods.put(DayOfWeek.WEDNESDAY, List.of(Period.getPeriod(DayOfWeek.WEDNESDAY, LocalTime.of(0, 0), LocalTime.of(23, 59))));
            daysPeriods.put(DayOfWeek.THURSDAY, List.of(Period.getPeriod(DayOfWeek.THURSDAY, LocalTime.of(0, 0), LocalTime.of(23, 59))));
            daysPeriods.put(DayOfWeek.FRIDAY, List.of(Period.getPeriod(DayOfWeek.FRIDAY, LocalTime.of(0, 0), LocalTime.of(23, 59))));
            daysPeriods.put(DayOfWeek.SATURDAY, List.of(Period.getPeriod(DayOfWeek.SATURDAY, LocalTime.of(0, 0), LocalTime.of(23, 59))));
            daysPeriods.put(DayOfWeek.SUNDAY, List.of(Period.getPeriod(DayOfWeek.SUNDAY, LocalTime.of(0, 0), LocalTime.of(23, 59))));
            return;
        }

        for (String slot : listSlots) {

            String[] parts = slot.split("=");
            if (parts.length < 2) {
                continue;
            }
            String day = parts[0];
            String[] periods = parts[1].split(",");
            if (periods.length < 2) {
                continue;
            }
            for (String onePeriod : periods) {
                String[] slice = onePeriod.split("-");
                Period period = new Period();
                period.startTime = LocalTime.parse(slice[0]);
                period.endTime = LocalTime.parse(slice[1]);
                if (day.toUpperCase().startsWith("DAY")) {
                    period.specificDay = LocalDate.parse(day, formatter);
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
            return cursor.toLocalDate().minusDays(1).atTime(23, 59, 59);
    }

    /**
     * Here the calculation to find a next period from the reference date
     *
     * @param referenceDate search at this moment
     * @param advance       if true, the next period in the calendar are found, else we search backward
     * @return an advanceResult; which contain the period adjusted, the orginal period, the new reference date
     */
    public AdvanceResult getNextPeriod(LocalDateTime referenceDate, boolean advance) {

        LocalDateTime cursor = referenceDate;

        for (int i = 0; i < 100; i++) {
            DayOfWeek currentDay = cursor.getDayOfWeek();

            // 1️⃣ specificDay match (priority)
            Optional<Period> specific = specificPeriods.stream()
                    .filter(p -> p.specificDay != null)
                    .filter(p -> p.specificDay.equals(currentDay))
                    .findFirst();
            if (specific.isPresent()) {
                if (matchPeriod(cursor, advance, specific.get())) {
                    // Ok, we can use this period, else we have to go to the next day
                    return AdvanceResult.getResult(cursor, advance, specific.get());
                }
            } else {
                // we may have multiple period in the same days
                LocalDateTime finalCursor = cursor;
                Optional<Period> periodDay = daysPeriods
                        .getOrDefault(currentDay, List.of())
                        .stream()
                        .filter(p -> matchPeriod(finalCursor, advance, p))
                        .findFirst();
                if (periodDay.isPresent()) {
                    return AdvanceResult.getResult(cursor, advance, periodDay.get());
                }
            }
            // Ok, we don't find a period for this day, advance the cursor to the next (or previous) day
            cursor = advanceNextDay(cursor, advance);
        }
        // not found after x iterations
        AdvanceResult result = new AdvanceResult();
        result.foundPeriod = false;
        return result;
    }

    private boolean matchPeriod(LocalDateTime reference, boolean advance, Period period) {
        LocalTime referenceTime = reference.toLocalTime();
        if (period.startTime.isBefore(referenceTime) && period.endTime.isAfter(referenceTime))
            return true;
        if (referenceTime.isBefore(period.startTime) && advance)
            return true;
        return referenceTime.isAfter(period.endTime) && !advance;

    }

    /**
     * The period can reference a day of week (Monday, Thursday) or especialy a date (July 14, 2026)
     */
    public static class Period {
        public DayOfWeek dayOfWeek;
        public LocalDate specificDay;
        public LocalTime startTime;
        public LocalTime endTime;

        public static Period getPeriod(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
            Period period = new Period();
            period.dayOfWeek = dayOfWeek;
            period.startTime = startTime;
            period.endTime = endTime;
            return period;
        }

        public long getMinutes() {
            return Duration.between(startTime, endTime).toMinutes();
        }
    }

    public static class AdvanceResult {
        public Period referencePeriod;
        public Period period;
        public LocalDateTime newDate;
        public boolean foundPeriod;

        public static AdvanceResult getResult(LocalDateTime referenceDate, boolean advance, Period referencePeriod) {
            AdvanceResult advanceResult = new AdvanceResult();
            advanceResult.referencePeriod = referencePeriod;
            advanceResult.foundPeriod = true;
            advanceResult.period = new Period();
            if (advance) {
                advanceResult.period.startTime = referenceDate.toLocalTime().isBefore(referencePeriod.startTime) ? referencePeriod.startTime : referenceDate.toLocalTime();
                advanceResult.period.endTime = referencePeriod.endTime;
                advanceResult.newDate = advanceResult.period.endTime.atDate(referenceDate.toLocalDate());
            } else {
                advanceResult.period.startTime = referencePeriod.startTime;
                advanceResult.period.endTime = referenceDate.toLocalTime().isAfter(referencePeriod.endTime) ? referencePeriod.endTime : referenceDate.toLocalTime();
                advanceResult.newDate = advanceResult.period.startTime.atDate(referenceDate.toLocalDate());
            }
            return advanceResult;
        }


    }
}
