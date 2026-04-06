package io.camunda.connector.calendaradvance.timemachine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.calendaradvance.toolbox.CalendarAdvanceError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class HolidayContainer {
    private final static HolidayContainer instance = new HolidayContainer();
    private final Logger logger = LoggerFactory.getLogger(HolidayContainer.class.getName());
    private final ConcurrentHashMap<String, CalendarHoliday> calendars = new ConcurrentHashMap<>();

    public static HolidayContainer getInstance() {
        return instance;
    }

    /**
     * Load in advance calendar.
     * Note: during the calculation, it is possible to go over a year, so the next year can be load dring the isHoliday method
     *
     * @param year          year to load
     * @param countriesCode list of code to load
     */
    public void loadCountries(int year, List<String> countriesCode) {
        for (String countryCode : countriesCode) {
            getCalendar(year, countryCode);
        }
    }

    /**
     * Return true if the date is a holiday in any of the countries code given
     *
     * @param date          date to check
     * @param countriesCode list of code
     * @return true if the day in an holiday
     */
    public boolean isHoliday(LocalDate date, List<String> countriesCode) {
        for (String countryCode : countriesCode) {
            CalendarHoliday calendarHoliday = getCalendar(date.getYear(), countryCode);
            if (calendarHoliday.listDays.contains(date))
                return true;
        }
        return false;
    }

    /**
     * Get a calendar, and if not present, load it
     *
     * @param countryCode code to load the calendar
     * @param year        year to load
     * @return the calendar Holiday
     * @throws ConnectorException any error
     */
    public CalendarHoliday getCalendar(int year, String countryCode) throws ConnectorException {
        String key = countryCode + "-" + year;
        try {
            return calendars.computeIfAbsent(key, k -> loadHoliday(year, countryCode));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ConnectorException ce) {
                throw ce;
            }
            throw new ConnectorException(CalendarAdvanceError.ERROR_CANT_GET_HOLIDAYS, "Error when trying to load the calendar " + e.getMessage());
        }
    }

    /**
     * Throw a runtime exception to pass the lambda
     *
     * @param year        year of the holiday to load
     * @param countryCode country code
     * @return the calendar Holiday
     * @throws RuntimeException in case of error
     */
    private CalendarHoliday loadHoliday(int year, String countryCode) throws RuntimeException {
        String url = "https://date.nager.at/api/v3/PublicHolidays/" + year + "/" + countryCode;
        long beginTime = System.currentTimeMillis();
        try {
            logger.debug("Call calendar[{}]", url);
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            // -- explode the result
            ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.readTree(response.body());
            CalendarHoliday calendarHoliday = new CalendarHoliday();
            calendarHoliday.country = countryCode;
            calendarHoliday.year = year;

            for (JsonNode node : root) {
                calendarHoliday.listDays.add(LocalDate.parse(node.get("date").asText()));
            }
            logger.info("Call calendar countryCode[{}] Year[{}] url[{}] with success, found {} holiday in {} ms", countryCode, year, url, calendarHoliday.listDays.size(), System.currentTimeMillis() - beginTime);
            return calendarHoliday;
        } catch (Exception e) {
            logger.error("Can't get the calendar for countryCode[{}] Year[{}] url[{}} in {} ms", countryCode, year, url, System.currentTimeMillis() - beginTime, e);
            throw new RuntimeException(new ConnectorException(CalendarAdvanceError.ERROR_CANT_GET_HOLIDAYS, "Error when trying to load the calendar " + e.getMessage()));
        }
    }

    /**
     * A calendar Holiday
     */
    public static class CalendarHoliday {
        public int year;
        public String country;
        public List<LocalDate> listDays = new ArrayList<>();
    }

}


