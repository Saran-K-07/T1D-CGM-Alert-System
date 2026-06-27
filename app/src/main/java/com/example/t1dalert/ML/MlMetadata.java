package com.example.t1dalert.ML;

public final class MlMetadata {
    public final String modelVersion;
    public final String modelFile;
    public final int windowSize;
    public final int horizonSteps;
    public final String[] featureOrder;
    public final float[] scalerDataMin;
    public final float[] scalerDataMax;
    public final float[] scalerScale;
    public final float[] scalerMinOffset;

    public MlMetadata(
            String modelVersion,
            String modelFile,
            int windowSize,
            int horizonSteps,
            String[] featureOrder,
            float[] scalerDataMin,
            float[] scalerDataMax,
            float[] scalerScale,
            float[] scalerMinOffset
    ) {
        this.modelVersion = modelVersion;
        this.modelFile = modelFile;
        this.windowSize = windowSize;
        this.horizonSteps = horizonSteps;
        this.featureOrder = featureOrder;
        this.scalerDataMin = scalerDataMin;
        this.scalerDataMax = scalerDataMax;
        this.scalerScale = scalerScale;
        this.scalerMinOffset = scalerMinOffset;
    }
}
