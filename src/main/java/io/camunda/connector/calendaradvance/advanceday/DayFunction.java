package io.camunda.connector.calendaradvance.advanceday;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import io.camunda.connector.calendaradvance.toolbox.CalendarAdvanceError;
import io.camunda.connector.calendaradvance.toolbox.SubFunction;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DayFunction implements SubFunction {
    private static final Map<String, String> listBpmnErrors = new HashMap<>();


    private final Logger logger = LoggerFactory.getLogger(DayFunction.class.getName());

    /**
     * @param pdfInput                 input
     * @param outboundConnectorContext context of the task
     * @return the output
     * @throws ConnectorException in case of any error
     */
    @Override
    public CalendarAdvanceOutput executeSubFunction(CalendarAdvanceInput pdfInput, OutboundConnectorContext outboundConnectorContext) throws ConnectorException {

        logger.debug("AdanceDayFunction Start PdfToImages");
        try {
            // add all pages from sources
            CalendarAdvanceOutput pdfOutput = new CalendarAdvanceOutput();
            return pdfOutput;

        } catch (Exception e) {
            logger.error("AdanceDayFunction During operation : ", e);
            throw new ConnectorException(CalendarAdvanceError.ERROR_DURING_OPERATION, "Error " + e);
        }

    }


    @Override
    public List<RunnerParameter> getInputsParameter() {
        return Collections.singletonList(CalendarAdvanceInput.parameterDirection
        );
    }

    @Override
    public List<RunnerParameter> getOutputsParameter() {
        return List.of(CalendarAdvanceOutput.parameterFoundDate);
    }

    @Override
    public Map<String, String> getBpmnErrors() {
        return listBpmnErrors;
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
        return "advance-day";
    }
}
