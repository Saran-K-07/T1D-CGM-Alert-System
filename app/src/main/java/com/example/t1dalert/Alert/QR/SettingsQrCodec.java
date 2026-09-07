package com.example.t1dalert.Alert.QR;

import org.json.JSONException;
import org.json.JSONObject;

public final class SettingsQrCodec {

    private static final String FIELD_VERSION = "v";
    private static final String FIELD_URL = "url";
    private static final String FIELD_ESCALATION_NUMBER = "escalation";
    private static final String FIELD_LOW_SGV = "low";
    private static final String FIELD_HIGH_SGV = "high";

    private SettingsQrCodec() {
    }

    public static String toPayload(
            String nightscoutUrl,
            String escalationNumber,
            String lowSgv,
            String highSgv
    ) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(FIELD_VERSION, 2);
            obj.put(FIELD_URL, safe(nightscoutUrl));
            obj.put(FIELD_ESCALATION_NUMBER, safe(escalationNumber));
            obj.put(FIELD_LOW_SGV, safe(lowSgv));
            obj.put(FIELD_HIGH_SGV, safe(highSgv));
        } catch (JSONException ignored) {
        }
        return "T1DSET:" + obj.toString();
    }

    public static ParsedSettings fromPayload(String payload) {
        if (payload == null || !payload.startsWith("T1DSET:")) {
            return null;
        }

        String json = payload.substring("T1DSET:".length());
        try {
            JSONObject obj = new JSONObject(json);
            return new ParsedSettings(
                    obj.optString(FIELD_URL, ""),
                    obj.optString(FIELD_ESCALATION_NUMBER, ""),
                    obj.optString(FIELD_LOW_SGV, "70"),
                    obj.optString(FIELD_HIGH_SGV, "180")
            );
        } catch (JSONException e) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class ParsedSettings {
        public final String nightscoutUrl;
        public final String escalationNumber;
        public final String lowSgv;
        public final String highSgv;

        public ParsedSettings(String nightscoutUrl, String escalationNumber, String lowSgv, String highSgv) {
            this.nightscoutUrl = nightscoutUrl;
            this.escalationNumber = escalationNumber;
            this.lowSgv = lowSgv;
            this.highSgv = highSgv;
        }
    }
}
