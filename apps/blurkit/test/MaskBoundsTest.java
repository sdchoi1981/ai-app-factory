package factory.kit;

import java.util.Arrays;

public final class MaskBoundsTest {
    public static void main(String[] args) {
        expect(new int[]{0, 0, 200, 100}, MaskBounds.pixels(0, 0, 1, 1, 200, 100));
        expect(new int[]{1, 0, 4, 3}, MaskBounds.pixels(.26f, .10f, .99f, .74f, 4, 4));
        expect(new int[]{0, 0, 1, 1}, MaskBounds.pixels(.1f, .1f, .2f, .2f, 1, 1));
        expect(new int[]{0, 5, 10, 10}, MaskBounds.pixels(-.2f, .5f, 1.1f, 1, 10, 10));
        assertRejected(() -> MaskBounds.pixels(.6f, .1f, .3f, .8f, 100, 100));
        assertRejected(() -> MaskBounds.pixels(Float.NaN, .1f, .9f, .9f, 100, 100));
        System.out.println("BlurKit whole-pixel mask edges and invalid bounds passed");
    }

    private static void expect(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(Arrays.toString(actual));
        }
    }

    private static void assertRejected(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("Expected invalid bounds");
        } catch (IllegalArgumentException expected) {
            // Expected failure.
        }
    }
}
