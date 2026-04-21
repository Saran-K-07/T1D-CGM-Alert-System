package com.example.t1dalert;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.io.File;
import java.util.List;

public final class MlRuntimeEngine {

    private static final String TAG = "T1DAlert-ML";
    private final Context appContext;
    private volatile MlMetadata metadata;
    private volatile OnnxPredictor predictor;

    public MlRuntimeEngine(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public MlPredictionResult predictWithLatest(int latestSgv, long timestampMs) {
        SharedPreferences prefs = AppPrefsStore.get(appContext);
        if (isDebugLoggingEnabled()) {
            Log.d(TAG, "predictWithLatest start: sgv=" + latestSgv + ", ts=" + timestampMs);
        }
        MlHistoryStore.AppendResult appendResult = MlHistoryStore.append(prefs, timestampMs, latestSgv);
        if (isDebugLoggingEnabled()) {
            Log.d(
                    TAG,
                    "history_append status=" + appendResult.status
                            + ", history_count=" + appendResult.historySize
                            + ", ref_ts=" + appendResult.referenceTimeMs
                            + ", needed=36"
            );
        }

        long lastInferenceAt = prefs.getLong(AppPrefs.KEY_ML_LAST_INFERENCE_AT, 0L);
        if (lastInferenceAt > 0L && timestampMs > 0L && (timestampMs - lastInferenceAt) < AppConfig.ML_MIN_INFERENCE_INTERVAL_MS) {
            if (isDebugLoggingEnabled()) {
                Log.d(TAG, "predictWithLatest skipped: throttled");
            }
            String currentStatus = prefs.getString(AppPrefs.KEY_ML_STATUS, MlRuntimeStatus.WARMING_UP);
            if (MlRuntimeStatus.READY.equals(currentStatus)) {
                int prediction = prefs.getInt(AppPrefs.KEY_ML_PREDICTION_MGDL, Integer.MIN_VALUE);
                if (prediction != Integer.MIN_VALUE) {
                    return MlPredictionResult.ready(prediction);
                }
            }
            return MlPredictionResult.statusOnly(currentStatus);
        }

        MlMetadata meta = ensureModelReady(prefs);
        if (meta == null) {
            if (isDebugLoggingEnabled()) {
                Log.w(TAG, "predictWithLatest skipped: model not ready, status=" + prefs.getString(AppPrefs.KEY_ML_STATUS, MlRuntimeStatus.MODEL_UNAVAILABLE));
            }
            return MlPredictionResult.statusOnly(prefs.getString(AppPrefs.KEY_ML_STATUS, MlRuntimeStatus.MODEL_UNAVAILABLE));
        }

        List<MlReading> window = MlHistoryStore.latestWindow(prefs, meta.windowSize);
        if (window.isEmpty()) {
            setStatus(prefs, MlRuntimeStatus.WARMING_UP);
            int historyCount = MlHistoryStore.readHistory(prefs).size();
            if (isDebugLoggingEnabled()) {
                Log.d(TAG, "predictWithLatest warming up: history_count=" + historyCount + ", need_window=" + meta.windowSize);
            }
            return MlPredictionResult.statusOnly(MlRuntimeStatus.WARMING_UP);
        }

        try {
            float[][][] input = MlFeatureBuilder.buildModelInput(window, meta);
            float normPrediction = predictor.predict(input);
            int mgdlPrediction = MlFeatureBuilder.denormalizePrediction(normPrediction, meta);
            if (isDebugLoggingEnabled()) {
                Log.d(TAG, "predictWithLatest success: norm=" + normPrediction + ", mgdl=" + mgdlPrediction);
            }

            MlPredictionSeriesStore.appendPoint(prefs, timestampMs, mgdlPrediction, latestSgv);

            prefs.edit()
                    .putString(AppPrefs.KEY_ML_STATUS, MlRuntimeStatus.READY)
                    .putInt(AppPrefs.KEY_ML_PREDICTION_MGDL, mgdlPrediction)
                    .putLong(AppPrefs.KEY_ML_PREDICTION_AT, timestampMs)
                    .putLong(AppPrefs.KEY_ML_LAST_INFERENCE_AT, timestampMs)
                    .apply();

            return MlPredictionResult.ready(mgdlPrediction);
        } catch (Exception e) {
            setStatus(prefs, MlRuntimeStatus.PREDICTION_FAILED);
            Log.e(TAG, "predictWithLatest failed");
            return MlPredictionResult.statusOnly(MlRuntimeStatus.PREDICTION_FAILED);
        }
    }

    private MlMetadata ensureModelReady(SharedPreferences prefs) {
        if (metadata != null && predictor != null) {
            return metadata;
        }
        synchronized (this) {
            if (metadata != null && predictor != null) {
                return metadata;
            }

            try {
                String metadataJson = MlAssetLoader.loadMetadataJson(appContext);
                MlMetadata parsedMeta;
                try {
                    parsedMeta = MlMetadataParser.parseAndValidate(metadataJson);
                    if (isDebugLoggingEnabled()) {
                        Log.d(TAG, "Metadata parsed: version=" + parsedMeta.modelVersion + ", model=" + parsedMeta.modelFile + ", window=" + parsedMeta.windowSize);
                    }
                } catch (Exception e) {
                    setStatus(prefs, MlRuntimeStatus.METADATA_INVALID);
                    Log.e(TAG, "Metadata invalid");
                    return null;
                }
                File modelFile = MlAssetLoader.prepareModelFile(appContext, parsedMeta.modelFile);
                OnnxPredictor onnxPredictor = new OnnxPredictor(modelFile.getAbsolutePath());

                metadata = parsedMeta;
                predictor = onnxPredictor;
                if (isDebugLoggingEnabled()) {
                    Log.d(TAG, "Model ready from: " + modelFile.getAbsolutePath());
                }
                return metadata;
            } catch (Exception e) {
                metadata = null;
                predictor = null;
                setStatus(prefs, MlRuntimeStatus.MODEL_UNAVAILABLE);
                Log.e(TAG, "Model unavailable");
                return null;
            }
        }
    }

    private void setStatus(SharedPreferences prefs, String status) {
        prefs.edit().putString(AppPrefs.KEY_ML_STATUS, status).apply();
    }

    private boolean isDebugLoggingEnabled() {
        return (appContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
