package io.camunda.advancecalendar;

import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceFunction;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import io.camunda.connector.calendaradvance.advancehour.HourFunction;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestZonedCalendar {

    private final Logger logger = LoggerFactory.getLogger(TestLocalTimeCalendar.class.getName());

    @Test
    public void testNowPlus30() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT"));
        calendarInput.startDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ'[GMT]'"));
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT50M";
        calendarInput.useHolidays = false;
        calendarInput.businessCalendar = List.of("24/7"); // only way to calculate the result
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate, output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            // calculate now + 50 m
            ZonedDateTime nowPlus30 = now.plusMinutes(30);

            assert (output.resultDate.equals(nowPlus30.toLocalDateTime()));
            assert(output.resultZonedDate.equals(nowPlus30));

            logger.info("testNowPlus30 OK ");
        } catch (Exception e) {
            logger.error("testNowPlus30", e);
            assert false;
        }
    }



}
