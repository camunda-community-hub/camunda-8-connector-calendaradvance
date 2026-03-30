package io.camunda.connector.calendaradvance.advancehour;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import io.camunda.connector.calendaradvance.advanceday.DayFunction;
import io.camunda.connector.calendaradvance.timemachine.SlotContainer;
import io.camunda.connector.calendaradvance.toolbox.CalendarAdvanceError;
import io.camunda.connector.calendaradvance.toolbox.SubFunction;
import io.camunda.connector.calendaradvance.toolbox.ValidateInput;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HourFunction implements SubFunction {


    public static final String ADVANCE_HOURS = "advance-hours";
    private final Logger logger = LoggerFactory.getLogger(DayFunction.class.getName());


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

            // add all pages from sources
            CalendarAdvanceOutput calendarOutput = new CalendarAdvanceOutput();

            SlotContainer slotContainer = new SlotContainer();
            slotContainer.setSlots(calendarInput.getBusinessCalendar());

            // There is a special use case here: if a ZonedDateTime is given AND the slotContainer is a 24/7, then we can enable the zoned callation: offset is just set to 0
            if (slotContainer.is247Calendar() && calendarInput.isZonedDateTime()) {
                calendarInput.forceZoneOffset(ZoneOffset.ofHours(0));
            }

            LocalDateTime cursor = calendarInput.getCalculatedStartDateLocalDateTime();


            long durationInMinutes = calendarInput.getDurationInMinutes();

            for (int i = 0; i < 1000; i++) {
                // Calculate the next period according the current date. The Period is adapted to the cursor
                SlotContainer.AdvanceResult advanceResult = slotContainer.getNextPeriod(cursor,
                        calendarInput.getAdvance(),
                        calendarInput.isUseHolidays(),
                        calendarInput.getHolidaysCountries());

                if (!advanceResult.foundPeriod) {
                    // This is the end here!
                    calendarOutput.foundDate = false;
                    return calendarOutput;
                }
                // reduce the duration by the period
                if (advanceResult.period.getMinutes() >= durationInMinutes) {
                    // This is the end!
                    SlotContainer.Period lastPeriod;
                    if (calendarInput.getAdvance()) {
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
                    calendarOutput.listPeriods.add(lastPeriod);
                    logger.debug("AdvanceDayFunction LAST Period [{}-{}]: {} mn : now {} ", lastPeriod.startTime, lastPeriod.endTime, lastPeriod.getMinutes(), cursor);

                    break; // end of the loop
                }
                durationInMinutes -= advanceResult.period.getMinutes();

                calendarOutput.listPeriods.add(advanceResult.period.cloneForRealPeriod(advanceResult.periodDate));

                cursor = advanceResult.newDate;
                logger.debug("AdvanceDayFunction Period [{}-{}]: {} mn : now {} for {} mn", advanceResult.period.startTime, advanceResult.period.endTime, advanceResult.period.getMinutes(), cursor, durationInMinutes);

            }
            calendarOutput.foundDate = true;
            calendarOutput.resultDate = cursor;
            if (calendarInput.getCalculatedZoneOffset() != null &&
                    (calendarInput.getBusinessZoneId() != null || slotContainer.is247Calendar()) ) {
                // resuoltDate is on the Business Calendar TimeZone, then we apply the offset reverse
                ZonedDateTime zdt=null;
                if (calendarInput.getBusinessZoneId()!=null) {
                    zdt = cursor.atZone(calendarInput.getBusinessZoneId());
                } else if (calendarInput.getCalculatedZoneOffset()!=null) {
                    zdt=cursor.atZone(calendarInput.getCalculatedZoneOffset());
                }

                calendarOutput.resultZonedDate = zdt ==null? null:zdt.withZoneSameInstant(calendarInput.getCalculatedZoneOffset());
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
                CalendarAdvanceInput.parameterDirection,
                CalendarAdvanceInput.parameterBusinessCalendar,
                CalendarAdvanceInput.parameterBusinessTimeZone,
                CalendarAdvanceInput.parameterUseHolidays,
                CalendarAdvanceInput.parameterHolidayCountries
        );
    }

    @Override
    public List<RunnerParameter> getOutputsParameter() {
        return List.of(CalendarAdvanceOutput.parameterFoundDate, CalendarAdvanceOutput.parameterResultDate, CalendarAdvanceOutput.parameterListPeriod);
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
                CalendarAdvanceError.ERROR_BAD_STARTDATE, CalendarAdvanceError.ERROR_BAD_STARTDATE_EXPLANATION
        );

    }
}