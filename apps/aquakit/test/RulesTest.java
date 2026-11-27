package factory.fun;

public final class RulesTest {
    public static void main(String[] args) {
        require(Rules.bound(-1) == 0 && Rules.bound(10000) == 9999 && Rules.bound(5) == 5, "point bounds");
        require(!Rules.canUnlock(1, 4) && Rules.canUnlock(1, 5) && Rules.canUnlock(5, 5), "unlock threshold");
        require(!Rules.canUnlock(0, 5) && !Rules.canUnlock(6, 100), "collection boundaries");
        require(!Rules.cooldownElapsed(3999, 1000, Rules.FEED_INTERVAL), "cooldown before threshold");
        require(Rules.cooldownElapsed(4000, 1000, Rules.FEED_INTERVAL), "cooldown at threshold");
        require(Rules.cooldownElapsed(100, 0, Rules.FEED_INTERVAL), "new aquarium can feed");
        require(Rules.cooldownElapsed(100, 1000, Rules.FEED_INTERVAL), "clock adjustment cannot lock feeding");
        System.out.println("AquaKit points, species unlocking and cooldown passed");
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
