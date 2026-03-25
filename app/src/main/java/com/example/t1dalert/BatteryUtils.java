package com.example.t1dalert;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

final class BatteryUtils {

    private BatteryUtils() {
    }

    static boolean isBatteryLowAndNotCharging(Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return false;
        }

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        if (isCharging || level < 0 || scale <= 0) {
            return false;
        }

        int percent = Math.round((level * 100f) / scale);
        return percent <= AppConfig.BATTERY_THROTTLE_PERCENT;
    }
}
