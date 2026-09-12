package dev.milky.pinto;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * タスクを端末内SQLiteへ永続化するデータアクセスクラス。
 *
 * <p>SQLやCursorの知識をこのクラス内へ閉じ込め、他のクラスはTaskだけを扱えるようにする。</p>
 */
public final class TaskStore extends SQLiteOpenHelper {
    /** データベースファイル名とスキーマの世代。 */
    private static final String DB_NAME = "pinto.db";
    private static final int DB_VERSION = 2;

    /** アプリ用のSQLiteOpenHelperを作る。 */
    public TaskStore(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /** 初回起動時に、全タスク項目を持つtasksテーブルを作成する。 */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "note TEXT NOT NULL DEFAULT ''," +
                "due_at INTEGER," +
                "priority INTEGER NOT NULL DEFAULT 1," +
                "completed INTEGER NOT NULL DEFAULT 0," +
                "created_at INTEGER NOT NULL," +
                "completed_at INTEGER," +
                "reminder_minutes INTEGER NOT NULL DEFAULT 10," +
                "repeat_type INTEGER NOT NULL DEFAULT 0," +
                "repeat_end INTEGER," +
                "series_id INTEGER" +
                ")");
    }

    /** 古いアプリから更新したとき、不足している繰り返し項目を追加する。 */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN repeat_type INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE tasks ADD COLUMN repeat_end INTEGER");
            db.execSQL("ALTER TABLE tasks ADD COLUMN series_id INTEGER");
        }
    }

    /** 新しいタスクを保存し、採番されたIDをTaskにも設定する。 */
    public long insert(Task task) {
        SQLiteDatabase db = getWritableDatabase();
        task.id = db.insertOrThrow("tasks", null, values(task));
        if (task.repeatType == Task.REPEAT_DAILY && task.seriesId == null) {
            // 最初の1件は自分のIDを系列IDにし、以後に作る日次タスクへ引き継ぐ。
            task.seriesId = task.id;
            ContentValues series = new ContentValues();
            series.put("series_id", task.seriesId);
            db.update("tasks", series, "id = ?", new String[]{String.valueOf(task.id)});
        }
        return task.id;
    }

    /** 指定タスクの全項目をIDに基づいて更新する。 */
    public void update(Task task) {
        if (task.repeatType == Task.REPEAT_DAILY && task.seriesId == null) {
            task.seriesId = task.id;
        }
        getWritableDatabase().update("tasks", values(task), "id = ?",
                new String[]{String.valueOf(task.id)});
    }

    /** 指定IDのタスクを削除する。 */
    public void delete(long id) {
        getWritableDatabase().delete("tasks", "id = ?", new String[]{String.valueOf(id)});
    }

    /** 指定IDのタスクを1件取得し、存在しない場合はnullを返す。 */
    public Task get(long id) {
        try (Cursor cursor = getReadableDatabase().query(
                "tasks", null, "id = ?", new String[]{String.valueOf(id)},
                null, null, null)) {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        }
    }

    /** 保存済みの全タスクを作成順で取得する。 */
    public List<Task> getAll() {
        List<Task> tasks = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "tasks", null, null, null, null, null, "created_at ASC")) {
            while (cursor.moveToNext()) tasks.add(fromCursor(cursor));
        }
        return tasks;
    }

    /**
     * 完了状態を更新する。毎日タスクを完了した場合は、同じトランザクションで次回分も作る。
     *
     * @return 新しく作成した次回タスク。作成しなかった場合はnull。
     */
    public Task setCompleted(long id, boolean completed) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Task current = get(db, id);
            if (current == null || current.completed == completed) {
                db.setTransactionSuccessful();
                return null;
            }

            ContentValues completion = new ContentValues();
            completion.put("completed", completed ? 1 : 0);
            if (completed) completion.put("completed_at", System.currentTimeMillis());
            else completion.putNull("completed_at");
            db.update("tasks", completion, "id = ?", new String[]{String.valueOf(id)});

            Task next = null;
            Long nextDue = Recurrence.nextDue(current);
            if (completed && nextDue != null && !hasOccurrence(db, current.seriesId, nextDue)) {
                // 完了した回を複製し、期限と完了情報だけを次回用へ差し替える。
                next = current.copy();
                next.id = 0;
                next.dueAt = nextDue;
                next.completed = false;
                next.completedAt = null;
                next.createdAt = System.currentTimeMillis();
                next.id = db.insertOrThrow("tasks", null, values(next));
            } else if (!completed && nextDue != null && current.seriesId != null) {
                // 完了を取り消した場合は、直前の完了操作で作った未完了の次回分を戻す。
                db.delete("tasks", "series_id = ? AND due_at = ? AND completed = 0",
                        new String[]{String.valueOf(current.seriesId), String.valueOf(nextDue)});
            }
            db.setTransactionSuccessful();
            return next;
        } finally {
            db.endTransaction();
        }
    }

    /** Taskの各フィールドをSQLiteへ渡せるContentValuesへ変換する。 */
    private ContentValues values(Task task) {
        ContentValues values = new ContentValues();
        values.put("title", task.title.trim());
        values.put("note", task.note == null ? "" : task.note.trim());
        if (task.dueAt == null) values.putNull("due_at"); else values.put("due_at", task.dueAt);
        values.put("priority", task.priority);
        values.put("completed", task.completed ? 1 : 0);
        values.put("created_at", task.createdAt);
        if (task.completedAt == null) values.putNull("completed_at");
        else values.put("completed_at", task.completedAt);
        values.put("reminder_minutes", task.reminderMinutes);
        values.put("repeat_type", task.repeatType);
        if (task.repeatEnd == null) values.putNull("repeat_end");
        else values.put("repeat_end", task.repeatEnd);
        if (task.seriesId == null) values.putNull("series_id");
        else values.put("series_id", task.seriesId);
        return values;
    }

    /** 現在行のCursorからTaskを復元する。null可能な日時項目もここで変換する。 */
    private Task fromCursor(Cursor cursor) {
        Task task = new Task();
        task.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        task.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        task.note = cursor.getString(cursor.getColumnIndexOrThrow("note"));
        int dueIndex = cursor.getColumnIndexOrThrow("due_at");
        task.dueAt = cursor.isNull(dueIndex) ? null : cursor.getLong(dueIndex);
        task.priority = cursor.getInt(cursor.getColumnIndexOrThrow("priority"));
        task.completed = cursor.getInt(cursor.getColumnIndexOrThrow("completed")) == 1;
        task.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        int completedIndex = cursor.getColumnIndexOrThrow("completed_at");
        task.completedAt = cursor.isNull(completedIndex) ? null : cursor.getLong(completedIndex);
        task.reminderMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("reminder_minutes"));
        task.repeatType = cursor.getInt(cursor.getColumnIndexOrThrow("repeat_type"));
        int repeatEndIndex = cursor.getColumnIndexOrThrow("repeat_end");
        task.repeatEnd = cursor.isNull(repeatEndIndex) ? null : cursor.getLong(repeatEndIndex);
        int seriesIndex = cursor.getColumnIndexOrThrow("series_id");
        task.seriesId = cursor.isNull(seriesIndex) ? null : cursor.getLong(seriesIndex);
        return task;
    }

    /** トランザクション中の同じDB接続を使って、指定IDのタスクを取得する。 */
    private Task get(SQLiteDatabase db, long id) {
        try (Cursor cursor = db.query("tasks", null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        }
    }

    /** 同じ系列・同じ期限のタスクがすでに存在するかを調べ、重複作成を防ぐ。 */
    private boolean hasOccurrence(SQLiteDatabase db, Long seriesId, long dueAt) {
        if (seriesId == null) return false;
        try (Cursor cursor = db.query("tasks", new String[]{"id"},
                "series_id = ? AND due_at = ?",
                new String[]{String.valueOf(seriesId), String.valueOf(dueAt)},
                null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }
}
