package com.instagramclone.utility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilityEnhancedTest {

    @Test
    @DisplayName("startOfToday returns midnight at current date in UTC")
    void startOfToday_ReturnsMidnight() {
        LocalDateTime startOfToday = DateTimeUtility.startOfToday();
        assertThat(startOfToday.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(startOfToday.toLocalDate()).isEqualTo(LocalDate.now(DateTimeUtility.UTC));
    }

    @Test
    @DisplayName("startOfWeek returns Monday midnight")
    void startOfWeek_ReturnsMondayMidnight() {
        LocalDateTime startOfWeek = DateTimeUtility.startOfWeek();
        assertThat(startOfWeek.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(startOfWeek.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(startOfWeek).isBeforeOrEqualTo(LocalDateTime.now(DateTimeUtility.UTC));
    }

    @Test
    @DisplayName("startOfMonth returns 1st of month midnight")
    void startOfMonth_ReturnsFirstOfMonth() {
        LocalDateTime startOfMonth = DateTimeUtility.startOfMonth();
        assertThat(startOfMonth.getDayOfMonth()).isEqualTo(1);
        assertThat(startOfMonth.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
    }

    @Test
    @DisplayName("startOfYear returns Jan 1st midnight")
    void startOfYear_ReturnsFirstOfYear() {
        LocalDateTime startOfYear = DateTimeUtility.startOfYear();
        assertThat(startOfYear.getDayOfYear()).isEqualTo(1);
        assertThat(startOfYear.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
    }

    @Test
    @DisplayName("addMilliseconds correctly adds elapsed millis")
    void addMilliseconds_AddsMillis() {
        Instant now = Instant.ofEpochMilli(1000);
        Instant later = DateTimeUtility.addMilliseconds(now, 5000);
        assertThat(later.toEpochMilli()).isEqualTo(6000);
    }

    @Test
    @DisplayName("addDays correctly increments local date time")
    void addDays_IncrementsDays() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime advanced = DateTimeUtility.addDays(base, 5);
        assertThat(advanced.getDayOfMonth()).isEqualTo(6);
    }

    @Test
    @DisplayName("formatDateAndTime returns expected formatted map")
    void formatDateAndTime_FormatsProperly() {
        LocalDate date = LocalDate.of(2026, 5, 20);
        LocalTime time = LocalTime.of(14, 30);
        Map<String, String> formatted = DateTimeUtility.formatDateAndTime(date, time);
        assertThat(formatted.get("date")).isEqualTo("20/05/2026");
        assertThat(formatted.get("time")).isEqualTo("02:30 pm");
    }

    @Test
    @DisplayName("isExpired detects past and future times")
    void isExpired_DetectsExpiry() {
        LocalDateTime past = LocalDateTime.now(DateTimeUtility.UTC).minusMinutes(5);
        LocalDateTime future = LocalDateTime.now(DateTimeUtility.UTC).plusMinutes(5);

        assertThat(DateTimeUtility.isExpired(past)).isTrue();
        assertThat(DateTimeUtility.isExpired(future)).isFalse();
        assertThat(DateTimeUtility.isExpired(null)).isTrue();
    }
}
