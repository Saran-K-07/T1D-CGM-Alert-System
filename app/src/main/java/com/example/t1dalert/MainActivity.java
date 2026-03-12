package com.example.t1dalert;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "T1DAlertPrefs";
    public static final String KEY_NIGHTSCOUT_URL = "nightscout_url";
    public static final String KEY_API_TOKEN = "api_token";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_LOW_SGV = "low_sgv";
    public static final String KEY_HIGH_SGV = "high_sgv";
    public static final String KEY_CONTACT_1 = "contact_1";
    public static final String KEY_CONTACT_2 = "contact_2";
    public static final String KEY_CONTACT_3 = "contact_3";
    public static final String KEY_CONTACT_4 = "contact_4";
    public static final String KEY_CONTACT_5 = "contact_5";
    public static final String KEY_CONTACT_1_NAME = "contact_1_name";
    public static final String KEY_CONTACT_2_NAME = "contact_2_name";
    public static final String KEY_CONTACT_3_NAME = "contact_3_name";
    public static final String KEY_CONTACT_4_NAME = "contact_4_name";
    public static final String KEY_CONTACT_5_NAME = "contact_5_name";
    public static final String KEY_LOW_ALERT_START_TIME = "low_alert_start_time";
    public static final String KEY_OVERLAY_ACTIVE = "overlay_active";
    public static final String KEY_SMS_SENT = "sms_sent";
    public static final String KEY_LAST_ALERTED_SGV = "last_alerted_sgv";
    public static final String KEY_FALL_DETECTED = "fall_detected";
    public static final String KEY_FALL_DETECTED_AT = "fall_detected_at";

    private TextInputEditText nightscoutUrlEditText;
    private TextInputEditText apiTokenEditText;
    private TextInputEditText accessTokenEditText;
    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Settings.canDrawOverlays(this)) {
                    startServiceAndLaunch();
                } else {
                    Toast.makeText(this, "Overlay permission required for full-screen alerts", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String url = sharedPreferences.getString(KEY_NIGHTSCOUT_URL, "");
        String apiToken = sharedPreferences.getString(KEY_API_TOKEN, "");
        String accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, "");

        if (!url.isEmpty() && !apiToken.isEmpty() && !accessToken.isEmpty()) {
            checkDndPermission(); // Check after data is set
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
        Button submitButton = findViewById(R.id.submit);

        submitButton.setOnClickListener(v -> {
                String nightscoutUrl = String.valueOf(nightscoutUrlEditText.getText()).trim();
                String inputApiToken = String.valueOf(apiTokenEditText.getText()).trim();
                String inputAccessToken = String.valueOf(accessTokenEditText.getText()).trim();

                if (nightscoutUrl.isEmpty() || inputApiToken.isEmpty() || inputAccessToken.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_NIGHTSCOUT_URL, nightscoutUrl);
                editor.putString(KEY_API_TOKEN, inputApiToken);
                editor.putString(KEY_ACCESS_TOKEN, inputAccessToken);
                editor.apply();
                Toast.makeText(MainActivity.this, "Saved!", Toast.LENGTH_SHORT).show();
                launchNextActivity();
        });
    }

    private void checkDndPermission() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) {
            Toast.makeText(this, "Grant 'Do Not Disturb access' in Settings > Sound > Do Not Disturb > Allow exceptions > Apps > T1DAlert for reliable alarms", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        } else {
            Log.d("MainActivity", "DND access granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 || requestCode == 101 || requestCode == 102) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchNextActivity();
            } else {
                Toast.makeText(this, "Permission required for app features to work", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchNextActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            return; // Wait for permission result
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 101);
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 102);
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            overlayPermissionLauncher.launch(intent);
            Toast.makeText(this, "Grant 'Display over other apps' permission for full-screen alerts", Toast.LENGTH_SHORT).show();
            return; // Wait for result
        }
        startServiceAndLaunch();
    }

    private void startServiceAndLaunch() {
        Intent serviceIntent = new Intent(MainActivity.this, CgmBackgroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        Intent intent = new Intent(MainActivity.this, LiveCgmActivity.class);
        startActivity(intent);
        finish();
    }
}
