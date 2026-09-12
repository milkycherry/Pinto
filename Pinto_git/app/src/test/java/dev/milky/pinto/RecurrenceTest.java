package dev.milky.pinto;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 毎日繰り返しの次回期限と終了日境界を確認する単体テスト。
 */
public class RecurrenceTest {
    /** 日次タスクが時刻を保ったまま1日進むことを確認する。 */
    @Test
    public void dailyRepeatKeepsTimeAndAdvancesOneDay() {
        Task task = dailyTask(
                LocalDateTime.of(2026, 9, 1, 8, 30),
                LocalDate.of(2026, 9, 3));

        Long next = Recurrence.nextDue(task);

        assertEquals(LocalDateTime.of(2026, 9, 2, 8, 30), TimeUtils.toLocalDateTime(next));
    }

    /** 終了日当日の予定までは作成されることを確認する。 */
    @Test
    public void repeatEndDateIsInclusive() {
        Task task = dailyTask(
                LocalDateTime.of(2026, 9, 2, 20, 0),
                LocalDate.of(2026, 9, 3));

        Long next = Recurrence.nextDue(task);

        assertEquals(LocalDateTime.of(2026, 9, 3, 20, 0), TimeUtils.toLocalDateTime(next));
    }

    /** 現在の期限が終了日に達した後は次回を作らないことを確認する。 */
    @Test
    public void stopsAfterRepeatEndDate() {
        Task task = dailyTask(
                LocalDateTime.of(2026, 9, 3, 20, 0),
                LocalDate.of(2026, 9, 3));

        assertNull(Recurrence.nextDue(task));
    }

    /** 繰り返さないタスクには次回期限がないことを確認する。 */
    @Test
    public void nonRepeatingTaskHasNoNextOccurrence() {
        Task task = new Task();
        task.dueAt = TimeUtils.toMillis(LocalDateTime.of(2026, 9, 1, 8, 0));
        task.repeatType = Task.REPEAT_NONE;

        assertNull(Recurrence.nextDue(task));
    }

    /** テストに必要な日次タスクを簡潔に組み立てる。 */
    private Task dailyTask(LocalDateTime due, LocalDate end) {
        Task task = new Task();
        task.dueAt = TimeUtils.toMillis(due);
        task.repeatType = Task.REPEAT_DAILY;
        task.repeatEnd = TimeUtils.endOfDay(end);
        return task;
    }
}
