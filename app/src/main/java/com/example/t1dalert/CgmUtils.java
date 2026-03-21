package com.example.t1dalert;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

    public static String buildNightscoutEntriesUrl(String nightscoutUrl, String apiToken, String accessToken) {
        if (nightscoutUrl == null || nightscoutUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("missing_url");
        }

        String normalized = nightscoutUrl.trim();
        String encodedApiToken = encodeForUrl(apiToken == null ? "" : apiToken);

        if (normalized.startsWith("http://")) {
            normalized = "http://" + encodedApiToken + "@" + normalized.substring(7);
        } else if (normalized.startsWith("https://")) {
            normalized = "https://" + encodedApiToken + "@" + normalized.substring(8);
        }

        return normalized + "/api/v1/entries?token=" + (accessToken == null ? "" : accessToken) + "&count=1";
    }

    public static int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String encodeForUrl(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
