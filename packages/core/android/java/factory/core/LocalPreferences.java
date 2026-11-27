package factory.core;

import android.content.Context;
import android.content.SharedPreferences;

public final class LocalPreferences {
    private final SharedPreferences preferences;

    public LocalPreferences(Context context, String namespace) {
        preferences = context.getSharedPreferences(namespace, Context.MODE_PRIVATE);
    }

    public int getInt(String key, int fallback) {
        try {
            return preferences.getInt(key, fallback);
        } catch (ClassCastException ignored) {
            return fallback;
        }
    }

    public void putInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }
}
