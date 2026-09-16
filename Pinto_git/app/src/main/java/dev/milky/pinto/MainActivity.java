package dev.milky.pinto;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

import java.util.List;

/*
  Pintoの画面全体を取りまとめるActivity。
  
  <p>Androidのライフサイクル、データ保存、通知予約の調整だけを担当し、
  画面の組み立ては {@link TaskListScreen}、入力画面は {@link TaskEditorDialog} に任せる。</p>
 */
public final class MainActivity extends android.app.Activity
        implements TaskListScreen.Listener, TaskEditorDialog.Listener {
    /* 端末内のSQLiteへタスクを保存する窓口。 */
    private TaskStore store;

    /* 一覧・検索・絞り込みを表示するホーム画面。 */
    private TaskListScreen taskListScreen;

    /* タスクの追加と編集を受け付けるダイアログ。 */
    private TaskEditorDialog taskEditorDialog;

    /* アプリ起動時に、保存先・通知・画面を初期化する。 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        store = new TaskStore(this);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        ReminderReceiver.ensureChannel(this, notificationManager);

        taskListScreen = new TaskListScreen(this, this);
        taskEditorDialog = new TaskEditorDialog(this, this);
        View root = taskListScreen.getRootView();
        setContentView(root);

        applySystemBars(root);
        requestNotificationPermissionIfNeeded();
        refreshTasks();
        handleIntent(getIntent());
    }

    /* 通知から既存Activityが再利用されたとき、対象タスクを開く。 */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /* 他画面から戻った場合にも、端末内の最新データで一覧を描き直す。 */
    @Override
    protected void onResume() {
        super.onResume();
        if (taskListScreen != null) refreshTasks();
    }

    /* Activity終了時にデータベース接続を閉じる。 */
    @Override
    protected void onDestroy() {
        if (store != null) store.close();
        super.onDestroy();
    }

    /* 保存済みタスクを読み込み、一覧画面へ渡す。 */
    private void refreshTasks() {
        List<Task> tasks = store.getAll();
        taskListScreen.render(tasks);
    }

    /* 一覧画面の追加ボタンから呼ばれ、空の編集ダイアログを開く。 */
    @Override
    public void onAddTaskRequested() {
        taskEditorDialog.show(null);
    }

    /* 選択されたタスクのコピーを編集ダイアログで開く。 */
    @Override
    public void onEditTaskRequested(Task task) {
        taskEditorDialog.show(task);
    }

    /* 完了状態を保存し、毎日繰り返すタスクなら翌日分の通知も予約する。 */
    @Override
    public void onTaskCompletionChanged(Task task, boolean completed) {
        Task next = store.setCompleted(task.id, completed);
        if (completed) {
            // 完了した回の通知は不要なので取り消す。
            AlarmScheduler.cancel(this, task.id);
            if (next != null) {
                // 繰り返し予定の次回分が作られた場合だけ、新しい通知を予約する。
                AlarmScheduler.schedule(this, next);
                Toast.makeText(this, "翌日分を追加しました", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 未完了へ戻したタスクは、保存後の値を読み直して通知を再予約する。
            Task updated = store.get(task.id);
            if (updated != null) AlarmScheduler.schedule(this, updated);
        }
        refreshTasks();
    }

    /* 一覧のメニューで変更された優先度だけを保存する。 */
    @Override
    public void onTaskPriorityChanged(Task task, int priority) {
        Task updated = task.copy();
        updated.priority = priority;
        store.update(updated);
        refreshTasks();
    }

    /* 編集ダイアログから渡された入力結果を追加または更新し、通知を予約する。 */
    @Override
    public void onTaskSaved(Task task, boolean isNewTask) {
        if (isNewTask) store.insert(task);
        else store.update(task);
        AlarmScheduler.schedule(this, task);
        refreshTasks();
    }

    /* 一覧または編集ダイアログからの削除依頼に対し、確認画面を表示する。 */
    @Override
    public void onDeleteTaskRequested(Task task) {
        showDeleteConfirmation(task);
    }

    /* 誤操作を防ぐため、削除前に確認してからデータと通知を消す。 */
    private void showDeleteConfirmation(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("タスクを削除しますか？")
                .setMessage("「" + task.title + "」は元に戻せません。")
                .setNegativeButton("キャンセル", null)
                .setPositiveButton("削除", (dialog, which) -> {
                    AlarmScheduler.cancel(this, task.id);
                    store.delete(task.id);
                    refreshTasks();
                })
                .show();
    }

    /* 通知から渡されたタスクIDを読み取り、該当する編集画面を一度だけ開く。 */
    private void handleIntent(Intent intent) {
        if (intent == null) return;
        long taskId = intent.getLongExtra(ReminderReceiver.EXTRA_TASK_ID, -1);
        if (taskId >= 0) {
            Task task = store.get(taskId);
            if (task != null) taskEditorDialog.show(task);
            intent.removeExtra(ReminderReceiver.EXTRA_TASK_ID);
        }
    }

    /* Android 13以降で通知権限が未許可の場合だけ、システムの許可画面を出す。 */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    /* ステータスバー等に内容が隠れないよう、安全領域を画面の余白へ反映する。 */
    @SuppressWarnings("deprecation")
    private void applySystemBars(View root) {
        getWindow().setStatusBarColor(PintoViewFactory.CREAM);
        getWindow().setNavigationBarColor(PintoViewFactory.CREAM);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(0, top, 0, bottom);
            return insets;
        });
    }
}
