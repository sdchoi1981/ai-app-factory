package com.factory.passportkit;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Publishes completed JPEG exports to Pictures/PassportKit. Call on a worker thread. */
public final class GalleryExport {
    private static final String FOLDER = "PassportKit";
    private static final String JPEG = "image/jpeg";

    private GalleryExport() {}

    public static Uri save(Context context, byte[] bytes, boolean sheet) throws IOException {
        if (bytes == null || bytes.length == 0) throw new IOException("JPEG export is empty");
        String name = (sheet ? "passport-sheet-" : "passport-photo-") + UUID.randomUUID() + ".jpg";
        if (Build.VERSION.SDK_INT >= 29) return saveToMediaStore(context, bytes, name);
        return saveLegacy(context, bytes, name);
    }

    @TargetApi(29)
    private static Uri saveToMediaStore(Context context, byte[] bytes, String name) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Images.Media.MIME_TYPE, JPEG);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + FOLDER);
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri uri = resolver.insert(collection, values);
        if (uri == null) throw new IOException("MediaStore did not create an image");
        try {
            try (OutputStream output = resolver.openOutputStream(uri)) {
                if (output == null) throw new IOException("MediaStore did not open the image");
                output.write(bytes);
            }
            ContentValues published = new ContentValues();
            published.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (resolver.update(uri, published, null, null) != 1) {
                throw new IOException("MediaStore did not publish the image");
            }
            return uri;
        } catch (IOException | RuntimeException failure) {
            try {
                resolver.delete(uri, null, null);
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    @SuppressWarnings("deprecation")
    private static Uri saveLegacy(Context context, byte[] bytes, String name) throws IOException {
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                FOLDER);
        if (!folder.isDirectory() && !folder.mkdirs() && !folder.isDirectory()) {
            throw new IOException("Pictures/PassportKit is unavailable");
        }
        File target = new File(folder, name);
        Uri shareUri = FileProvider.getUriForFile(context, context.getPackageName() + ".gallery", target);
        File pending = File.createTempFile(".passportkit-", ".tmp", folder);
        try {
            try (FileOutputStream output = new FileOutputStream(pending)) {
                output.write(bytes);
                output.getFD().sync();
            }
            Files.move(pending.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException failure) {
            try {
                Files.deleteIfExists(pending.toPath());
            } catch (IOException | RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        try {
            MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{target.getPath()},
                    new String[]{JPEG}, null);
        } catch (RuntimeException ignored) {
            // The complete image can still be opened or shared via its narrowly scoped URI.
        }
        return shareUri;
    }

    /** Separate authority keeps the legacy Pictures path out of the camera provider. */
    public static final class Provider extends FileProvider {}
}
