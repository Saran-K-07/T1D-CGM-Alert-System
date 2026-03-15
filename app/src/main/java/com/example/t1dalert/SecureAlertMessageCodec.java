package com.example.t1dalert;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class SecureAlertMessageCodec {

    private static final String KEY_ALERT_ENCRYPTION_SECRET = "alert_encryption_secret";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int KEY_LENGTH_BYTES = 32;

    private SecureAlertMessageCodec() {
    }

    public static String encrypt(Context context, SharedPreferences sharedPreferences, String plainMessage) {
        if (plainMessage == null || plainMessage.isEmpty()) {
            return plainMessage == null ? "" : plainMessage;
        }

        try {
            SecretKeySpec key = new SecretKeySpec(getOrCreateSecret(sharedPreferences), "AES");
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainMessage.getBytes(StandardCharsets.UTF_8));

            String ivEncoded = Base64.encodeToString(iv, Base64.NO_WRAP);
            String payloadEncoded = Base64.encodeToString(cipherText, Base64.NO_WRAP);
            return "ENCv1:" + ivEncoded + ":" + payloadEncoded;
        } catch (GeneralSecurityException e) {
            return plainMessage;
        }
    }

    public static String decrypt(SharedPreferences sharedPreferences, String encodedMessage) {
        if (encodedMessage == null || !encodedMessage.startsWith("ENCv1:")) {
            return encodedMessage == null ? "" : encodedMessage;
        }

        try {
            String[] parts = encodedMessage.split(":", 3);
            if (parts.length != 3) {
                return "";
            }

            byte[] iv = Base64.decode(parts[1], Base64.NO_WRAP);
            byte[] cipherBytes = Base64.decode(parts[2], Base64.NO_WRAP);
            SecretKeySpec key = new SecretKeySpec(getOrCreateSecret(sharedPreferences), "AES");

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static byte[] getOrCreateSecret(SharedPreferences sharedPreferences) {
        String accessToken = sharedPreferences.getString(MainActivity.KEY_ACCESS_TOKEN, "");
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                return Arrays.copyOf(digest.digest(accessToken.getBytes(StandardCharsets.UTF_8)), KEY_LENGTH_BYTES);
            } catch (Exception ignored) {
            }
        }

        String base64 = sharedPreferences.getString(KEY_ALERT_ENCRYPTION_SECRET, "");
        if (base64 != null && !base64.isEmpty()) {
            byte[] key = Base64.decode(base64, Base64.NO_WRAP);
            if (key.length == KEY_LENGTH_BYTES) {
                return key;
            }
        }

        byte[] freshKey = new byte[KEY_LENGTH_BYTES];
        new SecureRandom().nextBytes(freshKey);
        String freshKeyEncoded = Base64.encodeToString(freshKey, Base64.NO_WRAP);
        sharedPreferences.edit().putString(KEY_ALERT_ENCRYPTION_SECRET, freshKeyEncoded).apply();
        return freshKey;
    }
}
