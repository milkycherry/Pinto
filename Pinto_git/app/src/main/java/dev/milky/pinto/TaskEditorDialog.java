package dev.milky.pinto;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/*
  タスクの追加・編集に必要な入力欄と選択処理をまとめるダイアログ。
  
  <p>入力値の検証までを担当し、データベース保存は {@link Listener} へ依頼する。</p>
*/
final class TaskEditorDialog {
    /*
      編集結果を、保存処理を持つMainActivityへ渡すための窓口。
    */
    interface Listener {
        /* 検証済みタスクを追加または更新するよう依頼する。 */
        void onTaskSaved(Task task, boolean isNewTask);

        /* 編集中のタスクを削除するよう依頼する。 */
        void onDeleteTaskRequested(Task task);
    }

    /* Android標準ダイアログや日時選択画面を表示するActivity。 */
    private final Activity activity;

    /* 保存・削除依頼を受け取る相手。 */
    private final Listener listener;

    /* Pinto共通の入力欄・ボタン・余白を生成する補助クラス。 */
    private final PintoViewFactory ui;

    /* 編集ダイアログの通知先と共通UI生成を初期化する。 */
    TaskEditorDialog(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        this.ui = new PintoViewFactory(activity);
    }

    /*
    新規追加または既存タスク編集のダイアログを表示する。
    
    @param existing 編集対象。nullの場合は新規タスクとして開く。
    */
    void show(Task existing) {
        // 保存済みオブジェクトを入力途中で書き換えないよう、編集用コピーを使う。
        Task draft = existing == null ? new Task() : existing.copy();
        if (existing == null) draft.dueAt = TimeUtils.defaultTaskDue();

        Dialog dialog = new Dialog(activity);
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        LinearLayout panel = ui.vertical();
        panel.setPadding(ui.dp(22), ui.dp(18), ui.dp(22), ui.dp(26));
        panel.setBackground(ui.roundRect(PintoViewFactory.PAPER, ui.dp(24)));
        scroll.addView(panel);

        LinearLayout heading = ui.horizontal();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView dialogTitle = ui.text(
                existing == null ? "新しい予定" : "予定を編集",
                21,
                PintoViewFactory.INK,
                Typeface.BOLD);
        heading.addView(dialogTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button close = ui.compactButton("×");
        close.setTextSize(22);
        close.setOnClickListener(view -> dialog.dismiss());
        heading.addView(close, new LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)));
        panel.addView(heading);

        panel.addView(ui.fieldLabel("やること"), ui.marginTop(ui.dp(16)));
        EditText titleInput = ui.fieldInput("例：歯医者を予約する", true);
        titleInput.setText(draft.title);
        panel.addView(titleInput, ui.marginTop(ui.dp(7)));

        panel.addView(ui.fieldLabel("メモ"), ui.marginTop(ui.dp(16)));
        EditText noteInput = ui.fieldInput("必要な情報をメモ", false);
        noteInput.setText(draft.note);
        noteInput.setMinHeight(ui.dp(76));
        noteInput.setGravity(Gravity.TOP | Gravity.START);
        panel.addView(noteInput, ui.marginTop(ui.dp(7)));

        // 配列はラムダ内でも選択値を更新できる、1要素の可変入れ物として使う。
        int[] selectedPriority = new int[]{draft.priority};
        panel.addView(ui.fieldLabel("優先度"), ui.marginTop(ui.dp(16)));
        LinearLayout priorityRow = ui.horizontal();
        Button low = ui.choiceButton("低");
        Button medium = ui.choiceButton("中");
        Button high = ui.choiceButton("高");
        List<Button> priorityButtons = Arrays.asList(low, medium, high);
        View.OnClickListener choosePriority = view -> {
            if (view == low) selectedPriority[0] = Task.PRIORITY_LOW;
            else if (view == high) selectedPriority[0] = Task.PRIORITY_HIGH;
            else selectedPriority[0] = Task.PRIORITY_MEDIUM;
            stylePriorityChoices(priorityButtons, selectedPriority[0]);
        };
        for (Button button : priorityButtons) {
            button.setOnClickListener(choosePriority);
            priorityRow.addView(button, ui.weightedChoiceParams());
        }
        stylePriorityChoices(priorityButtons, selectedPriority[0]);
        panel.addView(priorityRow, ui.marginTop(ui.dp(7)));

