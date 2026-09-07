package com.example.t1dalert.Core;

public final class AppConfig {

    public static final long CGM_REFRESH_INTERVAL_MS = 30000L;
    public static final long LOW_ALERT_ESCALATION_MS = 60000L;
    public static final int LOW_DROP_DELTA_FOR_RE_ALERT = 20;
    public static final int HIGH_RISE_DELTA_FOR_RE_ALERT = 20;
    public static final long ML_MIN_INFERENCE_INTERVAL_MS = 4 * 60 * 1000L;
    public static final long ML_PREDICTION_HORIZON_MS = 60 * 60 * 1000L;
    public static final int ML_CHART_MAX_POINTS = 360;

    public static final int DEFAULT_LOW_SGV = 70;
    public static final int DEFAULT_HIGH_SGV = 180;
    public static final String DEFAULT_ESCALATION_NUMBER = "7603832319";
    public static final int BATTERY_THROTTLE_PERCENT = 20;

    public static final int REQUEST_POST_NOTIFICATIONS = 100;
    public static final int REQUEST_SEND_SMS = 102;
    public static final int REQUEST_LOCATION = 103;
    public static final int REQUEST_RECEIVE_SMS = 104;
    public static final int REQUEST_CAMERA = 105;

    public static final int ML_HISTORY_CAP = 72;
    public static final String ML_MODEL_ASSET_PATH = "ml/model.onnx";
    public static final String ML_METADATA_ASSET_PATH = "ml/metadata.json";
    public static final String ML_METADATA_VERSION = "bitmaml_1h_v1";

    private AppConfig() {
    }
}
