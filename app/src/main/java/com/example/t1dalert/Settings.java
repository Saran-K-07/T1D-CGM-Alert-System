package com.example.t1dalert;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class Settings extends AppCompatActivity {

    private TextInputEditText nightscoutUrlEditText;
    private TextInputEditText apiTokenEditText;
    private TextInputEditText accessTokenEditText;
    private TextInputEditText lowSgvEditText;
    private TextInputEditText highSgvEditText;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        nightscoutUrlEditText = findViewById(R.id.nightscout_url);
        apiTokenEditText = findViewById(R.id.api_token);
        accessTokenEditText = findViewById(R.id.access_token);
        lowSgvEditText = findViewById(R.id.low_sgv);
        highSgvEditText = findViewById(R.id.high_sgv);
        saveButton = findViewById(R.id.save_button);

        loadSettings();

        saveButton.setOnClickListener(v -> {
            saveSettings();
            finish();
        });
    }

    private void loadSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String nightscoutUrl = sharedPreferences.getString(MainActivity.KEY_NIGHTSCOUT_URL, "");
        String apiToken = sharedPreferences.getString(MainActivity.KEY_API_TOKEN, "");
        String accessToken = sharedPreferences.getString(MainActivity.KEY_ACCESS_TOKEN, "");
        String lowSgv = sharedPreferences.getString(MainActivity.KEY_LOW_SGV, "70");
        String highSgv = sharedPreferences.getString(MainActivity.KEY_HIGH_SGV, "180");

        nightscoutUrlEditText.setText(nightscoutUrl);
        apiTokenEditText.setText(apiToken);
        accessTokenEditText.setText(accessToken);
        lowSgvEditText.setText(lowSgv);
        highSgvEditText.setText(highSgv);
    }

    private void saveSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String nightscoutUrl = nightscoutUrlEditText.getText().toString().trim();
        String apiToken = apiTokenEditText.getText().toString().trim();
        String accessToken = accessTokenEditText.getText().toString().trim();
        String lowSgvString = lowSgvEditText.getText().toString().trim();
        String highSgvString = highSgvEditText.getText().toString().trim();

        editor.putString(MainActivity.KEY_NIGHTSCOUT_URL, nightscoutUrl);
        editor.putString(MainActivity.KEY_API_TOKEN, apiToken);
        editor.putString(MainActivity.KEY_ACCESS_TOKEN, accessToken);
        editor.putString(MainActivity.KEY_LOW_SGV, lowSgvString);
        editor.putString(MainActivity.KEY_HIGH_SGV, highSgvString);

        editor.apply();

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }
}
