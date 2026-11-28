package factory.kit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/** Identifies byte-for-byte candidates without retaining any photo bytes. */
final class DuplicateIndex<T> {
    private final Map<ByteBuffer, T> seen = new HashMap<>();

    T add(InputStream input, T photo) throws IOException {
        if (input == null) throw new IOException("unreadable photo");
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] buffer = new byte[32768];
        for (int count; (count = input.read(buffer)) != -1;) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            digest.update(buffer, 0, count);
        }
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
        return seen.putIfAbsent(ByteBuffer.wrap(digest.digest()).asReadOnlyBuffer(), photo);
    }
}
