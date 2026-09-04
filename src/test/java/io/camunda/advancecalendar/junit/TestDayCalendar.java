package io.camunda.advancecalendar.junit;

import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceFunction;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import io.camunda.connector.calendaradvance.advanceday.DayFunction;
import io.camunda.connector.calendaradvance.timemachine.SlotContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDayCalendar {
    private final Logger logger = LoggerFactory.getLogger(TestDayCalendar.class);

    @Test
    public void backBusinessDay() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-15";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_BACKWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_BUSINESSDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_AFTER;
        calendarInput.duration = "P2D";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {
                logger.info("backBusinessDay Result FoundDate:{} resultDate[{}] resultZonedDate[{}] periods[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));
                // Duration: 730 mn
                // Wednesday 15: 94mn
                // Tuesday 14 : holiday
                // Monday 13:  9h=> 540
                // Friday 10: 96 mn => 18:00-96=16:24
                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 10)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 10)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());
            logger.info("backBusinessDay OK ");

        } catch (Exception e) {
            logger.error("backBusinessDay", e);
            assert false;
        }
    }


    @Test
    public void advanceBusinessDay() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-09";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_BUSINESSDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_RESULT;
        calendarInput.duration = "P4D"; // Friday 12, Monday 13, Wed 15, Thrusday 16 
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {
                logger.info("advanceBusinessDay Result FoundDate:{} resultDate[{}] resultZonedDate[{}] period[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));
                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 16)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 16)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("advanceBusinessDay OK ");
        } catch (Exception e) {
            logger.error("advanceBusinessDay", e);
            assert false;
        }
    }


    @Test
    public void advanceBusinessDay247() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-09";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_BUSINESSDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_RESULT;
        calendarInput.duration = "P5D"; // Friday 12, Monday 13, Wed 15, Thursday 16
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = SlotContainer.CALENDAR_24_7;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {
                logger.info("advanceBusinessDay Result FoundDate:{} resultDate[{}] resultZonedDate[{}] period[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));
                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 15)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 15)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("advanceBusinessDay OK ");
        } catch (Exception e) {
            logger.error("advanceBusinessDay", e);
            assert false;
        }
    }


    @Test
    public void backCalendarDayOpen() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-15";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_BACKWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_AFTER;
        calendarInput.duration = "P2D"; // 15-2 = Monday 13
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {
                logger.info("testBackCalendarDayOpen Result FoundDate:{} resultDate[{}] resultZonedDate[{}]", result.foundDate, result.resultDate.toLocalDate(), result.resultZonedDate);
                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 13)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 13)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("testBackCalendarDayOpen OK ");
        } catch (Exception e) {
            logger.error("testBackCalendarDayOpen", e);
            assert false;
        }
    }

    @Test
    public void backCalendarDayClosedBeforePolicy() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-15";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_BACKWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_BEFORE;
        calendarInput.duration = "P3D"; // 15-3=Sunday 12 BEFORE => Friday 10
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {
                logger.info("testBackCalendarDayClosedAfterPolicy Result FoundDate:{} resultDate[{}] resultZonedDate[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate);

                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 10)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 10)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("testBackCalendarDayClosedAfterPolicy OK ");
        } catch (Exception e) {
            logger.error("testBackCalendarDayClosedAfterPolicy", e);
            assert false;
        }
    }


    @Test
    public void backCalendarDayClosedAfterPolicy() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-15";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_BACKWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_AFTER;
        calendarInput.duration = "P3D"; // 15-3=Sun 12 - AFTER => Monday 13
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {
                logger.info("testBackCalendarDayClosedAfterPolicy Result FoundDate:{} resultDate[{}] resultZonedDate[{}] periods[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 13)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 13)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("testBackCalendarDayClosedAfterPolicy OK ");
        } catch (Exception e) {
            logger.error("testBackCalendarDayClosedAfterPolicy", e);
            assert false;
        }
    }

    @Test
    public void backCalendarDayClosedResultPolicy() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-15";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_BACKWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_RESULT;
        calendarInput.duration = "P3D"; // 15-3=Sunday 12
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {

                logger.info("testBackCalendarDayClosedAfterPolicy Result FoundDate:{} resultDate[{}] resultZonedDate[{}] periods[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 12)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 12)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("testBackCalendarDayClosedAfterPolicy OK ");
        } catch (Exception e) {
            logger.error("testBackCalendarDayClosedAfterPolicy", e);
            assert false;
        }
    }

    @Test
    public void forwardCalendarDayOpen() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-10";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_AFTER;
        calendarInput.duration = "P3D";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {

                logger.info("testForwardCalendarDayOpen Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 13)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 13)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("testForwardCalendarDayOpen OK ");
        } catch (Exception e) {
            logger.error("testForwardCalendarDayOpen", e);
            assert false;
        }
    }

    @Test
    public void forwardCalendarDayClosedAfterPolicy() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-10";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_AFTER;
        calendarInput.duration = "P4D"; // 10+3=13 arrived July 14 : After==>15
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {

                logger.info("forwardCalendarDayClosedAfterPolicy Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 15)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 15)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("forwardCalendarDayClosedAfterPolicy OK ");
        } catch (Exception e) {
            logger.error("testForwardCalendarDayClosedAfterPolicy", e);
            assert false;
        }
    }


    @Test
    public void forwardCalendarDayClosedBeforePolicy() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-10";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_BEFORE;
        calendarInput.duration = "P2D"; // arrived Sunday => Saturday => Back on Friday!
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {

                logger.info("forwardCalendarDayClosedBeforePolicy Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 10)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 10)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("forwardCalendarDayClosedBeforePolicy OK ");
        } catch (Exception e) {
            logger.error("forwardCalendarDayClosedBeforePolicy", e);
            assert false;
        }
    }


    @Test
    public void forwardCalendarDayClosedResultPolicy() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-10";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_RESULT;
        calendarInput.duration = "P2D"; // arrived Sunday => Saturday 12
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {
                logger.info("forwardCalendarDayClosedResultPolicy Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 12)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 12)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("forwardCalendarDayClosedResultPolicy OK ");
        } catch (Exception e) {
            logger.error("forwardCalendarDayClosedResultPolicy", e);
            assert false;
        }
    }

    @Test
    public void backWeekCalendarDay() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-15";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_BACKWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_AFTER;
        calendarInput.duration = "P1W"; //15 - 1W => July 8
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {
                logger.info("backWeekBusinessDay Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));
                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 8)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 8)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("backWeekBusinessDay OK ");
        } catch (Exception e) {
            logger.error("backWeekBusinessDay", e);
            assert false;
        }
    }

    @Test
    public void advanceWeekCalendarDay() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-07-15";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_AFTER;
        calendarInput.duration = "P1W"; //15 + 1W => July 22
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput calendarAdvanceOutput = calendarFunction.execute(context);
            assert (calendarAdvanceOutput.getNumberOfResultDate() == 1);
            for (CalendarAdvanceOutput.Result result : calendarAdvanceOutput.getListResultDateCollection()) {
                logger.info("backWeekBusinessDay Result FoundDate:{} resultDate[{}] resultZonedDate[{}], periods[{}]",
                        result.foundDate,
                        result.resultDate.toLocalDate(),
                        result.resultZonedDate,
                        result.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));
                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 22)));
                assertNull(result.resultZonedDate);
            }
            // Check that result are correct set too
            assertTrue(calendarAdvanceOutput.isFoundDate());
            assert (calendarAdvanceOutput.getResultDate().toLocalDate().equals(LocalDate.of(2026, 7, 22)));
            assertNull(calendarAdvanceOutput.getResultZonedDate());

            logger.info("backWeekBusinessDay OK ");
        } catch (Exception e) {
            logger.error("backWeekBusinessDay", e);
            assert false;
        }
    }


}