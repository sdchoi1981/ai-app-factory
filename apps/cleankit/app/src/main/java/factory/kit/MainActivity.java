package factory.kit;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import factory.ui.KitUi;
import factory.ui.SystemInsets;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public final class MainActivity extends Activity {
    private static final int PICK = 1, MAX_PHOTOS = 20;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private List<Photo> selected = new ArrayList<>();
    private KitUi ui;
    private LinearLayout strip, results;
    private TextView empty, status;
    private Button choose, scan;
    private boolean busy, closed;

    private record Photo(Uri uri, String name, Bitmap preview) {}
    private record Match(Photo original, Photo copy) {}

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ui = new KitUi(this, 0xff246856, 0xff96e1ca);
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
        text(page, t("Check selected photos for exact duplicate files. No files are changed or deleted.",
                "선택한 사진에서 내용이 완전히 같은 파일을 찾습니다. 파일을 변경하거나 삭제하지 않습니다.",
                "選択した写真から内容が完全に同じファイルを探します。ファイルの変更や削除は行いません。"), 16, false);
        LinearLayout card = ui.card(page);
        text(card, t("Selected photos", "선택한 사진", "選択した写真"), 20, true);
        empty = text(card, t("Choose 2–20 photos to compare.",
                "비교할 사진을 2~20장 선택하세요.", "比較する写真を2〜20枚選択してください。"), 14, false);
        HorizontalScrollView track = new HorizontalScrollView(this);
        strip = new LinearLayout(this);
        track.addView(strip);
        card.addView(track, ui.space(-2, 4, 6));
        choose = button(card, t("Choose photos", "사진 선택", "写真を選択"), this::choosePhotos, false);
        scan = button(card, t("Find duplicates", "중복 찾기", "重複を検索"), this::scanPhotos, true);
        status = text(card, "", 15, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        page.addView(results);
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
        if (request != PICK || result != RESULT_OK || data == null || busy) return;
        LinkedHashSet<Uri> files = new LinkedHashSet<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            if (clip.getItemCount() > MAX_PHOTOS) {
                status.setText(t("Choose at most 20 photos.", "사진은 최대 20장까지 선택할 수 있습니다.", "写真は20枚まで選べます。"));
                return;
            }
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null) files.add(uri);
            }
        } else if (data.getData() != null) {
            files.add(data.getData());
        }
        if (!files.isEmpty()) loadSelection(files);
    }

    private void loadSelection(LinkedHashSet<Uri> files) {
        begin(t("Loading photos…", "사진 불러오는 중…", "写真を読み込み中…"));
        results.removeAllViews();
        String fallback = t("Photo ", "사진 ", "写真 ");
        worker.execute(() -> {
            List<Photo> loaded = new ArrayList<>();
            for (Uri uri : files) {
                if (Thread.currentThread().isInterrupted()) return;
                loaded.add(new Photo(uri, displayName(uri, fallback + (loaded.size() + 1)), preview(uri)));
            }
            runOnUiThread(() -> {
                if (closed) return;
                selected = loaded;
                renderSelection();
                finish(loaded.size() < 2
                        ? t("Select at least two photos.", "사진을 두 장 이상 선택하세요.", "写真を2枚以上選んでください。")
                        : t("Photos selected: ", "선택한 사진: ", "選択した写真: ") + loaded.size());
            });
        });
    }

    private String displayName(Uri uri, String fallback) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null) {
                    name = name.replaceAll("[\\p{Cc}\\p{Cf}]", "").trim();
                    if (!name.isEmpty()) {
                        int count = name.codePointCount(0, name.length());
                        return count <= 100 ? name : name.substring(0, name.offsetByCodePoints(0, 100)) + "…";
                    }
                }
            }
        } catch (RuntimeException ignored) {}
        return fallback;
    }

    private Bitmap preview(Uri uri) {
        try {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri),
                    (decoder, info, source) -> {
                        int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                        double scale = Math.min(1, 144d / Math.max(w, h));
                        decoder.setTargetSize(Math.max(1, (int) (w * scale)), Math.max(1, (int) (h * scale)));
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    });
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    private void scanPhotos() {
        if (busy || selected.size() < 2) return;
        List<Photo> photos = new ArrayList<>(selected);
        begin(t("Comparing file contents…", "파일 내용 비교 중…", "ファイルの内容を比較中…"));
        results.removeAllViews();
        worker.execute(() -> {
            List<Match> matches = new ArrayList<>();
            DuplicateIndex<Photo> index = new DuplicateIndex<>();
            int errors = 0;
            for (Photo photo : photos) {
                if (Thread.currentThread().isInterrupted()) return;
                try (InputStream input = getContentResolver().openInputStream(photo.uri())) {
                    Photo original = index.add(input, photo);
                    if (original != null) matches.add(new Match(original, photo));
                } catch (InterruptedIOException e) {
                    if (Thread.currentThread().isInterrupted()) return;
                    errors++;
                } catch (IOException | RuntimeException e) {
                    errors++;
                }
            }
            int unreadable = errors;
            runOnUiThread(() -> {
                if (closed) return;
                renderMatches(matches);
                finish(t("Exact duplicate copies: ", "완전 중복 사본: ", "完全一致のコピー: ") + matches.size()
                        + t(" · Unreadable: ", " · 읽기 실패: ", " · 読取不可: ") + unreadable);
            });
        });
    }

    private void renderSelection() {
        strip.removeAllViews();
        empty.setVisibility(selected.isEmpty() ? View.VISIBLE : View.GONE);
        for (Photo photo : selected) {
            LinearLayout.LayoutParams width = new LinearLayout.LayoutParams(ui.dp(118), -2);
            width.setMarginEnd(ui.dp(8));
            strip.addView(photoTile(photo, ""), width);
        }
    }

    private void renderMatches(List<Match> matches) {
        for (Match match : matches) {
            LinearLayout card = ui.card(results);
            LinearLayout pair = new LinearLayout(this);
            card.addView(pair);
            LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, -2, 1);
            first.setMarginEnd(ui.dp(8));
            pair.addView(photoTile(match.original(), t("First selected", "먼저 선택한 사진", "最初に選んだ写真")), first);
            pair.addView(photoTile(match.copy(), t("Exact copy", "완전 중복", "完全一致")), new LinearLayout.LayoutParams(0, -2, 1));
        }
    }

    private LinearLayout photoTile(Photo photo, String role) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        if (!role.isEmpty()) text(tile, role, 14, true);
        View thumbnail;
        if (photo.preview() == null) {
            TextView placeholder = ui.text(t("Preview unavailable", "미리보기 없음", "プレビュー不可"), 14, false);
            placeholder.setGravity(Gravity.CENTER);
            thumbnail = placeholder;
        } else {
            ImageView image = new ImageView(this);
            image.setImageBitmap(photo.preview());
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            thumbnail = image;
        }
        ui.preview(thumbnail);
        tile.addView(thumbnail, ui.space(ui.dp(104), 0, 0));
        TextView name = text(tile, photo.name(), 14, false);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return tile;
    }

    private void begin(String message) {
        busy = true;
        status.setText(message);
        updateControls();
    }

    private void finish(String message) {
        busy = false;
        status.setText(message);
        updateControls();
    }

    private void updateControls() {
        choose.setEnabled(!busy);
        scan.setEnabled(!busy && selected.size() >= 2);
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
        super.onDestroy();
    }
}
