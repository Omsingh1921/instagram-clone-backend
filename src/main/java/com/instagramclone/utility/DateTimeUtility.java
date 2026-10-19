package com.instagramclone.utility;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

/**
 * Reusable modern Java Date/Time helper adhering to Section 49.5 (Date/Time Utility).
 * Provides UTC-standardized timestamps, date range anchors, and ISO/custom formatters.
 */
public final class DateTimeUtility {

    public static final ZoneId UTC = ZoneOffset.UTC;
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private DateTimeUtility() {}

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC);
    }

    public static Instant nowInstant() {
        return Instant.now();
    }

    public static boolean isExpired(LocalDateTime targetTime) {
        if (targetTime == null) return true;
        return nowUtc().isAfter(targetTime);
    }

    public static boolean isInstantExpired(Instant targetTime) {
        if (targetTime == null) return true;
        return Instant.now().isAfter(targetTime);
    }

    public static String formatIso(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(ISO_FORMATTER);
    }

    // ==================== DATE RANGE ANCHORS ====================

    public static LocalDateTime startOfToday() {
        return startOfToday(UTC);
    }

    public static LocalDateTime startOfToday(ZoneId zone) {
        ZoneId effectiveZone = zone != null ? zone : UTC;
        return LocalDate.now(effectiveZone).atStartOfDay();
    }

    public static LocalDateTime startOfWeek() {
        return startOfWeek(UTC);
    }

    public static LocalDateTime startOfWeek(ZoneId zone) {
        ZoneId effectiveZone = zone != null ? zone : UTC;
        return LocalDate.now(effectiveZone)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
    }

    public static LocalDateTime startOfMonth() {
        return startOfMonth(UTC);
    }

    public static LocalDateTime startOfMonth(ZoneId zone) {
        ZoneId effectiveZone = zone != null ? zone : UTC;
        return LocalDate.now(effectiveZone)
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();
    }

    public static LocalDateTime startOfYear() {
        return startOfYear(UTC);
    }

    public static LocalDateTime startOfYear(ZoneId zone) {
        ZoneId effectiveZone = zone != null ? zone : UTC;
        return LocalDate.now(effectiveZone)
                .with(TemporalAdjusters.firstDayOfYear())
                .atStartOfDay();
    }

    // ==================== ARITHMETIC & FORMATTING ====================

    public static Instant addMilliseconds(Instant instant, long milliseconds) {
        if (instant == null) return null;
        return instant.plusMillis(milliseconds);
    }

    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) return null;
        return dateTime.plusDays(days);
    }

    public static Map<String, String> formatDateAndTime(LocalDate date, LocalTime time) {
        String formattedDate = date != null ? date.format(DATE_FORMATTER) : null;
        String formattedTime = time != null ? time.format(TIME_FORMATTER) : null;
        return Map.of(
                "date", formattedDate != null ? formattedDate : "",
                "time", formattedTime != null ? formattedTime : ""
        );
    }

    public static Instant toUtcInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    public static LocalDateTime toUtcLocalDateTime(Instant instant) {
        if (instant == null) return null;
        return LocalDateTime.ofInstant(instant, UTC);
    }
}
