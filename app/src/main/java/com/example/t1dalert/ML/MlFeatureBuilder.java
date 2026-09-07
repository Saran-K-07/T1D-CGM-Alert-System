package com.example.t1dalert.ML;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

final class MlFeatureBuilder {

    private static final String F_GLUCOSE = "glucose_level";
    private static final String F_MEAL = "meal";
    private static final String F_BOLUS = "bolus";
    private static final String F_HYPO = "hypoevent";
    private static final String F_SIN = "sin_hour";
    private static final String F_COS = "cos_hour";
    private static final String F_NIGHT = "isnight";
    private static final String F_MEALTIME = "ismealtime";
    private static final String F_EXERCISE = "exercise";
    private static final String F_STRESS = "stressors";

    private MlFeatureBuilder() {
    }

    static boolean isSupportedFeatureOrder(String[] featureOrder) {
        if (featureOrder == null || featureOrder.length < 8) {
            return false;
        }
        Set<String> seen = new HashSet<>();
        for (String name : featureOrder) {
            if (name == null || name.trim().isEmpty()) {
                return false;
            }
            if (!isKnownFeature(name)) {
                return false;
            }
            seen.add(name);
        }
        return seen.contains(F_GLUCOSE)
                && seen.contains(F_MEAL)
                && seen.contains(F_BOLUS)
                && seen.contains(F_HYPO)
                && seen.contains(F_SIN)
                && seen.contains(F_COS)
                && seen.contains(F_NIGHT)
                && seen.contains(F_MEALTIME);
    }

    static float[][][] buildModelInput(List<MlReading> history, MlMetadata meta) {
        int windowSize = meta.windowSize;
        int features = meta.featureOrder.length;
        float[][][] input = new float[1][windowSize][features];
        int start = history.size() - windowSize;

        for (int i = 0; i < windowSize; i++) {
            MlReading reading = history.get(start + i);
            int hour = (int) ((reading.timeMs / 3600000L) % 24);
            if (hour < 0) {
                hour += 24;
            }

            float[] row = new float[features];
            for (int j = 0; j < features; j++) {
                String feature = meta.featureOrder[j];
                row[j] = computeFeature(feature, reading, hour);
            }

            for (int j = 0; j < features; j++) {
                float normalized = row[j] * meta.scalerScale[j] + meta.scalerMinOffset[j];
                if (Float.isNaN(normalized) || Float.isInfinite(normalized)) {
                    normalized = 0f;
                }
                input[0][i][j] = normalized;
            }
        }

        return input;
    }

    static int denormalizePrediction(float normalized, MlMetadata meta) {
        float scale = meta.scalerScale[0];
        float minOffset = meta.scalerMinOffset[0];
        if (scale == 0f) {
            return Math.round(meta.scalerDataMin[0]);
        }
        float mgdl = (normalized - minOffset) / scale;
        return Math.round(mgdl);
    }

    private static boolean isKnownFeature(String name) {
        return F_GLUCOSE.equals(name)
                || F_MEAL.equals(name)
                || F_BOLUS.equals(name)
                || F_HYPO.equals(name)
                || F_SIN.equals(name)
                || F_COS.equals(name)
                || F_NIGHT.equals(name)
                || F_MEALTIME.equals(name)
                || F_EXERCISE.equals(name)
                || F_STRESS.equals(name);
    }

    private static float computeFeature(String feature, MlReading reading, int hour) {
        if (F_GLUCOSE.equals(feature)) {
            return reading.sgv;
        }
        if (F_MEAL.equals(feature) || F_BOLUS.equals(feature) || F_EXERCISE.equals(feature) || F_STRESS.equals(feature)) {
            return 0f;
        }
        if (F_HYPO.equals(feature)) {
            return reading.sgv < 70 ? 1f : 0f;
        }
        if (F_SIN.equals(feature)) {
            return (float) Math.sin(2d * Math.PI * hour / 24d);
        }
        if (F_COS.equals(feature)) {
            return (float) Math.cos(2d * Math.PI * hour / 24d);
        }
        if (F_NIGHT.equals(feature)) {
            return (hour >= 22 || hour <= 5) ? 1f : 0f;
        }
        if (F_MEALTIME.equals(feature)) {
            return (hour == 7 || hour == 8 || hour == 12 || hour == 13 || hour == 18 || hour == 19) ? 1f : 0f;
        }
        return 0f;
    }
}
