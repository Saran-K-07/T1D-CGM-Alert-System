package com.example.t1dalert;

import org.json.JSONException;
import org.json.JSONObject;

public final class ContactShareQrCodec {

    private static final String PREFIX = "T1DCONTACT:";
    private static final String FIELD_VERSION = "v";
    private static final String FIELD_TRUSTED_SENDERS = "trusted";
    private static final String FIELD_USER_PHONE = "phone";

    private ContactShareQrCodec() {
    }

    public static String toPayload(String trustedSendersCsv, String userPhone) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(FIELD_VERSION, 2);
            obj.put(FIELD_TRUSTED_SENDERS, safe(trustedSendersCsv));
            obj.put(FIELD_USER_PHONE, safe(userPhone));
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
            return new ParsedContactShare(
                    obj.optString(FIELD_TRUSTED_SENDERS, ""),
                    obj.optString(FIELD_USER_PHONE, "")
            );
        } catch (JSONException e) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class ParsedContactShare {
        public final String trustedSenders;
        public final String userPhone;

        public ParsedContactShare(String trustedSenders, String userPhone) {
            this.trustedSenders = trustedSenders;
            this.userPhone = userPhone;
        }
    }
}
