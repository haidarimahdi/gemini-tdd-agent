package com.example;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FindDayOfWeekTest {

    @Test
    void testFindDayOfWeek_TodayDate() {
        LocalDate date = LocalDate.of(2025, Month.OCTOBER, 28); // May 15, 2024
        FindDayOfWeek findDayOfWeek = new FindDayOfWeek();
        String dayOfWeek = findDayOfWeek.findDay(date);

        assertEquals("TUESDAY", dayOfWeek);
    }

    @Test
    void testFindDayOfWeek_LeapYear() {
        LocalDate date = LocalDate.of(2020, Month.FEBRUARY, 29); // February 29, 2020
        FindDayOfWeek findDayOfWeek = new FindDayOfWeek();
        String dayOfWeek = findDayOfWeek.findDay(date);

        assertEquals("SATURDAY", dayOfWeek);
    }

    @Test
    void testFindDayOfWeek_NewYear() {
        LocalDate date = LocalDate.of(2023, Month.JANUARY, 1); // January 1, 2023
        FindDayOfWeek findDayOfWeek = new FindDayOfWeek();
        String dayOfWeek = findDayOfWeek.findDay(date);

        assertEquals("SUNDAY", dayOfWeek);
    }
}
