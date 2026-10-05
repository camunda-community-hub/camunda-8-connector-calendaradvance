package org.camunda.advancecalendar.junit;

import org.camunda.connector.calendaradvance.timemachine.HolidayContainer;

import java.time.LocalDate;
import java.util.List;

/**
 * Preloads real public-holiday data (fetched once from date.nager.at and hardcoded here) into
 * HolidayContainer, so holiday-dependent tests are deterministic and don't make a live HTTP call.
 * Without this, tests fail in any sandboxed/offline environment where Java's HttpClient can't
 * establish its internal loopback connection - unrelated to which date is under test.
 * Call preload() once (e.g. in a @BeforeAll) before running a test that sets useHolidays = true.
 */
public class TestHolidayData {

    public static void preload() {
        HolidayContainer container = HolidayContainer.getInstance();
        container.preloadCalendar(2024, "FR", List.of(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 4, 1),
                LocalDate.of(2024, 5, 1),
                LocalDate.of(2024, 5, 8),
                LocalDate.of(2024, 5, 9),
                LocalDate.of(2024, 5, 20),
                LocalDate.of(2024, 7, 14),
                LocalDate.of(2024, 8, 15),
                LocalDate.of(2024, 11, 1),
                LocalDate.of(2024, 11, 11),
                LocalDate.of(2024, 12, 25)));

        container.preloadCalendar(2025, "FR", List.of(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 4, 21),
                LocalDate.of(2025, 5, 1),
                LocalDate.of(2025, 5, 8),
                LocalDate.of(2025, 5, 29),
                LocalDate.of(2025, 6, 9),
                LocalDate.of(2025, 7, 14),
                LocalDate.of(2025, 8, 15),
                LocalDate.of(2025, 11, 1),
                LocalDate.of(2025, 11, 11),
                LocalDate.of(2025, 12, 25)));

        container.preloadCalendar(2025, "US", List.of(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 20),
                LocalDate.of(2025, 2, 12),
                LocalDate.of(2025, 2, 17),
                LocalDate.of(2025, 4, 18),
                LocalDate.of(2025, 5, 8),
                LocalDate.of(2025, 5, 26),
                LocalDate.of(2025, 6, 19),
                LocalDate.of(2025, 7, 4),
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 10, 13),
                LocalDate.of(2025, 11, 11),
                LocalDate.of(2025, 11, 27),
                LocalDate.of(2025, 12, 25)));

        container.preloadCalendar(2026, "FR", List.of(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 6),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 8),
                LocalDate.of(2026, 5, 14),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 7, 14),
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 12, 25)));

        container.preloadCalendar(2026, "US", List.of(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 19),
                LocalDate.of(2026, 2, 12),
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 4, 3),
                LocalDate.of(2026, 5, 8),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 6, 19),
                LocalDate.of(2026, 7, 3),
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 26),
                LocalDate.of(2026, 12, 25)));

        container.preloadCalendar(2027, "FR", List.of(
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 3, 29),
                LocalDate.of(2027, 5, 1),
                LocalDate.of(2027, 5, 6),
                LocalDate.of(2027, 5, 8),
                LocalDate.of(2027, 5, 17),
                LocalDate.of(2027, 7, 14),
                LocalDate.of(2027, 8, 15),
                LocalDate.of(2027, 11, 1),
                LocalDate.of(2027, 11, 11),
                LocalDate.of(2027, 12, 25)));

        container.preloadCalendar(2027, "US", List.of(
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 1, 18),
                LocalDate.of(2027, 2, 12),
                LocalDate.of(2027, 2, 15),
                LocalDate.of(2027, 3, 26),
                LocalDate.of(2027, 5, 8),
                LocalDate.of(2027, 5, 31),
                LocalDate.of(2027, 6, 18),
                LocalDate.of(2027, 7, 5),
                LocalDate.of(2027, 9, 6),
                LocalDate.of(2027, 10, 11),
                LocalDate.of(2027, 11, 11),
                LocalDate.of(2027, 11, 25),
                LocalDate.of(2027, 12, 24)));
    }
}
