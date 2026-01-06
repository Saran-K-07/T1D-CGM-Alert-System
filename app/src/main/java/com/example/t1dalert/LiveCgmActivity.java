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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

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
            // Make sure your Settings activity class is named SettingsActivity
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
            cgmValueTextView.setText("URL?");
            Toast.makeText(this, "Nightscout URL not set in settings", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Corrected URL and API Logic ---
        // Ensure URL is correctly formatted with a trailing slash
        if (!nightscoutUrl.endsWith("/")) {
            nightscoutUrl += "/";
        }

        // Construct the base API URL
        String apiUrl = nightscoutUrl + "api/v1/entries.json?count=1";

        // Append the access token if it exists (for token-based authentication)
        if (!accessToken.isEmpty()) {
            apiUrl += "&token=" + accessToken;
        }

        Log.d("LiveCgmActivity", "Fetching data from: " + apiUrl);

        int finalLowSgv = Integer.parseInt(lowSgvString);
        int finalHighSgv = Integer.parseInt(highSgvString);

        // --- FIX: Use JsonArrayRequest for a JSON API, not StringRequest ---
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, apiUrl, null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            JSONObject entry = response.getJSONObject(0);
                            int sgv = entry.getInt("sgv");
                            String direction = entry.getString("direction");

                            cgmValueTextView.setText(String.valueOf(sgv));
                            cgmTrendTextView.setText(getTrendArrow(direction));

                            // Set text color based on SGV range
                            if (sgv < finalLowSgv || sgv > finalHighSgv) {
                                cgmValueTextView.setTextColor(Color.RED);
                            } else {
                                // FIX: Use a visible color for the "in-range" state
                                cgmValueTextView.setTextColor(Color.BLACK);
                            }
                        } else {
                            Log.w("LiveCgmActivity", "API returned an empty array.");
                            cgmValueTextView.setText("N/A");
                        }
                    } catch (JSONException e) {
                        Log.e("LiveCgmActivity", "Error parsing JSON response", e);
                        cgmValueTextView.setText("Err");
                    }
                },
                error -> {
                    Log.e("LiveCgmActivity", "Volley request failed: " + error.toString());
                    cgmValueTextView.setText("---");
                    cgmTrendTextView.setText("X");
                    Toast.makeText(LiveCgmActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                // Add the API Secret as a header if it exists. This is common for modern Nightscout setups.
                if (!apiToken.isEmpty()) {
                    headers.put("api-secret", apiToken);
                }
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private String getTrendArrow(String trendString) {
        if (trendString == null) return "?";
        switch (trendString) {
            case "DoubleUp": return "↑↑";
            case "SingleUp": return "↑";
            case "FortyFiveUp": return "↗";
            case "Flat": return "→";
            case "FortyFiveDown": return "↘";
            case "SingleDown": return "↓";
            case "DoubleDown": return "↓↓";
            default: return "?";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(cgmDataRefresher);
    }
}
