package com.example.t1dalert;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import android.content.Context;
import android.content.SharedPreferences;

public class LiveCgmActivity extends AppCompatActivity {

    private TextView cgmValueTextView;
    private TextView cgmTrendTextView;
    private RequestQueue requestQueue;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable cgmDataRefresher = new Runnable() {
        @Override
        public void run() {
            fetchCgmData();
            final int REFRESH_INTERVAL = 60000; // 1 minute
            handler.postDelayed(this, REFRESH_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_livecgm);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cgmValueTextView = findViewById(R.id.cgm_value);
        cgmTrendTextView = findViewById(R.id.cgm_trend);

        requestQueue = Volley.newRequestQueue(LiveCgmActivity.this);

        handler.post(cgmDataRefresher);
    }

    private void fetchCgmData() {
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String url = sharedPreferences.getString(MainActivity.KEY_NIGHTSCOUT_URL, "");
        String apiToken = sharedPreferences.getString(MainActivity.KEY_API_TOKEN, "");
        String accessToken = sharedPreferences.getString(MainActivity.KEY_ACCESS_TOKEN, "");

        try{
            apiToken = URLEncoder.encode(apiToken, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        if(url.startsWith("http://")) {
            url = url.substring(7);
            url = "http://" + apiToken + "@" + url;
        };

        if(url.startsWith("https://")){
            url = url.substring(8);
            url = "https://" + apiToken + "@" + url;
        }

        if(!url.startsWith("http://")&&!url.startsWith("https://")){
            url = "http://" + apiToken + "@" + url;
        }


        String apiUrl = url + "/api/v1/entries?token=" + accessToken + "&count=1";

        StringRequest request = new StringRequest(Request.Method.GET, apiUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            if (response != null && !response.trim().isEmpty()) {
                                String[] lines = response.split("\n");
                                if (lines.length > 0) {
                                    String[] parts = lines[0].split("\t");

                                    if (parts.length >= 4) {
                                        String sgv = parts[2];
                                        String direction = parts[3].replace("\"", "");

                                        cgmValueTextView.setText(sgv);
                                        cgmTrendTextView.setText(getTrendArrow(direction));
                                    } else {
                                        Log.w("LiveCgmActivity", "Unexpected data format: " + lines[0]);
                                        cgmValueTextView.setText("Fmt");
                                    }
                                }
                            } else {
                                Log.w("LiveCgmActivity", "API returned an empty response.");
                                cgmValueTextView.setText("N/A");
                            }
                        } catch (Exception e) {
                            Log.e("LiveCgmActivity", "Error parsing string response", e);
                            Toast.makeText(LiveCgmActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                            cgmValueTextView.setText("Err");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    Log.e("LiveCgmActivity", "Volley request failed: " + error.toString());
                    Toast.makeText(LiveCgmActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                    cgmValueTextView.setText("---");
                    cgmTrendTextView.setText("X");
                }});

        request.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    private String getTrendArrow(String trendString) {
        if (trendString == null) return "?";
        switch (trendString) {
            case "DoubleUp":
                return "↑↑";
            case "SingleUp":
                return "↑";
            case "FortyFiveUp":
                return "↗";
            case "Flat":
                return "→";
            case "FortyFiveDown":
                return "↘";
            case "SingleDown":
                return "↓";
            case "DoubleDown":
                return "↓↓";
            default:
                return "?";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(cgmDataRefresher);
    }
}
