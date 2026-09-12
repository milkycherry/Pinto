package dev.milky.pinto;

import java.util.Objects;

/**
 * Pintoで扱う1件のタスクを表すデータモデル。
 *
 * <p>SQLiteのtasksテーブルと同じ項目を持ち、画面・保存・通知の間で受け渡す。</p>
 */
public final class Task {
    /** 優先度を保存するときの数値。値が大きいほど優先度が高い。 */
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;

    /** 繰り返し方法を保存するときの数値。 */
    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_DAILY = 1;

    /** DB上の識別子と、利用者が入力・選択する基本情報。 */
    public long id;
    public String title;
    public String note;
    public Long dueAt;
    public int priority;
    public boolean completed;

    /** 作成・完了・通知に関する時刻情報。時刻はエポックミリ秒で保持する。 */
    public long createdAt;
    public Long completedAt;
    public int reminderMinutes;

    /** 繰り返しの方法・終了日時・同じ予定系列の識別子。 */
    public int repeatType;
    public Long repeatEnd;
    public Long seriesId;

    /** 新規タスク向けの既定値を設定する。 */
    public Task() {
        title = "";
        note = "";
        priority = PRIORITY_MEDIUM;
        createdAt = System.currentTimeMillis();
        reminderMinutes = 10;
        repeatType = REPEAT_NONE;
    }

    /** 編集中に元データを直接変更しないため、全項目を複製したTaskを返す。 */
    public Task copy() {
        Task copy = new Task();
        copy.id = id;
        copy.title = title;
        copy.note = note;
        copy.dueAt = dueAt;
        copy.priority = priority;
        copy.completed = completed;
        copy.createdAt = createdAt;
        copy.completedAt = completedAt;
        copy.reminderMinutes = reminderMinutes;
        copy.repeatType = repeatType;
        copy.repeatEnd = repeatEnd;
        copy.seriesId = seriesId;
        return copy;
    }

    /** DB上の同じIDを持つタスクを同一とみなす。 */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Task)) return false;
        return id == ((Task) other).id;
    }

    /** equalsと同じくIDを基準にハッシュ値を作る。 */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
