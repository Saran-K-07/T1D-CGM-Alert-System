package com.example.t1dalert;

public final class AppPrefs {

    public static final String PREFS_NAME = "T1DAlertPrefs";
    public static final String KEY_NIGHTSCOUT_URL = "nightscout_url";
    public static final String KEY_API_TOKEN = "api_token";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_SHARED_ALERT_KEY = "shared_alert_key";
    public static final String KEY_TRUSTED_SENDERS = "trusted_senders";
    public static final String KEY_USER_PHONE = "user_phone";
    public static final String KEY_ESCALATION_NUMBER = "escalation_number";
    public static final String KEY_LOW_SGV = "low_sgv";
    public static final String KEY_HIGH_SGV = "high_sgv";

    public static final String KEY_CONTACT_1 = "contact_1";
    public static final String KEY_CONTACT_2 = "contact_2";
    public static final String KEY_CONTACT_3 = "contact_3";
    public static final String KEY_CONTACT_4 = "contact_4";
    public static final String KEY_CONTACT_5 = "contact_5";
    public static final String KEY_CONTACT_1_NAME = "contact_1_name";
    public static final String KEY_CONTACT_2_NAME = "contact_2_name";
    public static final String KEY_CONTACT_3_NAME = "contact_3_name";
    public static final String KEY_CONTACT_4_NAME = "contact_4_name";
    public static final String KEY_CONTACT_5_NAME = "contact_5_name";

    public static final String[] CONTACT_KEYS = {
            KEY_CONTACT_1,
            KEY_CONTACT_2,
            KEY_CONTACT_3,
            KEY_CONTACT_4,
            KEY_CONTACT_5
    };

    public static final String[] CONTACT_NAME_KEYS = {
            KEY_CONTACT_1_NAME,
            KEY_CONTACT_2_NAME,
            KEY_CONTACT_3_NAME,
            KEY_CONTACT_4_NAME,
            KEY_CONTACT_5_NAME
    };

    public static final String KEY_LOW_ALERT_START_TIME = "low_alert_start_time";
    public static final String KEY_OVERLAY_ACTIVE = "overlay_active";
    public static final String KEY_SMS_SENT = "sms_sent";
    public static final String KEY_LAST_ALERTED_SGV = "last_alerted_sgv";
    public static final String KEY_FALL_DETECTED = "fall_detected";
    public static final String KEY_FALL_DETECTED_AT = "fall_detected_at";
    public static final String KEY_UNCONSCIOUS_LIKELY = "unconscious_likely";
    public static final String KEY_UNCONSCIOUS_CONFIDENCE = "unconscious_confidence";
    public static final String KEY_UNCONSCIOUS_TELEMETRY = "unconscious_telemetry";
    public static final String KEY_LAST_LOCATION = "last_location";
    public static final String KEY_LAST_LOCATION_AT = "last_location_at";

    public static final String KEY_METRIC_CGM_NETWORK_ERROR = "metric_cgm_network_error";
    public static final String KEY_METRIC_CGM_SCHEMA_ERROR = "metric_cgm_schema_error";
    public static final String KEY_METRIC_SMS_SEND_FAILURE = "metric_sms_send_failure";
    public static final String KEY_METRIC_DECRYPT_FAILURE = "metric_decrypt_failure";
    public static final String KEY_METRIC_UNTRUSTED_SENDER = "metric_untrusted_sender";
    public static final String KEY_METRIC_FALL_SENSOR_UNAVAILABLE = "metric_fall_sensor_unavailable";

    public static final String KEY_ML_HISTORY_JSON = "ml_history_json";
    public static final String KEY_ML_STATUS = "ml_status";
    public static final String KEY_ML_PREDICTION_MGDL = "ml_prediction_mgdl";
    public static final String KEY_ML_PREDICTION_AT = "ml_prediction_at";
    public static final String KEY_ML_LAST_INFERENCE_AT = "ml_last_inference_at";
    public static final String KEY_ML_PREDICTION_SERIES_JSON = "ml_prediction_series_json";
    public static final String KEY_ML_CHART_WINDOW_SIZE = "ml_chart_window_size";

    private AppPrefs() {
    }
}
