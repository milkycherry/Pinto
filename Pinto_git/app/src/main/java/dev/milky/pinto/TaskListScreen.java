package dev.milky.pinto;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
  ホーム画面の表示と、検索・絞り込み・タスク行の操作をまとめるクラス。
  
  <p>データの保存は行わず、利用者の操作を {@link Listener} 経由でMainActivityへ通知する。</p>
*/
final class TaskListScreen {
    /*
      一覧画面で発生した操作を、保存処理の担当へ渡すための窓口。
     */
    interface Listener {
        /* 新しいタスクの入力画面を開くよう依頼する。 */
        void onAddTaskRequested();

        /* 指定タスクの編集画面を開くよう依頼する。 */
        void onEditTaskRequested(Task task);

        /* 指定タスクの完了状態を変更するよう依頼する。 */
        void onTaskCompletionChanged(Task task, boolean completed);

        /* 指定タスクの優先度を変更するよう依頼する。 */
        void onTaskPriorityChanged(Task task, int priority);

        /* 指定タスクを削除するよう依頼する。 */
        void onDeleteTaskRequested(Task task);
    }

    /* Android標準部品の生成に使うActivity。 */
    private final Activity activity;

    /* 一覧画面の操作を受け取る相手。 */
    private final Listener listener;

    /* Pinto共通の色・余白・部品を生成する補助クラス。 */
    private final PintoViewFactory ui;

    /* Activityへそのまま渡せるホーム画面の最上位View。 */
    private final FrameLayout rootView;

    /* フィルターごとの選択状態を更新するために保持するボタン。 */
    private final Map<TaskSorter.Filter, Button> filterButtons = new LinkedHashMap<>();

    /* 現在の検索・絞り込みに使う、保存済みタスクのスナップショット。 */
    private List<Task> tasks = Collections.emptyList();

    /* 現在選択されている一覧フィルター。 */
    private TaskSorter.Filter currentFilter = TaskSorter.Filter.TODAY;

    /* 描画後に値を更新するホーム画面の部品。 */
    private LinearLayout listContainer;
    private EditText searchInput;
    private TextView summaryTitle;
    private TextView summarySubtitle;
    private ProgressBar summaryProgress;
    private TextView listSectionTitle;
    private TextView listSectionCount;

    /* 一覧画面を組み立て、操作の通知先を設定する。 */
    TaskListScreen(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        this.ui = new PintoViewFactory(activity);
        this.rootView = buildScreen();
    }

    /* Activityのコンテンツとして使う最上位Viewを返す。 */
    View getRootView() {
        return rootView;
    }

    /* 最新のタスク一覧を受け取り、集計カードと表示行を描き直す。 */
    void render(List<Task> source) {
        tasks = new ArrayList<>(source);
        updateSummary();
        renderTaskList();
    }

    /* ヘッダーから追加ボタンまでを含むホーム画面全体を作る。 */
    private FrameLayout buildScreen() {
        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(PintoViewFactory.CREAM);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(scroll, scrollParams);

        LinearLayout page = ui.vertical();
        page.setPadding(ui.dp(20), ui.dp(20), ui.dp(20), ui.dp(112));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        page.addView(buildHeader());
        page.addView(buildSummaryCard(), ui.marginTop(ui.dp(22)));
        page.addView(buildFilters(), ui.marginTop(ui.dp(18)));
        page.addView(buildSearch(), ui.marginTop(ui.dp(10)));
        page.addView(buildListHeader(), ui.marginTop(ui.dp(20)));

        listContainer = ui.vertical();
        page.addView(listContainer, ui.marginTop(ui.dp(3)));

        // 画面をスクロールしても追加操作へすぐ到達できるよう、右下に固定する。
        Button add = new Button(activity);
        add.setText("＋ 新しい予定");
        add.setTextColor(Color.WHITE);
        add.setTextSize(15);
        add.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        add.setAllCaps(false);
        add.setGravity(Gravity.CENTER);
        add.setPadding(ui.dp(18), 0, ui.dp(18), 0);
        add.setBackground(ui.roundRect(PintoViewFactory.GREEN_DARK, ui.dp(20)));
        add.setElevation(ui.dp(8));
        add.setOnClickListener(view -> listener.onAddTaskRequested());
        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(ui.dp(142), ui.dp(54));
        addParams.gravity = Gravity.BOTTOM | Gravity.END;
        addParams.setMargins(ui.dp(20), ui.dp(20), ui.dp(20), ui.dp(24));
        root.addView(add, addParams);
        return root;
    }

