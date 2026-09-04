package io.camunda.connector.calendaradvance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.connector.calendaradvance.timemachine.SlotContainer;
import io.camunda.connector.calendaradvance.toolbox.ParameterToolbox;
import io.camunda.connector.cherrytemplate.CherryOutput;
import io.camunda.connector.cherrytemplate.RunnerParameter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

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
            "List or Map of result dates", // label
            Object.class, // class
            RunnerParameter.Level.OPTIONAL, "List or Map of results date, populate by the list or Map when multiple durations (in input durations) are provided");
    private boolean foundDate;
    private LocalDateTime resultDate;
    private ZonedDateTime resultZonedDate;
    private List<SlotContainer.Period> listPeriods = new ArrayList<>();
    /**
     * Result can be a LIST or a MAP accordinf the input
     */
    public Object listResultDates;


    public CalendarAdvanceOutput(boolean resultIsAMap) {
        if (resultIsAMap)
            listResultDates = new HashMap<>();
        else
            listResultDates = new ArrayList<>();
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

    public Object getListResultDates() {
        return listResultDates;
    }


    /**
     * return true if the result is a Map, false if this is a List, null if this is not a map, not a list
     *
     * @return
     */
    @JsonIgnore
    public Boolean isResultIsAMap() {
        if (listResultDates instanceof List resultList)
            return false;
        if (listResultDates instanceof Map resultMap)
            return true;
        return null;
    }

    @JsonIgnore
    public Collection<Result> getListResultDateCollection() {
        if (listResultDates instanceof List resultList)
            return new LinkedHashSet<>(resultList);
        ;
        if (listResultDates instanceof Map resultMap)
            return resultMap.values();
        return null;
    }

    @JsonIgnore
    public Result getListResultDate(String position) {
        if (listResultDates instanceof List resultList)
            return (Result) resultList.get(Integer.parseInt(position));
        if (listResultDates instanceof Map resultMap)
            return (Result) resultMap.get(position);
        return null;
    }

    @JsonIgnore
    public int getNumberOfResultDate() {
        if (listResultDates instanceof List resultList)
            return resultList.size();
        if (listResultDates instanceof Map resultMap)
            return resultMap.size();
        return 1;
    }

    @JsonIgnore
    @Override
    public List<Map<String, Object>> getOutputParameters() {
        return ParameterToolbox.getOutputParameters();
    }

    public void addResult(String key, boolean foundDate, LocalDateTime localDateTime, ZonedDateTime zonedDateTime, List<SlotContainer.Period> listPeriods) {
        if (listResultDates instanceof List)
            ((List) listResultDates).add(new Result(foundDate, localDateTime, zonedDateTime, listPeriods));
        if (listResultDates instanceof Map)
            ((Map) listResultDates).put(key, new Result(foundDate, localDateTime, zonedDateTime, listPeriods));

        this.foundDate = foundDate;
        this.resultDate = localDateTime;
        this.resultZonedDate = zonedDateTime;
        this.listPeriods = listPeriods;
    }


    /**
     * Result information
     */
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

}
