package dev.milky.pinto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 端末再起動またはアプリ更新後に、保存済みタスクの通知予約を復元するReceiver。
 */
public final class BootReceiver extends BroadcastReceiver {
    /** 対象イベントだけを受け付け、全タスクをAlarmSchedulerへ再登録する。 */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;

        // AlarmManagerの予約は再起動で失われるため、DBを正として作り直す。
        TaskStore store = new TaskStore(context);
        for (Task task : store.getAll()) {
            AlarmScheduler.schedule(context, task);
        }
        store.close();
    }
}
