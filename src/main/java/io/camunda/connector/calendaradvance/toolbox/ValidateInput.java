package io.camunda.connector.calendaradvance.toolbox;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.advanceday.DayFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateInput {

    private static final Logger logger = LoggerFactory.getLogger(ValidateInput.class.getName());

    public static void validateInput(CalendarAdvanceInput input, boolean localDateTimeExpected) throws ConnectorException {
        if (localDateTimeExpected) {
            if (input.getCalculatedStartDateLocalDateTime() == null) {
                logger.error("ValidateInput: Input[{}}], expected strict ISO-8601 UTC format like 2026-01-16T15:34:00, with or without a timezone", input.startDate);
                throw new ConnectorException(CalendarAdvanceError.ERROR_NO_REFERENCE_START_DATE, "Input[" + input.startDate + "], expected strict ISO-8601 UTC format like 2026-01-16T15:34:00, with or without a timezone");
            }
        }
        if (input.isUseHolidays() && (input.getHolidaysCountries() == null || input.getHolidaysCountries().isEmpty())) {
            logger.error("ValidateInput: When useHolidays is set, a country code must be provided");
            throw new ConnectorException(CalendarAdvanceError.ERROR_NO_COUNTRIESCODE, "When useHolidays is set, a country code must be provided");
        }

        if (DayFunction.ADVANCE_DAYS.equals(input.getCalendarAdvanceFunction())) {
            if (input.getDayProgression() == null) {
                logger.error("ValidateInput: A dayProgression must be set");
                throw new ConnectorException(CalendarAdvanceError.ERROR_MISSING_INPUT, "A dayProgression must be set");
            }

        }

    }
}
