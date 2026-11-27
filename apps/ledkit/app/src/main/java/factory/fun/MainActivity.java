package factory.fun;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import factory.ui.KitUi;
import factory.ui.SystemInsets;

public final class MainActivity extends Activity {
    private static final int[] PALETTE = {Color.YELLOW, Color.WHITE, Color.CYAN, 0xffff69b4, 0xff66ff66};
    private KitUi ui;
    private SharedPreferences prefs;
    private Banner preview;
    private EditText message;
    private Spinner colors, directions;
    private SeekBar speed, size;
    private TextView status;
    private Button save, present;
    private Dialog fullscreen;

    private String t(String en, String ko, String ja) {
        String locale = getResources().getConfiguration().getLocales().get(0).getLanguage();
        return "ko".equals(locale) ? ko : "ja".equals(locale) ? ja : en;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("local", MODE_PRIVATE);
        ui = new KitUi(this, 0xff4338ca, 0xffc2baff);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ui.background);
        FrameLayout frame = new FrameLayout(this);
        LinearLayout page = ui.column();
        frame.addView(page, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        scroll.addView(frame);
        page.addView(ui.text(getString(R.string.app_name), 32, true));
        page.addView(ui.text(t("Create a moving sign for greetings, pickups or events.",
                "인사, 마중, 행사에 사용할 움직이는 전광판을 만드세요.",
                "挨拶やお迎え、イベントに使える電光掲示板を作れます。"), 16, false));

        LinearLayout display = ui.card(page);
        preview = new Banner();
        ui.preview(preview);
        preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        display.addView(preview, ui.space(ui.dp(210), 0, 6));
        present = button(display, t("Fullscreen", "전체 화면", "全画面表示"), true, this::openFullscreen);

        LinearLayout settings = ui.card(page);
        String messageLabel = t("Message", "문구", "メッセージ");
        settings.addView(ui.text(messageLabel, 16, true));
        message = new EditText(this);
        message.setSingleLine(true);
        message.setInputType(InputType.TYPE_CLASS_TEXT);
        message.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        message.setContentDescription(messageLabel);
        message.setTextColor(ui.ink);
        ui.field(message);
        message.setText(state == null ? prefs.getString("message", "HELLO!") : state.getString("message", ""));
        settings.addView(message, ui.space(-2, 0, 4));

        colors = options(settings, t("Text color", "글자 색상", "文字色"),
                t("Yellow", "노랑", "黄色"), t("White", "흰색", "白"), t("Cyan", "하늘색", "水色"),
                t("Pink", "분홍", "ピンク"), t("Green", "초록", "緑"));
        directions = options(settings, t("Motion", "이동", "動き"),
                t("Left", "왼쪽", "左"), t("Right", "오른쪽", "右"), t("Static", "고정", "固定"));
        speed = slider(settings, t("Speed", "속도", "速度"), 9);
        size = slider(settings, t("Text size", "글자 크기", "文字サイズ"), 8);
        colors.setSelection(Rules.clamp(state == null ? prefs.getInt("color", 0) : state.getInt("color"), 4));
        directions.setSelection(Rules.clamp(state == null ? prefs.getInt("direction", 0) : state.getInt("direction"), 2));
        speed.setProgress(Rules.clamp(state == null ? prefs.getInt("speed", 4) : state.getInt("speed"), 9));
        size.setProgress(Rules.clamp(state == null ? prefs.getInt("size", 4) : state.getInt("size"), 8));
        save = button(settings, t("Save on this device", "이 기기에 저장", "この端末に保存"), false, this::saveSettings);
        status = ui.text("", 14, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        settings.addView(status);

        message.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refresh(); }
            @Override public void afterTextChanged(Editable text) {}
        });
        AdapterView.OnItemSelectedListener selected = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View child, int position, long id) { refresh(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        colors.setOnItemSelectedListener(selected);
        directions.setOnItemSelectedListener(selected);
        refresh();
        setContentView(scroll);
        SystemInsets.apply(this, scroll);
    }

    private Button button(LinearLayout parent, String label, boolean primary, Runnable action) {
        Button result = new Button(this);
        result.setText(label);
        ui.button(result, primary);
        result.setOnClickListener(v -> action.run());
        parent.addView(result, ui.space(-2, 4, 2));
        return result;
    }

    private Spinner options(LinearLayout parent, String label, String... items) {
        parent.addView(ui.text(label, 16, true));
        Spinner result = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        result.setAdapter(adapter);
        result.setContentDescription(label);
        ui.field(result);
        parent.addView(result, ui.space(ui.dp(52), 0, 4));
        return result;
    }

    private SeekBar slider(LinearLayout parent, String label, int max) {
        parent.addView(ui.text(label, 16, true));
        SeekBar result = new SeekBar(this);
        result.setMax(max);
        result.setContentDescription(label);
        result.setProgressTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        result.setThumbTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        result.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (user) refresh();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        parent.addView(result, ui.space(ui.dp(48), 0, 2));
        return result;
    }

    private String draft() {
        return message.getText().toString().trim();
    }

    private void refresh() {
        String text = draft();
        boolean ready = !text.isEmpty();
        int direction = Rules.clamp(directions.getSelectedItemPosition(), 2);
        speed.setEnabled(direction != 2);
        save.setEnabled(ready);
        present.setEnabled(ready);
        preview.configure(text, PALETTE[Rules.clamp(colors.getSelectedItemPosition(), 4)], direction,
                speed.getProgress() + 1, size.getProgress());
        preview.setContentDescription(ready ? text : t("Empty preview", "빈 미리보기", "空のプレビュー"));
        status.setText(ready ? "" : t("Enter a message to save or show it.", "저장하거나 표시할 문구를 입력하세요.",
                "保存または表示するメッセージを入力してください。"));
    }

    private void saveSettings() {
        if (draft().isEmpty()) return;
        prefs.edit().putString("message", draft()).putInt("color", colors.getSelectedItemPosition())
                .putInt("direction", directions.getSelectedItemPosition()).putInt("speed", speed.getProgress())
                .putInt("size", size.getProgress()).apply();
        status.setText(t("Saved on this device", "이 기기에 저장했어요", "この端末に保存しました"));
    }

    private void openFullscreen() {
        if (fullscreen != null || draft().isEmpty()) return;
        Banner board = new Banner();
        board.configure(draft(), PALETTE[Rules.clamp(colors.getSelectedItemPosition(), 4)],
                Rules.clamp(directions.getSelectedItemPosition(), 2), speed.getProgress() + 1, size.getProgress());
        board.setContentDescription(draft() + ". " + t("Tap to leave fullscreen", "탭하여 전체 화면 종료", "タップで全画面を閉じる"));
        board.setFocusable(true);
        board.setOnClickListener(v -> fullscreen.dismiss());
        fullscreen = new Dialog(this, android.R.style.Theme_Material_NoActionBar_Fullscreen);
        fullscreen.setContentView(board);
        fullscreen.setOnDismissListener(d -> fullscreen = null);
        fullscreen.show();
        Window window = fullscreen.getWindow();
        if (window == null) return;
        window.setLayout(-1, -1);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowInsetsController insets = window.getInsetsController();
        if (insets != null) {
            insets.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            insets.hide(WindowInsets.Type.systemBars());
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("message", message.getText().toString());
        out.putInt("color", colors.getSelectedItemPosition());
        out.putInt("direction", directions.getSelectedItemPosition());
        out.putInt("speed", speed.getProgress());
        out.putInt("size", size.getProgress());
    }

    @Override protected void onDestroy() {
        if (fullscreen != null) fullscreen.dismiss();
        super.onDestroy();
    }

    private final class Banner extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String text = "";
        private int color, direction, rate, font;
        private long started;

        Banner() {
            super(MainActivity.this);
            paint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        }

        void configure(String text, int color, int direction, int rate, int font) {
            this.text = text;
            this.color = color;
            this.direction = direction;
            this.rate = rate;
            this.font = font;
            started = SystemClock.uptimeMillis();
            invalidate();
        }

        @Override public void onWindowFocusChanged(boolean focus) {
            super.onWindowFocusChanged(focus);
            if (focus) {
                started = SystemClock.uptimeMillis();
                invalidate();
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xff101320);
            paint.setColor(0xff293044);
            float pitch = ui.dp(12);
            for (float y = pitch / 2; y < getHeight(); y += pitch) {
                for (float x = pitch / 2; x < getWidth(); x += pitch) canvas.drawCircle(x, y, 1, paint);
            }
            if (text.isEmpty()) return;
            paint.setColor(color);
            paint.setTextSize(Math.max(12, getHeight() * (.25f + .075f * font)));
            float width = paint.measureText(text);
            float available = Math.max(1, getWidth() - ui.dp(16));
            if (direction == 2 && width > available) {
                paint.setTextSize(paint.getTextSize() * available / width);
                width = paint.measureText(text);
            }
            float distance = (getWidth() + width) / 2f
                    + (SystemClock.uptimeMillis() - started) / 1000f * ui.dp(rate * 24);
            float x = Rules.position(getWidth(), width, direction, distance);
            canvas.drawText(text, x, getHeight() / 2f - (paint.ascent() + paint.descent()) / 2f, paint);
            if (direction != 2 && isShown() && hasWindowFocus()) postInvalidateOnAnimation();
        }
    }
}
