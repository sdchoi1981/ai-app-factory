package factory.ads;

import java.util.function.BooleanSupplier;

public interface AdMobAdapter {
    String TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713";

    enum Placement {
        BANNER("ca-app-pub-3940256099942544/6300978111"),
        INTERSTITIAL("ca-app-pub-3940256099942544/1033173712"),
        REWARDED("ca-app-pub-3940256099942544/5224354917");

        public final String testUnitId;

        Placement(String testUnitId) {
            this.testUnitId = testUnitId;
        }
    }

    // Recheck canRequestAds immediately before loading and before showing.
    // A rewarded placement grants a reward only on the SDK's reward callback.
    void show(Placement placement, String unitId, BooleanSupplier canRequestAds);
    void clear();
}
