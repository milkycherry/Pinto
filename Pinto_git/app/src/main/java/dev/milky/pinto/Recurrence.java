package dev.milky.pinto;

import java.time.LocalDateTime;

public final class Recurrence {
    private Recurrence() {}

    public static Long nextDue(Task task) {
        if (task.repeatType != Task.REPEAT_DAILY || task.dueAt == null || task.repeatEnd == null) {
            return null;
        }
        LocalDateTime current = TimeUtils.toLocalDateTime(task.dueAt);
        long next = TimeUtils.toMillis(current.plusDays(1));
        return next <= task.repeatEnd ? next : null;
    }
}
