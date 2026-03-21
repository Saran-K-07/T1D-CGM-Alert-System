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

        if (!message.startsWith(PREFIX)) {
            return DecodeResult.plain(message);
        }

        String sharedKey = AlertKeyManager.getOrCreateSharedAlertKey(sharedPreferences);
        return decryptWithSharedKey(sharedKey, message);
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
            JSONObject envelope = new JSONObject(message.substring(PREFIX.length()));
            String version = envelope.optString("v", "");
            String nonceB64 = envelope.optString("n", "");
            String payloadB64 = envelope.optString("p", "");
            String hmacB64 = envelope.optString("h", "");

            if (!VERSION.equals(version) || nonceB64.isEmpty() || payloadB64.isEmpty() || hmacB64.isEmpty()) {
                return DecodeResult.invalid("schema_invalid");
            }

            byte[] keyBytes = deriveKey(sharedKey);
            String signingInput = version + "." + nonceB64 + "." + payloadB64;
            String computed = Base64.getEncoder().withoutPadding().encodeToString(hmacSha256(keyBytes, signingInput.getBytes(StandardCharsets.UTF_8)));
            if (!constantTimeEquals(hmacB64, computed)) {
                return DecodeResult.invalid("hmac_mismatch");
            }

            String payload = new String(Base64.getDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            return DecodeResult.secure(payload);
        } catch (Exception e) {
            return DecodeResult.invalid("decode_error");
        }
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
