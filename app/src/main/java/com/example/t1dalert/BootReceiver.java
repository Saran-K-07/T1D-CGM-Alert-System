package com.example.t1dalert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
            sharedPreferences.edit()
                    .remove(AppPrefs.KEY_ML_PREDICTION_MGDL)
                    .remove(AppPrefs.KEY_ML_PREDICTION_AT)
                    .remove(AppPrefs.KEY_ML_LAST_INFERENCE_AT)
                    .putString(AppPrefs.KEY_ML_STATUS, MlRuntimeStatus.WARMING_UP)
                    .apply();

            String url = sharedPreferences.getString(AppPrefs.KEY_NIGHTSCOUT_URL, "");
            String apiToken = sharedPreferences.getString(AppPrefs.KEY_API_TOKEN, "");
            String accessToken = sharedPreferences.getString(AppPrefs.KEY_ACCESS_TOKEN, "");

            if (!url.isEmpty() && !apiToken.isEmpty() && !accessToken.isEmpty()) {
                Intent serviceIntent = new Intent(context, CgmBackgroundService.class);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
    }
}
