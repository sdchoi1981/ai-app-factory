package factory.kit;

public final class LevelMathTest {
    public static void main(String[] args) {
        assertFalse(LevelMath.valid(0, 0, 0));
        assertFalse(LevelMath.valid(Float.NaN, 0, 10));
        assertFalse(LevelMath.valid(Float.POSITIVE_INFINITY, 0, 9.8f));
        if (!LevelMath.valid(0, 0, 9.8f)) throw new AssertionError("Gravity should be valid");
        equals(0, LevelMath.inclination(0, 0, 9.8f));
        equals(-45, LevelMath.inclination(-9.8f, 0, 9.8f));
        equals(90, LevelMath.inclination(9.8f, 0, 0));
        float[] horizontal = {2, 4, -2, -4};
        float[] vertical = {4, -2, -4, 2};
        for (int rotation = 0; rotation < 4; rotation++) {
            equals(horizontal[rotation], LevelMath.horizontal(2, 4, rotation));
            equals(vertical[rotation], LevelMath.vertical(2, 4, rotation));
        }
        if (!LevelMath.level(.3f, .3f)) throw new AssertionError("Expected near level");
        assertFalse(LevelMath.level(.4f, .4f));
        System.out.println("MeasureKit angle, rotation and level tests passed");
    }

    private static void equals(float expected, float actual) {
        if (Math.abs(expected - actual) > .001f) throw new AssertionError(expected + " != " + actual);
    }

    private static void assertFalse(boolean result) {
        if (result) throw new AssertionError("Expected false");
    }
}
