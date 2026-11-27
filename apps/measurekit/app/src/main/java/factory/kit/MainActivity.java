package factory.kit;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import factory.ui.KitUi;
import factory.ui.SystemInsets;

public final class MainActivity extends Activity implements SensorEventListener {
    private SensorManager manager;
    private Sensor sensor;
    private KitUi ui;
    private TextView readout, status;
    private Button setZero, resetZero;
    private LevelView level;
    private float x, y, zeroX, zeroY;
    private boolean hasReading, calibrated, isLevel;

    private String t(String en, String ko, String ja) {
        String language = getResources().getConfiguration().getLocales().get(0).getLanguage();
        return "ko".equals(language) ? ko : "ja".equals(language) ? ja : en;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            zeroX = state.getFloat("zeroX");
            zeroY = state.getFloat("zeroY");
            calibrated = state.getBoolean("calibrated");
        }
        ui = new KitUi(this, 0xff216b58, 0xff8ce1c6);
        ScrollView screen = new ScrollView(this);
        screen.setFillViewport(true);
        screen.setBackgroundColor(ui.background);
        LinearLayout page = ui.column();
        FrameLayout frame = new FrameLayout(this);
        frame.addView(page, new FrameLayout.LayoutParams(-1, -2, android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL));
        screen.addView(frame);
        page.addView(ui.text(getString(R.string.app_name), 32, true));
        page.addView(ui.text(t("Place the phone on the surface to measure its tilt.",
                "기울기를 측정할 면에 휴대전화를 놓으세요.", "端末を測る面に置いて傾きを確認します。"), 16, false));

        LinearLayout instrument = ui.card(page);
        level = new LevelView();
        level.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ui.preview(level);
        instrument.addView(level, ui.space(ui.dp(248), 0, 6));
        readout = ui.text("—", 26, true);
        readout.setGravity(android.view.Gravity.CENTER);
        instrument.addView(readout);
        status = ui.text("", 15, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        instrument.addView(status);

        LinearLayout actions = ui.card(page);
        setZero = new Button(this);
        setZero.setText(t("Set zero", "영점 설정", "ゼロ設定"));
        ui.button(setZero, true);
        setZero.setOnClickListener(v -> {
            if (!hasReading) return;
            zeroX = x;
            zeroY = y;
            calibrated = true;
            updateReading(true);
        });
        actions.addView(setZero, ui.space(-2, 0, 4));
        resetZero = new Button(this);
        resetZero.setText(t("Reset calibration", "보정 초기화", "補正リセット"));
        ui.button(resetZero, false);
        resetZero.setOnClickListener(v -> {
            zeroX = zeroY = 0;
            calibrated = false;
            updateReading(true);
        });
        actions.addView(resetZero, ui.space(-2, 4, 2));
        actions.addView(ui.text(t("Set zero on a known level reference. Sensor readings are approximate.",
                "수평인 기준면에서 영점을 설정하세요. 센서 측정값은 참고용입니다.",
                "水平な基準面でゼロ補正します。センサーの測定値は参考値です。"), 14, false));

        setContentView(screen);
        SystemInsets.apply(this, screen);
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (manager != null) {
            sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            if (sensor == null) sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        boolean reading = sensor != null && manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        showUnavailable(reading);
    }

    @Override protected void onPause() {
        if (manager != null) manager.unregisterListener(this);
        showUnavailable(true);
        super.onPause();
    }

    private void showUnavailable(boolean waiting) {
        hasReading = false;
        setZero.setEnabled(false);
        resetZero.setEnabled(calibrated);
        readout.setText("—");
        status.setText(waiting ? t("Waiting for a reading…", "센서 측정 대기 중…", "測定値を待っています…")
                : t("Tilt sensor unavailable", "기울기 센서를 사용할 수 없습니다", "傾きセンサーを利用できません"));
        level.invalidate();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putFloat("zeroX", zeroX);
        out.putFloat("zeroY", zeroY);
        out.putBoolean("calibrated", calibrated);
    }

    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateReading(false);
    }

    @Override public void onAccuracyChanged(Sensor changed, int accuracy) {}

    @Override public void onSensorChanged(SensorEvent event) {
        float a = event.values[0], b = event.values[1], c = event.values[2];
        if (!LevelMath.valid(a, b, c)) return;
        x = LevelMath.inclination(a, b, c);
        y = LevelMath.inclination(b, a, c);
        boolean first = !hasReading;
        hasReading = true;
        updateReading(first);
    }

    private void updateReading(boolean refreshStatus) {
        setZero.setEnabled(hasReading);
        resetZero.setEnabled(calibrated);
        if (!hasReading) return;
        int rotation = getDisplay() == null ? 0 : getDisplay().getRotation();
        float horizontal = LevelMath.horizontal(x - zeroX, y - zeroY, rotation);
        float vertical = LevelMath.vertical(x - zeroX, y - zeroY, rotation);
        String angles = String.format(getResources().getConfiguration().getLocales().get(0),
                "X %+.1f°   Y %+.1f°", horizontal, vertical);
        if (!angles.contentEquals(readout.getText())) readout.setText(angles);
        boolean nearZero = LevelMath.level(horizontal, vertical);
        if (refreshStatus || isLevel != nearZero) {
            isLevel = nearZero;
            status.setText(nearZero ? t("Level", "수평", "水平")
                    : t("Tilted", "기울어짐", "傾斜あり"));
        }
        level.horizontal = horizontal;
        level.vertical = vertical;
        level.invalidate();
    }

    private final class LevelView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float horizontal, vertical;

        LevelView() {
            super(MainActivity.this);
        }

        @Override protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float ring = Math.min(cx, cy) - ui.dp(20);
            if (ring <= 0) return;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(ui.dp(2));
            paint.setColor(ui.muted);
            canvas.drawCircle(cx, cy, ring, paint);
            canvas.drawCircle(cx, cy, ring / 4, paint);
            canvas.drawLine(cx - ring, cy, cx + ring, cy, paint);
            canvas.drawLine(cx, cy - ring, cx, cy + ring, paint);
            if (!hasReading) return;
            float scale = ring * .8f / Math.max(45f, (float) Math.hypot(horizontal, vertical));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(isLevel ? ui.accent : ui.ink);
            canvas.drawCircle(cx + horizontal * scale, cy - vertical * scale, ui.dp(13), paint);
        }
    }
}
