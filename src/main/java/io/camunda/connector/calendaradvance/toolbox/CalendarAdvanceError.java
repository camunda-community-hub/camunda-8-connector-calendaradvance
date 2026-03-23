package io.camunda.connector.calendaradvance.toolbox;

public class CalendarAdvanceError {

    public static final String ERROR_UNKNOWN_FUNCTION = "UNKNOWN_FUNCTION";
    public static final String ERROR_UNKNOWN_FUNCTION_EXPLANATION = "The function is unknown. There is a limited number of operation";

    public static final String ERROR_BAD_INPUTPARAMETER = "BAD_INPUTPARAMETER";
    public static final String ERROR_BAD_INPUTPARAMETER_EXPLANATION = "During the bind, some input does not have the expected type";


    public static final String ERROR_DURING_OPERATION = "ERROR_DURING_OPERATION";
    public static final String ERROR_DURING_OPERATION_EXPLANATION = "Error during the operation";

    public static final String ERROR_NO_REFERENCE_START_DATE = "ERROR_NO_REFERENCE_START_DATE";
    public static final String ERROR_NO_REFERENCE_START_DATE_EXPLANATION = "A reference start date must be provided in the correct format";

    public static final String ERROR_BAD_DURATION = "ERROR_BAD_DURATION";
    public static final String ERROR_BAD_DURATION_EXPLANATION = "Duration must be an ISO8601 format: P3D or PT14H54M or P2DT14H";

}
