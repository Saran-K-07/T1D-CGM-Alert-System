package com.example.t1dalert.Core;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppMetrics {

    private AppMetrics() {
    }

    public static void increment(Context context, String key) {
        SharedPreferences prefs = AppPrefsStore.get(context);
        int current = prefs.getInt(key, 0);
        prefs.edit().putInt(key, current + 1).apply();
    }
}
