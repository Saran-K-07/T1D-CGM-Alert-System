package com.example.t1dalert.ML;

import com.example.t1dalert.Core.AppConfig;

import org.json.JSONArray;
import org.json.JSONObject;

final class MlMetadataParser {

    private MlMetadataParser() {
    }

    static MlMetadata parseAndValidate(String json) {
        try {
            JSONObject root = new JSONObject(json);
            String modelVersion = root.optString("model_version", "");
            String modelFile = root.optString("model_file", "model.onnx");
            int windowSize = root.optInt("window_size", 0);
            int horizonSteps = root.optInt("horizon_steps", 0);

            JSONArray featureOrderJson = root.optJSONArray("feature_order");
            if (featureOrderJson == null || featureOrderJson.length() == 0) {
                throw new IllegalArgumentException("invalid_feature_order");
            }

            String[] featureOrder = new String[featureOrderJson.length()];
            for (int i = 0; i < featureOrderJson.length(); i++) {
                featureOrder[i] = featureOrderJson.optString(i, "");
            }

            if (!MlFeatureBuilder.isSupportedFeatureOrder(featureOrder)) {
                throw new IllegalArgumentException("unsupported_feature_order");
            }

            JSONObject scaler = root.optJSONObject("scaler");
            if (scaler == null) {
                throw new IllegalArgumentException("missing_scaler");
            }

            float[] dataMin = readFloatArray(scaler.optJSONArray("data_min"));
            float[] dataMax = readFloatArray(scaler.optJSONArray("data_max"));
            float[] scale = readFloatArray(scaler.optJSONArray("scale"));
            float[] minOffset = readFloatArray(scaler.optJSONArray("min_offset"));

            int featureCount = featureOrder.length;
            if (dataMin.length != featureCount
                    || dataMax.length != featureCount
                    || scale.length != featureCount
                    || minOffset.length != featureCount) {
                throw new IllegalArgumentException("invalid_scaler_shape");
            }

            if (windowSize <= 0 || horizonSteps <= 0) {
                throw new IllegalArgumentException("invalid_window_or_horizon");
            }

            if (modelVersion.isEmpty() || !AppConfig.ML_METADATA_VERSION.equals(modelVersion)) {
                throw new IllegalArgumentException("unsupported_model_version");
            }

            return new MlMetadata(
                    modelVersion,
                    modelFile,
                    windowSize,
                    horizonSteps,
                    featureOrder,
                    dataMin,
                    dataMax,
                    scale,
                    minOffset
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid_metadata", e);
        }
    }

    private static float[] readFloatArray(JSONArray arr) {
        if (arr == null) {
            return new float[0];
        }
        float[] out = new float[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            out[i] = (float) arr.optDouble(i, Float.NaN);
            if (Float.isNaN(out[i]) || Float.isInfinite(out[i])) {
                throw new IllegalArgumentException("invalid_scaler_value");
            }
        }
        return out;
    }
}
