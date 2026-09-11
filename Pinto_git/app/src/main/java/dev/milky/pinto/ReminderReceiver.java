package dev.milky.pinto;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_REMIND = "dev.milky.pinto.action.REMIND";
    public static final String ACTION_SNOOZE = "dev.milky.pinto.action.SNOOZE";
    public static final String ACTION_COMPLETE = "dev.milky.pinto.action.COMPLETE";
    public static final String EXTRA_TASK_ID = "task_id";
    private static final String CHANNEL_ID = "task_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
        if (taskId < 0) return;

        String action = intent.getAction();
        NotificationManager notifications = context.getSystemService(NotificationManager.class);
        if (ACTION_SNOOZE.equals(action)) {
            notifications.cancel(notificationId(taskId));
            AlarmScheduler.snooze(context, taskId, 10 * 60_000L);
            return;
        }
        if (ACTION_COMPLETE.equals(action)) {
            TaskStore store = new TaskStore(context);
            Task next = store.setCompleted(taskId, true);
            store.close();
            AlarmScheduler.cancel(context, taskId);
            if (next != null) AlarmScheduler.schedule(context, next);
            notifications.cancel(notificationId(taskId));
            return;
        }

        TaskStore store = new TaskStore(context);
        Task task = store.get(taskId);
        store.close();
        if (task == null || task.completed) return;
        showNotification(context, notifications, task);
    }

    private void showNotification(Context context, NotificationManager manager, Task task) {
        ensureChannel(context, manager);
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        Intent openIntent = new Intent(context, MainActivity.class)
                .putExtra(EXTRA_TASK_ID, task.id)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(
                context,
                (int) task.id,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String content = task.dueAt == null ? "期限が近づいています" : TimeUtils.relativeDue(task.dueAt);
        if (task.repeatType == Task.REPEAT_DAILY) content = "毎日 · " + content;
        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(task.title)
                .setContentText(content)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(android.app.Notification.CATEGORY_REMINDER)
                .setPriority(android.app.Notification.PRIORITY_HIGH)
                .addAction(0, "10分後", AlarmScheduler.pendingIntent(context, task.id, ACTION_SNOOZE))
                .addAction(0, "完了", AlarmScheduler.pendingIntent(context, task.id, ACTION_COMPLETE))
                .build();
        manager.notify(notificationId(task.id), notification);
    }

    public static void ensureChannel(Context context, NotificationManager manager) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_description));
        manager.createNotificationChannel(channel);
    }

    private static int notificationId(long taskId) {
        return (int) (taskId & 0x7fffffff);
    }
}
