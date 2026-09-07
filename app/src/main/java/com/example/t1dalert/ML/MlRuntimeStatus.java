package com.example.t1dalert.ML;

public final class MlRuntimeStatus {
    public static final String READY = "ready";
    public static final String WARMING_UP = "warming_up";
    public static final String MODEL_UNAVAILABLE = "model_unavailable";
    public static final String METADATA_INVALID = "metadata_invalid";
    public static final String PREDICTION_FAILED = "prediction_failed";

    private MlRuntimeStatus() {
    }
}
