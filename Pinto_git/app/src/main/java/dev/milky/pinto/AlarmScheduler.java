package dev.milky.pinto;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class AlarmScheduler {
    private AlarmScheduler() {}

    public static void schedule(Context context, Task task) {
        cancel(context, task.id);
        if (task.completed || task.dueAt == null) return;

        long triggerAt = task.dueAt - task.reminderMinutes * 60_000L;
        if (triggerAt <= System.currentTimeMillis()) return;

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent(context, task.id, ReminderReceiver.ACTION_REMIND)
        );
    }

    public static void snooze(Context context, long taskId, long delayMillis) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMillis,
                pendingIntent(context, taskId, ReminderReceiver.ACTION_REMIND)
        );
    }

    public static void cancel(Context context, long taskId) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent(context, taskId, ReminderReceiver.ACTION_REMIND));
    }

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

    private static int requestCode(long taskId, String action) {
        int base = (int) (taskId & 0x3fffffff);
        if (ReminderReceiver.ACTION_COMPLETE.equals(action)) return base | 0x40000000;
        if (ReminderReceiver.ACTION_SNOOZE.equals(action)) return base | 0x20000000;
        return base;
    }
}
