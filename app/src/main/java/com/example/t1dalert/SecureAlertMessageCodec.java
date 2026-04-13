package com.example.t1dalert;

import android.content.SharedPreferences;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class SecureAlertMessageCodec {

    private static final String VERSION = "2";
    private static final String PREFIX = "ALRTv2:";

    private SecureAlertMessageCodec() {
    }

    public static String encrypt(SharedPreferences sharedPreferences, String plainMessage) {
        String sharedKey = AlertKeyManager.getOrCreateSharedAlertKey(sharedPreferences);
        return encryptWithSharedKey(sharedKey, plainMessage);
    }

    public static DecodeResult decrypt(SharedPreferences sharedPreferences, String message) {
        if (message == null || message.trim().isEmpty()) {
            return DecodeResult.invalid("empty_message");
        }

        String trimmed = message.trim();
        if (!trimmed.startsWith(PREFIX)) {
            return DecodeResult.plain(trimmed);
        }

        String sharedKey = sharedPreferences
                .getString(AppPrefs.KEY_SHARED_ALERT_KEY, "")
                .trim();
        return decryptWithSharedKey(sharedKey, trimmed);
    }

    static String encryptWithSharedKey(String sharedKey, String plainMessage) {
        if (plainMessage == null || plainMessage.trim().isEmpty()) {
            return "";
        }

        if (sharedKey.isEmpty()) {
            return plainMessage;
        }

        try {
            byte[] keyBytes = deriveKey(sharedKey);
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            String nonceB64 = Base64.getEncoder().withoutPadding().encodeToString(nonce);
            String payloadB64 = Base64.getEncoder().withoutPadding().encodeToString(plainMessage.getBytes(StandardCharsets.UTF_8));

            String signingInput = VERSION + "." + nonceB64 + "." + payloadB64;
            String hmacB64 = Base64.getEncoder().withoutPadding().encodeToString(hmacSha256(keyBytes, signingInput.getBytes(StandardCharsets.UTF_8)));

            JSONObject envelope = new JSONObject();
            envelope.put("v", VERSION);
            envelope.put("n", nonceB64);
            envelope.put("p", payloadB64);
            envelope.put("h", hmacB64);
            return PREFIX + envelope.toString();
        } catch (Exception e) {
            return plainMessage;
        }
    }

    static DecodeResult decryptWithSharedKey(String sharedKey, String message) {
        if (message == null || message.trim().isEmpty()) {
            return DecodeResult.invalid("empty_message");
        }

        if (!message.startsWith(PREFIX)) {
            return DecodeResult.plain(message);
        }

        if (sharedKey.isEmpty()) {
            return DecodeResult.invalid("missing_shared_key");
        }

        try {
            JSONObject envelope = new JSONObject(extractEnvelopeJson(message.substring(PREFIX.length())));
            String version = envelope.optString("v", "");
            String nonceB64 = envelope.optString("n", "");
            String payloadB64 = envelope.optString("p", "");
            String hmacB64 = envelope.optString("h", "");

            if (!VERSION.equals(version) || nonceB64.isEmpty() || payloadB64.isEmpty() || hmacB64.isEmpty()) {
                return DecodeResult.invalid("schema_invalid");
            }

            byte[] keyBytes = deriveKey(sharedKey);
            String normalizedNonce = normalizeBase64(nonceB64);
            String normalizedPayload = normalizeBase64(payloadB64);
            String signingInput = version + "." + normalizedNonce + "." + normalizedPayload;
            byte[] computedHmac = hmacSha256(keyBytes, signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] incomingHmac = decodeBase64Flexible(hmacB64);
            if (!constantTimeEqualsBytes(incomingHmac, computedHmac)) {
                return DecodeResult.invalid("hmac_mismatch");
            }

            String payload = new String(decodeBase64Flexible(normalizedPayload), StandardCharsets.UTF_8);
            return DecodeResult.secure(payload);
        } catch (Exception e) {
            return DecodeResult.invalid("decode_error");
        }
    }

    private static String extractEnvelopeJson(String raw) {
        String value = raw == null ? "" : raw.trim();
        int firstBrace = value.indexOf('{');
        int lastBrace = value.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return value.substring(firstBrace, lastBrace + 1);
        }
        return value;
    }

    private static byte[] decodeBase64Flexible(String value) {
        String normalized = normalizeBase64(value);
        String padded = addBase64Padding(normalized);
        try {
            return Base64.getDecoder().decode(padded);
        } catch (IllegalArgumentException first) {
            return Base64.getUrlDecoder().decode(padded);
        }
    }

    private static String normalizeBase64(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\n", "").replace("\r", "").replace("=", "");
    }

    private static String addBase64Padding(String value) {
        int mod = value.length() % 4;
        if (mod == 0) {
            return value;
        }
        if (mod == 1) {
            throw new IllegalArgumentException("invalid_base64_length");
        }
        return value + (mod == 2 ? "==" : "=");
    }

    private static byte[] deriveKey(String sharedKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(sharedKey.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ab.length != bb.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < ab.length; i++) {
            r |= ab[i] ^ bb[i];
        }
        return r == 0;
    }

    private static boolean constantTimeEqualsBytes(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length; i++) {
            r |= a[i] ^ b[i];
        }
        return r == 0;
    }

    public static final class DecodeResult {
        public final String message;
        public final boolean isSecure;
        public final boolean isValid;
        public final String errorCode;

        private DecodeResult(String message, boolean isSecure, boolean isValid, String errorCode) {
            this.message = message;
            this.isSecure = isSecure;
            this.isValid = isValid;
            this.errorCode = errorCode;
        }

        static DecodeResult plain(String message) {
            return new DecodeResult(message, false, true, "");
        }

        static DecodeResult secure(String message) {
            return new DecodeResult(message, true, true, "");
        }

        static DecodeResult invalid(String errorCode) {
            return new DecodeResult("", false, false, errorCode);
        }
    }
}
