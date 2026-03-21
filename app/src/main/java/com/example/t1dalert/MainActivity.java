package com.example.t1dalert;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
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
    private TextInputEditText nightscoutUrlEditText;
    private TextInputEditText apiTokenEditText;
    private TextInputEditText accessTokenEditText;
    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Settings.canDrawOverlays(this)) {
                    startServiceAndLaunch();
                } else {
                    Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        String url = sharedPreferences.getString(AppPrefs.KEY_NIGHTSCOUT_URL, "");
        String apiToken = sharedPreferences.getString(AppPrefs.KEY_API_TOKEN, "");
        String accessToken = sharedPreferences.getString(AppPrefs.KEY_ACCESS_TOKEN, "");

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
                    Toast.makeText(MainActivity.this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(AppPrefs.KEY_NIGHTSCOUT_URL, nightscoutUrl);
                editor.putString(AppPrefs.KEY_API_TOKEN, inputApiToken);
                editor.putString(AppPrefs.KEY_ACCESS_TOKEN, inputAccessToken);
                AlertKeyManager.getOrCreateSharedAlertKey(sharedPreferences);
                editor.apply();
                Toast.makeText(MainActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                launchNextActivity();
        });
    }

    private void checkDndPermission() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) {
            Toast.makeText(this, R.string.dnd_permission_hint, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppConfig.REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchNextActivity();
            } else {
                Toast.makeText(this, R.string.notification_permission_required, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchNextActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, AppConfig.REQUEST_POST_NOTIFICATIONS);
            return; // Wait for permission result
        }
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            overlayPermissionLauncher.launch(intent);
            Toast.makeText(this, R.string.overlay_permission_hint, Toast.LENGTH_SHORT).show();
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
