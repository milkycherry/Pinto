package dev.milky.pinto;

import java.util.Objects;

public final class Task {
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;
    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_DAILY = 1;

    public long id;
    public String title;
    public String note;
    public Long dueAt;
    public int priority;
    public boolean completed;
    public long createdAt;
    public Long completedAt;
    public int reminderMinutes;
    public int repeatType;
    public Long repeatEnd;
    public Long seriesId;

    public Task() {
        title = "";
        note = "";
        priority = PRIORITY_MEDIUM;
        createdAt = System.currentTimeMillis();
        reminderMinutes = 10;
        repeatType = REPEAT_NONE;
    }

    public Task copy() {
        Task copy = new Task();
        copy.id = id;
        copy.title = title;
        copy.note = note;
        copy.dueAt = dueAt;
        copy.priority = priority;
        copy.completed = completed;
        copy.createdAt = createdAt;
        copy.completedAt = completedAt;
        copy.reminderMinutes = reminderMinutes;
        copy.repeatType = repeatType;
        copy.repeatEnd = repeatEnd;
        copy.seriesId = seriesId;
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Task)) return false;
        return id == ((Task) other).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
