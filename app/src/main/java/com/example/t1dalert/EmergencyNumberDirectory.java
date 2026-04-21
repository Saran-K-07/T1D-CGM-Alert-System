package com.example.t1dalert;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EmergencyNumberDirectory {

    private static final String ASSET_FILE = "emergency_numbers.json";
    private static final Map<String, Entry> COUNTRY_MAP = new HashMap<>();
    private static boolean loaded;

    private EmergencyNumberDirectory() {
    }

    public static Entry find(Context context, String countryIso) {
        ensureLoaded(context);
        if (countryIso == null || countryIso.trim().isEmpty()) {
            return null;
        }
        return COUNTRY_MAP.get(countryIso.trim().toUpperCase(Locale.US));
    }

    public static List<String> getAllCountryIsos(Context context) {
        ensureLoaded(context);
        ArrayList<String> items = new ArrayList<>(COUNTRY_MAP.keySet());
        Collections.sort(items);
        return items;
    }

    private static synchronized void ensureLoaded(Context context) {
        if (loaded) {
            return;
        }
        loaded = true;

        try (InputStream inputStream = context.getAssets().open(ASSET_FILE);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            String json = new String(output.toByteArray(), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);
            JSONArray countries = root.optJSONArray("countries");
            if (countries == null) {
                return;
            }
            for (int i = 0; i < countries.length(); i++) {
                JSONObject item = countries.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String iso = normalizeIso(item.optString("iso", ""));
                String emergencyNumber = item.optString("emergencyNumber", "").trim();
                boolean smsSupported = item.optBoolean("smsSupported", false);
                if (TextUtils.isEmpty(iso) || TextUtils.isEmpty(emergencyNumber)) {
                    continue;
                }
                COUNTRY_MAP.put(iso, new Entry(iso, emergencyNumber, smsSupported));
            }
        } catch (Exception ignored) {
        }
    }

    private static String normalizeIso(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim().toUpperCase(Locale.US);
        return cleaned.length() == 2 ? cleaned : "";
    }

    public static final class Entry {
        public final String countryIso;
        public final String emergencyNumber;
        public final boolean smsSupported;

        Entry(String countryIso, String emergencyNumber, boolean smsSupported) {
            this.countryIso = countryIso;
            this.emergencyNumber = emergencyNumber;
            this.smsSupported = smsSupported;
        }
    }
}
