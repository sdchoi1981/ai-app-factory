package factory.kit;

final class LevelMath {
    private LevelMath() {}

    static boolean valid(float x, float y, float z) {
        double magnitude = Math.hypot(Math.hypot(x, y), z);
        return Double.isFinite(magnitude) && magnitude > 0.1;
    }

    static float inclination(float axis, float orthogonal, float z) {
        return (float) Math.toDegrees(Math.atan2(axis, Math.hypot(orthogonal, z)));
    }

    static float horizontal(float x, float y, int rotation) {
        return switch (rotation) {
            case 1 -> y;
            case 2 -> -x;
            case 3 -> -y;
            default -> x;
        };
    }

    static float vertical(float x, float y, int rotation) {
        return switch (rotation) {
            case 1 -> -x;
            case 2 -> -y;
            case 3 -> x;
            default -> y;
        };
    }

    static boolean level(float x, float y) {
        return Math.hypot(x, y) <= 0.5;
    }
}
