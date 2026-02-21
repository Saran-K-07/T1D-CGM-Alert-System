package com.example.t1dalert;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Toast;

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

    private TextInputEditText nightscoutUrlEditText;
    private TextInputEditText apiTokenEditText;
    private TextInputEditText accessTokenEditText;
    private Button submitButton;
    private static final int REQUEST_OVERLAY_PERMISSION = 200;

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
                SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_NIGHTSCOUT_URL, nightscoutUrl);
                editor.putString(KEY_API_TOKEN, apiToken);
                editor.putString(KEY_ACCESS_TOKEN, accessToken);
                editor.apply();
                Toast.makeText(MainActivity.this, "Saved!", Toast.LENGTH_SHORT).show();
                launchNextActivity();
            }
        });
    }

    private void checkDndPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (!nm.isNotificationPolicyAccessGranted()) {
                Toast.makeText(this, "Grant 'Do Not Disturb access' in Settings > Sound > Do Not Disturb > Allow exceptions > Apps > T1DAlert for reliable alarms", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
            } else {
                Log.d("MainActivity", "DND access granted");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startServiceAndLaunch();
            } else {
                Toast.makeText(this, "Overlay permission required for full-screen alerts", Toast.LENGTH_SHORT).show();
                // Optionally, proceed without overlay or ask again
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
                return; // Wait for permission result
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 101);
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 102);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            Toast.makeText(this, "Grant 'Display over other apps' permission for full-screen alerts", Toast.LENGTH_SHORT).show();
            return; // Wait for result
        }
        startServiceAndLaunch();
    }

    private void startServiceAndLaunch() {
        Intent serviceIntent = new Intent(MainActivity.this, CgmBackgroundService.class);
        startService(serviceIntent);
        Intent intent = new Intent(MainActivity.this, LiveCgmActivity.class);
        startActivity(intent);
        finish();
    }
}