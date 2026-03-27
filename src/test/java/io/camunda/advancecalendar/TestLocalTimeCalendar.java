package io.camunda.advancecalendar;


import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceFunction;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import io.camunda.connector.calendaradvance.advancehour.HourFunction;
import io.camunda.connector.calendaradvance.timemachine.SlotContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TestLocalTimeCalendar {
    private final Logger logger = LoggerFactory.getLogger(TestLocalTimeCalendar.class.getName());

    @Test
    public void testHourForwardHoliday() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-01-16T15:34:00";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT6H";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("US");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);
            // Duration: 360 mn
            // Friday 16: 2:26 = 146
            // Monday 19 : holiday in the US
            // Tuesday 20:  360-146=214 mn => 09:00+214mn (3h34) =12:34
            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}]", output.foundDate, output.resultDate, output.resultZonedDate);
            assertTrue(output.foundDate);
            assertEquals(LocalDateTime.of(2026, 1, 20, 12, 34), output.resultDate);
            assertNull(output.resultZonedDate);
            logger.info("testHourForwardHoliday OK ");
        } catch (Exception e) {
            logger.error("testHourAdvanceHoliday", e);
            assert false;
        }
    }

    @Test
    public void testHourBackwardHoliday() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-07-15T10:34:00";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_BACKWARD;
        calendarInput.duration = "PT12H10M";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);
            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}]", output.foundDate, output.resultDate, output.resultZonedDate);
            // Duration: 730 mn
            // Wednesday 15: 94mn
            // Tuesday 14 : holiday
            // Monday 13:  9h=> 540
            // Friday 10: 96 mn => 18:00-96=16:24
            assertTrue(output.foundDate);
            assert (output.resultDate.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.of(2026, 7, 10, 16, 24)));
            assertNull(output.resultZonedDate);
            logger.info("testHourBackwardHoliday OK ");
        } catch (Exception e) {
            logger.error("testHourBackwardHoliday", e);
            assert false;
        }
    }

    @Test
    public void testHourMultipleSlots() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-03-26T11:50:00";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT18H20M";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("US");
        calendarInput.businessCalendar = List.of(
                "Monday=09:00:00-12:00:00,14:10:00-18:00:00",
                "Tuesday=09:00:00-12:00:00,14:10:00-18:00:00",
                "Wednesday=09:00:00-12:00:00,14:10:00-18:00:00",
                "Thursday=09:00:00-12:00:00,14:10:00-18:00:00",
                "Friday=09:00:00-12:00:00");

        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate, output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));
            // Duration: 730 mn
            // Wednesday 15: 94mn
            // Tuesday 14 : holiday
            // Monday 13:  9h=> 540
            // Friday 10: 96 mn => 18:00-96=16:24
            /*
                    |Day         |Use                                              |Relicat                        |
                    |-------------|--------------------------------------------------|--------------------------------|
                    |Thursday 26  |11:50:00-12:00:00+14:10:00-18:00:00=10+230mn |1100-240=860|
                    |Friday 27    |09:00:00-12:00:00:180mn                       |860-180=680|
                    |Saturday 29  |Close                                            | |
                    |Sunday 30    |Close                                            | |
                    |Monday 30    |09:00:00-12:00:00=180mn                       |680-180=500|
                    |             |14:10:00-18:00:00=230mn                       |500-230=270|
                    |Tuesday 31   |09:00:00-12:00:00:180mn                       |270-180=90|
                    |             |14:10:00-18:00:00=230mn                       |230>96:09:00+90mn=10:30|

             */
            assertTrue(output.foundDate);
            assert (output.resultDate.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.of(2026, 3, 31, 15, 40)));
            assertNull(output.resultZonedDate);
            logger.info("testHourMultipleSlots OK ");
        } catch (Exception e) {
            logger.error("testHourAdvanceHoliday", e);
            assert false;
        }
    }

    @Test
    public void testHourMultipleSlotsReverse() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-03-31T15:48:00";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_BACKWARD;
        calendarInput.duration = "PT18H20M";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("US");
        calendarInput.businessCalendar = List.of(
                "Monday=09:00:00-12:00:00,14:10:00-18:00:00",
                "Tuesday=09:00:00-12:00:00,14:10:00-18:00:00",
                "Wednesday=09:00:00-12:00:00,14:10:00-18:00:00",
                "Thursday=09:00:00-12:00:00,14:10:00-18:00:00",
                "Friday=09:00:00-12:00:00");

        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate, output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));
            // Duration: 730 mn
            // Wednesday 15: 94mn
            // Tuesday 14 : holiday
            // Monday 13:  9h=> 540
            // Friday 10: 96 mn => 18:00-96=16:24
            /*
                    |Day         |Use                                              |Relicat                        |
                    |-------------|--------------------------------------------------|--------------------------------|
                    |Thursday 26  |11:50:00-12:00:00+14:10:00-18:00:00=10+230mn |1100-240=860|
                    |Friday 27    |09:00:00-12:00:00:180mn                       |860-180=680|
                    |Saturday 29  |Close                                            | |
                    |Sunday 30    |Close                                            | |
                    |Monday 30    |09:00:00-12:00:00=180mn                       |680-180=500|
                    |             |14:10:00-18:00:00=230mn                       |500-230=270|
                    |Tuesday 31   |09:00:00-12:00:00:180mn                       |270-180=90|
                    |             |14:10:00-18:00:00=230mn                       |230>96:09:00+90mn=10:30|

             */
            assertTrue(output.foundDate);
            assert (output.resultDate.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.of(2026, 3, 26, 11, 58)));
            assertNull(output.resultZonedDate);
            logger.info("testHourMultipleSlotsReverse OK ");
        } catch (Exception e) {
            logger.error("testHourMultipleSlotsReverse", e);
            assert false;
        }
    }

    @Test
    public void testHourOverTwoHolidays() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-07-02T17:15:00";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT60H50M";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("US","FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate, output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            assert (output.resultDate.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.of(2026, 7, 15, 15, 5)));
            assertNull(output.resultZonedDate);
            logger.info("testHourOverTwoHolidays OK ");
        } catch (Exception e) {
            logger.error("testHourAdvanceHoliday", e);
            assert false;
        }
    }

    @Test
    public void testNewYear() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-12-30T13:54";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT15H15M";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("US");
        calendarInput.businessCalendar = null;

        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate, output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            assert (output.resultDate.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.of(2027, 1, 4, 11, 9)));
            assertNull(output.resultZonedDate);
            logger.info("testNewYear OK ");
        } catch (Exception e) {
            logger.error("testHourAdvanceHoliday", e);
            assert false;
        }
    }




    @Test
    public void test247() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-09-11T13:54";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "P10D";
        calendarInput.useHolidays = false;
        calendarInput.businessCalendar = List.of(SlotContainer.SLOT_24_7);

        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate, output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            assert (output.resultDate.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.of(2026, 9, 21, 13, 54)));
            assertNull(output.resultZonedDate);
            logger.info("test247 OK ");
        } catch (Exception e) {
            logger.error("test247", e);
            assert false;
        }
    }

    @Test
    public void test247Reverse() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-09-11T13:54";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_BACKWARD;
        calendarInput.duration = "P2D";
        calendarInput.useHolidays = false;
        calendarInput.businessCalendar = List.of(SlotContainer.SLOT_24_7);

        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate, output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            assert (output.resultDate.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.of(2026, 9, 9, 13, 54)));
            assertNull(output.resultZonedDate);
            logger.info("test247Reverse OK ");
        } catch (Exception e) {
            logger.error("test247Reverse", e);
            assert false;
        }
    }

    @Test
    public void testSpecificDate() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-05-13T15:18";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT20H";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar =  List.of("Monday=09:00:00-18:00:00",
                "Tuesday=09:00:00-18:00:00",
                "Wednesday=09:00:00-18:00:00",
                "Thursday=09:00:00-18:00:00",
                "Friday=09:00:00-18:00:00",
                SlotContainer.SPECIFIC_DAY_PREFIX+"2026/05/14=09:00-11:40", // Ascension day : it's a holiday in France, but we are open
                SlotContainer.SPECIFIC_DAY_PREFIX+"2026/05/15=09:00-11:50"); // Day after Ascension

        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate, output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            assert (output.resultDate.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.of(2026, 5, 19, 11, 48)));
            assertNull(output.resultZonedDate);
            logger.info("testSpecificDate OK ");
        } catch (Exception e) {
            logger.error("testSpecificDate", e);
            assert false;
        }
    }

    @Test
    public void testAfterPeriodSpecificDate() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-05-14T15:18";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT20H";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar =  List.of("Monday=09:00:00-18:00:00",
                "Tuesday=09:00:00-18:00:00",
                "Wednesday=09:00:00-18:00:00",
                "Thursday=09:00:00-18:00:00",
                "Friday=09:00:00-18:00:00",
                SlotContainer.SPECIFIC_DAY_PREFIX+"2026/05/14=09:00-12:00", // Ascension day : but start time is AFTER
                SlotContainer.SPECIFIC_DAY_PREFIX+"2026/05/15=09:00-12:00"); // Day after Ascension



        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate, output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            assert (output.resultDate.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.of(2026, 5, 19, 17, 0)));
            assertNull(output.resultZonedDate);
            logger.info("testSpecificDate OK ");
        } catch (Exception e) {
            logger.error("testSpecificDate", e);
            assert false;
        }
    }

    @Test
    public void testAftertwoPeriodsSpecificDate() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-05-14T13:18"; // start mid-second period
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT20H";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar =  List.of("Monday=09:00:00-18:00:00",
                "Tuesday=09:00:00-18:00:00",
                "Wednesday=09:00:00-18:00:00",
                "Thursday=09:00:00-18:00:00",
                "Friday=09:00:00-18:00:00",
                SlotContainer.SPECIFIC_DAY_PREFIX+"2026/05/14=09:00-12:00, 13:00-14:30, 17:00-19:00", // Ascension day : it's a holiday in France, but we are open
                SlotContainer.SPECIFIC_DAY_PREFIX+"2026/05/15=09:00-12:00"); // Day after Ascension


        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate, output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            assert (output.resultDate.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.of(2026, 5, 19, 13, 48)));
            assertNull(output.resultZonedDate);
            logger.info("testSpecificDate OK ");
        } catch (Exception e) {
            logger.error("testSpecificDate", e);
            assert false;
        }
    }

}
