package io.camunda.connector.calendaradvance.advancehour;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import io.camunda.connector.calendaradvance.advanceday.DayFunction;
import io.camunda.connector.calendaradvance.timemachine.HolidayContainer;
import io.camunda.connector.calendaradvance.timemachine.SlotContainer;
import io.camunda.connector.calendaradvance.toolbox.CalendarAdvanceError;
import io.camunda.connector.calendaradvance.toolbox.SubFunction;
import io.camunda.connector.calendaradvance.toolbox.ValidateInput;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HourFunction implements SubFunction {


    private final Logger logger = LoggerFactory.getLogger(DayFunction.class.getName());


    /**
     * @param calendarInput            input
     * @param outboundConnectorContext context of the task
     * @return the output
     * @throws ConnectorException in case of any error
     */
    @Override
    public CalendarAdvanceOutput executeSubFunction(CalendarAdvanceInput calendarInput, OutboundConnectorContext outboundConnectorContext) throws ConnectorException {

        logger.debug("AdanceDayFunction Start");
        try {

            // First, calculate the date according all parameters
            calendarInput.calculateReferenceDateLocalDateTime();

            // Validate input
            ValidateInput.validateInput(calendarInput, true);

            // add all pages from sources
            CalendarAdvanceOutput calendarOutput = new CalendarAdvanceOutput();

            if (calendarInput.isUseHolidays()) {
                HolidayContainer.getInstance().loadCountries(calendarInput.getHolidaysCountries());
            }

            SlotContainer slotContainer = new SlotContainer();
            slotContainer.setSlots(calendarInput.getBusinessCalendar());
            calendarInput.calculateReferenceDateLocalDateTime();
            LocalDateTime cursor = calendarInput.getCalculatedStartDateLocalDateTime();


            long durationInMinutes = calendarInput.getDurationInMinutes();

            for (int i = 0; i < 1000; i++) {
                if (calendarInput.isUseHolidays() && HolidayContainer.getInstance().isHoliday(cursor.toLocalDate(), calendarInput.getHolidaysCountries())) {
                    // we have to adance to the next day
                    cursor = slotContainer.advanceNextDay(cursor, calendarInput.getAdvance());
                } else {

                    SlotContainer.AdvanceResult advanceResult = slotContainer.getNextPeriod(cursor, calendarInput.getAdvance());

                    if (!advanceResult.foundPeriod) {
                        // This is the end here!
                        calendarOutput.foundDate = false;
                        return calendarOutput;
                    }
                    // reduce the duration by the period
                    if (advanceResult.period.getMinutes() > durationInMinutes) {
                        // This is the end!

                        if (calendarInput.getAdvance())
                            cursor = LocalDateTime.of(LocalDate.from(cursor), advanceResult.period.startTime)
                                    .plusMinutes(durationInMinutes);
                        else
                            cursor = LocalDateTime.of(LocalDate.from(cursor), advanceResult.period.endTime)
                                    .minusMinutes(durationInMinutes);
                        logger.debug("AdvanceDayFunction LAST Period [{}-{}]: {} mn : now {} ", advanceResult.period.startTime, advanceResult.period.endTime, advanceResult.period.getMinutes(), cursor);

                        break; // end of the loop
                    }
                    durationInMinutes -= advanceResult.period.getMinutes();

                    cursor = advanceResult.newDate;
                    logger.debug("AdvanceDayFunction Period [{}-{}]: {} mn : now {} for {} mn", advanceResult.period.startTime, advanceResult.period.endTime, advanceResult.period.getMinutes(), cursor, durationInMinutes);
                }
            }
            calendarOutput.foundDate = true;
            calendarOutput.resultDate = cursor;
            if (calendarInput.getCalculatedZoneOffset() != null && calendarInput.getBusinessZoneId() != null) {
                // resuoltDate is on the Business Calendar TimeZone, then we apply the offset reverse
                ZonedDateTime zdt = cursor.atZone(calendarInput.getBusinessZoneId());
                calendarOutput.resultZonedDate = zdt.withZoneSameInstant(calendarInput.getCalculatedZoneOffset());
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
        return "advance-hours";
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
        return List.of(CalendarAdvanceOutput.parameterFoundDate, CalendarAdvanceOutput.parameterFResultDate);
    }

    @Override
    public Map<String, String> getBpmnErrors() {
        return Map.of(CalendarAdvanceError.ERROR_BAD_DURATION, CalendarAdvanceError.ERROR_BAD_DURATION_EXPLANATION,
                CalendarAdvanceError.ERROR_DURING_OPERATION, CalendarAdvanceError.ERROR_DURING_OPERATION_EXPLANATION);
    }
}