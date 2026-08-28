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
    public static final String OUTPUT_LISTRESULTDATES = "listResultDates";
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
            "List of periods Date", // label
            LocalDateTime.class, // class
            RunnerParameter.Level.OPTIONAL, "List of periods calculated");
    public static final RunnerParameter parameterListResultDates = new RunnerParameter(OUTPUT_LISTRESULTDATES, // name
            "List of result dates", // label
            List.class, // class
            RunnerParameter.Level.OPTIONAL, "List of results date, populate by a list when multiple durations (in input durations) are provided");
    private boolean foundDate;
    private LocalDateTime resultDate;
    private ZonedDateTime resultZonedDate;
    private List<SlotContainer.Period> listPeriods = new ArrayList<>();
    public List<Result> listResultDates = new ArrayList<>();

    public static class Result {
        public boolean foundDate;
        public LocalDateTime resultDate;
        public ZonedDateTime resultZonedDate;
        public List<SlotContainer.Period> listPeriods = new ArrayList<>();

        public Result(boolean foundDate,
                      LocalDateTime resultDate,
                      ZonedDateTime resultZonedDate,
                      List<SlotContainer.Period> listPeriods) {
            this.foundDate = foundDate;
            this.resultDate = resultDate;
            this.resultZonedDate = resultZonedDate;
            this.listPeriods = listPeriods;
        }

    }

    public boolean isFoundDate() {
        return foundDate;
    }

    public LocalDateTime getResultDate() {
        return resultDate;
    }

    public ZonedDateTime getResultZonedDate() {
        return resultZonedDate;
    }

    public List<SlotContainer.Period> getListPeriods() {
        return listPeriods;
    }

    public List<Result> getListResultDates() {
        return listResultDates;
    }



    @JsonIgnore
    @Override
    public List<Map<String, Object>> getOutputParameters() {
        return ParameterToolbox.getOutputParameters();
    }

    public void addResult(boolean foundDate,LocalDateTime localDateTime, ZonedDateTime zonedDateTime, List<SlotContainer.Period> listPeriods) {
        listResultDates.add(new Result(foundDate, localDateTime, zonedDateTime, listPeriods));
        this.foundDate = foundDate;
        this.resultDate = localDateTime;
        this.resultZonedDate = resultZonedDate;
        this.listPeriods = listPeriods;
    }


}
