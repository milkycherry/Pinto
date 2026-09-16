package dev.milky.pinto;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/*
  Pintoで繰り返し使う色・余白・入力欄などを生成するUI部品工場。
  <p>画面クラスが細かな装飾コードを持たず、表示内容と操作の流れに集中できるようにする。</p>
*/
final class PintoViewFactory {
    /** アプリ全体で共有するカラーパレット。 */
    static final int CREAM = Color.rgb(247, 244, 238);
    static final int PAPER = Color.rgb(255, 253, 248);
    static final int INK = Color.rgb(32, 35, 31);
    static final int MUTED = Color.rgb(111, 113, 106);
    static final int GREEN = Color.rgb(66, 107, 90);
    static final int GREEN_DARK = Color.rgb(36, 76, 62);
    static final int GREEN_SOFT = Color.rgb(226, 237, 230);
    static final int RED = Color.rgb(181, 73, 65);
    static final int RED_SOFT = Color.rgb(249, 231, 228);
    static final int LINE = Color.rgb(226, 223, 214);

    /* 端末密度やAndroid標準部品を参照するためのContext。 */
    private final Context context;

    /* 指定画面のContextに合わせたUI部品工場を作る。 */
    PintoViewFactory(Context context) {
        this.context = context;
    }

    /* 子要素を上から下へ並べるレイアウトを作る。 */
    LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    /* 子要素を左から右へ並べるレイアウトを作る。 */
    LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    /* Pinto共通の文字設定を適用したTextViewを作る。 */
    TextView text(String value, float size, int color, int style) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setIncludeFontPadding(false);
        return view;
    }

    /* 入力欄の上に表示する小さな見出しを作る。 */
    TextView fieldLabel(String value) {
        return text(value, 13, MUTED, Typeface.BOLD);
    }

    /* 枠線と余白を統一したテキスト入力欄を作る。 */
    EditText fieldInput(String hint, boolean singleLine) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setHintTextColor(Color.rgb(155, 155, 147));
        input.setTextColor(INK);
        input.setTextSize(15);
        input.setSingleLine(singleLine);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackground(strokedRoundRect(Color.TRANSPARENT, LINE, dp(14), 1));
        return input;
    }

    /* 閉じる・削除・メニューなどに使う装飾の少ないボタンを作る。 */
    Button compactButton(String value) {
        Button button = new Button(context);
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

    /* 日付や優先度など、選択肢に使う枠付きボタンを作る。 */
    Button choiceButton(String value) {
        Button button = compactButton(value);
        button.setTextSize(14);
        button.setBackground(strokedRoundRect(Color.TRANSPARENT, LINE, dp(14), 1));
        return button;
    }

    /* ショートカット用に文字を小さくした選択ボタンを作る。 */
    Button miniChoice(String value) {
        Button button = choiceButton(value);
        button.setTextSize(12);
        return button;
    }

    /* 横一列の選択肢を同じ幅で配置するためのLayoutParamsを作る。 */
    LinearLayout.LayoutParams weightedChoiceParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
        params.setMarginEnd(dp(6));
        return params;
    }

    /* 上側だけに指定余白を持つ、横幅いっぱいのLayoutParamsを作る。 */
    LinearLayout.LayoutParams marginTop(int value) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = value;
        return params;
    }

    /* 開始側だけに指定余白を持つLayoutParamsを作る。 */
    LinearLayout.LayoutParams marginStart(int value) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(value);
        return params;
    }

    /* 単色の角丸背景を作る。 */
    GradientDrawable roundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    /* 枠線付きの角丸背景を作る。 */
    GradientDrawable strokedRoundRect(int color, int strokeColor, int radius, int strokeDp) {
        GradientDrawable drawable = roundRect(color, radius);
        drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    /* 端末の画面密度に合わせてdpを実ピクセルへ変換する。 */
    int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
