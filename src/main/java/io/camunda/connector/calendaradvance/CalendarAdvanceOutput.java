package io.camunda.connector.calendaradvance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.connector.calendaradvance.timemachine.SlotContainer;
import io.camunda.connector.calendaradvance.toolbox.ParameterToolbox;
import io.camunda.connector.cherrytemplate.CherryOutput;
import io.camunda.connector.cherrytemplate.RunnerParameter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CalendarAdvanceOutput implements CherryOutput {

    public static final String OUTPUT_FOUNDDATE = "foundDate";
    public static final String OUTPUT_RESULTDATE = "resultDate";
    public static final String OUTPUT_LISTPERIOD = "resultDate";
    public static final RunnerParameter parameterFoundDate = new RunnerParameter(OUTPUT_FOUNDDATE, // name
            "Found Date", // label
            Boolean.class, // class
            RunnerParameter.Level.REQUIRED, "Return if a date is found by the calculation");
    public static final RunnerParameter parameterResultDate = new RunnerParameter(OUTPUT_RESULTDATE, // name
            "Result Date", // label
            LocalDateTime.class, // class
            RunnerParameter.Level.REQUIRED, "Date, LocalDateTime format, returned");
    public static final RunnerParameter parameterListPeriod = new RunnerParameter(OUTPUT_LISTPERIOD, // name
            "List of period Date", // label
            LocalDateTime.class, // class
            RunnerParameter.Level.REQUIRED, "List of period calculated");
    public boolean foundDate;
    public LocalDateTime resultDate;
    public ZonedDateTime resultZonedDate;
    public List<SlotContainer.Period> listPeriods = new ArrayList<>();

    @JsonIgnore
    @Override
    public List<Map<String, Object>> getOutputParameters() {
        return ParameterToolbox.getOutputParameters();
    }

}
