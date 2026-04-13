package com.example.t1dalert;

public final class CgmUtils {

    private CgmUtils() {
    }

    public static String getTrendArrow(String trendString) {
        if (trendString == null) {
            return "?";
        }
        switch (trendString) {
            case "DoubleUp":
                return "↑↑";
            case "SingleUp":
                return "↑";
            case "FortyFiveUp":
                return "↗";
            case "Flat":
                return "→";
            case "FortyFiveDown":
                return "↘";
            case "SingleDown":
                return "↓";
            case "DoubleDown":
                return "↓↓";
            default:
                return "?";
        }
    }

    public static String buildNightscoutEntriesUrl(String nightscoutUrl) {
        if (nightscoutUrl == null || nightscoutUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("missing_url");
        }

        String normalized = nightscoutUrl.trim();
        return normalized + "/api/v1/entries?count=1";
    }

    public static String buildLegacyNightscoutEntriesUrl(String nightscoutUrl, String apiToken, String accessToken) {
        return buildNightscoutEntriesUrl(nightscoutUrl);
    }

    public static int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
