package factory.fun;

public final class RulesTest {
    public static void main(String[] args) {
        equals(40, Rules.position(100, 20, 2, 8));
        equals(100, Rules.position(100, 20, 0, 0));
        equals(-20, Rules.position(100, 20, 1, 0));
        equals(100, Rules.position(100, 20, 0, 120));
        equals(40, Rules.position(100, 20, 0, 60));
        equals(40, Rules.position(100, 20, 1, 60));
        equals(90, Rules.position(100, 20, 1, -10));
        equals(0, Rules.clamp(-3, 4));
        equals(4, Rules.clamp(9, 4));
        equals(3, Rules.clamp(3, 4));
        System.out.println("LEDKit animation, wraparound and setting bounds verified");
    }

    private static void equals(float expected, float actual) {
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
}
