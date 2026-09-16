package dev.milky.pinto;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/*
  タスクの通知時刻をAndroidのAlarmManagerへ登録・延期・解除するクラス。
*/
public final class AlarmScheduler {
    /* インスタンス化せず、staticメソッドだけを利用する。 */
    private AlarmScheduler() {}

    /* 未完了かつこれからに通知時刻があるタスクだけをAlarmManagerへ登録する。 */
    public static void schedule(Context context, Task task) {
        cancel(context, task.id);
        if (task.completed || task.dueAt == null) return;

        // 期限から「何分前に知らせるか」を引いて実際の通知時刻を求める。
        long triggerAt = task.dueAt - task.reminderMinutes * 60_000L;
        if (triggerAt <= System.currentTimeMillis()) return;

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent(context, task.id, ReminderReceiver.ACTION_REMIND)
        );
    }

    /* 指定した待ち時間の後に、同じタスクをもう一度通知する。 */
    public static void snooze(Context context, long taskId, long delayMillis) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMillis,
                pendingIntent(context, taskId, ReminderReceiver.ACTION_REMIND)
        );
    }

    /* 指定タスクの予約済み通知を解除する。 */
    public static void cancel(Context context, long taskId) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent(context, taskId, ReminderReceiver.ACTION_REMIND));
    }

    /* 通知・完了・スヌーズの操作をBroadcastReceiverへ届けるPendingIntentを作る。 */
    public static PendingIntent pendingIntent(Context context, long taskId, String action) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setAction(action)
                .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId);
        return PendingIntent.getBroadcast(
                context,
                requestCode(taskId, action),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /* 同じタスクでも操作種別ごとに衝突しないrequestCodeを作る。 */
    private static int requestCode(long taskId, String action) {
        int base = (int) (taskId & 0x3fffffff);
        if (ReminderReceiver.ACTION_COMPLETE.equals(action)) return base | 0x40000000;
        if (ReminderReceiver.ACTION_SNOOZE.equals(action)) return base | 0x20000000;
        return base;
    }
}
