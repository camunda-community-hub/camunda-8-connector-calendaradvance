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
    public static final String DAY_PROGRESSION = "dayProgression";
    public static final String DAY_PROGRESSION_V_BUSINESSDAY = "businessday";
    public static final String DAY_PROGRESSION_V_CALENDARDAY = "calendarday";

    public static final String TARGET_PROGRESSION = "targetProgression";
    public static final String TARGET_PROGRESSION_RESULT = "result";
    public static final String TARGET_PROGRESSION_AFTER = "after";
    public static final String TARGET_PROGRESSION_BEFORE = "before";

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
            "The business Calendar can be attached to a timezone. Region Based and Fixed UTC offsets accepted. Visit https://en.wikipedia.org/wiki/List_of_tz_database_time_zones")
            .setVisibleInTemplate();

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

    public static final RunnerParameter parameterDayProgression = new RunnerParameter(
            CalendarAdvanceInput.DAY_PROGRESSION, // name
            "Days progression", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Method to advance one day: business day (only open day in business calendar are used) or calendar day")
            .addChoice(CalendarAdvanceInput.DAY_PROGRESSION_V_BUSINESSDAY, "Business day")
            .addChoice(CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY, "Calendar day");

    public static final RunnerParameter parameterTargetProgression = new RunnerParameter(
            CalendarAdvanceInput.TARGET_PROGRESSION, // name
            "Target progression", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "The target day policy: Result (even if the day is not a business day), After (advance until a open day is found - in reverse, move backward), Before (first business day before the target day).")
            .addChoice(CalendarAdvanceInput.TARGET_PROGRESSION_RESULT, "Result day")
            .addChoice(CalendarAdvanceInput.TARGET_PROGRESSION_AFTER, "After")
            .addChoice(CalendarAdvanceInput.TARGET_PROGRESSION_BEFORE, "Before");


    private final Logger logger = LoggerFactory.getLogger(CalendarAdvanceInput.class.getName());
    public String calendarAdvanceFunction;

    public List<String> businessCalendar;
    public String duration;
    public String direction;
    public Object startDate;
    public boolean useHolidays;
    public List<String> holidaysCountries;

    public String businessTimeZone;
    public String dayProgression;
    public String targetProgression;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private LocalDateTime calculatedInputLocalDateTime;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private ZoneOffset calculatedInputStartDateZoneOffset;
    private boolean zonedDateTime = false;

    public String getCalendarAdvanceFunction() {
        return calendarAdvanceFunction;
    }

    public List<String> getBusinessCalendar() {
        return businessCalendar;
    }

    public boolean isDirectionForward() {
        return DIRECTION_V_FORWARD.equals(direction);
    }

    public TYPEPERIOD getTypePeriod() {
        if (getDuration() != null) {
            return TYPEPERIOD.TIME;
        }
        Period period = getPeriod();
        if (period == null) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_DURATION, "Duration[" + duration + "] is not ISO8601");
        }
        if (period.getMonths() > 0)
            return TYPEPERIOD.MONTH;

        if (period.getYears() > 0)
            return TYPEPERIOD.YEAR;
        return TYPEPERIOD.DAY;
    }

    /**
     * Get the duration in minutes
     *
     * @return the duration, exception if the duration can't be calculated
     * @throws ConnectorException in case the duration can't be get
     */
    public long getDurationInMinutes() throws ConnectorException {
        if (getDuration() == null)
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_DURATION, "Duration[" + duration + "] is not valid");
        return getDuration().toMinutes();
    }

    public long getDurationInDays() throws ConnectorException {
        Period period = getPeriod();
        Duration duration = getDuration();
        if (period == null && duration == null) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_DURATION, "Duration[" + duration + "] is not ISO8601");
        }
        if (duration != null)
            return duration.toDays();
        // So here, peiod !=null
        if (period.getDays() == 0) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_DURATION, "Duration[" + duration + "] is not in days");
        }
        return period.getDays();
    }

    public long getDurationInMonths() throws ConnectorException {
        Period period = getPeriod();
        if (period == null) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_DURATION, "Duration[" + duration + "] is not ISO8601");
        }
        if (period.getMonths() == 0) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_DURATION, "Duration[" + duration + "] is not in Month");
        }
        return period.getMonths();
    }

    public long getDurationInYears() throws ConnectorException {
        Period period = getPeriod();
        if (period == null) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_DURATION, "Duration[" + duration + "] is not ISO8601");
        }
        if (period.getYears() == 0) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_DURATION, "Duration[" + duration + "] is not in year");
        }
        return period.getYears();
    }

    private Duration getDuration() {
        try {
            return Duration.parse(duration);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Period getPeriod() {
        try {
            return Period.parse(duration);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public LocalDate getReferenceDateLocalDate() {
        if (startDate instanceof LocalDate referenceDateLocalDate) {
            return referenceDateLocalDate;
        } else {
            return calculatedInputLocalDateTime.toLocalDate();
        }
    }


    /* ******************************************************************** */
    /*                                                                      */
    /*  Reference local                                                     */
    /*                                                                      */
    /* ******************************************************************** */

    public ZoneId getBusinessZoneId() {
        return (businessTimeZone != null && !businessTimeZone.isBlank())
                ? ZoneId.of(businessTimeZone)
                : null;
    }

    public LocalDateTime getCalculatedStartDateLocalDateTime() {
        return calculatedInputLocalDateTime;
    }

    public ZoneOffset getCalculatedStartDateZoneOffset() {
        return calculatedInputStartDateZoneOffset;
    }

    public String getTargetProgression() {
        return targetProgression;
    }

    public String getDayProgression() {
        return dayProgression;
    }

    /**
     * Force the zoneOffset
     *
     * @param calculatedZoneOffset force this zoneOffset
     */
    public void forceStartZoneOffset(ZoneOffset calculatedZoneOffset) {
        this.calculatedInputStartDateZoneOffset = calculatedZoneOffset;
        zonedDateTime = true;
    }

    /**
     * Return true if at input, a zoned date time was provided
     *
     * @return true if the date is zoned
     */
    public boolean isZonedDateTime() {
        return zonedDateTime;
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

    public void calculateReferenceDateLocalDateTime() {
        try {
            calculatedInputStartDateZoneOffset = null;
            if (startDate == null) {
                calculatedInputLocalDateTime = null;
                return;
            }

            if (startDate instanceof LocalDate referenceDateLocalDate) {
                calculatedInputLocalDateTime = referenceDateLocalDate.atStartOfDay();
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
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(startDateString);
                calculatedFromZonedDateTime(zdt);
                return;
            } catch (DateTimeParseException e) {
                // do nothing,
            }


            // no timezone in input → LocalDateTime
            try {
                LocalDateTime ldt = LocalDateTime.parse(startDateString);
                calculatedFromLocalDateTime(ldt);
                return;
            } catch (DateTimeParseException e) {
                // do nothing
            }

            LocalDate date = LocalDate.parse(startDateString);
            calculatedInputLocalDateTime = date.atStartOfDay();

        } catch (Exception e) {
            logger.error("Error getting reference date", e);
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_STARTDATE, "Error getting reference date", e);
        }

    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  calculate Reference Date                                            */
    /*                                                                      */
    /*  This class should be call at begining to calculated                 */
    /* calculatedInputLocalDateTime and calculatedInputZoneoffset           */
    /* ******************************************************************** */

    private void calculatedFromLocalDateTime(LocalDateTime ldt) {
        ZoneId businessCalendarZoneId = getBusinessZoneId();
        // Attach zone (no conversion, just interpretation)
        if (businessCalendarZoneId != null) {
            calculatedInputLocalDateTime = ldt.atZone(businessCalendarZoneId).toLocalDateTime();
        } else {
            calculatedInputLocalDateTime = ldt;
        }
        calculatedInputStartDateZoneOffset = null;

    }

    private void calculatedFromZonedDateTime(ZonedDateTime startDatezdt) {
        zonedDateTime = true;
        calculatedInputLocalDateTime = startDatezdt.toLocalDateTime();
        ZoneId businessCalendarZoneId = getBusinessZoneId();
        if (businessCalendarZoneId != null) {
            // We get the time at the Calendar timezone
            ZonedDateTime calendarTime = startDatezdt.withZoneSameInstant(businessCalendarZoneId);
            calculatedInputLocalDateTime = calendarTime.toLocalDateTime();
            calculatedInputStartDateZoneOffset = startDatezdt.getOffset();

        }
    }

    private void calculatedFromOffsetDateTime(OffsetDateTime odt) {
        zonedDateTime = true;
        calculatedInputLocalDateTime = odt.toLocalDateTime();
        ZoneId businessCalendarZoneId = getBusinessZoneId();
        if (businessCalendarZoneId != null) {
            calculatedInputStartDateZoneOffset = businessCalendarZoneId.getRules()
                    .getOffset(odt.toInstant());
            zonedDateTime = true;
        }
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  Period                                                              */
    /*                                                                      */
    /* ******************************************************************** */
    public enum TYPEPERIOD {TIME, DAY, MONTH, YEAR}

}
