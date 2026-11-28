package factory.kit;

import android.app.Activity;
import android.content.ClipData;
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
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import factory.ui.KitUi;
import factory.ui.SystemInsets;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public final class MainActivity extends Activity {
    private static final int PICK = 1, SAVE = 2, MAX_FRAMES = 20;
    private final List<Frame> frames = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private KitUi ui;
    private LinearLayout strip;
    private TextView status, empty;
    private Button choose, create;
    private EditText size, delay;
    private File pending;
    private boolean busy, closed;

    private record Frame(Uri uri, Bitmap preview) {}

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ui = new KitUi(this, 0xff3155b7, 0xffa9bfff);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ui.background);
        LinearLayout page = ui.column();
        FrameLayout shell = new FrameLayout(this);
        shell.addView(page, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        scroll.addView(shell);
        setContentView(scroll);
        SystemInsets.apply(this, scroll);

        text(page, getString(R.string.app_name), 30, true);
        text(page, t("Make a looping GIF from photos on your device.",
                "기기의 사진으로 반복 재생 GIF를 만듭니다.", "端末の写真からループGIFを作成します。"), 16, false);
        LinearLayout card = ui.card(page);
        text(card, t("Frame order", "프레임 순서", "フレーム順"), 20, true);
        empty = text(card, t("Choose 2–20 photos to see and arrange the frames.",
                "사진을 2~20장 선택해 순서를 확인하고 바꿀 수 있습니다.", "写真を2〜20枚選び、順序を確認・変更できます。"), 14, false);
        HorizontalScrollView track = new HorizontalScrollView(this);
        track.setHorizontalScrollBarEnabled(true);
        strip = new LinearLayout(this);
        track.addView(strip);
        card.addView(track, ui.space(-2, 4, 6));
        choose = button(card, t("Choose photos", "사진 선택", "写真を選択"), this::choosePhotos, true);

        LinearLayout settings = ui.card(page);
        size = number(settings, t("Maximum frame side (64–480 px)", "프레임 최대 변 (64–480 px)", "フレームの最大辺（64〜480 px）"), "320");
        delay = number(settings, t("Frame duration (50–3000 ms)", "프레임 간격 (50–3000 ms)", "フレーム間隔（50〜3000 ms）"), "300");
        text(settings, t("Photos fit within a square without cropping. GIFs use a 256-color palette.",
                "사진을 자르지 않고 정사각형에 맞춥니다. GIF는 256색을 사용합니다.",
                "写真は切り抜かず正方形に収めます。GIFは256色です。"), 14, false);
        create = button(settings, t("Create GIF", "GIF 만들기", "GIFを作成"), this::export, true);
        status = text(settings, "", 15, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        updateControls();
    }

    private void choosePhotos() {
        if (busy) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(intent, PICK);
        } catch (RuntimeException e) {
            status.setText(t("No photo picker is available.", "사진 선택기를 열 수 없습니다.", "写真選択画面を開けません。"));
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK && result == RESULT_OK && data != null) {
            LinkedHashSet<Uri> selection = new LinkedHashSet<>();
            ClipData clip = data.getClipData();
            if (clip != null) {
                if (clip.getItemCount() > MAX_FRAMES) {
                    status.setText(t("Choose at most 20 photos.", "사진은 최대 20장까지 선택할 수 있습니다.", "写真は20枚まで選べます。"));
                    return;
                }
                for (int i = 0; i < clip.getItemCount(); i++) {
                    Uri uri = clip.getItemAt(i).getUri();
                    if (uri != null) selection.add(uri);
                }
            } else if (data.getData() != null) {
                selection.add(data.getData());
            }
            if (!selection.isEmpty()) loadFrames(selection);
        } else if (request == SAVE && pending != null) {
            File source = pending;
            pending = null;
            if (result == RESULT_OK && data != null && data.getData() != null) {
                save(source, data.getData());
            } else {
                source.delete();
                finish(t("Save cancelled.", "저장이 취소되었습니다.", "保存をキャンセルしました。"));
            }
        }
    }

    private void loadFrames(LinkedHashSet<Uri> selection) {
        begin(t("Loading previews…", "미리보기 불러오는 중…", "プレビューを読み込み中…"));
        worker.execute(() -> {
            List<Frame> loaded = new ArrayList<>();
            for (Uri uri : selection) {
                if (Thread.currentThread().isInterrupted()) break;
                Bitmap bitmap = null;
                try {
                    bitmap = decode(uri, 160);
                } catch (IOException | RuntimeException | OutOfMemoryError ignored) {}
                loaded.add(new Frame(uri, bitmap));
            }
            runOnUiThread(() -> {
                if (closed) return;
                frames.clear();
                frames.addAll(loaded);
                finish(loaded.size() < 2
                        ? t("Select at least two photos.", "사진을 두 장 이상 선택하세요.", "写真を2枚以上選んでください。")
                        : t("Check the frame order before exporting.", "내보내기 전에 프레임 순서를 확인하세요.", "書き出す前にフレームの順序を確認してください。"));
            });
        });
    }

    private void export() {
        if (busy || frames.size() < 2) return;
        int side;
        int milliseconds;
        try {
            side = readNumber(size, 64, 480);
            milliseconds = readNumber(delay, 50, 3000);
        } catch (IllegalArgumentException e) {
            status.setText(t("Enter a side of 64–480 px and a duration of 50–3000 ms.",
                    "프레임 크기 64~480px, 간격 50~3000ms를 입력하세요.", "最大辺64〜480 px、間隔50〜3000 msを入力してください。"));
            return;
        }
        List<Frame> ordered = new ArrayList<>(frames);
        begin(t("Creating GIF…", "GIF 만드는 중…", "GIFを作成中…"));
        worker.execute(() -> {
            File file = null;
            try {
                file = File.createTempFile("gifkit-export-", ".tmp", getCacheDir());
                writeGif(file, ordered, side, milliseconds);
                File completed = file;
                runOnUiThread(() -> chooseDestination(completed));
            } catch (IOException | RuntimeException | OutOfMemoryError e) {
                if (file != null) file.delete();
                runOnUiThread(() -> finish(t("Could not create the GIF. Check the selected photos.",
                        "GIF를 만들지 못했습니다. 선택한 사진을 확인하세요.", "GIFを作成できません。選択した写真を確認してください。")));
            }
        });
    }

    private void writeGif(File file, List<Frame> ordered, int side, int milliseconds) throws IOException {
        Bitmap square = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file));
                GifWriter gif = new GifWriter(output, side, side)) {
            Canvas canvas = new Canvas(square);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            int[] pixels = new int[side * side];
            for (Frame frame : ordered) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                Bitmap bitmap = decode(frame.uri(), side);
                try {
                    canvas.drawColor(Color.WHITE);
                    float scale = Math.min(side / (float) bitmap.getWidth(), side / (float) bitmap.getHeight());
                    float w = bitmap.getWidth() * scale;
                    float h = bitmap.getHeight() * scale;
                    canvas.drawBitmap(bitmap, null, new RectF((side - w) / 2, (side - h) / 2,
                            (side + w) / 2, (side + h) / 2), paint);
                    square.getPixels(pixels, 0, side, 0, 0, side, side);
                    gif.frame(pixels, milliseconds);
                } finally {
                    bitmap.recycle();
                }
            }
        } finally {
            square.recycle();
        }
    }

    private void chooseDestination(File completed) {
        if (closed) {
            completed.delete();
            return;
        }
        pending = completed;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("image/gif")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "GifKit.gif");
        try {
            startActivityForResult(intent, SAVE);
        } catch (RuntimeException e) {
            pending = null;
            completed.delete();
            finish(t("No save picker is available.", "저장 선택기를 열 수 없습니다.", "保存先の選択画面を開けません。"));
        }
    }

    private void save(File source, Uri destination) {
        begin(t("Saving GIF…", "GIF 저장 중…", "GIFを保存中…"));
        worker.execute(() -> {
            boolean saved = false;
            try {
                try (InputStream input = new FileInputStream(source);
                        OutputStream output = getContentResolver().openOutputStream(destination, "wt")) {
                    if (output == null) throw new IOException();
                    byte[] buffer = new byte[32768];
                    for (int n; (n = input.read(buffer)) != -1;) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                        output.write(buffer, 0, n);
                    }
                }
                saved = true;
            } catch (IOException | RuntimeException e) {
                try {
                    DocumentsContract.deleteDocument(getContentResolver(), destination);
                } catch (IOException | RuntimeException ignored) {}
            } finally {
                source.delete();
            }
            boolean success = saved;
            runOnUiThread(() -> finish(success
                    ? t("GIF saved.", "GIF를 저장했습니다.", "GIFを保存しました。")
                    : t("Could not save the GIF.", "GIF를 저장하지 못했습니다.", "GIFを保存できませんでした。")));
        });
    }

    private Bitmap decode(Uri uri, int maxSide) throws IOException {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri),
                (decoder, info, source) -> {
                    int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                    double scale = Math.min(1, maxSide / (double) Math.max(w, h));
                    decoder.setTargetSize(Math.max(1, (int) (w * scale)), Math.max(1, (int) (h * scale)));
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
    }

    private void renderFrames() {
        strip.removeAllViews();
        empty.setVisibility(frames.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < frames.size(); i++) {
            int index = i;
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams width = new LinearLayout.LayoutParams(ui.dp(132), -2);
            width.setMarginEnd(ui.dp(8));
            strip.addView(column, width);
            String position = t("Frame ", "프레임 ", "フレーム ") + (i + 1);
            Bitmap bitmap = frames.get(i).preview();
            if (bitmap != null) {
                ImageView image = new ImageView(this);
                image.setImageBitmap(bitmap);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setContentDescription(position);
                ui.preview(image);
                column.addView(image, ui.space(ui.dp(116), 0, 0));
            } else {
                TextView placeholder = ui.text(t("Preview unavailable", "미리보기 없음", "プレビュー不可"), 14, false);
                placeholder.setGravity(Gravity.CENTER);
                ui.preview(placeholder);
                column.addView(placeholder, ui.space(ui.dp(116), 0, 0));
            }
            text(column, position, 14, true);
            Button move = button(column, t("Move earlier", "앞으로 이동", "前へ移動"), () -> {
                if (busy || index == 0) return;
                frames.add(index - 1, frames.remove(index));
                renderFrames();
                status.setText(position + t(" moved earlier.", "을 앞으로 이동했습니다.", "を前へ移動しました。"));
            }, false);
            move.setContentDescription(position + " · " + move.getText());
            move.setEnabled(i > 0 && !busy);
        }
    }

    private void begin(String message) {
        busy = true;
        status.setText(message);
        updateControls();
    }

    private void finish(String message) {
        if (closed) return;
        busy = false;
        status.setText(message);
        updateControls();
    }

    private void updateControls() {
        choose.setEnabled(!busy);
        create.setEnabled(!busy && frames.size() >= 2);
        size.setEnabled(!busy);
        delay.setEnabled(!busy);
        renderFrames();
    }

    private EditText number(LinearLayout parent, String label, String initial) {
        TextView title = text(parent, label, 15, false);
        EditText field = new EditText(this);
        field.setId(View.generateViewId());
        title.setLabelFor(field.getId());
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setSingleLine(true);
        field.setText(initial);
        ui.field(field);
        parent.addView(field, ui.space(-2, 0, 8));
        return field;
    }

    private int readNumber(EditText field, int min, int max) {
        int value = Integer.parseInt(field.getText().toString().trim());
        if (value < min || value > max) throw new IllegalArgumentException();
        return value;
    }

    private TextView text(LinearLayout parent, String value, int size, boolean heading) {
        TextView view = ui.text(value, size, heading);
        if (heading) view.setAccessibilityHeading(true);
        parent.addView(view);
        return view;
    }

    private Button button(LinearLayout parent, String label, Runnable action, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        ui.button(button, primary);
        button.setOnClickListener(v -> action.run());
        parent.addView(button, ui.space(-2, 3, 3));
        return button;
    }

    private String t(String en, String ko, String ja) {
        return switch (getResources().getConfiguration().getLocales().get(0).getLanguage()) {
            case "ko" -> ko;
            case "ja" -> ja;
            default -> en;
        };
    }

    @Override protected void onDestroy() {
        closed = true;
        worker.shutdownNow();
        if (pending != null) pending.delete();
        super.onDestroy();
    }
}
