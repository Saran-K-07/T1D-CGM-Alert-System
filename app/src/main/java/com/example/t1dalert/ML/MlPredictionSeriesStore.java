package com.example.t1dalert.ML;

import android.content.SharedPreferences;

import com.example.t1dalert.Core.AppConfig;
import com.example.t1dalert.Core.AppPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class MlPredictionSeriesStore {

    static final class Point {
        final long timeMs;
        final int predictedMgdl;
        final int actualMgdl;

        Point(long timeMs, int predictedMgdl, int actualMgdl) {
            this.timeMs = timeMs;
            this.predictedMgdl = predictedMgdl;
            this.actualMgdl = actualMgdl;
        }
    }

    private MlPredictionSeriesStore() {
    }

    static void appendPoint(SharedPreferences prefs, long timeMs, int predictedMgdl, int actualMgdl) {
        List<Point> points = readPoints(prefs);
        points.add(new Point(timeMs, predictedMgdl, actualMgdl));
        while (points.size() > AppConfig.ML_CHART_MAX_POINTS) {
            points.remove(0);
        }
        savePoints(prefs, points);
    }

    static List<Point> readPoints(SharedPreferences prefs) {
        String raw = prefs.getString(AppPrefs.KEY_ML_PREDICTION_SERIES_JSON, "");
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            JSONArray arr = new JSONArray(raw);
            List<Point> points = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                long t = o.optLong("t", 0L);
                int p = o.optInt("p", Integer.MIN_VALUE);
                int a = o.optInt("a", Integer.MIN_VALUE);
                if (t > 0L && p != Integer.MIN_VALUE && a != Integer.MIN_VALUE) {
                    points.add(new Point(t, p, a));
                }
            }
            return points;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private static void savePoints(SharedPreferences prefs, List<Point> points) {
        JSONArray arr = new JSONArray();
        for (Point p : points) {
            JSONObject o = new JSONObject();
            try {
                o.put("t", p.timeMs);
                o.put("p", p.predictedMgdl);
                o.put("a", p.actualMgdl);
            } catch (Exception ignored) {
            }
            arr.put(o);
        }
        prefs.edit().putString(AppPrefs.KEY_ML_PREDICTION_SERIES_JSON, arr.toString()).apply();
    }
}
