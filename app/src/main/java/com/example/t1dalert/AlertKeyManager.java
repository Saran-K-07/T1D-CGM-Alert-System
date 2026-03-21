package com.example.t1dalert;

import android.content.SharedPreferences;
import android.util.Base64;

import java.security.SecureRandom;

public final class AlertKeyManager {

    private static final int KEY_BYTES = 32;

    private AlertKeyManager() {
    }

    public static String getOrCreateSharedAlertKey(SharedPreferences prefs) {
        String existing = prefs.getString(AppPrefs.KEY_SHARED_ALERT_KEY, "").trim();
        if (!existing.isEmpty()) {
            return existing;
        }

        byte[] bytes = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(bytes);
        String generated = Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.NO_PADDING);
        prefs.edit().putString(AppPrefs.KEY_SHARED_ALERT_KEY, generated).apply();
        return generated;
    }
}
