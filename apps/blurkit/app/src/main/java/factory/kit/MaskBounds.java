package factory.kit;

/** Maps a normalized selection to whole pixels, rounding outward for complete coverage. */
final class MaskBounds {
    private MaskBounds() {}

    static int[] pixels(float left, float top, float right, float bottom, int width, int height) {
        if (width <= 0 || height <= 0 || !Float.isFinite(left) || !Float.isFinite(top)
                || !Float.isFinite(right) || !Float.isFinite(bottom)
                || left >= right || top >= bottom) {
            throw new IllegalArgumentException("Invalid redaction bounds");
        }
        return new int[]{
                (int) Math.floor(clamp(left) * width),
                (int) Math.floor(clamp(top) * height),
                (int) Math.ceil(clamp(right) * width),
                (int) Math.ceil(clamp(bottom) * height)
        };
    }

    private static double clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }
}
