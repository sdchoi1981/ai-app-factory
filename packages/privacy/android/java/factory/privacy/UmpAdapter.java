package factory.privacy;

/** An Android app's Google UMP integration implements this on the main thread. */
public interface UmpAdapter {
    // Refresh UMP consent info at startup, then load/show the form if required.
    // Call completion on the main thread whether the operation succeeded or failed.
    void refresh(Runnable completion);
    boolean canRequestAds();
    boolean isPrivacyOptionsRequired();
    void showPrivacyOptions(Runnable completion);
}