        Long[] selectedDue = new Long[]{draft.dueAt};
        panel.addView(
                ui.fieldLabel("予定日時（繰り返しの開始）"),
                ui.marginTop(ui.dp(16)));
        Button dueButton = ui.choiceButton(dueButtonText(selectedDue[0]));
        dueButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        dueButton.setPadding(ui.dp(16), 0, ui.dp(16), 0);
        dueButton.setOnClickListener(
                view -> showDateTimePicker(selectedDue, dueButton));
        panel.addView(dueButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(50)));

        LinearLayout dueShortcuts = ui.horizontal();
        dueShortcuts.setGravity(Gravity.CENTER_VERTICAL);
        Button today = ui.miniChoice("今日");
        Button tomorrow = ui.miniChoice("明日");
        Button weekend = ui.miniChoice("週末");
        Button none = ui.miniChoice("なし");
        today.setOnClickListener(
                view -> setDueShortcut(selectedDue, dueButton, LocalDate.now()));
        tomorrow.setOnClickListener(
                view -> setDueShortcut(
                        selectedDue, dueButton, LocalDate.now().plusDays(1)));
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
            dueShortcuts.addView(button, ui.weightedChoiceParams());
        }
        panel.addView(dueShortcuts, ui.marginTop(ui.dp(8)));

        int[] selectedRepeat = new int[]{draft.repeatType};
        Long[] selectedRepeatEnd = new Long[]{draft.repeatEnd};
        panel.addView(ui.fieldLabel("繰り返し"), ui.marginTop(ui.dp(16)));
        LinearLayout repeatRow = ui.horizontal();
        Button noRepeat = ui.choiceButton("繰り返さない");
        Button daily = ui.choiceButton("毎日");
        List<Button> repeatButtons = Arrays.asList(noRepeat, daily);
        repeatRow.addView(noRepeat, ui.weightedChoiceParams());
        repeatRow.addView(daily, ui.weightedChoiceParams());
        panel.addView(repeatRow, ui.marginTop(ui.dp(7)));

        LinearLayout repeatDetails = ui.vertical();
        repeatDetails.addView(ui.fieldLabel("終了日（この日を含む）"));
        Button repeatEndButton = ui.choiceButton(
                repeatEndButtonText(selectedRepeatEnd[0]));
        repeatEndButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        repeatEndButton.setPadding(ui.dp(16), 0, ui.dp(16), 0);
        repeatDetails.addView(repeatEndButton, ui.marginTop(ui.dp(7)));
        TextView repeatHint = ui.text(
                "完了すると翌日分を自動で追加します",
                12,
                PintoViewFactory.MUTED,
                Typeface.NORMAL);
        repeatDetails.addView(repeatHint, ui.marginTop(ui.dp(6)));
        panel.addView(repeatDetails, ui.marginTop(ui.dp(12)));

        View.OnClickListener chooseRepeat = view -> {
            selectedRepeat[0] = view == daily ? Task.REPEAT_DAILY : Task.REPEAT_NONE;
            if (selectedRepeat[0] == Task.REPEAT_DAILY) {
                // 毎日を選んだ場合は、未設定の開始・終了日時に安全な初期値を入れる。
                if (selectedDue[0] == null) {
                    selectedDue[0] = TimeUtils.defaultTaskDue();
                    dueButton.setText(dueButtonText(selectedDue[0]));
                }
                if (selectedRepeatEnd[0] == null
                        || selectedRepeatEnd[0] < selectedDue[0]) {
                    LocalDate start = TimeUtils.toLocalDateTime(
                            selectedDue[0]).toLocalDate();
                    selectedRepeatEnd[0] = TimeUtils.endOfDay(start.plusMonths(1));
                    repeatEndButton.setText(
                            repeatEndButtonText(selectedRepeatEnd[0]));
                }
            }
            styleRepeatChoices(repeatButtons, selectedRepeat[0]);
            repeatDetails.setVisibility(
                    selectedRepeat[0] == Task.REPEAT_DAILY
                            ? View.VISIBLE
                            : View.GONE);
        };
        noRepeat.setOnClickListener(chooseRepeat);
        daily.setOnClickListener(chooseRepeat);
        styleRepeatChoices(repeatButtons, selectedRepeat[0]);
        repeatDetails.setVisibility(
                selectedRepeat[0] == Task.REPEAT_DAILY ? View.VISIBLE : View.GONE);
        repeatEndButton.setOnClickListener(
                view -> showRepeatEndPicker(
                        selectedDue, selectedRepeatEnd, repeatEndButton));

        panel.addView(ui.fieldLabel("通知"), ui.marginTop(ui.dp(16)));
        String[] reminderLabels = {"期限ちょうど", "10分前", "1時間前", "1日前"};
        int[] reminderValues = {0, 10, 60, 1440};
        Spinner reminder = new Spinner(activity);
        ArrayAdapter<String> reminderAdapter = new ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                reminderLabels);
        reminder.setAdapter(reminderAdapter);
        int selectedReminder = 1;
        for (int i = 0; i < reminderValues.length; i++) {
            if (reminderValues[i] == draft.reminderMinutes) selectedReminder = i;
        }
        reminder.setSelection(selectedReminder);
        reminder.setBackground(ui.strokedRoundRect(
                Color.TRANSPARENT, PintoViewFactory.LINE, ui.dp(14), 1));
        reminder.setPadding(ui.dp(12), 0, ui.dp(12), 0);
        panel.addView(reminder, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(50)));

        LinearLayout actions = ui.horizontal();
        actions.setGravity(Gravity.CENTER_VERTICAL);
        if (existing != null) {
            Button delete = ui.compactButton("削除");
            delete.setTextColor(PintoViewFactory.RED);
            delete.setOnClickListener(view -> {
                dialog.dismiss();
                listener.onDeleteTaskRequested(existing);
            });
            actions.addView(delete, new LinearLayout.LayoutParams(
                    ui.dp(76), ui.dp(52)));
        } else {
            View spacer = new View(activity);
            actions.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        }

        Button save = new Button(activity);
        save.setText(existing == null ? "追加する" : "保存する");
        save.setTextColor(Color.WHITE);
        save.setTextSize(15);
        save.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        save.setAllCaps(false);
        save.setBackground(ui.roundRect(PintoViewFactory.GREEN_DARK, ui.dp(16)));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ui.dp(132), ui.dp(52));
        saveParams.setMarginStart(ui.dp(10));
        actions.addView(save, saveParams);
        panel.addView(actions, ui.marginTop(ui.dp(22)));

        // 入力内容をTaskへ戻し、矛盾がない場合だけ保存担当へ渡す。
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
            draft.repeatEnd = selectedRepeat[0] == Task.REPEAT_DAILY
                    ? selectedRepeatEnd[0]
                    : null;
            if (draft.repeatType == Task.REPEAT_DAILY && draft.dueAt == null) {
                Toast.makeText(
                        activity,
                        "繰り返しの開始日時を設定してください",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (draft.repeatType == Task.REPEAT_DAILY
                    && (draft.repeatEnd == null || draft.repeatEnd < draft.dueAt)) {
                Toast.makeText(
                        activity,
                        "終了日は開始日以降にしてください",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            draft.reminderMinutes =
                    reminderValues[reminder.getSelectedItemPosition()];
            listener.onTaskSaved(draft, existing == null);
            dialog.dismiss();
        });

        configureWindow(dialog, scroll);
        dialog.show();
        titleInput.requestFocus();
    }

    /* 画面下から開く編集パネルとして、幅・位置・背景の暗さを設定する。 */
    private void configureWindow(Dialog dialog, ScrollView content) {
        dialog.setContentView(content);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(ignored -> {
            Window shown = dialog.getWindow();
            if (shown == null) return;

            shown.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            shown.setGravity(Gravity.BOTTOM);
            WindowManager.LayoutParams attributes = shown.getAttributes();
            attributes.dimAmount = 0.45f;
            shown.setAttributes(attributes);
            shown.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        });
    }

    /* 選択中の優先度を色と太字で強調する。 */
    private void stylePriorityChoices(List<Button> buttons, int selectedPriority) {
        for (int index = 0; index < buttons.size(); index++) {
            Button button = buttons.get(index);
            boolean selected = index == selectedPriority;
            int color = index == Task.PRIORITY_HIGH
                    ? PintoViewFactory.RED
                    : (index == Task.PRIORITY_LOW
                            ? PintoViewFactory.MUTED
                            : PintoViewFactory.GREEN);
            int soft = index == Task.PRIORITY_HIGH
                    ? PintoViewFactory.RED_SOFT
                    : (index == Task.PRIORITY_LOW
                            ? Color.rgb(237, 236, 232)
                            : PintoViewFactory.GREEN_SOFT);
            button.setTextColor(selected ? color : PintoViewFactory.MUTED);
            button.setTypeface(
                    Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            button.setBackground(ui.strokedRoundRect(
                    selected ? soft : Color.TRANSPARENT,
                    selected ? color : PintoViewFactory.LINE,
                    ui.dp(14),
                    1));
        }
    }

    /* 選択中の繰り返し方法を緑色で強調する。 */
    private void styleRepeatChoices(List<Button> buttons, int selectedRepeat) {
        for (int index = 0; index < buttons.size(); index++) {
            Button button = buttons.get(index);
            boolean selected = index == selectedRepeat;
            button.setTextColor(
                    selected ? PintoViewFactory.GREEN_DARK : PintoViewFactory.MUTED);
            button.setTypeface(
                    Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            button.setBackground(ui.strokedRoundRect(
                    selected ? PintoViewFactory.GREEN_SOFT : Color.TRANSPARENT,
                    selected ? PintoViewFactory.GREEN : PintoViewFactory.LINE,
                    ui.dp(14),
                    1));
        }
    }

    /* 日付を選んだ後に時刻を選び、期限をミリ秒へ変換して保持する。 */
    private void showDateTimePicker(Long[] selectedDue, Button dueButton) {
        LocalDateTime initial = selectedDue[0] == null
                ? TimeUtils.toLocalDateTime(TimeUtils.defaultTaskDue())
                : TimeUtils.toLocalDateTime(selectedDue[0]);
        DatePickerDialog datePicker = new DatePickerDialog(
                activity,
                (picker, year, month, day) -> {
                    LocalDate selectedDate = LocalDate.of(year, month + 1, day);
                    TimePickerDialog timePicker = new TimePickerDialog(
                            activity,
                            (time, hour, minute) -> {
                                selectedDue[0] = TimeUtils.atDateAndTime(
                                        selectedDate, hour, minute);
                                dueButton.setText(dueButtonText(selectedDue[0]));
                            },
                            initial.getHour(),
                            initial.getMinute(),
                            true);
                    timePicker.show();
                },
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth());
        datePicker.show();
    }

    /* 今日・明日・週末のショートカットを20時の期限へ変換する。 */
    private void setDueShortcut(Long[] selectedDue, Button button, LocalDate date) {
        selectedDue[0] = TimeUtils.atDateAndTime(date, 20, 0);
        button.setText(dueButtonText(selectedDue[0]));
    }

    /* 繰り返し終了日を選び、その日の23:59:59までを有効範囲にする。 */
    private void showRepeatEndPicker(
            Long[] selectedDue,
            Long[] selectedEnd,
            Button endButton
    ) {
        LocalDate initial;
        if (selectedEnd[0] != null) {
            initial = TimeUtils.toLocalDateTime(selectedEnd[0]).toLocalDate();
        } else if (selectedDue[0] != null) {
            initial = TimeUtils.toLocalDateTime(
                    selectedDue[0]).toLocalDate().plusMonths(1);
        } else {
            initial = LocalDate.now().plusMonths(1);
        }

        DatePickerDialog picker = new DatePickerDialog(
                activity,
                (view, year, month, day) -> {
                    LocalDate selected = LocalDate.of(year, month + 1, day);
                    selectedEnd[0] = TimeUtils.endOfDay(selected);
                    endButton.setText(repeatEndButtonText(selectedEnd[0]));
                },
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth());
        picker.show();
    }

    /* 期限選択ボタンに表示する文言を作る。 */
    private String dueButtonText(Long dueAt) {
        return dueAt == null
                ? "期限を設定しない"
                : TimeUtils.relativeDue(dueAt) + "  ▾";
    }

    /* 繰り返し終了日ボタンに表示する文言を作る。 */
    private String repeatEndButtonText(Long repeatEnd) {
        return repeatEnd == null
                ? "終了日を選ぶ"
                : TimeUtils.dateOnly(repeatEnd) + "まで  ▾";
    }
}