    /* アプリアイコン、日付、短いキャッチコピーを含むヘッダーを作る。 */
    private View buildHeader() {
        LinearLayout row = ui.horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView mark = ui.text("✓", 19, Color.WHITE, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(ui.roundRect(PintoViewFactory.GREEN_DARK, ui.dp(12)));
        row.addView(mark, new LinearLayout.LayoutParams(ui.dp(42), ui.dp(42)));

        LinearLayout words = ui.vertical();
        TextView title = ui.text("Pinto", 25, PintoViewFactory.INK, Typeface.BOLD);
        TextView date = ui.text(
                TimeUtils.headerDate(), 13, PintoViewFactory.MUTED, Typeface.NORMAL);
        words.addView(title);
        words.addView(date, ui.marginTop(ui.dp(1)));
        LinearLayout.LayoutParams wordParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wordParams.setMarginStart(ui.dp(12));
        row.addView(words, wordParams);

        TextView hint = ui.text(
                "忘れる前に、ピン✓", 12, PintoViewFactory.GREEN, Typeface.BOLD);
        row.addView(hint);
        return row;
    }

    /* 今日の残件数と完了率を示す集計カードを作る。 */
    private View buildSummaryCard() {
        LinearLayout card = ui.vertical();
        card.setPadding(ui.dp(20), ui.dp(18), ui.dp(20), ui.dp(18));
        card.setBackground(ui.roundRect(PintoViewFactory.GREEN_DARK, ui.dp(22)));
        card.setElevation(ui.dp(3));

        summaryTitle = ui.text("今日の予定", 20, Color.WHITE, Typeface.BOLD);
        summarySubtitle = ui.text(
                "読み込み中…", 13, Color.rgb(220, 235, 227), Typeface.NORMAL);
        summaryProgress = new ProgressBar(
                activity, null, android.R.attr.progressBarStyleHorizontal);
        summaryProgress.setMax(100);
        summaryProgress.setProgressTintList(
                ColorStateList.valueOf(Color.rgb(153, 204, 177)));
        summaryProgress.setProgressBackgroundTintList(
                ColorStateList.valueOf(Color.rgb(75, 118, 100)));
        summaryProgress.setIndeterminate(false);

        card.addView(summaryTitle);
        card.addView(summarySubtitle, ui.marginTop(ui.dp(4)));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(7));
        progressParams.topMargin = ui.dp(14);
        card.addView(summaryProgress, progressParams);
        return card;
    }

    /* 今日・これから・すべて・完了を切り替えるフィルターボタンを作る。 */
    private View buildFilters() {
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = ui.horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(row);
        addFilterChip(row, TaskSorter.Filter.TODAY, "今日");
        addFilterChip(row, TaskSorter.Filter.UPCOMING, "これから");
        addFilterChip(row, TaskSorter.Filter.ALL, "すべて");
        addFilterChip(row, TaskSorter.Filter.COMPLETED, "完了");
        updateFilterButtons();
        return scroll;
    }

