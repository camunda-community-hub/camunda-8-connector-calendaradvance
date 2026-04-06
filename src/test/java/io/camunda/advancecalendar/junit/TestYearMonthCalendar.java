package io.camunda.advancecalendar.junit;

import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceFunction;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import io.camunda.connector.calendaradvance.advanceday.DayFunction;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestYearMonthCalendar {
    private final Logger logger = LoggerFactory.getLogger(TestYearMonthCalendar.class);

    @Test
    public void forwardMonthCalendarDay() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-06-14";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_AFTER;
        calendarInput.duration = "P1M";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);
            logger.info("forwardMonthCalendarDay Result FoundDate:{} resultDate[{}] resultZonedDate[{}] periods[{}]",
                    output.foundDate,
                    output.resultDate.toLocalDate(),
                    output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));
            assertTrue(output.foundDate);
            assert (output.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 15)));
            assertNull(output.resultZonedDate);
            logger.info("forwardMonthCalendarDay OK ");
        } catch (Exception e) {
            logger.error("forwardMonthCalendarDay", e);
            assert false;
        }
    }


    @Test
    public void backwardYearCalendarDay() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = DayFunction.ADVANCE_DAYS;
        calendarInput.startDate = "2026-12-25";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_BACKWARD;
        calendarInput.dayProgression = CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY;
        calendarInput.targetProgression = CalendarAdvanceInput.TARGET_PROGRESSION_BEFORE;
        calendarInput.duration = "P2Y";
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);
            logger.info("backwardYearCalendarDay Result FoundDate:{} resultDate[{}] resultZonedDate[{}] periods[{}]",
                    output.foundDate,
                    output.resultDate.toLocalDate(),
                    output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));
            assertTrue(output.foundDate);
            assert (output.resultDate.toLocalDate().equals(LocalDate.of(2024, 12, 24)));
            assertNull(output.resultZonedDate);
            logger.info("backwardYearCalendarDay OK ");
        } catch (Exception e) {
            logger.error("backwardYearCalendarDay", e);
            assert false;
        }
    }
}
