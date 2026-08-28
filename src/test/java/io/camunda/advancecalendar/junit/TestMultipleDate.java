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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMultipleDate {
    private final Logger logger = LoggerFactory.getLogger(TestMultipleDate.class);

    @Test
    public void multipleDates() {
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
        calendarInput.durations = List.of("P2D","P3D") ;
        calendarInput.useHolidays = true;
        calendarInput.holidaysCountries = List.of("FR");
        calendarInput.businessCalendar = null;
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            assert (output.listResultDates.size() == 2);
            CalendarAdvanceOutput.Result result = output.listResultDates.get(0);
                logger.info("testBackCalendarDayOpen Result FoundDate:{} resultDate[{}] resultZonedDate[{}]", result.foundDate, result.resultDate.toLocalDate(), result.resultZonedDate);
                assertTrue(result.foundDate);
                assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 13)));
                assertNull(result.resultZonedDate);

             result = output.listResultDates.get(1);
            logger.info("testBackCalendarDayClosedAfterPolicy Result FoundDate:{} resultDate[{}] resultZonedDate[{}]",
                    result.foundDate,
                    result.resultDate.toLocalDate(),
                    result.resultZonedDate);

            assertTrue(result.foundDate);
            assert (result.resultDate.toLocalDate().equals(LocalDate.of(2026, 7, 13)));
            assertNull(result.resultZonedDate);


            logger.info("testBackCalendarDayOpen OK ");
        } catch (Exception e) {
            logger.error("testBackCalendarDayOpen", e);
            assert false;
        }
    }


}
