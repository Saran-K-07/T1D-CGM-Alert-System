package com.example.t1dalert;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public final class CgmParser {

    private CgmParser() {
    }

    public static CgmData parseLatest(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new IllegalArgumentException("empty_response");
        }

        String body = responseBody.trim();

        // 1) Standard Nightscout: JSON array
        if (body.startsWith("[")) {
            return parseFromJsonArray(body);
        }

        // 2) Some proxies/wrappers return an object with nested arrays
        if (body.startsWith("{")) {
            return parseFromJsonObject(body);
        }

        // 3) Backward compatibility for line/tab formats
        return parseFromLegacyDelimited(body);
    }

    private static CgmData parseFromJsonArray(String body) {
        try {
            JSONArray entries = new JSONArray(body);
            if (entries.length() == 0) {
                throw new IllegalArgumentException("empty_entries");
            }

            JSONObject first = entries.getJSONObject(0);
            if (!first.has("sgv")) {
                throw new IllegalArgumentException("missing_sgv");
            }

            int sgv = first.getInt("sgv");
            String direction = first.optString("direction", "");
            return new CgmData(sgv, direction);
        } catch (JSONException e) {
            throw new IllegalArgumentException("invalid_json", e);
        }
    }

    private static CgmData parseFromJsonObject(String body) {
        try {
            JSONObject root = new JSONObject(body);

            JSONArray entries = root.optJSONArray("entries");
            if (entries == null) {
                entries = root.optJSONArray("result");
            }
            if (entries == null) {
                entries = root.optJSONArray("data");
            }

            if (entries == null || entries.length() == 0) {
                throw new IllegalArgumentException("missing_entries_array");
            }

            JSONObject first = entries.getJSONObject(0);
            if (!first.has("sgv")) {
                throw new IllegalArgumentException("missing_sgv");
            }

            int sgv = first.getInt("sgv");
            String direction = first.optString("direction", "");
            return new CgmData(sgv, direction);
        } catch (JSONException e) {
            throw new IllegalArgumentException("invalid_json_object", e);
        }
    }

    private static CgmData parseFromLegacyDelimited(String body) {
        String firstLine = body.split("\\n")[0];
        String[] parts = firstLine.split("\\t");
        if (parts.length < 4) {
            throw new IllegalArgumentException("unsupported_format");
        }

        try {
            int sgv = Integer.parseInt(parts[2].replace("\"", "").trim());
            String direction = parts[3].replace("\"", "").trim();
            return new CgmData(sgv, direction);
        } catch (Exception e) {
            throw new IllegalArgumentException("legacy_parse_failed", e);
        }
    }
}
