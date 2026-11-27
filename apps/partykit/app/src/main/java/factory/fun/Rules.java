package factory.fun;

import java.util.LinkedHashSet;

final class Rules {
    private Rules() {}

    static String[] choices(String input) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String line : input.split("\\R")) {
            String choice = line.trim();
            if (!choice.isEmpty()) set.add(choice);
        }
        if (set.size() < 2 || set.size() > 30) throw new IllegalArgumentException();
        return set.toArray(new String[0]);
    }

    static float normalizedAngle(float angle) {
        return (angle % 360 + 360) % 360;
    }

    static float spinEnd(float start, int selected, int size) {
        float target = 270 - (selected + .5f) * 360 / size;
        return start + 1080 + normalizedAngle(target - start);
    }

    static String abbreviate(String value, int maxCodePoints) {
        if (value.codePointCount(0, value.length()) <= maxCodePoints) return value;
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints - 1)) + "…";
    }
}
