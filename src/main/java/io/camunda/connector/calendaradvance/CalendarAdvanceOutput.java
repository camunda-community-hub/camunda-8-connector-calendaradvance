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
    public static final String OUTPUT_RESULTZONEDDATE = "resultZonedDate";
    public static final String OUTPUT_LISTPERIODS = "listPeriods";
    public static final RunnerParameter parameterFoundDate = new RunnerParameter(OUTPUT_FOUNDDATE, // name
            "Found Date", // label
            Boolean.class, // class
            RunnerParameter.Level.OPTIONAL, "Return if a date is found by the calculation");
    public static final RunnerParameter parameterResultDate = new RunnerParameter(OUTPUT_RESULTDATE, // name
            "Result Date", // label
            LocalDateTime.class, // class
            RunnerParameter.Level.OPTIONAL, "Date, LocalDateTime format (2026-03-30T15:20:00)");
    public static final RunnerParameter parameterResultZonedDate = new RunnerParameter(OUTPUT_RESULTZONEDDATE, // name
            "Result Zoned Date", // label
            ZonedDateTime.class, // class
            RunnerParameter.Level.OPTIONAL, "Zoned Date, ZonedDateTime format (2026-03-30T15:20:00-06:00)");
    public static final RunnerParameter parameterListPeriods = new RunnerParameter(OUTPUT_LISTPERIODS, // name
            "List of period Date", // label
            LocalDateTime.class, // class
            RunnerParameter.Level.OPTIONAL, "List of period calculated");
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
