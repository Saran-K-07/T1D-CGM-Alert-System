package com.example.t1dalert;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CgmRepository {

    public interface Listener {
        void onSuccess(CgmData data);

        void onSchemaError();

        void onNetworkError();
    }

    private final Context appContext;
    private final RequestQueue requestQueue;

    public CgmRepository(Context context, RequestQueue requestQueue) {
        this.appContext = context.getApplicationContext();
        this.requestQueue = requestQueue;
    }

    public void fetchLatest(Listener listener) {
        SharedPreferences prefs = AppPrefsStore.get(appContext);
        String url = prefs.getString(AppPrefs.KEY_NIGHTSCOUT_URL, "");
        String apiToken = prefs.getString(AppPrefs.KEY_API_TOKEN, "");
        String accessToken = prefs.getString(AppPrefs.KEY_ACCESS_TOKEN, "");
        boolean developerMode = prefs.getBoolean(AppPrefs.KEY_DEVELOPER_MODE, false);

        final String primaryUrl;
        try {
            primaryUrl = CgmUtils.buildNightscoutEntriesUrl(url);
        } catch (Exception e) {
            listener.onSchemaError();
            AppMetrics.increment(appContext, AppPrefs.KEY_METRIC_CGM_SCHEMA_ERROR);
            return;
        }

        final boolean[] triedCompatibilityFallback = {false};
        sendRequest(primaryUrl, apiToken, accessToken, false, new Listener() {
            @Override
            public void onSuccess(CgmData data) {
                listener.onSuccess(data);
            }

            @Override
            public void onSchemaError() {
                if (developerMode && !triedCompatibilityFallback[0]) {
                    triedCompatibilityFallback[0] = true;
                    tryLegacy(listener, url, apiToken, accessToken);
                    return;
                }
                listener.onSchemaError();
                AppMetrics.increment(appContext, AppPrefs.KEY_METRIC_CGM_SCHEMA_ERROR);
            }

            @Override
            public void onNetworkError() {
                if (developerMode && !triedCompatibilityFallback[0]) {
                    triedCompatibilityFallback[0] = true;
                    tryLegacy(listener, url, apiToken, accessToken);
                    return;
                }
                listener.onNetworkError();
                AppMetrics.increment(appContext, AppPrefs.KEY_METRIC_CGM_NETWORK_ERROR);
            }
        });
    }

    private void tryLegacy(Listener listener, String url, String apiToken, String accessToken) {
        final String legacyUrl;
        try {
            legacyUrl = CgmUtils.buildLegacyNightscoutEntriesUrl(url, apiToken, accessToken);
        } catch (Exception e) {
            listener.onSchemaError();
            AppMetrics.increment(appContext, AppPrefs.KEY_METRIC_CGM_SCHEMA_ERROR);
            return;
        }
        sendRequest(legacyUrl, apiToken, accessToken, true, listener);
    }

    private void sendRequest(String apiUrl, String apiToken, String accessToken, boolean compatibilityMode, Listener listener) {
        StringRequest request = new StringRequest(Request.Method.GET, apiUrl,
                response -> {
                    try {
                        CgmData data = CgmParser.parseLatest(response);
                        listener.onSuccess(data);
                    } catch (Exception e) {
                        listener.onSchemaError();
                    }
                },
                error -> handleNetworkError(error, listener)) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String trimmedApiToken = apiToken == null ? "" : apiToken.trim();
                if (!trimmedApiToken.isEmpty()) {
                    if (compatibilityMode) {
                        headers.put("api-secret", trimmedApiToken);
                    } else {
                        String hashed = sha1Hex(trimmedApiToken);
                        if (!hashed.isEmpty()) {
                            headers.put("api-secret", hashed);
                        }
                    }
                }
                String trimmedAccessToken = accessToken == null ? "" : accessToken.trim();
                if (!trimmedAccessToken.isEmpty()) {
                    headers.put("Authorization", "Bearer " + trimmedAccessToken);
                }
                return headers;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    private void handleNetworkError(VolleyError error, Listener listener) {
        listener.onNetworkError();
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format(Locale.US, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
