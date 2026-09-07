package com.example.t1dalert.ML;

public final class MlPredictionResult {
    public final String status;
    public final Integer predictedMgdl;

    private MlPredictionResult(String status, Integer predictedMgdl) {
        this.status = status;
        this.predictedMgdl = predictedMgdl;
    }

    public static MlPredictionResult statusOnly(String status) {
        return new MlPredictionResult(status, null);
    }

    public static MlPredictionResult ready(int predictionMgdl) {
        return new MlPredictionResult(MlRuntimeStatus.READY, predictionMgdl);
    }
}
