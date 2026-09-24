package org.camunda.connector.calendaradvance.advancehour;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.camunda.connector.calendaradvance.CalendarAdvanceInput;
import org.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import org.camunda.connector.calendaradvance.timemachine.SlotContainer;
import org.camunda.connector.calendaradvance.toolbox.CalendarAdvanceError;
import org.camunda.connector.calendaradvance.toolbox.SubFunction;
import org.camunda.connector.calendaradvance.toolbox.ValidateInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HourFunction implements SubFunction {


    public static final String ADVANCE_HOURS = "advance-hours";
    private final Logger logger = LoggerFactory.getLogger(HourFunction.class.getName());


    /**
     * @param calendarInput            input
     * @param outboundConnectorContext context of the task
     * @return the output
     * @throws ConnectorException in case of any error
     */
    @Override
    public CalendarAdvanceOutput executeSubFunction(CalendarAdvanceInput calendarInput, OutboundConnectorContext outboundConnectorContext) throws ConnectorException {
        logger.debug("HourFunction Start");
        try {
            // First, calculate the date according all parameters
            calendarInput.calculateReferenceDateLocalDateTime();

            // Validate input
            ValidateInput.validateInput(calendarInput, true);

            // Now start the calculation
            CalendarAdvanceOutput calendarOutput = new CalendarAdvanceOutput(calendarInput.isDurationAMap());

            SlotContainer slotContainer = new SlotContainer();
            slotContainer.setSlots(calendarInput.getBusinessCalendar());

            // There is a special use case here: if a ZonedDateTime is given AND the slotContainer is a 24/7, then we can enable the zoned callation: offset is just set to 0
            if (slotContainer.is247Calendar() && calendarInput.isZonedDateTime()) {
                calendarInput.forceStartZoneOffset(ZoneOffset.ofHours(0));
            }


            logger.info("AdvanceHourFunction StartDateCalculation: {}", calendarInput.getExplanationStartDateCalculation());

            for (String position : calendarInput.getPositionDurations()) {
                List<SlotContainer.Period> listPeriods = new ArrayList<>();
                LocalDateTime cursor = calendarInput.getCalculatedStartDateLocalDateTime();
                long durationInMinutes = calendarInput.getDurationInMinutes(position);
                logger.info("AdvanceHourFunction.start: Position[{}] StartDate[{}] BusinessZoneId[{}] Duration[{}] In Mn[{}] direction [{}] Holidays[{}] HolidayCountries[{}]",
                        position,
                        cursor,
                        calendarInput.getBusinessZoneId(),
                        calendarInput.getPrivateDuration(position, false),
                        durationInMinutes,
                        calendarInput.isDirectionForward(),
                        calendarInput.isUseHolidays(),
                        calendarInput.getHolidaysCountries());
                for (int i = 0; i < 1000; i++) {
                    // Calculate the next period according the current date. The Period is adapted to the cursor
                    SlotContainer.AdvanceResult advanceResult = slotContainer.getNextPeriod(cursor,
                            calendarInput.isDirectionForward(),
                            calendarInput.isUseHolidays(),
                            calendarInput.getHolidaysCountries(),
                            calendarInput.getHolidayCalendarPolicy());

                    if (!advanceResult.foundPeriod) {
                        // This is the end here!
                        if (calendarInput.isErrorWhenNoDateFound()) {
                            throw new ConnectorException(CalendarAdvanceError.ERROR_NO_DATE_FOUND, "No date found for position [" + position + "]");
                        }
                        calendarOutput.addResult(position, false, null, null, null);
                        return calendarOutput;
                    }
                    // reduce the duration by the period
                    if (advanceResult.period.getMinutes() >= durationInMinutes) {
                        // This is the end!
                        SlotContainer.Period lastPeriod;
                        if (calendarInput.isDirectionForward()) {
                            cursor = LocalDateTime.of(advanceResult.periodDate, advanceResult.period.startTime)
                                    .plusMinutes(durationInMinutes);
                            lastPeriod = SlotContainer.Period.getPeriod(advanceResult.period.dayOfWeek,
                                            advanceResult.period.startTime,
                                            cursor.toLocalTime())
                                    .setDateOfPeriod(advanceResult.periodDate);
                        } else {
                            // Attention, end of the period may my MIDNIGHT, i.e. 23:59+1
                            if (LocalTime.MIDNIGHT.equals(advanceResult.period.endTime) || SlotContainer.MIDNIGHT_MINUS.equals(advanceResult.period.endTime))
                                cursor = LocalDateTime.of(advanceResult.periodDate, LocalTime.of(23, 59))
                                        .minusMinutes(durationInMinutes - 1);
                            else
                                cursor = LocalDateTime.of(advanceResult.periodDate, advanceResult.period.endTime)
                                        .minusMinutes(durationInMinutes);
                            lastPeriod = SlotContainer.Period.getPeriod(advanceResult.period.dayOfWeek,
                                            cursor.toLocalTime(),
                                            advanceResult.period.endTime)
                                    .setDateOfPeriod(advanceResult.periodDate);

                        }
                        listPeriods.add(lastPeriod);
                        logger.debug("AdvanceHourFunction.end: Position[{}] LAST Period [{}-{}]: {} mn : now {} ", position, lastPeriod.startTime, lastPeriod.endTime, lastPeriod.getMinutes(), cursor);

                        break; // end of the loop
                    }
                    durationInMinutes -= advanceResult.period.getMinutes();

                    listPeriods.add(advanceResult.period.cloneForRealPeriod(advanceResult.periodDate));

                    cursor = advanceResult.newDate;
                    logger.info("AdvanceHourFunction Position[{}] Period [{}-{}]: {} mn : cursor {} for {} mn", position, advanceResult.period.startTime, advanceResult.period.endTime, advanceResult.period.getMinutes(), cursor, durationInMinutes);

                }
                ZonedDateTime zonedDateTime = null;
                if (calendarInput.getCalculatedStartDateZoneOffset() != null &&
                        (calendarInput.getBusinessZoneId() != null || slotContainer.is247Calendar())) {
                    // resultDate is on the Business Calendar TimeZone, then we apply the offset reverse
                    ZonedDateTime zdt = null;
                    if (calendarInput.getBusinessZoneId() != null) {
                        zdt = cursor.atZone(calendarInput.getBusinessZoneId());
                    } else if (calendarInput.getCalculatedStartDateZoneOffset() != null) {
                        zdt = cursor.atZone(calendarInput.getCalculatedStartDateZoneOffset());
                    }

                    zonedDateTime = zdt == null ? null : zdt.toInstant().atOffset(calendarInput.getCalculatedStartDateZoneOffset()).toZonedDateTime();
                }
                calendarOutput.addResult(position, true, cursor, zonedDateTime, listPeriods);
                logger.info("AdvanceHourFunction.end:   Position[{}] StartDate[{}] BusinessZoneId[{}] Duration[{}] In Mn[{}] direction [{}] Holidays[{}] HolidayCountries[{}] ResultLocalDateTime[{}] BusinessZoneId[{}] ResultZonedDateTime[{}]",
                        position,
                        calendarInput.getCalculatedStartDateLocalDateTime(),
                        calendarInput.getBusinessZoneId(),
                        calendarInput.getPrivateDuration(position, false),
                        durationInMinutes,
                        calendarInput.isDirectionForward(),
                        calendarInput.isUseHolidays(),
                        calendarInput.getHolidaysCountries(),
                        cursor,
                        calendarInput.getBusinessZoneId(),
                        zonedDateTime);
            }

            // Throuw an error if one result is not found
            if (calendarInput.isErrorWhenNoDateFound()) {
                for (String position : calendarInput.getPositionDurations()) {
                    CalendarAdvanceOutput.Result result = calendarOutput.getListResultDate(position);
                    if (!result.foundDate)
                        throw new ConnectorException(CalendarAdvanceError.ERROR_NO_DATE_FOUND, "No date found for [" + position + "]");
                }
            }
            return calendarOutput;

        } catch (ConnectorException ce) {
            // already log
            throw ce;
        } catch (Exception e) {
            logger.error("AdanceDayFunction During operation : ", e);
            throw new ConnectorException(CalendarAdvanceError.ERROR_DURING_OPERATION, "Error " + e);
        }

    }

    @Override
    public String getSubFunctionName() {
        return "Advance hours";
    }

    @Override
    public String getSubFunctionDescription() {
        return "Advance in the Calendar based on hours time.";
    }

    @Override
    public String getSubFunctionType() {
        return ADVANCE_HOURS;
    }


    @Override
    public List<RunnerParameter> getInputsParameter() {
        return Arrays.asList(
                CalendarAdvanceInput.parameterStartDay,
                CalendarAdvanceInput.parameterDuration,
                CalendarAdvanceInput.parameterDurations,
                CalendarAdvanceInput.parameterDirection,
                CalendarAdvanceInput.parameterBusinessCalendar,
                CalendarAdvanceInput.parameterBusinessTimeZone,
                CalendarAdvanceInput.parameterUseHolidays,
                CalendarAdvanceInput.parameterHolidayCountries,
                CalendarAdvanceInput.parameterHolidayCalendarPolicy,
                CalendarAdvanceInput.parameterErrorWhenNoDateFound

        );
    }

    @Override
    public List<RunnerParameter> getOutputsParameter() {
        return List.of(CalendarAdvanceOutput.parameterFoundDate,
                CalendarAdvanceOutput.parameterResultDate,
                CalendarAdvanceOutput.parameterResultZonedDate,
                CalendarAdvanceOutput.parameterListPeriods,
                CalendarAdvanceOutput.parameterListResultDates);
    }

    @Override
    public Map<String, String> getBpmnErrors() {
        return Map.of(CalendarAdvanceError.ERROR_BAD_DURATION, CalendarAdvanceError.ERROR_BAD_DURATION_EXPLANATION,
                CalendarAdvanceError.ERROR_DURING_OPERATION, CalendarAdvanceError.ERROR_DURING_OPERATION_EXPLANATION,
                CalendarAdvanceError.ERROR_CANT_GET_HOLIDAYS, CalendarAdvanceError.ERROR_CANT_GET_HOLIDAYS_EXPLANATION,
                CalendarAdvanceError.ERROR_NO_COUNTRIESCODE, CalendarAdvanceError.ERROR_NO_COUNTRIESCODE_EXPLANATION,
                CalendarAdvanceError.ERROR_NO_REFERENCE_START_DATE, CalendarAdvanceError.ERROR_NO_REFERENCE_START_DATE_EXPLANATION,
                CalendarAdvanceError.ERROR_BAD_PERIOD, CalendarAdvanceError.ERROR_BAD_PERIOD_EXPLANATION,
                CalendarAdvanceError.ERROR_BAD_INPUTPARAMETER, CalendarAdvanceError.ERROR_BAD_INPUTPARAMETER_EXPLANATION,
                CalendarAdvanceError.ERROR_BAD_STARTDATE, CalendarAdvanceError.ERROR_BAD_STARTDATE_EXPLANATION,
                CalendarAdvanceError.ERROR_NO_DATE_FOUND, CalendarAdvanceError.ERROR_NO_DATE_FOUND_EXPLANATION
        );

    }
}