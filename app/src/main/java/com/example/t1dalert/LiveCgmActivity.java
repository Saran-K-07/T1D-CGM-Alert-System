package com.example.t1dalert;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class LiveCgmActivity extends AppCompatActivity {

    private TextView cgmValueTextView;
    private TextView cgmTrendTextView;
    private TextView mlPredictionValueTextView;
    private TextView mlPredictionStatusTextView;
    private CgmRepository cgmRepository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable cgmDataRefresher = new Runnable() {
        @Override
        public void run() {
            fetchCgmData();
            handler.postDelayed(this, AppConfig.CGM_REFRESH_INTERVAL_MS);
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
        mlPredictionValueTextView = findViewById(R.id.ml_prediction_value);
        mlPredictionStatusTextView = findViewById(R.id.ml_prediction_status);
        Button settingsButton = findViewById(R.id.settings_button);

        RequestQueue requestQueue = Volley.newRequestQueue(LiveCgmActivity.this);
        cgmRepository = new CgmRepository(this, requestQueue);

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(LiveCgmActivity.this, Settings.class);
            startActivity(intent);
        });

        handler.post(cgmDataRefresher);
    }

    private void fetchCgmData() {
        SharedPreferences sharedPreferences = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        if (sharedPreferences.getString(AppPrefs.KEY_NIGHTSCOUT_URL, "").trim().isEmpty()) {
            cgmValueTextView.setText(R.string.live_cgm_missing_url_short);
            cgmTrendTextView.setText(R.string.live_cgm_trend_unavailable_short);
            Toast.makeText(this, R.string.nightscout_url_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        int low = CgmUtils.parseIntOrDefault(
                sharedPreferences.getString(AppPrefs.KEY_LOW_SGV, String.valueOf(AppConfig.DEFAULT_LOW_SGV)),
                AppConfig.DEFAULT_LOW_SGV
        );
        int high = CgmUtils.parseIntOrDefault(
                sharedPreferences.getString(AppPrefs.KEY_HIGH_SGV, String.valueOf(AppConfig.DEFAULT_HIGH_SGV)),
                AppConfig.DEFAULT_HIGH_SGV
        );

        cgmRepository.fetchLatest(new CgmRepository.Listener() {
            @Override
            public void onSuccess(CgmData data) {
                cgmValueTextView.setText(String.valueOf(data.sgv));
                cgmTrendTextView.setText(CgmUtils.getTrendArrow(data.direction));
                if (data.sgv < low || data.sgv > high) {
                    cgmValueTextView.setTextColor(ContextCompat.getColor(LiveCgmActivity.this, R.color.cgm_alert_red));
                } else {
                    cgmValueTextView.setTextColor(ContextCompat.getColor(LiveCgmActivity.this, R.color.cgm_text_normal));
                }
                updateMlStatus(sharedPreferences);
            }

            @Override
            public void onSchemaError() {
                cgmValueTextView.setText(R.string.live_cgm_format_short);
                cgmTrendTextView.setText(R.string.live_cgm_trend_unavailable_short);
                updateMlStatus(sharedPreferences);
                Toast.makeText(LiveCgmActivity.this, R.string.error_parsing_data, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNetworkError() {
                cgmValueTextView.setText(R.string.live_cgm_loading_failed_short);
                cgmTrendTextView.setText(R.string.live_cgm_trend_unavailable_short);
                updateMlStatus(sharedPreferences);
                Toast.makeText(LiveCgmActivity.this, R.string.error_fetching_data, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMlStatus(SharedPreferences prefs) {
        String status = prefs.getString(AppPrefs.KEY_ML_STATUS, MlRuntimeStatus.WARMING_UP);
        int prediction = prefs.getInt(AppPrefs.KEY_ML_PREDICTION_MGDL, Integer.MIN_VALUE);

        if (MlRuntimeStatus.READY.equals(status) && prediction != Integer.MIN_VALUE) {
            mlPredictionValueTextView.setText(getString(R.string.ml_prediction_value, prediction));
            mlPredictionStatusTextView.setText(R.string.ml_status_ready);
            return;
        }

        mlPredictionValueTextView.setText(R.string.ml_prediction_placeholder);
        if (MlRuntimeStatus.MODEL_UNAVAILABLE.equals(status)) {
            mlPredictionStatusTextView.setText(R.string.ml_status_model_unavailable);
        } else if (MlRuntimeStatus.METADATA_INVALID.equals(status)) {
            mlPredictionStatusTextView.setText(R.string.ml_status_metadata_invalid);
        } else if (MlRuntimeStatus.PREDICTION_FAILED.equals(status)) {
            mlPredictionStatusTextView.setText(R.string.ml_status_prediction_failed);
        } else {
            mlPredictionStatusTextView.setText(R.string.ml_status_warming);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(cgmDataRefresher);
    }
}
