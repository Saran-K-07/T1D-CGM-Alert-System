package com.example.t1dalert;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Button;
import android.view.View;
import android.widget.Toast;
import android.content.Context;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "T1DAlertPrefs";
    public static final String KEY_NIGHTSCOUT_URL = "nightscout_url";
    public static final String KEY_API_TOKEN = "api_token";
    public static final String KEY_ACCESS_TOKEN = "access_token";

    private TextInputEditText nightscoutUrlEditText;
    private TextInputEditText apiTokenEditText;
    private TextInputEditText accessTokenEditText;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String url = sharedPreferences.getString(KEY_NIGHTSCOUT_URL, "");
        String apiToken = sharedPreferences.getString(KEY_API_TOKEN, "");
        String accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, "");

        if (!url.isEmpty() && !apiToken.isEmpty() && !accessToken.isEmpty()) {
            launchNextActivity();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nightscoutUrlEditText = findViewById(R.id.nightscout_url);
        apiTokenEditText = findViewById(R.id.api_token);
        accessTokenEditText = findViewById(R.id.access_token);
        submitButton = findViewById(R.id.submit);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nightscoutUrl = nightscoutUrlEditText.getText().toString().trim();
                String apiToken = apiTokenEditText.getText().toString().trim();
                String accessToken = accessTokenEditText.getText().toString().trim();

                if (nightscoutUrl.isEmpty() || apiToken.isEmpty() || accessToken.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveData(nightscoutUrl, apiToken, accessToken);
                launchNextActivity();
            }
        });
    }

    private void saveData(String url, String apiToken, String accessToken) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_NIGHTSCOUT_URL, url);
        editor.putString(KEY_API_TOKEN, apiToken);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.apply();
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
    }

    private void launchNextActivity() {
        Intent intent = new Intent(MainActivity.this, LiveCgmActivity.class);
        startActivity(intent);
        finish();
    }
}