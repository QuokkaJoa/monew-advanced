package com.part2.monew.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeUtil {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    public static Timestamp parseTimestamp(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        if (dateTimeString.startsWith("T") && !dateTimeString.contains("-")) {
            return null;
        }

        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeString);
            return Timestamp.from(offsetDateTime.toInstant());
        } catch (DateTimeParseException e1) {
            try {
                Instant instant = Instant.parse(dateTimeString);
                return Timestamp.from(instant);
            } catch (DateTimeParseException e2) {
                try {
                    LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString,
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return Timestamp.from(localDateTime.atZone(KOREA_ZONE).toInstant());
                } catch (DateTimeParseException e3) {
                    try {
                        java.time.LocalDate localDate = java.time.LocalDate.parse(dateTimeString);
                        LocalDateTime startOfDay = localDate.atStartOfDay();
                        return Timestamp.from(startOfDay.atZone(KOREA_ZONE).toInstant());
                    } catch (DateTimeParseException e4) {
                        return null;
                    }
                }
            }
        }
    }


    public static Timestamp parseTimestampAsNextDayStart(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        if (dateTimeString.startsWith("T") && !dateTimeString.contains("-")) {
            return null;
        }

        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeString);
            return Timestamp.from(offsetDateTime.toInstant());
        } catch (DateTimeParseException e1) {
            try {
                Instant instant = Instant.parse(dateTimeString);
                return Timestamp.from(instant);
            } catch (DateTimeParseException e2) {
                try {
                    LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString,
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return Timestamp.from(localDateTime.atZone(KOREA_ZONE).toInstant());
                } catch (DateTimeParseException e3) {
                    try {
                        java.time.LocalDate localDate = java.time.LocalDate.parse(dateTimeString);
                        LocalDateTime nextDayStart = localDate.plusDays(1).atStartOfDay();
                        return Timestamp.from(nextDayStart.atZone(KOREA_ZONE).toInstant());
                    } catch (DateTimeParseException e4) {
                        return null;
                    }
                }
            }
        }
    }

}