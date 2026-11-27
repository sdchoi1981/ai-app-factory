package factory.fun;

final class Rules {
    static final int SPECIES = 6;
    static final int UNLOCK_COST = 5;
    static final long FEED_INTERVAL = 3000;

    static int bound(int value) {
        return Math.max(0, Math.min(9999, value));
    }

    static boolean canUnlock(int species, int points) {
        return species >= 1 && species < SPECIES && points >= UNLOCK_COST;
    }

    static boolean cooldownElapsed(long now, long last, long interval) {
        return last <= 0 || now < last || now - last >= interval;
    }

    private Rules() {}
}
