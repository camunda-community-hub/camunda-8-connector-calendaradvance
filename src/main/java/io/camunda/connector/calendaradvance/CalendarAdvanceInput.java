package io.camunda.connector.calendaradvance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.calendaradvance.toolbox.CalendarAdvanceError;
import io.camunda.connector.calendaradvance.toolbox.ParameterToolbox;
import io.camunda.connector.cherrytemplate.CherryInput;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * the JsonIgnoreProperties is mandatory: the template may contain additional widget to help the designer, especially on the OPTIONAL parameters
 * This avoids the MAPPING Exception
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalendarAdvanceInput implements CherryInput {
    /**
     * Attention, each Input here must be added in the PdfFunction, list of InputVariables
     */
    public static final String CALENDARADVANCEFUNCTION = "calendarAdvanceFunction";
    /**
     * Input need for ExtractPages
     */
    public static final String DIRECTION = "direction";
    public static final String DIRECTION_V_FORWARD = "forward";
    public static final String DIRECTION_V_BACKWARD = "backward";

    public static final String START_DATE = "startDate";
    public static final String BUSINESS_CALENDAR = "businessCalendar";
    public static final String BUSINESS_TIMEZONE = "businessTimeZone";
    public static final String USE_HOLIDAYS = "useHolidays";
    public static final String HOLIDAYS_COUNTRIES = "holidaysCountries";


    public static final String DURATION = "duration";


    public static final RunnerParameter parameterStartDay = new RunnerParameter(
            CalendarAdvanceInput.START_DATE, // name
            "Start Date", // label
            Date.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Calculation sart with this date");

    public static final RunnerParameter parameterDuration = new RunnerParameter(
            CalendarAdvanceInput.DURATION,
            // name
            "Duration", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Duration ISO 8601 : P2DT5H23M54S");

    public static final RunnerParameter parameterDirection = new RunnerParameter(
            CalendarAdvanceInput.DIRECTION,
            // name
            "Direction", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Direction to advance")
            .addChoice(DIRECTION_V_FORWARD, "forward")
            .addChoice(DIRECTION_V_BACKWARD, "backward");

    public static final RunnerParameter parameterBusinessCalendar = new RunnerParameter(
            CalendarAdvanceInput.BUSINESS_CALENDAR, // name
            "Business Calendar", // label
            List.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "List of business calendar slot. Default is Monday to Friday, 09:00 to 18:00");

    public static final RunnerParameter parameterBusinessTimeZone = new RunnerParameter(
            CalendarAdvanceInput.BUSINESS_TIMEZONE, // name
            "Business TimeZone", // label
            String.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "The business Calendar can be attached to a timezone. Region Based and Fixed UTC offsets accepted. Visit https://en.wikipedia.org/wiki/List_of_tz_database_time_zones");

    public static final RunnerParameter parameterUseHolidays = new RunnerParameter(
            CalendarAdvanceInput.USE_HOLIDAYS, // name
            "Use holidays", // label
            Boolean.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Holidays on country is used and considered as closed");

    public static final RunnerParameter parameterHolidayCountries = new RunnerParameter(
            CalendarAdvanceInput.HOLIDAYS_COUNTRIES, // name
            "Holiday countries", // label
            List.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "List of countries (\"FR\", \"US\"). List of available countrycodes <a href=\\\"https://date.nager.at/Country\\\" target=\\\"_blank\\\">here</a> ")
            .addCondition(CalendarAdvanceInput.USE_HOLIDAYS, List.of("true"));

    private final Logger logger = LoggerFactory.getLogger(CalendarAdvanceInput.class.getName());
    public String calendarAdvanceFunction;

    public List<String> businessCalendar;
    public String duration;
    public String direction;
    public Object startDate;
    public boolean useHolidays;
    public List<String> holidaysCountries;

    public String businessTimeZone;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private LocalDateTime calculatedInputLocalDateTime;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private ZoneOffset calculatedInputZoneoffset;

    public String getCalendarAdvanceFunction() {
        return calendarAdvanceFunction;
    }

    public List<String> getBusinessCalendar() {
        return businessCalendar;
    }

    public boolean getAdvance() {
        return DIRECTION_V_FORWARD.equals(direction);
    }

    public long getDurationInMinutes() throws ConnectorException {
        try {
            Duration durationObject = Duration.parse(duration);
            return durationObject.toMinutes();
        } catch (DateTimeParseException e) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_DURATION, "Duration[" + duration + "] is not ISO8601");
        }

    }

    public LocalDate getReferenceDateLocalDate() {
        if (startDate instanceof LocalDate referenceDateLocalDate) {
            return referenceDateLocalDate;
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM");
            return LocalDate.parse(startDate.toString(), formatter);
        }
    }

    public ZoneId getBusinessZoneId() {
        return (businessTimeZone != null && !businessTimeZone.isBlank())
                ? ZoneId.of(businessTimeZone)
                : null;
    }

    public LocalDateTime getCalculatedStartDateLocalDateTime() {
        return calculatedInputLocalDateTime;
    }

    public ZoneOffset getCalculatedZoneOffset() {
        return calculatedInputZoneoffset;
    }

    public Object getStartDate() {
        return startDate;
    }

    public boolean isUseHolidays() {
        return useHolidays;
    }

    public List<String> getHolidaysCountries() {
        return holidaysCountries;
    }

    @Override
    public List<Map<String, Object>> getInputParameters() {
        return ParameterToolbox.getInputParameters();
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  calculate Reference Date                                            */
    /*                                                                      */
    /*  This class should be call at begining to calculated                 */
    /* calculatedInputLocalDateTime and calculatedInputZoneoffset           */
    /* ******************************************************************** */

    public void calculateReferenceDateLocalDateTime() {
        try {
            if (startDate == null) {
                calculatedInputLocalDateTime = null;
                calculatedInputZoneoffset = null;
                return;
            }

            // ---------- Specific type LocalDateTime
            if (startDate instanceof LocalDateTime startDateLocalDateTime) {
                calculatedFromLocalDateTime(startDateLocalDateTime);
                return;
            }
            // ----------- Specific type ZonedDateTime
            if (startDate instanceof ZonedDateTime startDateZonedDateTime) {
                calculatedFromZonedDateTime(startDateZonedDateTime);
                return;
            }
            if (startDate instanceof OffsetDateTime startDateOffsetDateTime) {
                calculatedFromOffsetDateTime(startDateOffsetDateTime);
                return;
            }
            // String
            String startDateString = startDate.toString();
            // String offsetDateTime
            if (startDateString.endsWith("Z") || startDateString.matches(".*[+-]\\d{2}:\\d{2}$")) {
                OffsetDateTime odt = OffsetDateTime.parse(startDateString);
                calculatedFromOffsetDateTime(odt);
                return;
            }
            // String ZonedDateTime
            if (startDateString.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:Z|[+-]\\d{2}:\\d{2})\\[[A-Za-z_]+(?:/[A-Za-z_]+)+\\]$")) {
                ZonedDateTime zdt = ZonedDateTime.parse(startDateString);
                calculatedFromZonedDateTime(zdt);
                return;
            }

            // no timezone in input → LocalDateTime
            LocalDateTime ldt = LocalDateTime.parse(startDateString);
            calculatedFromLocalDateTime(ldt);


        } catch (Exception e) {
            logger.error("Error getting reference date", e);
        }

    }

    private void calculatedFromLocalDateTime(LocalDateTime ldt) {
        ZoneId businessCalendarZoneId = getBusinessZoneId();
        // Attach zone (no conversion, just interpretation)
        if (businessCalendarZoneId != null) {
            calculatedInputLocalDateTime = ldt.atZone(businessCalendarZoneId).toLocalDateTime();
        } else {
            calculatedInputLocalDateTime = ldt;
        }
        calculatedInputZoneoffset = null;

    }

    private void calculatedFromZonedDateTime(ZonedDateTime zdt) {
        calculatedInputLocalDateTime = zdt.toLocalDateTime();
        ZoneId businessCalendarZoneId = getBusinessZoneId();
        if (businessCalendarZoneId != null) {
            calculatedInputZoneoffset = zdt
                    .withZoneSameInstant(businessCalendarZoneId)
                    .getOffset();
        }
    }

    private void calculatedFromOffsetDateTime(OffsetDateTime odt) {
        calculatedInputLocalDateTime = odt.toLocalDateTime();
        ZoneId businessCalendarZoneId = getBusinessZoneId();
        if (businessCalendarZoneId != null) {
            calculatedInputZoneoffset = businessCalendarZoneId.getRules()
                    .getOffset(odt.toInstant());
        }
    }

}
