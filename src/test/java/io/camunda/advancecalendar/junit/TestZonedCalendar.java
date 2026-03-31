package io.camunda.advancecalendar.junit;

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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        calendarInput.startDate = now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
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



    @Test
    public void testNewYorkLosAngeles() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        ZonedDateTime zdtNewYork = ZonedDateTime.of(
                2026, 3, 30,   // year, month, day
                9, 14, 0, 0,   // hour, minute, second, nano
                ZoneId.of("America/New_York")
        );
        calendarInput.startDate = zdtNewYork.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT2H10M";
        calendarInput.useHolidays = false;
        calendarInput.businessCalendar = SlotContainer.getWeekSlots(LocalTime.of(9,00), LocalTime.of(18,00));
        calendarInput.businessTimeZone = "America/Los_Angeles";

        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] sourceZoneDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate,
                    calendarInput.startDate,
                    output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            // calculate now + 50 m
            ZonedDateTime resultNewYork = ZonedDateTime.of(
                    2026, 3, 30,   // year, month, day
                    14, 10, 0, 0,   // hour, minute, second, nano
                    ZoneId.of("America/New_York")
            );

            assertEquals(output.resultZonedDate.toInstant().toEpochMilli(), resultNewYork.toInstant().toEpochMilli());
            assertEquals(output.resultZonedDate.getOffset(), zdtNewYork.getOffset());
            assertEquals(LocalDateTime.of(2026, 3, 30, 11, 10), output.resultDate);

            logger.info("testNewYorkLosAngeles OK ");
        } catch (Exception e) {
            logger.error("testNewYorkLosAngeles", e);
            assert false;
        }
    }


    @Test
    public void testDenverNewYork() {
        CalendarAdvanceInput calendarInput = new CalendarAdvanceInput();
        OutboundConnectorContext context = mock(OutboundConnectorContext.class);
        // make bindVariables return your prepared input
        when(context.bindVariables(CalendarAdvanceInput.class)).thenReturn(calendarInput);

        // populate your input
        calendarInput.calendarAdvanceFunction = HourFunction.ADVANCE_HOURS;
        ZonedDateTime zdtDenver = ZonedDateTime.of(
                2026, 3, 30,   // year, month, day
                15, 20, 0, 0,   // hour, minute, second, nano
                ZoneId.of("America/Denver")
        );
        calendarInput.startDate = zdtDenver.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        calendarInput.direction = CalendarAdvanceInput.DIRECTION_V_FORWARD;
        calendarInput.duration = "PT2H10M";
        calendarInput.useHolidays = false;
        calendarInput.businessCalendar = SlotContainer.getWeekSlots(LocalTime.of(9,00), LocalTime.of(18,00));
        calendarInput.businessTimeZone = "America/New_York";

        CalendarAdvanceFunction calendarFunction = new CalendarAdvanceFunction();
        try {
            CalendarAdvanceOutput output = calendarFunction.execute(context);

            logger.info("Result FoundDate:{} resultDate[{}] sourceZoneDate[{}] resultZonedDate[{}] Periods[{}]", output.foundDate, output.resultDate,
                    calendarInput.startDate,
                    output.resultZonedDate,
                    output.listPeriods.stream().map(Object::toString).collect(Collectors.joining(", ")));

            assertTrue(output.foundDate);
            // calculate now + 50 m
            ZonedDateTime resultDenver = ZonedDateTime.of(
                    2026, 3, 31,   // year, month, day
                    8, 30, 0, 0,   // hour, minute, second, nano
                    ZoneId.of("America/Denver")
            );

            assertEquals(output.resultZonedDate.toInstant().toEpochMilli(), resultDenver.toInstant().toEpochMilli());
            assert(output.resultZonedDate.getOffset().equals(zdtDenver.getOffset()));
            assertEquals(LocalDateTime.of(2026, 3, 31, 10, 30), output.resultDate);

            logger.info("testNewYorkLosAngeles OK ");
        } catch (Exception e) {
            logger.error("testNewYorkLosAngeles", e);
            assert false;
        }
    }
}
