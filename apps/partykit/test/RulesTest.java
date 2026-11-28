package factory.fun;

import java.util.Arrays;

public class RulesTest {
    public static void main(String[] args) {
        if (!Arrays.equals(Rules.choices(" A\nB\nA\n "), new String[]{"A", "B"})) {
            throw new AssertionError("Choices must be deduplicated in input order");
        }
        if (Rules.choices("사과\r\nみかん").length != 2) throw new AssertionError();
        try {
            Rules.choices("A");
            throw new AssertionError();
        } catch (IllegalArgumentException expected) {}

        for (int size = 2; size <= 30; size++) {
            for (int selected = 0; selected < size; selected++) {
                for (float start : new float[]{0, 42, 359.5f}) {
                    float end = Rules.spinEnd(start, selected, size);
                    float pointer = Rules.normalizedAngle(270 - end);
                    if (end - start < 1079.99f || (int) (pointer / (360f / size)) != selected) {
                        throw new AssertionError("Pointer must land inside selected sector");
                    }
                }
            }
        }
        if (!Rules.abbreviate("🤹🎨🎭🏖🐈", 3).equals("🤹🎨…")) throw new AssertionError();
        System.out.println("Choice input, wheel position and Unicode rendering verified");
    }
}
