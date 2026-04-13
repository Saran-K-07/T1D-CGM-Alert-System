package com.example.t1dalert;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

final class AppPrefsStore {

    private AppPrefsStore() {
    }

    static SharedPreferences get(Context context) {
        Context appContext = context.getApplicationContext();
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    appContext,
                    AppPrefs.PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            boolean isDebuggable = (appContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) {
                return appContext.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
            }
            throw new IllegalStateException("secure_prefs_unavailable", e);
        }
    }
}
