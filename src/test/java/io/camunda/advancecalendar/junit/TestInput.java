package io.camunda.advancecalendar.junit;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceFunction;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.advancehour.HourFunction;
import io.camunda.connector.calendaradvance.timemachine.SlotContainer;
import io.camunda.connector.calendaradvance.toolbox.CalendarAdvanceError;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestInput {
    private final Logger logger = LoggerFactory.getLogger(TestInput.class.getName());

    @Test
    public void testNoCountryCode() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = "2026-05-13T15:18:00";
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT20H";
        calendarInput.useHolidays = true;
        calendarInput.businessCalendar = List.of("Monday=09:00:00-18:00:00",
                "Tuesday=09:00:00-18:00:00",
                "Wednesday=09:00:00-18:00:00",
                "Thursday=09:00:00-18:00:00",
                "Friday=09:00:00-18:00:00",
                SlotContainer.SPECIFIC_DAY_PREFIX + "2026/05/14:09:00-12:00", // Ascension day : it's a holiday in France, but we are open
                SlotContainer.SPECIFIC_DAY_PREFIX + "2026/05/15:09:00-12:00"); // Day after Ascension

        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            calendarFunction.execute(context);

            logger.info("No exception received when exception");

            fail();
        } catch (ConnectorException ce) {
            if (CalendarAdvanceError.ERROR_NO_COUNTRIESCODE.equals(ce.getErrorCode())) {
                logger.info("Receive no countriescode as expected");
            } else {
                logger.error("Received different error", ce);
                fail();
            }
        } catch (Exception e) {
            logger.error("testNoCountryCode", e);
            fail();
        }
    }
    @Test
    public void testNoStartDate() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        calendarInput.startDate = null;
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT20H";
        calendarInput.useHolidays = false;
        calendarInput.businessCalendar = List.of("Monday=09:00:00-18:00:00",
                "Tuesday=09:00:00-18:00:00",
                "Wednesday=09:00:00-18:00:00",
                "Thursday=09:00:00-18:00:00",
                "Friday=09:00:00-18:00:00",
                SlotContainer.SPECIFIC_DAY_PREFIX + "2026/05/14:09:00-12:00", // Ascension day : it's a holiday in France, but we are open
                SlotContainer.SPECIFIC_DAY_PREFIX + "2026/05/15:09:00-12:00"); // Day after Ascension

        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            calendarFunction.execute(context);

            logger.info("No exception received when exception");

            fail();
            logger.info("test247 OK ");
        } catch (ConnectorException ce) {
            if (CalendarAdvanceError.ERROR_NO_REFERENCE_START_DATE.equals(ce.getErrorCode())) {
                logger.info("Receive no reference date as expected");
            } else {
                logger.error("Received different error", ce);
                fail();
            }
        } catch (Exception e) {
            logger.error("testNoStartDate", e);
            fail();
        }
    }
}
