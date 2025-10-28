package com.example;

import java.time.LocalDate;
import java.time.DayOfWeek;

public class FindDayOfWeek {

    public String findDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek.toString();
    }
}