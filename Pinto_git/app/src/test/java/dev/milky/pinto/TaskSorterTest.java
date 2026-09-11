package dev.milky.pinto;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TaskSorterTest {
    private static final long TODAY = 1_000_000L;
    private static final long TOMORROW = 2_000_000L;

    @Test
    public void sameDueDateSortsHighPriorityFirst() {
        Task low = task(1, "低", 1_500_000L, Task.PRIORITY_LOW, false);
        Task high = task(2, "高", 1_500_000L, Task.PRIORITY_HIGH, false);

        List<Task> result = TaskSorter.filterAndSort(
                Arrays.asList(low, high), TaskSorter.Filter.TODAY, "", TODAY, TOMORROW);

        assertEquals("高", result.get(0).title);
        assertEquals("低", result.get(1).title);
    }

    @Test
    public void todayIncludesOverdueAndExcludesCompleted() {
        Task overdue = task(1, "期限切れ", 500_000L, Task.PRIORITY_MEDIUM, false);
        Task completed = task(2, "完了", 1_500_000L, Task.PRIORITY_HIGH, true);

        List<Task> result = TaskSorter.filterAndSort(
                Arrays.asList(overdue, completed), TaskSorter.Filter.TODAY, "", TODAY, TOMORROW);

        assertEquals(1, result.size());
        assertEquals("期限切れ", result.get(0).title);
    }

    @Test
    public void searchChecksTitleAndNote() {
        Task task = task(1, "買い物", null, Task.PRIORITY_MEDIUM, false);
        task.note = "牛乳を忘れない";

        List<Task> result = TaskSorter.filterAndSort(
                Arrays.asList(task), TaskSorter.Filter.ALL, "牛乳", TODAY, TOMORROW);

        assertEquals(1, result.size());
    }

    private Task task(long id, String title, Long dueAt, int priority, boolean completed) {
        Task task = new Task();
        task.id = id;
        task.title = title;
        task.dueAt = dueAt;
        task.priority = priority;
        task.completed = completed;
        task.createdAt = id;
        return task;
    }
}
