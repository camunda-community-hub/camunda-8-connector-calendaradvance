package io.camunda.connector.calendaradvance.advanceday;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import io.camunda.connector.calendaradvance.timemachine.SlotContainer;
import io.camunda.connector.calendaradvance.toolbox.CalendarAdvanceError;
import io.camunda.connector.calendaradvance.toolbox.SubFunction;
import io.camunda.connector.calendaradvance.toolbox.ValidateInput;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Stream;

public class DayFunction implements SubFunction {

    public static final String ADVANCE_DAYS = "advance-days";

    private final Logger logger = LoggerFactory.getLogger(DayFunction.class.getName());

    /**
     * @param calendarInput            input
     * @param outboundConnectorContext context of the task
     * @return the output
     * @throws ConnectorException in case of any error
     */
    @Override
    public CalendarAdvanceOutput executeSubFunction(CalendarAdvanceInput calendarInput, OutboundConnectorContext outboundConnectorContext) throws ConnectorException {
        logger.debug("DayFunction Start");
        try {

            // First, calculate the date according all parameters
            calendarInput.calculateReferenceDateLocalDateTime();

            // Validate input
            ValidateInput.validateInput(calendarInput, true);

            // Now start the calculation
            CalendarAdvanceOutput calendarOutput = new CalendarAdvanceOutput();

            SlotContainer slotContainer = new SlotContainer();
            slotContainer.setSlots(calendarInput.getBusinessCalendar());


            AdvanceDayResult advanceDayResult;
            if (CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY.equals(calendarInput.getDayProgression())) {
                advanceDayResult = advanceCalendarByDay(calendarInput);
            } else if (CalendarAdvanceInput.DAY_PROGRESSION_V_BUSINESSDAY.equals(calendarInput.getDayProgression())) {
                advanceDayResult = advanceCalendarByBusinessDay(calendarInput);
            } else {
                throw new ConnectorException(CalendarAdvanceError.ERROR_DURING_OPERATION, "Unknow progression [" + calendarInput.getDayProgression()
                        + "] Expect [" + CalendarAdvanceInput.DAY_PROGRESSION_V_CALENDARDAY
                        + "," + CalendarAdvanceInput.DAY_PROGRESSION_V_BUSINESSDAY + "]");
            }
            if (!advanceDayResult.foundDate) {
                // This is the end here!
                calendarOutput.foundDate = false;
                return calendarOutput;
            }

            calendarOutput.resultDate = advanceDayResult.resultLocalDate.atStartOfDay();
            calendarOutput.foundDate = true;

            // Check the target Policy - we need to do that only if the progression is not BUSINESSDAY, because with BUSINESS DAY, of course the current date is open
            if (!CalendarAdvanceInput.DAY_PROGRESSION_V_BUSINESSDAY.equals(calendarInput.getDayProgression())) {
                AdvanceDayResult advanceResultAdjust = adjustTarget(calendarInput, advanceDayResult.resultLocalDate);
                if (!advanceResultAdjust.foundDate) {
                    calendarOutput.foundDate = false;
                    return calendarOutput;
                }

                calendarOutput.foundDate = true;
                calendarOutput.listPeriods = Stream.of(advanceDayResult.listPeriods, advanceResultAdjust.listPeriods)
                        .flatMap(Collection::stream)
                        .toList();
                calendarOutput.resultDate = advanceResultAdjust.resultLocalDate.atStartOfDay();
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
    public List<RunnerParameter> getInputsParameter() {
        return Arrays.asList(
                CalendarAdvanceInput.parameterStartDay,
                CalendarAdvanceInput.parameterDuration,
                CalendarAdvanceInput.parameterDirection,
                CalendarAdvanceInput.parameterBusinessCalendar,
                CalendarAdvanceInput.parameterUseHolidays,
                CalendarAdvanceInput.parameterHolidayCountries,
                CalendarAdvanceInput.parameterDayProgression,
                CalendarAdvanceInput.parameterTargetProgression
        );
    }

    @Override
    public List<RunnerParameter> getOutputsParameter() {
        return List.of(CalendarAdvanceOutput.parameterFoundDate,
                CalendarAdvanceOutput.parameterResultDate,
                CalendarAdvanceOutput.parameterListPeriods
        );
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
                CalendarAdvanceError.ERROR_MISSING_INPUT, CalendarAdvanceError.ERROR_MISSING_INPUT_EXPLANATION);
    }

    @Override
    public String getSubFunctionName() {
        return "Advance days";
    }

    @Override
    public String getSubFunctionDescription() {
        return "Advance the calendar by days. Advance my be forward or backward";
    }

    @Override
    public String getSubFunctionType() {
        return ADVANCE_DAYS;
    }

    /**
     * Advance Calendar by day, not by business day.
     *
     * @param calendarInput input to get all parameters
     * @return the advanceResult, which contains the period and the result
     * @throws ConnectorException for any error
     */
    private AdvanceDayResult advanceCalendarByDay(CalendarAdvanceInput calendarInput) throws ConnectorException {
        LocalDate cursor = calendarInput.getReferenceDateLocalDate();
        CalendarAdvanceInput.TYPEPERIOD type = calendarInput.getTypePeriod();

        LocalDate endDate;
        if (type == CalendarAdvanceInput.TYPEPERIOD.DAY || type == CalendarAdvanceInput.TYPEPERIOD.TIME) {
            endDate = cursor.plusDays((calendarInput.isDirectionForward() ? 1 : -1) * calendarInput.getDurationInDays());
        } else if (type == CalendarAdvanceInput.TYPEPERIOD.MONTH) {
            endDate = cursor.plusMonths((calendarInput.isDirectionForward() ? 1 : -1) * calendarInput.getDurationInMonths());
        } else if (type == CalendarAdvanceInput.TYPEPERIOD.YEAR) {
            endDate = cursor.plusYears((calendarInput.isDirectionForward() ? 1 : -1) * calendarInput.getDurationInYears());
        } else {
            throw new ConnectorException(CalendarAdvanceError.ERROR_BAD_DURATION, "Unknow type");
        }


        AdvanceDayResult dayResult = new AdvanceDayResult();

        int count = 0;
        while (!cursor.equals(endDate)) {
            count++;
            if (count > 1000) {
                dayResult.foundDate = false;
                return dayResult;
            }
            cursor = cursor.plusDays((calendarInput.isDirectionForward() ? 1 : -1));

            dayResult.listPeriods.add(SlotContainer.Period.getPeriod(cursor.getDayOfWeek(),
                    LocalDateTime.MIN.toLocalTime(),
                    LocalDateTime.MAX.toLocalTime()).setDateOfPeriod(cursor));

        }

        dayResult.foundDate = true;
        dayResult.resultLocalDate = cursor;
        return dayResult;
    }

    /**
     * Advance calendar by business day
     *
     * @param calendarInput input to get all parameters
     * @return the advanceResult, which contains the period and the result
     * @throws ConnectorException in case of error
     */
    private AdvanceDayResult advanceCalendarByBusinessDay(CalendarAdvanceInput calendarInput) throws
            ConnectorException {

        AdvanceDayResult dayResult = new AdvanceDayResult();


        LocalDate cursor = calendarInput.getReferenceDateLocalDate();

        long durationInDays = calendarInput.getDurationInDays();
        SlotContainer slotContainer = new SlotContainer();
        slotContainer.setSlots(calendarInput.getBusinessCalendar());

        // invariant: we are on a date (cursor) with a number of day to adance.
        // first, if the day to advance is 0, the current day is the day
        // particular case: numberofday=0, then we return this day (don't move)
        for (int i = 0; i < 1000; i++) {
            if (durationInDays <= 0) {
                break;
            }

            // Now, we search the next period. We start at the end of the day (or begining if we go forward)
            SlotContainer.AdvanceResult advanceResult = slotContainer.getNextPeriod(
                    calendarInput.isDirectionForward() ? LocalDateTime.of(cursor, SlotContainer.MIDNIGHT_MINUS) : cursor.atStartOfDay(),
                    calendarInput.isDirectionForward(),
                    calendarInput.isUseHolidays(),
                    calendarInput.getHolidaysCountries());

            if (!advanceResult.foundPeriod) {
                // This is the end here!
                dayResult.foundDate = false;
                return dayResult;
            }

            // Perfect, we have a period. The cursor move that period day, and we count --
            // note: if there are multiple period on the day, not important because the exploration start at the end of the day.
            dayResult.listPeriods.add(SlotContainer.Period.getPeriod(advanceResult.periodDate.getDayOfWeek(),
                    LocalDateTime.MIN.toLocalTime(),
                    LocalDateTime.MAX.toLocalTime()).setDateOfPeriod(advanceResult.periodDate));

            durationInDays--;
            cursor = advanceResult.periodDate;
            logger.debug("AdvanceDayFunction Day: [{}] NumberofDays[{}] ", cursor, durationInDays);

        }
        dayResult.foundDate = true;
        dayResult.resultLocalDate = cursor;

        return dayResult;
    }

    /**
     * AdjustTarget : the calculated day may be an holiday, so we need to adjust this date according the policy
     */
    private AdvanceDayResult adjustTarget(CalendarAdvanceInput calendarInput, LocalDate cursor) {
        AdvanceDayResult dayResult = new AdvanceDayResult();
        if (CalendarAdvanceInput.TARGET_PROGRESSION_RESULT.equals(calendarInput.getTargetProgression())) {
            dayResult.foundDate = true;
            dayResult.resultLocalDate = cursor;
            return dayResult;
        }
        int direction =
                (CalendarAdvanceInput.TARGET_PROGRESSION_BEFORE.equals(calendarInput.getTargetProgression()) ? -1 : 1);

        SlotContainer slotContainer = new SlotContainer();
        slotContainer.setSlots(calendarInput.getBusinessCalendar());
        // First, we want to test the CURRENT day, it may be an open day!
        // direction > 0 (forward), start 00:00. direction<0 (backward), test 23:59+1
        SlotContainer.AdvanceResult advanceResult = slotContainer.getNextPeriod(
                direction > 0 ? cursor.atStartOfDay() : LocalDateTime.of(cursor, LocalTime.MIDNIGHT),
                direction > 0,
                calendarInput.isUseHolidays(),
                calendarInput.getHolidaysCountries());

        if (!advanceResult.foundPeriod) {
            // This is the end here!
            dayResult.foundDate = false;
            return dayResult;
        }
        dayResult.foundDate = true;
        dayResult.resultLocalDate = advanceResult.periodDate;
        dayResult.listPeriods.add(SlotContainer.Period.getPeriod(advanceResult.periodDate.getDayOfWeek(),
                LocalDateTime.MIN.toLocalTime(),
                LocalDateTime.MAX.toLocalTime()).setDateOfPeriod(advanceResult.periodDate));
        return dayResult;
    }

    private static class AdvanceDayResult {
        public List<SlotContainer.Period> listPeriods = new ArrayList<>();
        public boolean foundDate;
        public LocalDate resultLocalDate;
    }
}