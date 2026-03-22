package com.example.t1dalert;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MlHistoryStore {

    private static final long MIN_SAMPLE_GAP_MS = 4 * 60 * 1000L;

    private MlHistoryStore() {
    }

    static void append(SharedPreferences prefs, long timeMs, int sgv) {
        List<MlReading> history = readHistory(prefs);
        if (!history.isEmpty()) {
            MlReading last = history.get(history.size() - 1);
            if (timeMs <= last.timeMs) {
                return;
            }
            if ((timeMs - last.timeMs) < MIN_SAMPLE_GAP_MS) {
                return;
            }
        }
        history.add(new MlReading(timeMs, sgv));
        while (history.size() > AppConfig.ML_HISTORY_CAP) {
            history.remove(0);
        }
        saveHistory(prefs, history);
    }

    static List<MlReading> readHistory(SharedPreferences prefs) {
        String json = prefs.getString(AppPrefs.KEY_ML_HISTORY_JSON, "");
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            JSONArray array = new JSONArray(json);
            ArrayList<MlReading> out = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                long timeMs = obj.optLong("t", 0L);
                int sgv = obj.optInt("s", -1);
                if (timeMs > 0L && sgv > 0) {
                    out.add(new MlReading(timeMs, sgv));
                }
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    static List<MlReading> latestWindow(SharedPreferences prefs, int windowSize) {
        List<MlReading> history = readHistory(prefs);
        if (history.size() < windowSize) {
            return Collections.emptyList();
        }
        return history.subList(history.size() - windowSize, history.size());
    }

    private static void saveHistory(SharedPreferences prefs, List<MlReading> history) {
        JSONArray array = new JSONArray();
        for (MlReading reading : history) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("t", reading.timeMs);
                obj.put("s", reading.sgv);
            } catch (Exception ignored) {
            }
            array.put(obj);
        }
        prefs.edit().putString(AppPrefs.KEY_ML_HISTORY_JSON, array.toString()).apply();
    }
}
