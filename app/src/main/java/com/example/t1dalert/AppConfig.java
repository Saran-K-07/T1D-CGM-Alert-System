package com.example.t1dalert;

public final class AppConfig {

    public static final long CGM_REFRESH_INTERVAL_MS = 15000L;
    public static final long LOW_ALERT_ESCALATION_MS = 60000L;
    public static final int LOW_DROP_DELTA_FOR_RE_ALERT = 20;

    public static final int DEFAULT_LOW_SGV = 70;
    public static final int DEFAULT_HIGH_SGV = 180;

    public static final int REQUEST_POST_NOTIFICATIONS = 100;
    public static final int REQUEST_READ_CONTACTS = 101;
    public static final int REQUEST_SEND_SMS = 102;
    public static final int REQUEST_LOCATION = 103;
    public static final int REQUEST_RECEIVE_SMS = 104;
    public static final int REQUEST_CAMERA = 105;

    public static final int ML_FEATURE_COUNT = 10;
    public static final int ML_HISTORY_CAP = 72;
    public static final String ML_MODEL_ASSET_PATH = "ml/model.onnx";
    public static final String ML_METADATA_ASSET_PATH = "ml/metadata.json";
    public static final String ML_METADATA_VERSION = "bitmaml_1h_v1";

    private AppConfig() {
    }
}
