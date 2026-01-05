package com.example.t1dalert;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class LiveCgmActivity extends AppCompatActivity {

    private TextView cgmValueTextView;
    private TextView cgmTrendTextView;
    private Button settingsButton;
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
        settingsButton = findViewById(R.id.settings_button);

        requestQueue = Volley.newRequestQueue(LiveCgmActivity.this);

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(LiveCgmActivity.this, Settings.class);
            startActivity(intent);
        });

        handler.post(cgmDataRefresher);
    }

    private void fetchCgmData() {
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String nightscoutUrl = sharedPreferences.getString(MainActivity.KEY_NIGHTSCOUT_URL, "");
        String apiToken = sharedPreferences.getString(MainActivity.KEY_API_TOKEN, "");
        String accessToken = sharedPreferences.getString(MainActivity.KEY_ACCESS_TOKEN, "");
        String lowSgvString = sharedPreferences.getString(MainActivity.KEY_LOW_SGV, "70");
        String highSgvString = sharedPreferences.getString(MainActivity.KEY_HIGH_SGV, "180");

        if (nightscoutUrl.isEmpty()) {
            Toast.makeText(this, "Nightscout URL not set", Toast.LENGTH_SHORT).show();
            return;
        }

        int lowSgv = 70;
        int highSgv = 180;
        try {
            lowSgv = Integer.parseInt(lowSgvString);
            highSgv = Integer.parseInt(highSgvString);
        } catch (NumberFormatException e) {
            Log.e("LiveCgmActivity", "Invalid SGV values in SharedPreferences", e);
        }

        String apiUrl = nightscoutUrl + "/api/v1/entries?count=1&token=" + accessToken;

        int finalLowSgv = lowSgv;
        int finalHighSgv = highSgv;
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
                                        int sgv = Integer.parseInt(parts[2]);
                                        String direction = parts[3].replace("\"", "");

                                        cgmValueTextView.setText(String.valueOf(sgv));
                                        cgmTrendTextView.setText(getTrendArrow(direction));

                                        if (sgv < finalLowSgv || sgv > finalHighSgv) {
                                            cgmValueTextView.setTextColor(Color.RED);
                                        } else {
                                            cgmValueTextView.setTextColor(Color.BLACK);
                                        }

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
                }}) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("api-secret", apiToken);
                return headers;
            }
        };

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
