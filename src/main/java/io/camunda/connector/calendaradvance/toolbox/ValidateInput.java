package io.camunda.connector.calendaradvance.toolbox;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.advanceday.DayFunction;

public class ValidateInput {

    public static void validateInput(CalendarAdvanceInput input, boolean localDateTimeExpected) throws ConnectorException {
        if (localDateTimeExpected) {
            if (input.getCalculatedStartDateLocalDateTime() == null)
                throw new ConnectorException(CalendarAdvanceError.ERROR_NO_REFERENCE_START_DATE, "Input[" + input.startDate + "], expected strict ISO-8601 UTC format like 2026-01-16T15:34:00, with or without a timezone");
        }
        if (input.isUseHolidays() && (input.getHolidaysCountries() == null || input.getHolidaysCountries().isEmpty())) {
            throw new ConnectorException(CalendarAdvanceError.ERROR_NO_COUNTRIESCODE, "When useHolidays is set, a country code must be provided");
        }

        if (DayFunction.ADVANCE_DAYS.equals(input.getCalendarAdvanceFunction())) {
            if (input.getDayProgression() == null)
                throw new ConnectorException(CalendarAdvanceError.ERROR_MISSING_INPUT, "A dayProgress must be set");

        }

    }
}
