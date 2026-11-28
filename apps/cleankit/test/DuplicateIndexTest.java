package factory.kit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Arrays;

public final class DuplicateIndexTest {
    public static void main(String[] args) throws IOException {
        DuplicateIndex<String> index = new DuplicateIndex<>();
        byte[] photo = new byte[80_000];
        Arrays.fill(photo, (byte) 7);
        byte[] different = photo.clone();
        different[different.length - 1] = 8;

        equal(null, index.add(stream(photo), "first"));
        equal("first", index.add(stream(photo.clone()), "second"));
        equal(null, index.add(stream(different), "different"));
        equal("first", index.add(stream(photo.clone()), "third"));
        equal("different", index.add(stream(different.clone()), "different-copy"));

        try {
            index.add(new InputStream() {
                @Override public int read() throws IOException {
                    throw new IOException("unreadable");
                }
            }, "failed");
            throw new AssertionError("Expected the source error");
        } catch (IOException expected) {
            // A failed read leaves the prior match unchanged.
        }
        equal("first", index.add(stream(photo), "after-failure"));

        Thread.currentThread().interrupt();
        try {
            index.add(stream(different), "cancelled");
            throw new AssertionError("Expected an interrupted scan");
        } catch (InterruptedIOException expected) {
            // Cancellation does not register a partial hash.
        } finally {
            Thread.interrupted();
        }
        equal("different", index.add(stream(different), "after-interruption"));
        System.out.println("CleanKit: duplicate grouping, distinct bytes, input errors and interruption checked");
    }

    private static InputStream stream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private static void equal(String expected, String actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
