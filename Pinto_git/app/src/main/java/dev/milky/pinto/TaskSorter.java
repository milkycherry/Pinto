package dev.milky.pinto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TaskSorter {
    public enum Filter { TODAY, UPCOMING, ALL, COMPLETED }

    private TaskSorter() {}

    public static List<Task> filterAndSort(
            List<Task> source,
            Filter filter,
            String query,
            long startOfToday,
            long startOfTomorrow
    ) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.JAPANESE);
        List<Task> result = new ArrayList<>();

        for (Task task : source) {
            if (!matchesFilter(task, filter, startOfToday, startOfTomorrow)) continue;
            String searchable = (task.title + " " + task.note).toLowerCase(Locale.JAPANESE);
            if (!needle.isEmpty() && !searchable.contains(needle)) continue;
            result.add(task);
        }

        result.sort(Comparator
                .comparing((Task task) -> task.completed)
                .thenComparing(task -> task.dueAt == null)
                .thenComparingLong(task -> task.dueAt == null ? Long.MAX_VALUE : task.dueAt)
                .thenComparing((Task task) -> task.priority, Comparator.reverseOrder())
                .thenComparingLong(task -> task.createdAt));
        return result;
    }

    private static boolean matchesFilter(Task task, Filter filter, long today, long tomorrow) {
        switch (filter) {
            case TODAY:
                if (task.completed) return false;
                return task.dueAt != null && task.dueAt < tomorrow;
            case UPCOMING:
                return !task.completed && (task.dueAt == null || task.dueAt >= tomorrow);
            case COMPLETED:
                return task.completed;
            case ALL:
            default:
                return !task.completed;
        }
    }
}
