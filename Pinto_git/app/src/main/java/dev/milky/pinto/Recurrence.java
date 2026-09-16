package dev.milky.pinto;

import java.time.LocalDateTime;

/*
  繰り返しタスクについて、次に作成すべき期限を計算するクラス。
*/
public final class Recurrence {
    /* インスタンス化せず、staticメソッドだけを利用する。 */
    private Recurrence() {}

    /* 毎日タスクの次回期限を返し、終了日を越える場合はnullを返す。 */
    public static Long nextDue(Task task) {
        if (task.repeatType != Task.REPEAT_DAILY || task.dueAt == null || task.repeatEnd == null) {
            return null;
        }
        LocalDateTime current = TimeUtils.toLocalDateTime(task.dueAt);
        // 時刻部分を保ったまま日付だけを1日進める。
        long next = TimeUtils.toMillis(current.plusDays(1));
        return next <= task.repeatEnd ? next : null;
    }
}
