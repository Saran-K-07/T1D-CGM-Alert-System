package com.example.t1dalert;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppMetrics {

    private AppMetrics() {
    }

    public static void increment(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        int current = prefs.getInt(key, 0);
        prefs.edit().putInt(key, current + 1).apply();
    }
}