    /* タイトルとメモを対象にする検索入力欄を作る。 */
    private View buildSearch() {
        searchInput = new EditText(activity);
        searchInput.setSingleLine(true);
        searchInput.setHint("タイトル・メモを検索");
        searchInput.setHintTextColor(Color.rgb(145, 145, 137));
        searchInput.setTextColor(PintoViewFactory.INK);
        searchInput.setTextSize(14);
        searchInput.setPadding(ui.dp(16), 0, ui.dp(16), 0);
        searchInput.setMinHeight(ui.dp(48));
        searchInput.setBackground(ui.strokedRoundRect(
                PintoViewFactory.PAPER, PintoViewFactory.LINE, ui.dp(15), 1));
        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            /* 入力が変わるたび、現在のタスクから該当行だけを再描画する。 */
            @Override
            public void afterTextChanged(Editable editable) {
                renderTaskList();
            }
        });
        return searchInput;
    }

    /* タスク一覧の見出しと件数表示を作る。 */
    private View buildListHeader() {
        LinearLayout row = ui.horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        listSectionTitle = ui.text(
                "今日のタスク", 16, PintoViewFactory.INK, Typeface.BOLD);
        listSectionCount = ui.text("0件", 12, PintoViewFactory.MUTED, Typeface.BOLD);
        listSectionCount.setGravity(Gravity.CENTER);
        listSectionCount.setPadding(ui.dp(10), ui.dp(4), ui.dp(10), ui.dp(4));
        listSectionCount.setBackground(
                ui.roundRect(Color.rgb(237, 236, 232), ui.dp(12)));
        row.addView(listSectionTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(listSectionCount);
        return row;
    }

    /* フィルター1個分のボタンを追加し、選択時に一覧を更新する。 */
    private void addFilterChip(
            LinearLayout row,
            TaskSorter.Filter filter,
            String label
    ) {
        Button button = new Button(activity);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(ui.dp(18), 0, ui.dp(18), 0);
        button.setOnClickListener(view -> {
            currentFilter = filter;
            updateFilterButtons();
            renderTaskList();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ui.dp(42));
        params.setMarginEnd(ui.dp(8));
        row.addView(button, params);
        filterButtons.put(filter, button);
    }

    /* 選択中のフィルターだけを濃い緑色にして、現在位置を見分けやすくする。 */
    private void updateFilterButtons() {
        for (Map.Entry<TaskSorter.Filter, Button> entry : filterButtons.entrySet()) {
            boolean selected = entry.getKey() == currentFilter;
            Button button = entry.getValue();
            button.setTextColor(selected ? Color.WHITE : PintoViewFactory.MUTED);
            button.setTypeface(
                    Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            button.setBackground(selected
                    ? ui.roundRect(PintoViewFactory.GREEN_DARK, ui.dp(14))
                    : ui.strokedRoundRect(
                            PintoViewFactory.PAPER,
                            PintoViewFactory.LINE,
                            ui.dp(14),
                            1));
        }
    }

    /* 全タスクから今日の残件数と完了率を計算し、集計カードへ反映する。 */
    private void updateSummary() {
        long today = TimeUtils.startOfToday();
        long tomorrow = TimeUtils.startOfTomorrow();
        int remaining = 0;
        int completedToday = 0;
        for (Task task : tasks) {
            if (!task.completed && task.dueAt != null && task.dueAt < tomorrow) remaining++;
            if (task.completedAt != null
                    && task.completedAt >= today
                    && task.completedAt < tomorrow) {
                completedToday++;
            }
        }

        int total = remaining + completedToday;
        int percent = total == 0 ? 0 : Math.round(completedToday * 100f / total);
        summaryTitle.setText(remaining == 0
                ? "今日の予定は片づきました"
                : activity.getString(R.string.today_remaining, remaining));
        if (total == 0) {
            summarySubtitle.setText("右下から予定を追加しましょう");
        } else if (completedToday == 0) {
            summarySubtitle.setText("まず1つ、終わらせてみましょう");
        } else {
            summarySubtitle.setText(
                    activity.getString(R.string.summary_completed, completedToday, percent));
        }
        summaryProgress.setProgress(percent, true);
    }

    /* 検索語とフィルターに一致するタスクだけを並べ直して表示する。 */
    private void renderTaskList() {
        if (listContainer == null) return;
        String query = searchInput == null ? "" : searchInput.getText().toString();
        List<Task> visible = TaskSorter.filterAndSort(
                tasks,
                currentFilter,
                query,
                TimeUtils.startOfToday(),
                TimeUtils.startOfTomorrow());

        listSectionTitle.setText(filterSectionTitle());
        listSectionCount.setText(
                activity.getString(R.string.task_count, visible.size()));

        listContainer.removeAllViews();
        if (visible.isEmpty()) {
            listContainer.addView(buildEmptyState());
            return;
        }
        for (Task task : visible) {
            listContainer.addView(buildTaskRow(task), ui.marginTop(ui.dp(9)));
        }
    }

    /* 選択中のフィルターに対応する一覧見出しを返す。 */
    private String filterSectionTitle() {
        switch (currentFilter) {
            case UPCOMING:
                return "これからのタスク";
            case ALL:
                return "未完了のタスク";
            case COMPLETED:
                return "完了したタスク";
            case TODAY:
            default:
                return "今日のタスク";
        }
    }

    /* 該当タスクがない場合の案内と、次に行いやすい操作ボタンを作る。 */
    private View buildEmptyState() {
        LinearLayout empty = ui.vertical();
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(ui.dp(20), ui.dp(44), ui.dp(20), ui.dp(44));

        TextView icon = ui.text("✓", 28, PintoViewFactory.GREEN, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(ui.roundRect(PintoViewFactory.GREEN_SOFT, ui.dp(24)));
        empty.addView(icon, new LinearLayout.LayoutParams(ui.dp(58), ui.dp(58)));

        String message;
        switch (currentFilter) {
            case TODAY:
                message = "今日のタスクはありません";
                break;
            case COMPLETED:
                message = "完了したタスクはまだありません";
                break;
            default:
                message = "該当するタスクはありません";
                break;
        }
        TextView title = ui.text(message, 16, PintoViewFactory.INK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        empty.addView(title, ui.marginTop(ui.dp(14)));

        boolean hasQuery = searchInput != null
                && !searchInput.getText().toString().trim().isEmpty();
        TextView subtitle = ui.text(
                hasQuery
                        ? "検索条件を変えると見つかるかもしれません"
                        : "必要な操作を1つだけ選びましょう",
                13,
                PintoViewFactory.MUTED,
                Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER);
        empty.addView(subtitle, ui.marginTop(ui.dp(6)));

        Button action = ui.choiceButton(hasQuery
                ? "検索をクリア"
                : (currentFilter == TaskSorter.Filter.COMPLETED
                        ? "今日の予定を見る"
                        : "予定を追加"));
        action.setTextColor(PintoViewFactory.GREEN_DARK);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        action.setOnClickListener(view -> {
            if (hasQuery) {
                searchInput.setText("");
            } else if (currentFilter == TaskSorter.Filter.COMPLETED) {
                currentFilter = TaskSorter.Filter.TODAY;
                updateFilterButtons();
                renderTaskList();
            } else {
                listener.onAddTaskRequested();
            }
        });
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                ui.dp(150), ui.dp(46));
        actionParams.topMargin = ui.dp(16);
        empty.addView(action, actionParams);
        return empty;
    }

    /* 1件分の完了チェック・内容・優先度・メニューを含むカードを作る。 */
    private View buildTaskRow(Task task) {
        LinearLayout card = ui.horizontal();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(ui.dp(13), ui.dp(13), ui.dp(8), ui.dp(13));
        card.setBackground(ui.strokedRoundRect(
                PintoViewFactory.PAPER,
                TimeUtils.isOverdue(task) ? PintoViewFactory.RED_SOFT : PintoViewFactory.LINE,
                ui.dp(18),
                1));
        card.setElevation(ui.dp(1));
        card.setOnClickListener(view -> listener.onEditTaskRequested(task));

        CheckBox check = new CheckBox(activity);
        check.setChecked(task.completed);
        check.setButtonTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{PintoViewFactory.GREEN, Color.rgb(170, 172, 165)}));
        check.setContentDescription(task.completed ? "未完了に戻す" : "完了にする");
        check.setOnCheckedChangeListener(
                (button, checked) -> listener.onTaskCompletionChanged(task, checked));
        card.addView(check, new LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)));

        LinearLayout content = ui.vertical();
        TextView title = ui.text(
                task.title,
                16,
                task.completed ? PintoViewFactory.MUTED : PintoViewFactory.INK,
                Typeface.BOLD);
        title.setMaxLines(2);
        content.addView(title);

        LinearLayout meta = ui.horizontal();
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.addView(priorityLabel(task.priority));
        if (task.dueAt != null) {
            String dueLabel = (TimeUtils.isOverdue(task) ? "期限切れ · " : "")
                    + TimeUtils.relativeDue(task.dueAt);
            TextView due = ui.text(
                    dueLabel,
                    12,
                    TimeUtils.isOverdue(task)
                            ? PintoViewFactory.RED
                            : PintoViewFactory.MUTED,
                    Typeface.BOLD);
            meta.addView(due, ui.marginStart(ui.dp(8)));
        } else {
            TextView noDue = ui.text(
                    "期限なし", 12, PintoViewFactory.MUTED, Typeface.NORMAL);
            meta.addView(noDue, ui.marginStart(ui.dp(8)));
        }
        content.addView(meta, ui.marginTop(ui.dp(7)));

        if (task.repeatType == Task.REPEAT_DAILY && task.repeatEnd != null) {
            TextView repeat = ui.text(
                    "↻ 毎日 · " + TimeUtils.dateOnly(task.repeatEnd) + "まで",
                    12,
                    PintoViewFactory.GREEN,
                    Typeface.BOLD);
            content.addView(repeat, ui.marginTop(ui.dp(5)));
        }
        if (!task.note.isEmpty()) {
            TextView note = ui.text(
                    task.note, 12, PintoViewFactory.MUTED, Typeface.NORMAL);
            note.setMaxLines(1);
            content.addView(note, ui.marginTop(ui.dp(5)));
        }
        card.addView(content, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button menu = ui.compactButton("⋮");
        menu.setTextSize(22);
        menu.setPadding(0, 0, 0, 0);
        menu.setContentDescription("タスクのメニュー");
        menu.setOnClickListener(view -> showTaskMenu(view, task));
        card.addView(menu, new LinearLayout.LayoutParams(ui.dp(44), ui.dp(48)));
        return card;
    }

    /* 数値の優先度を「高・中・低」の色付きラベルへ変換する。 */
    private TextView priorityLabel(int priority) {
        String label;
        int foreground;
        int background;
        if (priority == Task.PRIORITY_HIGH) {
            label = "高";
            foreground = PintoViewFactory.RED;
            background = PintoViewFactory.RED_SOFT;
        } else if (priority == Task.PRIORITY_LOW) {
            label = "低";
            foreground = PintoViewFactory.MUTED;
            background = Color.rgb(237, 236, 232);
        } else {
            label = "中";
            foreground = PintoViewFactory.GREEN;
            background = PintoViewFactory.GREEN_SOFT;
        }

        TextView view = ui.text(label, 11, foreground, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(ui.dp(8), ui.dp(3), ui.dp(8), ui.dp(3));
        view.setBackground(ui.roundRect(background, ui.dp(10)));
        return view;
    }

    /* タスク行の三点ボタンから、編集・優先度変更・削除メニューを表示する。 */
    private void showTaskMenu(View anchor, Task task) {
        PopupMenu popup = new PopupMenu(activity, anchor);
        popup.getMenu().add("編集");
        popup.getMenu().add("優先度：高");
        popup.getMenu().add("優先度：中");
        popup.getMenu().add("優先度：低");
        popup.getMenu().add("削除");
        popup.setOnMenuItemClickListener(item -> {
            String label = item.getTitle().toString();
            if (label.equals("編集")) {
                listener.onEditTaskRequested(task);
            } else if (label.equals("削除")) {
                listener.onDeleteTaskRequested(task);
            } else if (label.endsWith("高")) {
                listener.onTaskPriorityChanged(task, Task.PRIORITY_HIGH);
            } else if (label.endsWith("低")) {
                listener.onTaskPriorityChanged(task, Task.PRIORITY_LOW);
            } else {
                listener.onTaskPriorityChanged(task, Task.PRIORITY_MEDIUM);
            }
            return true;
        });
        popup.show();
    }

    /*
      TextWatcherのうち今回使わない前後イベントを空実装にし、入力後だけを書けるようにする。
    */
    private abstract static class SimpleTextWatcher implements TextWatcher {
        /* 入力変更前の処理は不要。 */
        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

        /* 入力途中の処理は不要。 */
        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {}
    }
}
