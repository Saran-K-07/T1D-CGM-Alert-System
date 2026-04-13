package com.example.t1dalert;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ContactShareQrCodec {

    private static final String PREFIX = "T1DCONTACT:";
    private static final String FIELD_VERSION = "v";
    private static final String FIELD_TRUSTED_SENDERS = "trusted";
    private static final String FIELD_USER_PHONE = "phone";
    private static final String FIELD_TRUSTED_RECEIVERS = "receivers";
    private static final String FIELD_RECEIVER_PHONE = "p";
    private static final String FIELD_RECEIVER_NAME = "n";
    private static final String FIELD_MODE = "mode";
    private static final String FIELD_ALERT_KEY = "k";
    private static final String MODE_FULL = "full";
    private static final String MODE_RECEIVER_ONLY = "receiver_only";
    private static final String MODE_EMERGENCY_CONTACT_ONLY = "emergency_contact_only";
    private static final String MODE_TRUSTED_SENDER_ONLY = "trusted_sender_only";

    private ContactShareQrCodec() {
    }

    public static String toPayload(String trustedSendersCsv, String userPhone) {
        return toPayload(trustedSendersCsv, userPhone, new LinkedHashMap<>(), "");
    }

    public static String toPayload(String trustedSendersCsv, String userPhone, Map<String, String> trustedReceivers) {
        return toPayload(trustedSendersCsv, userPhone, trustedReceivers, "");
    }

    public static String toPayload(
            String trustedSendersCsv,
            String userPhone,
            Map<String, String> trustedReceivers,
            String sharedAlertKey
    ) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(FIELD_VERSION, 3);
            obj.put(FIELD_MODE, MODE_FULL);
            obj.put(FIELD_TRUSTED_SENDERS, safe(trustedSendersCsv));
            obj.put(FIELD_USER_PHONE, safe(userPhone));
            obj.put(FIELD_TRUSTED_RECEIVERS, toReceiversJson(trustedReceivers));
            obj.put(FIELD_ALERT_KEY, safe(sharedAlertKey));
        } catch (JSONException ignored) {
        }
        return PREFIX + obj.toString();
    }

    public static String toTrustedReceiverOnlyPayload(String phone, String name) {
        LinkedHashMap<String, String> receiver = new LinkedHashMap<>();
        String safePhone = safe(phone);
        if (!safePhone.isEmpty()) {
            receiver.put(safePhone, safe(name));
        }
        JSONObject obj = new JSONObject();
        try {
            obj.put(FIELD_VERSION, 3);
            obj.put(FIELD_MODE, MODE_RECEIVER_ONLY);
            obj.put(FIELD_TRUSTED_SENDERS, "");
            obj.put(FIELD_USER_PHONE, "");
            obj.put(FIELD_TRUSTED_RECEIVERS, toReceiversJson(receiver));
            obj.put(FIELD_ALERT_KEY, "");
        } catch (JSONException ignored) {
        }
        return PREFIX + obj.toString();
    }

    public static String toEmergencyContactPayload(String phone, String name) {
        return toEmergencyContactPayload(phone, name, "");
    }

    public static String toEmergencyContactPayload(String phone, String name, String sharedAlertKey) {
        LinkedHashMap<String, String> receiver = new LinkedHashMap<>();
        String safePhone = safe(phone);
        if (!safePhone.isEmpty()) {
            receiver.put(safePhone, safe(name));
        }
        JSONObject obj = new JSONObject();
        try {
            obj.put(FIELD_VERSION, 4);
            obj.put(FIELD_MODE, MODE_EMERGENCY_CONTACT_ONLY);
            obj.put(FIELD_TRUSTED_SENDERS, "");
            obj.put(FIELD_USER_PHONE, "");
            obj.put(FIELD_TRUSTED_RECEIVERS, toReceiversJson(receiver));
            obj.put(FIELD_ALERT_KEY, safe(sharedAlertKey));
        } catch (JSONException ignored) {
        }
        return PREFIX + obj.toString();
    }

    public static String toTrustedSenderOnlyPayload(String phone, String name) {
        return toTrustedSenderOnlyPayload(phone, name, "");
    }

    public static String toTrustedSenderOnlyPayload(String phone, String name, String sharedAlertKey) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(FIELD_VERSION, 4);
            obj.put(FIELD_MODE, MODE_TRUSTED_SENDER_ONLY);
            obj.put(FIELD_TRUSTED_SENDERS, safe(phone));
            obj.put(FIELD_USER_PHONE, "");
            obj.put(FIELD_TRUSTED_RECEIVERS, new JSONArray());
            obj.put(FIELD_ALERT_KEY, safe(sharedAlertKey));
        } catch (JSONException ignored) {
        }
        return PREFIX + obj.toString();
    }

    public static ParsedContactShare fromPayload(String payload) {
        if (payload == null || !payload.startsWith(PREFIX)) {
            return null;
        }
        try {
            JSONObject obj = new JSONObject(payload.substring(PREFIX.length()));
            String mode = safe(obj.optString(FIELD_MODE, MODE_FULL));
            return new ParsedContactShare(
                    obj.optString(FIELD_TRUSTED_SENDERS, ""),
                    obj.optString(FIELD_USER_PHONE, ""),
                    parseReceivers(obj.optJSONArray(FIELD_TRUSTED_RECEIVERS)),
                    obj.optString(FIELD_ALERT_KEY, ""),
                    MODE_RECEIVER_ONLY.equals(mode) || MODE_EMERGENCY_CONTACT_ONLY.equals(mode),
                    MODE_TRUSTED_SENDER_ONLY.equals(mode)
            );
        } catch (JSONException e) {
            return null;
        }
    }

    private static JSONArray toReceiversJson(Map<String, String> trustedReceivers) {
        JSONArray array = new JSONArray();
        if (trustedReceivers == null || trustedReceivers.isEmpty()) {
            return array;
        }
        for (Map.Entry<String, String> entry : trustedReceivers.entrySet()) {
            String phone = safe(entry.getKey());
            if (phone.isEmpty()) {
                continue;
            }
            JSONObject receiver = new JSONObject();
            try {
                receiver.put(FIELD_RECEIVER_PHONE, phone);
                receiver.put(FIELD_RECEIVER_NAME, safe(entry.getValue()));
                array.put(receiver);
            } catch (JSONException ignored) {
            }
        }
        return array;
    }

    private static LinkedHashMap<String, String> parseReceivers(JSONArray receivers) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        if (receivers == null) {
            return map;
        }
        for (int i = 0; i < receivers.length(); i++) {
            JSONObject receiver = receivers.optJSONObject(i);
            if (receiver == null) {
                continue;
            }
            String phone = safe(receiver.optString(FIELD_RECEIVER_PHONE, ""));
            if (phone.isEmpty()) {
                continue;
            }
            map.put(phone, safe(receiver.optString(FIELD_RECEIVER_NAME, "")));
        }
        return map;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class ParsedContactShare {
        public final String trustedSenders;
        public final String userPhone;
        public final LinkedHashMap<String, String> trustedReceivers;
        public final String sharedAlertKey;
        public final boolean trustedReceiverOnly;
        public final boolean trustedSenderOnly;

        public ParsedContactShare(
                String trustedSenders,
                String userPhone,
                LinkedHashMap<String, String> trustedReceivers,
                String sharedAlertKey,
                boolean trustedReceiverOnly,
                boolean trustedSenderOnly
        ) {
            this.trustedSenders = trustedSenders;
            this.userPhone = userPhone;
            this.trustedReceivers = trustedReceivers == null ? new LinkedHashMap<>() : trustedReceivers;
            this.sharedAlertKey = safe(sharedAlertKey);
            this.trustedReceiverOnly = trustedReceiverOnly;
            this.trustedSenderOnly = trustedSenderOnly;
        }
    }
}
