package io.camunda.connector.calendaradvance.toolbox;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.calendaradvance.CalendarAdvanceInput;
import io.camunda.connector.calendaradvance.CalendarAdvanceOutput;
import io.camunda.connector.cherrytemplate.RunnerParameter;

import java.util.List;
import java.util.Map;

public interface SubFunction {
    CalendarAdvanceOutput executeSubFunction(CalendarAdvanceInput pdfInput, OutboundConnectorContext context) throws ConnectorException;

    String getSubFunctionName();

    String getSubFunctionDescription();

    String getSubFunctionType();

    List<RunnerParameter> getInputsParameter();

    List<RunnerParameter> getOutputsParameter();

    Map<String, String> getBpmnErrors();

    enum TypeParameter {INPUT, OUTPUT}

}
