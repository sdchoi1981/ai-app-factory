package factory.kit;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import factory.ui.KitUi;
import factory.ui.SystemInsets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int PICK = 1;
    private static final int SAVE = 2;
    private static final int MAX_SIDE = 2048;
    private static final int MAX_MARKS = 100;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<RectF> marks = new ArrayList<>();
    private KitUi ui;
    private MaskView preview;
    private TextView status;
    private TextView markCount;
    private Button pickButton;
    private Button undoButton;
    private Button saveButton;
    private Bitmap current;
    private File pendingOutput;
    private boolean busy;
    private boolean closed;

    private String t(String en,String ko,String ja) {
        String lang = getResources().getConfiguration().getLocales().get(0).getLanguage();
        return "ko".equals(lang) ? ko : "ja".equals(lang) ? ja : en;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        ui = new KitUi(this, 0xff804180, 0xffefb7ec);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ui.background);
        LinearLayout page = ui.column();
        FrameLayout frame = new FrameLayout(this);
        frame.addView(page, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        scroll.addView(frame);
        setContentView(scroll);
        SystemInsets.apply(this, scroll);
        page.addView(ui.text(getString(R.string.app_name), 32, true));
        page.addView(ui.text(t("Cover private details with solid black rectangles before sharing.",
                "공유하기 전에 민감한 정보를 검은색 사각형으로 가리세요.",
                "共有前に個人情報を黒い四角形で隠してください。"), 15, false));

        LinearLayout photo = ui.card(page);
        preview = new MaskView();
        preview.setContentDescription(t("Photo redaction preview", "사진 가림 미리보기", "写真の黒塗りプレビュー"));
        ui.preview(preview);
        photo.addView(preview, ui.space(ui.dp(320), 0, 8));
        pickButton = button(photo, t("Choose photo", "사진 선택", "写真を選択"), true, this::pick);

        LinearLayout actions = ui.card(page);
        markCount = ui.text("", 15, false);
        actions.addView(markCount);
        markCount.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        undoButton = button(actions, t("Undo last area", "마지막 영역 취소", "最後の範囲を取消"),
                false, () -> {
                    if (!marks.isEmpty()) marks.remove(marks.size() - 1);
                    updateControls();
                });
        saveButton = button(actions, t("Save redacted image", "가린 사진 저장", "黒塗り画像を保存"),
                true, this::exportImage);
        status = ui.text("", 15, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        actions.addView(status);
        status.setText(t("Choose a photo, then drag to cover a private area.",
                "사진을 선택한 뒤 드래그하여 가릴 영역을 지정하세요.",
                "写真を選び、隠したい範囲をドラッグしてください。"));
        page.addView(ui.text(t("Export creates a new PNG without source metadata. Check that all private details are covered.",
                "내보낸 PNG에는 원본 메타데이터가 복사되지 않습니다. 민감한 정보가 모두 가려졌는지 확인하세요.",
                "出力するPNGには元のメタデータを含みません。個人情報がすべて隠れているか確認してください。"), 14, false));
        updateControls();
    }

    private Button button(LinearLayout container, String text, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        ui.button(button, primary);
        button.setOnClickListener(v -> action.run());
        container.addView(button, ui.space(-2, 3, 4));
        return button;
    }

    private void updateControls() {
        pickButton.setEnabled(!busy);
        undoButton.setEnabled(!busy && !marks.isEmpty());
        saveButton.setEnabled(!busy && current != null && !marks.isEmpty());
        markCount.setText(String.format(getResources().getConfiguration().getLocales().get(0),
                t("Covered areas: %d", "가린 영역: %d개", "黒塗り範囲: %dか所"), marks.size()));
        preview.invalidate();
    }

    private void pick() {
        if (busy) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(intent, PICK);
        } catch (ActivityNotFoundException e) {
            status.setText(t("No file picker", "파일 선택기 없음", "ファイル選択不可"));
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        Uri uri = result == RESULT_OK && data != null ? data.getData() : null;
        if (request == PICK && uri != null) load(uri);
        if (request == SAVE) {
            File source = pendingOutput;
            pendingOutput = null;
            if (source != null && uri != null) writeDocument(source, uri);
            else {
                if (source != null) source.delete();
                busy = false;
                status.setText(t("Save canceled", "저장 취소됨", "保存を中止しました"));
                updateControls();
            }
        }
    }

    private void load(Uri uri) {
        busy = true;
        status.setText(t("Opening photo…", "사진을 여는 중…", "写真を開いています…"));
        updateControls();
        worker.execute(() -> {
            Bitmap decoded = null;
            try {
                decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri),
                        (decoder, info, source) -> {
                            int width = info.getSize().getWidth();
                            int height = info.getSize().getHeight();
                            double scale = Math.min(1d, MAX_SIDE / (double) Math.max(width, height));
                            decoder.setTargetSize(Math.max(1, (int) (width * scale)),
                                    Math.max(1, (int) (height * scale)));
                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        });
            } catch (IOException | RuntimeException | OutOfMemoryError ignored) {
                // A failed pick must leave the existing image and redactions intact.
            }
            Bitmap ready = decoded;
            runOnUiThread(() -> {
                if (closed) {
                    if (ready != null) ready.recycle();
                    return;
                }
                busy = false;
                if (ready == null) {
                    status.setText(t("Could not open photo", "사진을 열 수 없습니다", "写真を開けません"));
                } else {
                    if (current != null) current.recycle();
                    current = ready;
                    marks.clear();
                    preview.clearDrag();
                    status.setText(t("Drag across a private area to cover it.",
                            "가릴 영역을 드래그하세요.", "隠す範囲をドラッグしてください。"));
                }
                updateControls();
            });
        });
    }

    private void exportImage() {
        if (busy || current == null || marks.isEmpty()) return;
        Bitmap snapshot;
        try {
            snapshot = current.copy(Bitmap.Config.ARGB_8888, true);
            if (snapshot == null) throw new IllegalStateException("Could not copy image");
        } catch (RuntimeException | OutOfMemoryError e) {
            exportFailed();
            return;
        }
        ArrayList<RectF> exportMarks = new ArrayList<>(marks);
        busy = true;
        status.setText(t("Preparing PNG…", "PNG 생성 중…", "PNGを準備中…"));
        updateControls();
        worker.execute(() -> {
            File temporary = null;
            try {
                Canvas canvas = new Canvas(snapshot);
                Paint black = new Paint();
                black.setColor(Color.BLACK);
                for (RectF mark : exportMarks) {
                    int[] bounds = MaskBounds.pixels(mark.left, mark.top, mark.right, mark.bottom,
                            snapshot.getWidth(), snapshot.getHeight());
                    canvas.drawRect(bounds[0], bounds[1], bounds[2], bounds[3], black);
                }
                temporary = File.createTempFile("blurkit-", ".png", getCacheDir());
                try (OutputStream output = new FileOutputStream(temporary)) {
                    if (!snapshot.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new IOException();
                }
                File ready = temporary;
                runOnUiThread(() -> chooseDestination(ready));
            } catch (IOException | RuntimeException | OutOfMemoryError e) {
                if (temporary != null) temporary.delete();
                runOnUiThread(this::exportFailed);
            } finally {
                snapshot.recycle();
            }
        });
    }

    private void chooseDestination(File ready) {
        if (closed) {
            ready.delete();
            return;
        }
        pendingOutput = ready;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("image/png");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "BlurKit.png");
        try {
            startActivityForResult(intent, SAVE);
        } catch (ActivityNotFoundException e) {
            pendingOutput = null;
            ready.delete();
            exportFailed();
        }
    }

    private void writeDocument(File source, Uri uri) {
        worker.execute(() -> {
            try (InputStream input = new FileInputStream(source);
                 OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                if (output == null) throw new IOException();
                byte[] buffer = new byte[32768];
                for (int count; (count = input.read(buffer)) != -1;) output.write(buffer, 0, count);
            } catch (IOException | RuntimeException e) {
                try {
                    DocumentsContract.deleteDocument(getContentResolver(), uri);
                } catch (IOException | RuntimeException ignored) {
                    // A provider may not support deleting its incomplete document.
                }
                runOnUiThread(this::exportFailed);
                return;
            } finally {
                source.delete();
            }
            runOnUiThread(() -> {
                if (closed) return;
                busy = false;
                status.setText(t("Saved redacted PNG", "가린 PNG 저장 완료", "黒塗りPNGを保存しました"));
                updateControls();
            });
        });
    }

    private void exportFailed() {
        if (closed) return;
        busy = false;
        status.setText(t("Could not save image", "이미지를 저장할 수 없습니다", "画像を保存できません"));
        updateControls();
    }

    @Override protected void onDestroy() {
        closed = true;
        worker.shutdown();
        if (pendingOutput != null) pendingOutput.delete();
        if (current != null) current.recycle();
        current = null;
        super.onDestroy();
    }

    private final class MaskView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final RectF box = new RectF();
        private float startX;
        private float startY;
        private RectF drag;

        MaskView() { super(MainActivity.this); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (current == null) {
                box.setEmpty();
                paint.setColor(ui.muted);
                paint.setTextSize(ui.dp(16));
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(t("No photo selected", "선택한 사진 없음", "写真が未選択です"),
                        getWidth() / 2f, getHeight() / 2f, paint);
                return;
            }
            float scale = Math.min(getWidth() / (float) current.getWidth(), getHeight() / (float) current.getHeight());
            float w = current.getWidth() * scale;
            float h = current.getHeight() * scale;
            box.set((getWidth() - w) / 2, (getHeight() - h) / 2, (getWidth() + w) / 2, (getHeight() + h) / 2);
            paint.setColor(Color.BLACK);
            canvas.drawBitmap(current, null, box, paint);
            for (RectF mark : marks) drawMark(canvas, mark);
            if (drag != null) drawMark(canvas, drag);
        }

        private void drawMark(Canvas canvas, RectF mark) {
            canvas.drawRect(box.left + mark.left * box.width(), box.top + mark.top * box.height(),
                    box.left + mark.right * box.width(), box.top + mark.bottom * box.height(), paint);
        }

        private float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }

        void clearDrag() { drag = null; }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (current == null || busy || box.isEmpty()) return false;
            float x = clamp((event.getX() - box.left) / box.width());
            float y = clamp((event.getY() - box.top) / box.height());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (!box.contains(event.getX(), event.getY())) return false;
                    if (marks.size() >= MAX_MARKS) {
                        status.setText(t("Undo an area before adding another.",
                                "영역을 추가하려면 먼저 기존 가림을 취소하세요.",
                                "範囲を追加するには先に黒塗りを取り消してください。"));
                        return true;
                    }
                    getParent().requestDisallowInterceptTouchEvent(true);
                    startX = x;
                    startY = y;
                    drag = new RectF(x, y, x, y);
                    return true;
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    if (drag == null) return true;
                    drag.set(Math.min(startX, x), Math.min(startY, y), Math.max(startX, x), Math.max(startY, y));
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        if (drag.width() > .002f && drag.height() > .002f) marks.add(drag);
                        clearDrag();
                        getParent().requestDisallowInterceptTouchEvent(false);
                        updateControls();
                        performClick();
                    } else invalidate();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_POINTER_DOWN:
                    clearDrag();
                    getParent().requestDisallowInterceptTouchEvent(false);
                    invalidate();
                    return true;
                default:
                    return true;
            }
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }
    }
}
