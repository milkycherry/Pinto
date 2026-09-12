package dev.milky.pinto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 保存済みタスクから、画面に表示するタスクの絞り込みと並べ替えを行うクラス。
 */
public final class TaskSorter {
    /** ホーム画面で選べる4種類の表示条件。 */
    public enum Filter { TODAY, UPCOMING, ALL, COMPLETED }

    /** インスタンス化せず、staticメソッドだけを利用する。 */
    private TaskSorter() {}

    /**
     * フィルターと検索語に一致するタスクを抽出し、期限・優先度順で返す。
     * 元のリスト自体は変更しない。
     */
    public static List<Task> filterAndSort(
            List<Task> source,
            Filter filter,
            String query,
            long startOfToday,
            long startOfTomorrow
    ) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.JAPANESE);
        List<Task> result = new ArrayList<>();

        // タイトルとメモを1つの検索対象として、部分一致で絞り込む。
        for (Task task : source) {
            if (!matchesFilter(task, filter, startOfToday, startOfTomorrow)) continue;
            String searchable = (task.title + " " + task.note).toLowerCase(Locale.JAPANESE);
            if (!needle.isEmpty() && !searchable.contains(needle)) continue;
            result.add(task);
        }

        // 未完了、期限あり、期限の早さ、優先度の高さ、作成順の順に整列する。
        result.sort(Comparator
                .comparing((Task task) -> task.completed)
                .thenComparing(task -> task.dueAt == null)
                .thenComparingLong(task -> task.dueAt == null ? Long.MAX_VALUE : task.dueAt)
                .thenComparing((Task task) -> task.priority, Comparator.reverseOrder())
                .thenComparingLong(task -> task.createdAt));
        return result;
    }

    /** 選択中フィルターに1件のタスクが含まれるかを判定する。 */
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
