package com.example.t1dalert;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class EmergencyMessageFormatter {

    private EmergencyMessageFormatter() {
    }

    public static String build(
            String sgv,
            String trend,
            boolean fallDetected,
            long fallDetectedAt,
            boolean unconsciousLikely,
            float unconsciousConfidence,
            String lastLocation
    ) {
        String safeSgv = sgv == null ? "---" : sgv;
        String safeTrend = trend == null ? "?" : trend;

        if (unconsciousLikely) {
            int confidencePercent = Math.round(unconsciousConfidence * 100f);
            String detectedTime = formatTimestamp(fallDetectedAt);
            String locationPart = (lastLocation == null || lastLocation.isEmpty())
                    ? "Loc: unavailable"
                    : "Loc: " + lastLocation;
            return "AMBULANCE REQUEST. Possible unconscious diabetic patient (suspected hypoglycemia after fall). "
                    + "SGV " + safeSgv + " mg/dL " + safeTrend + ". "
                    + locationPart + ". "
                    + "Detected: " + detectedTime + ". "
                    + "Priority: HIGH (" + confidencePercent + "%).";
        }

        if (fallDetected) {
            String detectedTime = formatTimestamp(fallDetectedAt);
            return "Emergency diabetic low sugar alert with possible fall. SGV "
                    + safeSgv + " mg/dL " + safeTrend + ". Detected: " + detectedTime + ".";
        }

        return "Emergency: Low blood sugar alert. SGV " + safeSgv + " mg/dL " + safeTrend + ". Please check immediately.";
    }

    static String formatTimestamp(long timestampMs) {
        long ts = timestampMs > 0 ? timestampMs : System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(new Date(ts));
    }
}
