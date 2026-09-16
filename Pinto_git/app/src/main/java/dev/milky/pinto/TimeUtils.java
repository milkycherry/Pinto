package dev.milky.pinto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/*
  日付・時刻の変換、既定期限、画面表示用の日本語整形をまとめるクラス。
*/
public final class TimeUtils {
    /* 表示言語と、端末設定に従うタイムゾーン。 */
    private static final Locale JAPANESE = Locale.JAPANESE;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    /* インスタンス化せず、staticメソッドだけを利用する。 */
    private TimeUtils() {}

    /* 今日の0時をエポックミリ秒で返す。 */
    public static long startOfToday() {
        return LocalDate.now().atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    /* 明日の0時をエポックミリ秒で返す。 */
    public static long startOfTomorrow() {
        return LocalDate.now().plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    /* 新規タスクの既定期限として、未来になる今日20時または次の正時を返す。 */
    public static long defaultTaskDue() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = now.withHour(20).withMinute(0).withSecond(0).withNano(0);
        if (!due.isAfter(now)) due = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        return due.atZone(ZONE).toInstant().toEpochMilli();
    }

    /* 指定した日付・時・分をエポックミリ秒へ変換する。 */
    public static long atDateAndTime(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(ZONE).toInstant().toEpochMilli();
    }

    /* エポックミリ秒を端末タイムゾーンのLocalDateTimeへ変換する。 */
    public static LocalDateTime toLocalDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZONE);
    }

    /* LocalDateTimeを端末タイムゾーンのエポックミリ秒へ変換する。 */
    public static long toMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZONE).toInstant().toEpochMilli();
    }

    /* 指定日の最後の1ナノ秒をエポックミリ秒へ変換する。 */
    public static long endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZONE).minusNanos(1).toInstant().toEpochMilli();
    }

    /* 日付だけを「2026/9/12(土)」形式で表示する。 */
    public static String dateOnly(long millis) {
        return toLocalDateTime(millis).format(DateTimeFormatter.ofPattern("yyyy/M/d(E)", JAPANESE));
    }

    /* ホーム画面のヘッダー用に「9月12日 土」形式の日付を返す。 */
    public static String headerDate() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() + "月" + today.getDayOfMonth() + "日 "
                + today.getDayOfWeek().getDisplayName(TextStyle.SHORT, JAPANESE);
    }

    /* 今日・明日・昨日は相対表現、それ以外は日付と時刻で期限を表示する。 */
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

    /* 未完了で、期限が現在時刻より前なら期限切れと判定する。 */
    public static boolean isOverdue(Task task) {
        return !task.completed && task.dueAt != null && task.dueAt < System.currentTimeMillis();
    }
}
