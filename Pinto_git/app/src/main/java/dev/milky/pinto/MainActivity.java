package dev.milky.pinto;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MainActivity extends android.app.Activity {
    private static final int CREAM = Color.rgb(247, 244, 238);
    private static final int PAPER = Color.rgb(255, 253, 248);
    private static final int INK = Color.rgb(32, 35, 31);
    private static final int MUTED = Color.rgb(111, 113, 106);
    private static final int GREEN = Color.rgb(66, 107, 90);
    private static final int GREEN_DARK = Color.rgb(36, 76, 62);
    private static final int GREEN_SOFT = Color.rgb(226, 237, 230);
    private static final int RED = Color.rgb(181, 73, 65);
    private static final int RED_SOFT = Color.rgb(249, 231, 228);
    private static final int LINE = Color.rgb(226, 223, 214);

    private TaskStore store;
    private LinearLayout listContainer;
    private EditText searchInput;
    private TextView summaryTitle;
    private TextView summarySubtitle;
    private ProgressBar summaryProgress;
    private TextView listSectionTitle;
    private TextView listSectionCount;
    private TaskSorter.Filter currentFilter = TaskSorter.Filter.TODAY;
    private final Map<TaskSorter.Filter, Button> filterButtons = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new TaskStore(this);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        ReminderReceiver.ensureChannel(this, notificationManager);

        FrameLayout root = buildScreen();
        setContentView(root);
        applySystemBars(root);
        requestNotificationPermissionIfNeeded();
        refresh();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (listContainer != null) refresh();
    }

    @Override
    protected void onDestroy() {
        store.close();
        super.onDestroy();
    }

    private FrameLayout buildScreen() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(CREAM);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(scroll, scrollParams);

        LinearLayout page = vertical();
        page.setPadding(dp(20), dp(20), dp(20), dp(112));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        page.addView(buildHeader());
        page.addView(buildSummaryCard(), marginTop(dp(22)));
        page.addView(buildFilters(), marginTop(dp(18)));
        page.addView(buildSearch(), marginTop(dp(10)));
        page.addView(buildListHeader(), marginTop(dp(20)));

        listContainer = vertical();
        page.addView(listContainer, marginTop(dp(3)));

        Button add = new Button(this);
        add.setText("＋ 新しい予定");
        add.setTextColor(Color.WHITE);
        add.setTextSize(15);
        add.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        add.setAllCaps(false);
        add.setGravity(Gravity.CENTER);
        add.setPadding(dp(18), 0, dp(18), 0);
        add.setBackground(roundRect(GREEN_DARK, dp(20)));
        add.setElevation(dp(8));
        add.setOnClickListener(view -> showTaskDialog(null));
        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(dp(142), dp(54));
        addParams.gravity = Gravity.BOTTOM | Gravity.END;
        addParams.setMargins(dp(20), dp(20), dp(20), dp(24));
        root.addView(add, addParams);
        return root;
    }

    private View buildHeader() {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView mark = text("✓", 19, Color.WHITE, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(roundRect(GREEN_DARK, dp(12)));
        row.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout words = vertical();
        TextView title = text("Pinto", 25, INK, Typeface.BOLD);
        TextView date = text(TimeUtils.headerDate(), 13, MUTED, Typeface.NORMAL);
        words.addView(title);
        words.addView(date, marginTop(dp(1)));
        LinearLayout.LayoutParams wordParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wordParams.setMarginStart(dp(12));
        row.addView(words, wordParams);

        TextView hint = text("忘れる前に、ピン✓", 12, GREEN, Typeface.BOLD);
        row.addView(hint);
        return row;
    }

    private View buildSummaryCard() {
        LinearLayout card = vertical();
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        card.setBackground(roundRect(GREEN_DARK, dp(22)));
        card.setElevation(dp(3));

        summaryTitle = text("今日の予定", 20, Color.WHITE, Typeface.BOLD);
        summarySubtitle = text("読み込み中…", 13, Color.rgb(220, 235, 227), Typeface.NORMAL);
        summaryProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        summaryProgress.setMax(100);
        summaryProgress.setProgressTintList(ColorStateList.valueOf(Color.rgb(153, 204, 177)));
        summaryProgress.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(75, 118, 100)));
        summaryProgress.setIndeterminate(false);

        card.addView(summaryTitle);
        card.addView(summarySubtitle, marginTop(dp(4)));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(7));
        progressParams.topMargin = dp(14);
        card.addView(summaryProgress, progressParams);
        return card;
    }

    private View buildFilters() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(row);
        addFilterChip(row, TaskSorter.Filter.TODAY, "今日");
        addFilterChip(row, TaskSorter.Filter.UPCOMING, "これから");
        addFilterChip(row, TaskSorter.Filter.ALL, "すべて");
        addFilterChip(row, TaskSorter.Filter.COMPLETED, "完了");
        updateFilterButtons();
        return scroll;
    }

    private View buildSearch() {
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("タイトル・メモを検索");
        searchInput.setHintTextColor(Color.rgb(145, 145, 137));
        searchInput.setTextColor(INK);
        searchInput.setTextSize(14);
        searchInput.setPadding(dp(16), 0, dp(16), 0);
        searchInput.setMinHeight(dp(48));
        searchInput.setBackground(strokedRoundRect(PAPER, LINE, dp(15), 1));
        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable editable) { refresh(); }
        });
        return searchInput;
    }

    private View buildListHeader() {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        listSectionTitle = text("今日のタスク", 16, INK, Typeface.BOLD);
        listSectionCount = text("0件", 12, MUTED, Typeface.BOLD);
        listSectionCount.setGravity(Gravity.CENTER);
        listSectionCount.setPadding(dp(10), dp(4), dp(10), dp(4));
        listSectionCount.setBackground(roundRect(Color.rgb(237, 236, 232), dp(12)));
        row.addView(listSectionTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(listSectionCount);
        return row;
    }

    private void addFilterChip(LinearLayout row, TaskSorter.Filter filter, String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(18), 0, dp(18), 0);
        button.setOnClickListener(view -> {
            currentFilter = filter;
            updateFilterButtons();
            refresh();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        params.setMarginEnd(dp(8));
        row.addView(button, params);
        filterButtons.put(filter, button);
    }

    private void updateFilterButtons() {
        for (Map.Entry<TaskSorter.Filter, Button> entry : filterButtons.entrySet()) {
            boolean selected = entry.getKey() == currentFilter;
            Button button = entry.getValue();
            button.setTextColor(selected ? Color.WHITE : MUTED);
            button.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            button.setBackground(selected
                    ? roundRect(GREEN_DARK, dp(14))
                    : strokedRoundRect(PAPER, LINE, dp(14), 1));
        }
    }

    private void refresh() {
        if (listContainer == null) return;
        List<Task> all = store.getAll();
        updateSummary(all);
        String query = searchInput == null ? "" : searchInput.getText().toString();
        List<Task> visible = TaskSorter.filterAndSort(
                all, currentFilter, query, TimeUtils.startOfToday(), TimeUtils.startOfTomorrow());

        if (listSectionTitle != null) listSectionTitle.setText(filterSectionTitle());
        if (listSectionCount != null) listSectionCount.setText(getString(R.string.task_count, visible.size()));

        listContainer.removeAllViews();
        if (visible.isEmpty()) {
            listContainer.addView(buildEmptyState());
            return;
        }
        for (Task task : visible) {
            listContainer.addView(buildTaskRow(task), marginTop(dp(9)));
        }
    }

    private void updateSummary(List<Task> tasks) {
        long today = TimeUtils.startOfToday();
        long tomorrow = TimeUtils.startOfTomorrow();
        int remaining = 0;
        int completedToday = 0;
        for (Task task : tasks) {
            if (!task.completed && task.dueAt != null && task.dueAt < tomorrow) remaining++;
            if (task.completedAt != null && task.completedAt >= today && task.completedAt < tomorrow) {
                completedToday++;
            }
        }
        int total = remaining + completedToday;
        int percent = total == 0 ? 0 : Math.round(completedToday * 100f / total);
        summaryTitle.setText(remaining == 0
                ? "今日の予定は片づきました"
                : getString(R.string.today_remaining, remaining));
        if (total == 0) summarySubtitle.setText("右下から予定を追加しましょう");
        else if (completedToday == 0) summarySubtitle.setText("まず1つ、終わらせてみましょう");
        else summarySubtitle.setText(getString(R.string.summary_completed, completedToday, percent));
        summaryProgress.setProgress(percent, true);
    }

    private String filterSectionTitle() {
        switch (currentFilter) {
            case UPCOMING: return "これからのタスク";
            case ALL: return "未完了のタスク";
            case COMPLETED: return "完了したタスク";
            case TODAY:
            default: return "今日のタスク";
        }
    }

    private View buildEmptyState() {
        LinearLayout empty = vertical();
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(20), dp(44), dp(20), dp(44));
        TextView icon = text("✓", 28, GREEN, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(roundRect(GREEN_SOFT, dp(24)));
        empty.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));
        String message;
        switch (currentFilter) {
            case TODAY: message = "今日のタスクはありません"; break;
            case COMPLETED: message = "完了したタスクはまだありません"; break;
            default: message = "該当するタスクはありません"; break;
        }
        TextView title = text(message, 16, INK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        empty.addView(title, marginTop(dp(14)));
        boolean hasQuery = searchInput != null && !searchInput.getText().toString().trim().isEmpty();
        TextView subtitle = text(hasQuery
                ? "検索条件を変えると見つかるかもしれません"
                : "必要な操作を1つだけ選びましょう", 13, MUTED, Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER);
        empty.addView(subtitle, marginTop(dp(6)));

        Button action = choiceButton(hasQuery
                ? "検索をクリア"
                : (currentFilter == TaskSorter.Filter.COMPLETED ? "今日の予定を見る" : "予定を追加"));
        action.setTextColor(GREEN_DARK);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        action.setOnClickListener(view -> {
            if (hasQuery) searchInput.setText("");
            else if (currentFilter == TaskSorter.Filter.COMPLETED) {
                currentFilter = TaskSorter.Filter.TODAY;
                updateFilterButtons();
                refresh();
            } else showTaskDialog(null);
        });
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(dp(150), dp(46));
        actionParams.topMargin = dp(16);
        empty.addView(action, actionParams);
        return empty;
    }

    private View buildTaskRow(Task task) {
        LinearLayout card = horizontal();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(13), dp(8), dp(13));
        card.setBackground(strokedRoundRect(PAPER, TimeUtils.isOverdue(task) ? RED_SOFT : LINE, dp(18), 1));
        card.setElevation(dp(1));
        card.setOnClickListener(view -> showTaskDialog(task));

        CheckBox check = new CheckBox(this);
        check.setChecked(task.completed);
        check.setButtonTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{GREEN, Color.rgb(170, 172, 165)}));
        check.setContentDescription(task.completed ? "未完了に戻す" : "完了にする");
        check.setOnCheckedChangeListener((button, checked) -> {
            Task next = store.setCompleted(task.id, checked);
            if (checked) {
                AlarmScheduler.cancel(this, task.id);
                if (next != null) {
                    AlarmScheduler.schedule(this, next);
                    Toast.makeText(this, "翌日分を追加しました", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Task updated = store.get(task.id);
                if (updated != null) AlarmScheduler.schedule(this, updated);
            }
            refresh();
        });
        card.addView(check, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout content = vertical();
        TextView title = text(task.title, 16, task.completed ? MUTED : INK, Typeface.BOLD);
        title.setMaxLines(2);
        content.addView(title);

        LinearLayout meta = horizontal();
        meta.setGravity(Gravity.CENTER_VERTICAL);
        TextView priority = priorityLabel(task.priority);
        meta.addView(priority);
        if (task.dueAt != null) {
            String dueLabel = (TimeUtils.isOverdue(task) ? "期限切れ · " : "") + TimeUtils.relativeDue(task.dueAt);
            TextView due = text(dueLabel, 12, TimeUtils.isOverdue(task) ? RED : MUTED, Typeface.BOLD);
            LinearLayout.LayoutParams dueParams = marginStart(dp(8));
            meta.addView(due, dueParams);
        } else {
            TextView noDue = text("期限なし", 12, MUTED, Typeface.NORMAL);
            meta.addView(noDue, marginStart(dp(8)));
        }
        content.addView(meta, marginTop(dp(7)));
        if (task.repeatType == Task.REPEAT_DAILY && task.repeatEnd != null) {
            TextView repeat = text("↻ 毎日 · " + TimeUtils.dateOnly(task.repeatEnd) + "まで",
                    12, GREEN, Typeface.BOLD);
            content.addView(repeat, marginTop(dp(5)));
        }
        if (!task.note.isEmpty()) {
            TextView note = text(task.note, 12, MUTED, Typeface.NORMAL);
            note.setMaxLines(1);
            content.addView(note, marginTop(dp(5)));
        }
        card.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button menu = new Button(this);
        menu.setText("⋮");
        menu.setTextSize(22);
        menu.setTextColor(MUTED);
        menu.setAllCaps(false);
        menu.setMinWidth(0);
        menu.setMinHeight(0);
        menu.setPadding(0, 0, 0, 0);
        menu.setBackgroundColor(Color.TRANSPARENT);
        menu.setContentDescription("タスクのメニュー");
        menu.setOnClickListener(view -> showTaskMenu(view, task));
        card.addView(menu, new LinearLayout.LayoutParams(dp(44), dp(48)));
        return card;
    }

    private TextView priorityLabel(int priority) {
        String label;
        int foreground;
        int background;
        if (priority == Task.PRIORITY_HIGH) {
            label = "高"; foreground = RED; background = RED_SOFT;
        } else if (priority == Task.PRIORITY_LOW) {
            label = "低"; foreground = MUTED; background = Color.rgb(237, 236, 232);
        } else {
            label = "中"; foreground = GREEN; background = GREEN_SOFT;
        }
        TextView view = text(label, 11, foreground, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(3), dp(8), dp(3));
        view.setBackground(roundRect(background, dp(10)));
        return view;
    }

    private void showTaskMenu(View anchor, Task task) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("編集");
        popup.getMenu().add("優先度：高");
        popup.getMenu().add("優先度：中");
        popup.getMenu().add("優先度：低");
        popup.getMenu().add("削除");
        popup.setOnMenuItemClickListener(item -> {
            String label = item.getTitle().toString();
            if (label.equals("編集")) showTaskDialog(task);
            else if (label.equals("削除")) confirmDelete(task);
            else {
                if (label.endsWith("高")) task.priority = Task.PRIORITY_HIGH;
                else if (label.endsWith("低")) task.priority = Task.PRIORITY_LOW;
                else task.priority = Task.PRIORITY_MEDIUM;
                store.update(task);
                refresh();
            }
            return true;
        });
        popup.show();
    }

    private void showTaskDialog(Task existing) {
        Task draft = existing == null ? new Task() : existing.copy();
        if (existing == null) draft.dueAt = TimeUtils.defaultTaskDue();

        Dialog dialog = new Dialog(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout panel = vertical();
        panel.setPadding(dp(22), dp(18), dp(22), dp(26));
        panel.setBackground(roundRect(PAPER, dp(24)));
        scroll.addView(panel);

        LinearLayout heading = horizontal();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView dialogTitle = text(existing == null ? "新しい予定" : "予定を編集", 21, INK, Typeface.BOLD);
        heading.addView(dialogTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button close = compactButton("×");
        close.setTextSize(22);
        close.setOnClickListener(view -> dialog.dismiss());
        heading.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        panel.addView(heading);

        TextView titleLabel = fieldLabel("やること");
        panel.addView(titleLabel, marginTop(dp(16)));
        EditText titleInput = fieldInput("例：歯医者を予約する", true);
        titleInput.setText(draft.title);
        panel.addView(titleInput, marginTop(dp(7)));

        panel.addView(fieldLabel("メモ"), marginTop(dp(16)));
        EditText noteInput = fieldInput("必要な情報をメモ", false);
        noteInput.setText(draft.note);
        noteInput.setMinHeight(dp(76));
        noteInput.setGravity(Gravity.TOP | Gravity.START);
        panel.addView(noteInput, marginTop(dp(7)));

        panel.addView(fieldLabel("優先度"), marginTop(dp(16)));
        LinearLayout priorityRow = horizontal();
        int[] selectedPriority = new int[]{draft.priority};
        Button low = choiceButton("低");
        Button medium = choiceButton("中");
        Button high = choiceButton("高");
        List<Button> priorityButtons = Arrays.asList(low, medium, high);
        View.OnClickListener choosePriority = view -> {
            if (view == low) selectedPriority[0] = Task.PRIORITY_LOW;
            else if (view == high) selectedPriority[0] = Task.PRIORITY_HIGH;
            else selectedPriority[0] = Task.PRIORITY_MEDIUM;
            stylePriorityChoices(priorityButtons, selectedPriority[0]);
        };
        for (Button button : priorityButtons) {
            button.setOnClickListener(choosePriority);
            priorityRow.addView(button, weightedChoiceParams());
        }
        stylePriorityChoices(priorityButtons, selectedPriority[0]);
        panel.addView(priorityRow, marginTop(dp(7)));

        panel.addView(fieldLabel("予定日時（繰り返しの開始）"), marginTop(dp(16)));
        Long[] selectedDue = new Long[]{draft.dueAt};
        Button dueButton = choiceButton(dueButtonText(selectedDue[0]));
        dueButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        dueButton.setPadding(dp(16), 0, dp(16), 0);
        dueButton.setOnClickListener(view -> showDateTimePicker(selectedDue, dueButton));
        panel.addView(dueButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

        LinearLayout dueShortcuts = horizontal();
        dueShortcuts.setGravity(Gravity.CENTER_VERTICAL);
        Button today = miniChoice("今日");
        Button tomorrow = miniChoice("明日");
        Button weekend = miniChoice("週末");
        Button none = miniChoice("なし");
        today.setOnClickListener(view -> setDueShortcut(selectedDue, dueButton, LocalDate.now()));
        tomorrow.setOnClickListener(view -> setDueShortcut(selectedDue, dueButton, LocalDate.now().plusDays(1)));
        weekend.setOnClickListener(view -> {
            LocalDate date = LocalDate.now();
            int days = (6 - date.getDayOfWeek().getValue() + 7) % 7;
            if (days == 0) days = 7;
            setDueShortcut(selectedDue, dueButton, date.plusDays(days));
        });
        none.setOnClickListener(view -> {
            selectedDue[0] = null;
            dueButton.setText(dueButtonText(null));
        });
        for (Button button : Arrays.asList(today, tomorrow, weekend, none)) {
            dueShortcuts.addView(button, weightedChoiceParams());
        }
        panel.addView(dueShortcuts, marginTop(dp(8)));

        panel.addView(fieldLabel("繰り返し"), marginTop(dp(16)));
        int[] selectedRepeat = new int[]{draft.repeatType};
        Long[] selectedRepeatEnd = new Long[]{draft.repeatEnd};
        LinearLayout repeatRow = horizontal();
        Button noRepeat = choiceButton("繰り返さない");
        Button daily = choiceButton("毎日");
        List<Button> repeatButtons = Arrays.asList(noRepeat, daily);
        repeatRow.addView(noRepeat, weightedChoiceParams());
        repeatRow.addView(daily, weightedChoiceParams());
        panel.addView(repeatRow, marginTop(dp(7)));

        LinearLayout repeatDetails = vertical();
        repeatDetails.addView(fieldLabel("終了日（この日を含む）"));
        Button repeatEndButton = choiceButton(repeatEndButtonText(selectedRepeatEnd[0]));
        repeatEndButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        repeatEndButton.setPadding(dp(16), 0, dp(16), 0);
        repeatDetails.addView(repeatEndButton, marginTop(dp(7)));
        TextView repeatHint = text("完了すると翌日分を自動で追加します", 12, MUTED, Typeface.NORMAL);
        repeatDetails.addView(repeatHint, marginTop(dp(6)));
        panel.addView(repeatDetails, marginTop(dp(12)));

        View.OnClickListener chooseRepeat = view -> {
            selectedRepeat[0] = view == daily ? Task.REPEAT_DAILY : Task.REPEAT_NONE;
            if (selectedRepeat[0] == Task.REPEAT_DAILY) {
                if (selectedDue[0] == null) {
                    selectedDue[0] = TimeUtils.defaultTaskDue();
                    dueButton.setText(dueButtonText(selectedDue[0]));
                }
                if (selectedRepeatEnd[0] == null || selectedRepeatEnd[0] < selectedDue[0]) {
                    LocalDate start = TimeUtils.toLocalDateTime(selectedDue[0]).toLocalDate();
                    selectedRepeatEnd[0] = TimeUtils.endOfDay(start.plusMonths(1));
                    repeatEndButton.setText(repeatEndButtonText(selectedRepeatEnd[0]));
                }
            }
            styleRepeatChoices(repeatButtons, selectedRepeat[0]);
            repeatDetails.setVisibility(selectedRepeat[0] == Task.REPEAT_DAILY ? View.VISIBLE : View.GONE);
        };
        noRepeat.setOnClickListener(chooseRepeat);
        daily.setOnClickListener(chooseRepeat);
        styleRepeatChoices(repeatButtons, selectedRepeat[0]);
        repeatDetails.setVisibility(selectedRepeat[0] == Task.REPEAT_DAILY ? View.VISIBLE : View.GONE);
        repeatEndButton.setOnClickListener(view -> showRepeatEndPicker(selectedDue, selectedRepeatEnd, repeatEndButton));

        panel.addView(fieldLabel("通知"), marginTop(dp(16)));
        String[] reminderLabels = {"期限ちょうど", "10分前", "1時間前", "1日前"};
        int[] reminderValues = {0, 10, 60, 1440};
        Spinner reminder = new Spinner(this);
        ArrayAdapter<String> reminderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, reminderLabels);
        reminder.setAdapter(reminderAdapter);
        int selectedReminder = 1;
        for (int i = 0; i < reminderValues.length; i++) {
            if (reminderValues[i] == draft.reminderMinutes) selectedReminder = i;
        }
        reminder.setSelection(selectedReminder);
        reminder.setBackground(strokedRoundRect(Color.TRANSPARENT, LINE, dp(14), 1));
        reminder.setPadding(dp(12), 0, dp(12), 0);
        panel.addView(reminder, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

        LinearLayout actions = horizontal();
        actions.setGravity(Gravity.CENTER_VERTICAL);
        if (existing != null) {
            Button delete = compactButton("削除");
            delete.setTextColor(RED);
            delete.setOnClickListener(view -> {
                dialog.dismiss();
                confirmDelete(existing);
            });
            actions.addView(delete, new LinearLayout.LayoutParams(dp(76), dp(52)));
        } else {
            View spacer = new View(this);
            actions.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        }
        Button save = new Button(this);
        save.setText(existing == null ? "追加する" : "保存する");
        save.setTextColor(Color.WHITE);
        save.setTextSize(15);
        save.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        save.setAllCaps(false);
        save.setBackground(roundRect(GREEN_DARK, dp(16)));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(dp(132), dp(52));
        saveParams.setMarginStart(dp(10));
        actions.addView(save, saveParams);
        panel.addView(actions, marginTop(dp(22)));

        save.setOnClickListener(view -> {
            String value = titleInput.getText().toString().trim();
            if (value.isEmpty()) {
                titleInput.setError("やることを入力してください");
                titleInput.requestFocus();
                return;
            }
            draft.title = value;
            draft.note = noteInput.getText().toString().trim();
            draft.priority = selectedPriority[0];
            draft.dueAt = selectedDue[0];
            draft.repeatType = selectedRepeat[0];
            draft.repeatEnd = selectedRepeat[0] == Task.REPEAT_DAILY ? selectedRepeatEnd[0] : null;
            if (draft.repeatType == Task.REPEAT_DAILY && draft.dueAt == null) {
                Toast.makeText(this, "繰り返しの開始日時を設定してください", Toast.LENGTH_SHORT).show();
                return;
            }
            if (draft.repeatType == Task.REPEAT_DAILY
                    && (draft.repeatEnd == null || draft.repeatEnd < draft.dueAt)) {
                Toast.makeText(this, "終了日は開始日以降にしてください", Toast.LENGTH_SHORT).show();
                return;
            }
            draft.reminderMinutes = reminderValues[reminder.getSelectedItemPosition()];
            if (existing == null) store.insert(draft); else store.update(draft);
            AlarmScheduler.schedule(this, draft);
            dialog.dismiss();
            refresh();
        });

        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(ignored -> {
            Window shown = dialog.getWindow();
            if (shown != null) {
                shown.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                shown.setGravity(Gravity.BOTTOM);
                WindowManager.LayoutParams attributes = shown.getAttributes();
                attributes.dimAmount = 0.45f;
                shown.setAttributes(attributes);
                shown.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
        });
        dialog.show();
        titleInput.requestFocus();
    }

    private void stylePriorityChoices(List<Button> buttons, int selectedPriority) {
        for (int index = 0; index < buttons.size(); index++) {
            Button button = buttons.get(index);
            boolean selected = index == selectedPriority;
            int color = index == Task.PRIORITY_HIGH ? RED : (index == Task.PRIORITY_LOW ? MUTED : GREEN);
            int soft = index == Task.PRIORITY_HIGH ? RED_SOFT
                    : (index == Task.PRIORITY_LOW ? Color.rgb(237, 236, 232) : GREEN_SOFT);
            button.setTextColor(selected ? color : MUTED);
            button.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            button.setBackground(strokedRoundRect(selected ? soft : Color.TRANSPARENT,
                    selected ? color : LINE, dp(14), 1));
        }
    }

    private void styleRepeatChoices(List<Button> buttons, int selectedRepeat) {
        for (int index = 0; index < buttons.size(); index++) {
            Button button = buttons.get(index);
            boolean selected = index == selectedRepeat;
            button.setTextColor(selected ? GREEN_DARK : MUTED);
            button.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            button.setBackground(strokedRoundRect(selected ? GREEN_SOFT : Color.TRANSPARENT,
                    selected ? GREEN : LINE, dp(14), 1));
        }
    }

    private void showDateTimePicker(Long[] selectedDue, Button dueButton) {
        LocalDateTime initial = selectedDue[0] == null
                ? TimeUtils.toLocalDateTime(TimeUtils.defaultTaskDue())
                : TimeUtils.toLocalDateTime(selectedDue[0]);
        DatePickerDialog datePicker = new DatePickerDialog(this, (picker, year, month, day) -> {
            LocalDate selectedDate = LocalDate.of(year, month + 1, day);
            TimePickerDialog timePicker = new TimePickerDialog(this, (time, hour, minute) -> {
                selectedDue[0] = TimeUtils.atDateAndTime(selectedDate, hour, minute);
                dueButton.setText(dueButtonText(selectedDue[0]));
            }, initial.getHour(), initial.getMinute(), true);
            timePicker.show();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        datePicker.show();
    }

    private void setDueShortcut(Long[] selectedDue, Button button, LocalDate date) {
        selectedDue[0] = TimeUtils.atDateAndTime(date, 20, 0);
        button.setText(dueButtonText(selectedDue[0]));
    }

    private void showRepeatEndPicker(Long[] selectedDue, Long[] selectedEnd, Button endButton) {
        LocalDate initial;
        if (selectedEnd[0] != null) {
            initial = TimeUtils.toLocalDateTime(selectedEnd[0]).toLocalDate();
        } else if (selectedDue[0] != null) {
            initial = TimeUtils.toLocalDateTime(selectedDue[0]).toLocalDate().plusMonths(1);
        } else {
            initial = LocalDate.now().plusMonths(1);
        }
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            LocalDate selected = LocalDate.of(year, month + 1, day);
            selectedEnd[0] = TimeUtils.endOfDay(selected);
            endButton.setText(repeatEndButtonText(selectedEnd[0]));
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        picker.show();
    }

    private String dueButtonText(Long dueAt) {
        return dueAt == null ? "期限を設定しない" : TimeUtils.relativeDue(dueAt) + "  ▾";
    }

    private String repeatEndButtonText(Long repeatEnd) {
        return repeatEnd == null ? "終了日を選ぶ" : TimeUtils.dateOnly(repeatEnd) + "まで  ▾";
    }

    private void confirmDelete(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("タスクを削除しますか？")
                .setMessage("「" + task.title + "」は元に戻せません。")
                .setNegativeButton("キャンセル", null)
                .setPositiveButton("削除", (dialog, which) -> {
                    AlarmScheduler.cancel(this, task.id);
                    store.delete(task.id);
                    refresh();
                })
                .show();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        long taskId = intent.getLongExtra(ReminderReceiver.EXTRA_TASK_ID, -1);
        if (taskId >= 0) {
            Task task = store.get(taskId);
            if (task != null) showTaskDialog(task);
            intent.removeExtra(ReminderReceiver.EXTRA_TASK_ID);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void applySystemBars(View root) {
        getWindow().setStatusBarColor(CREAM);
        getWindow().setNavigationBarColor(CREAM);
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

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView fieldLabel(String value) {
        return text(value, 13, MUTED, Typeface.BOLD);
    }

    private EditText fieldInput(String hint, boolean singleLine) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(Color.rgb(155, 155, 147));
        input.setTextColor(INK);
        input.setTextSize(15);
        input.setSingleLine(singleLine);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackground(strokedRoundRect(Color.TRANSPARENT, LINE, dp(14), 1));
        return input;
    }

    private Button compactButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(14);
        button.setTextColor(MUTED);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    private Button choiceButton(String value) {
        Button button = compactButton(value);
        button.setTextSize(14);
        button.setBackground(strokedRoundRect(Color.TRANSPARENT, LINE, dp(14), 1));
        return button;
    }

    private Button miniChoice(String value) {
        Button button = choiceButton(value);
        button.setTextSize(12);
        return button;
    }

    private LinearLayout.LayoutParams weightedChoiceParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
        params.setMarginEnd(dp(6));
        return params;
    }

    private LinearLayout.LayoutParams marginTop(int value) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = value;
        return params;
    }

    private LinearLayout.LayoutParams marginStart(int value) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(value);
        return params;
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable strokedRoundRect(int color, int strokeColor, int radius, int strokeDp) {
        GradientDrawable drawable = roundRect(color, radius);
        drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
