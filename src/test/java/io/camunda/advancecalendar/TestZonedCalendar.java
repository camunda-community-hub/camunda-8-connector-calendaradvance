package io.camunda.advancecalendar;

import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceFunction;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import io.camunda.connector.calendaradvance.advancehour.HourFunction;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestZonedCalendar {

    private final Logger logger = LoggerFactory.getLogger(TestLocalTimeCalendar.class.getName());

    @Test
    public void testNowPlus50() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT")).truncatedTo(ChronoUnit.MINUTES);
        calendarInput.startDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ'[GMT]'"));
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT50M";
        calendarInput.useHolidays = false;
        calendarInput.businessCalendar = List.of("24/7"); // only way to calculate the result
        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] sourceZoneDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate,
                    calendarInput.startDate,
                    output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            // calculate now + 50 m
            ZonedDateTime nowPlus50Zoned = now.plusMinutes(50);
            LocalDateTime nowPlus50Local = nowPlus50Zoned.toLocalDateTime();

            assert(output.resultZonedDate.toInstant().toEpochMilli() == nowPlus50Zoned.toInstant().toEpochMilli());
            assert (output.resultDate.equals(nowPlus50Local));

            logger.info("testNowPlus50 OK ");
        } catch (Exception e) {
            logger.error("testNowPlus50", e);
            assert false;
        }
    }



}
