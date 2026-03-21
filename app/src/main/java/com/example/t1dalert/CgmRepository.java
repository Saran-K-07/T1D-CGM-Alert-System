package com.example.t1dalert;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

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
        SharedPreferences prefs = appContext.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        String url = prefs.getString(AppPrefs.KEY_NIGHTSCOUT_URL, "");
        String apiToken = prefs.getString(AppPrefs.KEY_API_TOKEN, "");
        String accessToken = prefs.getString(AppPrefs.KEY_ACCESS_TOKEN, "");

        final String apiUrl;
        try {
            apiUrl = CgmUtils.buildNightscoutEntriesUrl(url, apiToken, accessToken);
        } catch (Exception e) {
            listener.onSchemaError();
            AppMetrics.increment(appContext, AppPrefs.KEY_METRIC_CGM_SCHEMA_ERROR);
            return;
        }

        StringRequest request = new StringRequest(Request.Method.GET, apiUrl,
                response -> {
                    try {
                        CgmData data = CgmParser.parseLatest(response);
                        listener.onSuccess(data);
                    } catch (Exception e) {
                        listener.onSchemaError();
                        AppMetrics.increment(appContext, AppPrefs.KEY_METRIC_CGM_SCHEMA_ERROR);
                    }
                },
                error -> {
                    listener.onNetworkError();
                    AppMetrics.increment(appContext, AppPrefs.KEY_METRIC_CGM_NETWORK_ERROR);
                });

        request.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }
}
