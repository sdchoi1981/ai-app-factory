package factory.fun;

final class Rules {
    private Rules() {}

    static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    static float position(float screen, float width, int direction, float distance) {
        if (direction == 2) return (screen - width) / 2;
        float total = Math.max(1, screen + width);
        float offset = ((distance % total) + total) % total;
        return direction == 0 ? screen - offset : offset - width;
    }
}
