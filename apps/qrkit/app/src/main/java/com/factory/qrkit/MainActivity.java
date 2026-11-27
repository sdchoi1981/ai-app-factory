package com.factory.qrkit;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.zxing.common.BitMatrix;

import java.nio.charset.StandardCharsets;

import factory.ui.KitUi;
import factory.ui.SystemInsets;

public final class MainActivity extends Activity {
    private static final int SCAN_REQUEST = 421;
    private static final int CAMERA_REQUEST = 422;
    private static final int MAX_BYTES = 1200;

    private KitUi ui;
    private ScrollView scroll;
    private LinearLayout createPanel;
    private LinearLayout scanPanel;
    private Button createTab;
    private Button scanTab;
    private EditText input;
    private TextView byteCount;
    private TextView previewHint;
    private TextView scanStatus;
    private TextView scannedResult;
    private ImageView qrImage;
    private ImageView emptyImage;
    private Button generate;
    private Button copy;
    private String scanned = "";
    private boolean showingScan;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        ui = new KitUi(this, 0xff184b45, 0xffa5ecd6);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ui.background);
        FrameLayout center = new FrameLayout(this);
        LinearLayout page = ui.column();
        center.addView(page, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        scroll.addView(center);

        LinearLayout masthead = new LinearLayout(this);
        masthead.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_launcher);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        masthead.addView(mark, new LinearLayout.LayoutParams(ui.dp(52), ui.dp(52)));
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setPaddingRelative(ui.dp(12), 0, 0, 0);
        masthead.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView title = addText(heading, R.string.app_name, 30, true);
        markHeading(title);
        addText(heading, R.string.workspace_summary, 13, false);
        page.addView(masthead, ui.space(-2, 0, 12));

        LinearLayout tabs = new LinearLayout(this);
        createTab = newTab(tabs, R.string.create_tab, () -> showMode(false, true));
        scanTab = newTab(tabs, R.string.scan_tab, () -> showMode(true, true));
        page.addView(tabs, ui.space(-2, 0, 14));

        createPanel = section(page);
        buildCreatePanel();
        scanPanel = section(page);
        buildScanPanel();
        showMode(state != null && state.getBoolean("scan_mode"), false);
        setContentView(scroll);
        SystemInsets.apply(this, scroll);
    }

    private LinearLayout section(LinearLayout parent) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        parent.addView(section, ui.space(-2, 0, 0));
        return section;
    }

    private Button newTab(LinearLayout parent, int label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(0, -2, 1);
        layout.setMarginEnd(ui.dp(4));
        layout.setMarginStart(ui.dp(4));
        parent.addView(button, layout);
        return button;
    }

    private void buildCreatePanel() {
        LinearLayout preview = ui.card(createPanel);
        preview.setBackground(ui.shape(ui.tint, 26, ui.outline));
        markHeading(addText(preview, R.string.preview_title, 19, true));

        FrameLayout stage = new FrameLayout(this) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int size = Math.min(View.MeasureSpec.getSize(widthSpec), ui.dp(216));
                int square = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
                super.onMeasure(square, square);
            }
        };
        stage.setBackground(ui.shape(0xffffffff, 18, 0));
        stage.setClipToOutline(true);
        LinearLayout.LayoutParams stageSize = new LinearLayout.LayoutParams(-2, -2);
        stageSize.gravity = Gravity.CENTER_HORIZONTAL;
        stageSize.setMargins(0, ui.dp(8), 0, ui.dp(8));
        preview.addView(stage, stageSize);
        emptyImage = new ImageView(this);
        emptyImage.setImageResource(R.drawable.qr_placeholder);
        emptyImage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        emptyImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        stage.addView(emptyImage, new FrameLayout.LayoutParams(ui.dp(104), ui.dp(104), Gravity.CENTER));
        qrImage = new ImageView(this);
        qrImage.setContentDescription(getString(R.string.qr_image));
        qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrImage.setVisibility(View.GONE);
        stage.addView(qrImage, new FrameLayout.LayoutParams(-1, -1));

        previewHint = addText(preview, R.string.preview_empty, 14, false);
        previewHint.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);

        TextView inputLabel = addText(createPanel, R.string.input_label, 17, true);
        input = new EditText(this);
        input.setId(View.generateViewId());
        input.setSaveEnabled(false);
        inputLabel.setLabelFor(input.getId());
        input.setHint(R.string.input_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(2);
        input.setMaxLines(4);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_BYTES)});
        input.setTextColor(ui.ink);
        input.setHintTextColor(ui.muted);
        ui.field(input);
        createPanel.addView(input, ui.space(-2, 0, 0));
        byteCount = addText(createPanel, R.string.input_limit, 13, false);
        generate = addButton(createPanel, R.string.generate, true, this::generate);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                updateInput();
                if (qrImage.getVisibility() == View.VISIBLE) resetPreview();
            }
            @Override public void afterTextChanged(Editable value) {}
        });
        updateInput();
    }

    private void buildScanPanel() {
        LinearLayout viewfinder = ui.card(scanPanel);
        viewfinder.setBackground(ui.shape(ui.tint, 26, ui.outline));
        markHeading(addText(viewfinder, R.string.scan_title, 19, true));
        ImageView art = new ImageView(this);
        art.setImageResource(R.drawable.scan_placeholder);
        art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        art.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        viewfinder.addView(art, ui.space(ui.dp(144), 4, 4));
        addText(viewfinder, R.string.scan_guide, 15, false);
        addButton(viewfinder, R.string.open_camera, true, this::requestScan);

        scanStatus = addText(scanPanel, R.string.scan_privacy, 14, false);
        scanStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout result = ui.card(scanPanel);
        markHeading(addText(result, R.string.result_title, 17, true));
        scannedResult = addText(result, R.string.result_empty, 18, false);
        scannedResult.setTextColor(ui.ink);
        scannedResult.setTextIsSelectable(true);
        scannedResult.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        copy = addButton(result, R.string.copy_result, false, this::copyResult);
        copy.setEnabled(false);
    }

    private void markHeading(View heading) {
        if (Build.VERSION.SDK_INT >= 28) heading.setAccessibilityHeading(true);
    }

    private TextView addText(LinearLayout parent, int label, int size, boolean heading) {
        TextView text = ui.text(getString(label), size, heading);
        parent.addView(text, ui.space(-2, 0, 0));
        return text;
    }

    private Button addButton(LinearLayout parent, int label, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        ui.button(button, primary);
        button.setOnClickListener(view -> action.run());
        parent.addView(button, ui.space(-2, 8, 0));
        return button;
    }

    private void showMode(boolean scan, boolean transitions) {
        if (transitions && showingScan == scan) return;
        showingScan = scan;
        if (scan) hideKeyboard();
        createTab.setSelected(!scan);
        scanTab.setSelected(scan);
        ui.button(createTab, !scan);
        ui.button(scanTab, scan);
        createPanel.setVisibility(scan ? View.GONE : View.VISIBLE);
        scanPanel.setVisibility(scan ? View.VISIBLE : View.GONE);
        LinearLayout active = scan ? scanPanel : createPanel;
        active.animate().cancel();
        active.setAlpha(1f);
        if (transitions && ValueAnimator.areAnimatorsEnabled()) {
            active.setAlpha(0.65f);
            active.animate().alpha(1f).setDuration(140).start();
        }
        scroll.scrollTo(0, 0);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        // Only navigation state survives recreation. QR payloads are never saved by the app.
        state.putBoolean("scan_mode", showingScan);
        super.onSaveInstanceState(state);
    }

    private void hideKeyboard() {
        if (input.hasFocus()) {
            InputMethodManager keyboard = getSystemService(InputMethodManager.class);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            input.clearFocus();
        }
    }

    private void updateInput() {
        String value = input.getText().toString();
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        byteCount.setText(getString(R.string.byte_count, bytes, MAX_BYTES));
        generate.setEnabled(!value.trim().isEmpty() && bytes <= MAX_BYTES);
        input.setError(bytes > MAX_BYTES ? getString(R.string.input_limit) : null);
    }

    private void resetPreview() {
        qrImage.setImageDrawable(null);
        qrImage.setVisibility(View.GONE);
        emptyImage.setVisibility(View.VISIBLE);
        previewHint.setText(R.string.preview_empty);
    }

    private void generate() {
        try {
            BitMatrix bits = QrCodec.encode(input.getText().toString(), 512);
            Bitmap bitmap = Bitmap.createBitmap(QrCodec.pixels(bits), bits.getWidth(), bits.getHeight(),
                    Bitmap.Config.ARGB_8888);
            qrImage.setImageBitmap(bitmap);
            emptyImage.setVisibility(View.GONE);
            qrImage.setVisibility(View.VISIBLE);
            previewHint.setText(R.string.preview_ready);
            hideKeyboard();
            scroll.smoothScrollTo(0, 0);
        } catch (Exception error) {
            resetPreview();
            previewHint.setText(R.string.input_limit);
        }
    }

    private void requestScan() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            scanStatus.setText(R.string.no_camera);
            return;
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            try { requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST); }
            catch (RuntimeException error) { scanStatus.setText(R.string.permission_failed); }
            return;
        }
        launchScan();
    }

    private void launchScan() {
        try { startActivityForResult(new Intent(this, ScannerActivity.class), SCAN_REQUEST); }
        catch (RuntimeException error) { scanStatus.setText(R.string.camera_unavailable); }
    }

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != CAMERA_REQUEST) return;
        if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) launchScan();
        else scanStatus.setText(R.string.permission_required);
    }

    @Override
    protected void onActivityResult(int request, int code, Intent data) {
        super.onActivityResult(request, code, data);
        if (request != SCAN_REQUEST) return;
        showMode(true, false);
        String value = data == null ? null : data.getStringExtra(ScannerActivity.EXTRA_TEXT);
        scanned = code == RESULT_OK && value != null ? value : "";
        if (scanned.isEmpty()) {
            scannedResult.setText(R.string.result_empty);
            scanStatus.setText(data != null && data.getBooleanExtra(ScannerActivity.EXTRA_ERROR, false)
                    ? R.string.camera_failed : R.string.scan_cancelled);
        } else {
            scannedResult.setText(scanned);
            scanStatus.setText(R.string.scan_privacy);
        }
        copy.setEnabled(!scanned.isEmpty());
    }

    private void copyResult() {
        if (scanned.isEmpty()) return;
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("QR", scanned));
            scanStatus.setText(R.string.result_copied);
        }
    }
}
