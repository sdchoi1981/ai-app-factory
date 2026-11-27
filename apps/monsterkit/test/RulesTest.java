package factory.fun;

public final class RulesTest {
    public static void main(String[] args) {
        require(Rules.bound(-1) == 0 && Rules.bound(10000) == 9999 && Rules.bound(10) == 10, "XP bounds");
        require(!Rules.evolved(false, 10) && !Rules.evolved(true, 9) && Rules.evolved(true, 10), "evolution state");
        require(!Rules.canUnlock(1, false, 10) && Rules.canUnlock(1, true, 10), "unlock requires evolution");
        require(!Rules.canUnlock(0, true, 10) && !Rules.canUnlock(6, true, 10), "collection boundaries");
        require(!Rules.cooldownElapsed(2999, 1000) && Rules.cooldownElapsed(3000, 1000), "cooldown threshold");
        require(Rules.cooldownElapsed(100, 0) && Rules.cooldownElapsed(100, 1000), "first care and clock adjustment");
        System.out.println("MonsterKit XP, evolution, egg unlocking and cooldown passed");
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
