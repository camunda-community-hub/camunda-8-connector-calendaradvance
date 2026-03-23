package io.camunda.connector.calendaradvance.timemachine;

import java.time.LocalDate;
import java.util.List;


public class HolidayContainer {

    private final static HolidayContainer instance = new HolidayContainer();

    public static HolidayContainer getInstance() {
        return instance;
    }

    public void loadCountries(List<String> countries) {

    }

    public boolean isHoliday(LocalDate date, List<String> countries) {
        return false;
    }
}
