package factory.fun;

final class Rules {
    static final int SLOTS = 6;
    static final int EVOLUTION_XP = 10;
    static final int CARE_INTERVAL = 2000;

    static int bound(int value) {
        return Math.max(0, Math.min(9999, value));
    }

    static boolean evolved(boolean hatched, int xp) {
        return hatched && xp >= EVOLUTION_XP;
    }

    static boolean canUnlock(int slots, boolean hatched, int xp) {
        return slots >= 1 && slots < SLOTS && evolved(hatched, xp);
    }

    static boolean cooldownElapsed(long now, long last) {
        return last <= 0 || now < last || now - last >= CARE_INTERVAL;
    }

    private Rules() {}
}
