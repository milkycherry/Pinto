package dev.milky.pinto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public final class TimeUtils {
    private static final Locale JAPANESE = Locale.JAPANESE;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private TimeUtils() {}

    public static long startOfToday() {
        return LocalDate.now().atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    public static long startOfTomorrow() {
        return LocalDate.now().plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    public static long defaultTaskDue() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = now.withHour(20).withMinute(0).withSecond(0).withNano(0);
        if (!due.isAfter(now)) due = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        return due.atZone(ZONE).toInstant().toEpochMilli();
    }

    public static long atDateAndTime(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(ZONE).toInstant().toEpochMilli();
    }

    public static LocalDateTime toLocalDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZONE);
    }

    public static long toMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZONE).toInstant().toEpochMilli();
    }

    public static long endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZONE).minusNanos(1).toInstant().toEpochMilli();
    }

    public static String dateOnly(long millis) {
        return toLocalDateTime(millis).format(DateTimeFormatter.ofPattern("yyyy/M/d(E)", JAPANESE));
    }

    public static String headerDate() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() + "月" + today.getDayOfMonth() + "日 "
                + today.getDayOfWeek().getDisplayName(TextStyle.SHORT, JAPANESE);
    }

    public static String relativeDue(long millis) {
        LocalDateTime dateTime = toLocalDateTime(millis);
        LocalDate date = dateTime.toLocalDate();
        LocalDate today = LocalDate.now();
        String time = dateTime.format(DateTimeFormatter.ofPattern("H:mm", JAPANESE));
        if (date.equals(today)) return "今日 " + time;
        if (date.equals(today.plusDays(1))) return "明日 " + time;
        if (date.equals(today.minusDays(1))) return "昨日 " + time;
        if (date.getYear() == today.getYear()) {
            return dateTime.format(DateTimeFormatter.ofPattern("M/d(E) H:mm", JAPANESE));
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy/M/d H:mm", JAPANESE));
    }

    public static boolean isOverdue(Task task) {
        return !task.completed && task.dueAt != null && task.dueAt < System.currentTimeMillis();
    }
}
