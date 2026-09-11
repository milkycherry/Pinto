package dev.milky.pinto;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RecurrenceTest {
    @Test
    public void dailyRepeatKeepsTimeAndAdvancesOneDay() {
        Task task = dailyTask(
                LocalDateTime.of(2026, 9, 1, 8, 30),
                LocalDate.of(2026, 9, 3));

        Long next = Recurrence.nextDue(task);

        assertEquals(LocalDateTime.of(2026, 9, 2, 8, 30), TimeUtils.toLocalDateTime(next));
    }

    @Test
    public void repeatEndDateIsInclusive() {
        Task task = dailyTask(
                LocalDateTime.of(2026, 9, 2, 20, 0),
                LocalDate.of(2026, 9, 3));

        Long next = Recurrence.nextDue(task);

        assertEquals(LocalDateTime.of(2026, 9, 3, 20, 0), TimeUtils.toLocalDateTime(next));
    }

    @Test
    public void stopsAfterRepeatEndDate() {
        Task task = dailyTask(
                LocalDateTime.of(2026, 9, 3, 20, 0),
                LocalDate.of(2026, 9, 3));

        assertNull(Recurrence.nextDue(task));
    }

    @Test
    public void nonRepeatingTaskHasNoNextOccurrence() {
        Task task = new Task();
        task.dueAt = TimeUtils.toMillis(LocalDateTime.of(2026, 9, 1, 8, 0));
        task.repeatType = Task.REPEAT_NONE;

        assertNull(Recurrence.nextDue(task));
    }

    private Task dailyTask(LocalDateTime due, LocalDate end) {
        Task task = new Task();
        task.dueAt = TimeUtils.toMillis(due);
        task.repeatType = Task.REPEAT_DAILY;
        task.repeatEnd = TimeUtils.endOfDay(end);
        return task;
    }
}
