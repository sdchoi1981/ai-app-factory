package com.factory.datekit;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DateMath {
    private DateMath() {}
    public static LocalDate checked(LocalDate date) {
        if (date.getYear() < 1900 || date.getYear() > 9999) throw new IllegalArgumentException("Date outside supported range");
        return date;
    }
    public static long difference(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(checked(start), checked(end));
    }
    public static long inclusive(LocalDate start, LocalDate end) {
        return Math.abs(difference(start,end)) + 1;
    }
    public static LocalDate offset(LocalDate start, long days) {
        if (days < -4000000 || days > 4000000) throw new IllegalArgumentException("Offset outside supported range");
        return checked(checked(start).plusDays(days));
    }
    public static String dday(LocalDate today, LocalDate target) {
        long days = difference(today,target);
        return days == 0 ? "D-Day" : days > 0 ? "D-" + days : "D+" + (-days);
    }
}
