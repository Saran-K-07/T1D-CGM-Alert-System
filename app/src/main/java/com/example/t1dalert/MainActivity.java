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

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_APP_RUNTIME_PERMISSIONS = 200;

    private TextInputEditText nightscoutUrlEditText;
    private TextInputEditText apiTokenEditText;
    private TextInputEditText accessTokenEditText;
    private TextInputEditText userPhoneEditText;
    private final ActivityResultLauncher<ScanOptions> qrScanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result == null || result.getContents() == null || result.getContents().trim().isEmpty()) {
                    return;
                }

                SettingsQrCodec.ParsedSettings parsed = SettingsQrCodec.fromPayload(result.getContents().trim());
                if (parsed == null) {
                    Toast.makeText(this, R.string.invalid_settings_qr, Toast.LENGTH_SHORT).show();
                    return;
                }
                applyScannedSettings(parsed);
            });
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
        String userPhone = sharedPreferences.getString(AppPrefs.KEY_USER_PHONE, "");

        if (!url.isEmpty() && !apiToken.isEmpty() && !accessToken.isEmpty() && !userPhone.isEmpty()) {
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
        userPhoneEditText = findViewById(R.id.user_phone);
        Button submitButton = findViewById(R.id.submit);
        Button scanSettingsQrButton = findViewById(R.id.scan_settings_qr_button_main);

        nightscoutUrlEditText.setText(url);
        apiTokenEditText.setText(apiToken);
        accessTokenEditText.setText(accessToken);
        userPhoneEditText.setText(userPhone);

        submitButton.setOnClickListener(v -> {
                String nightscoutUrl = String.valueOf(nightscoutUrlEditText.getText()).trim();
                String inputApiToken = String.valueOf(apiTokenEditText.getText()).trim();
                String inputAccessToken = String.valueOf(accessTokenEditText.getText()).trim();
                String inputUserPhone = String.valueOf(userPhoneEditText.getText()).trim();

                if (nightscoutUrl.isEmpty() || inputApiToken.isEmpty() || inputAccessToken.isEmpty() || inputUserPhone.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(AppPrefs.KEY_NIGHTSCOUT_URL, nightscoutUrl);
                editor.putString(AppPrefs.KEY_API_TOKEN, inputApiToken);
                editor.putString(AppPrefs.KEY_ACCESS_TOKEN, inputAccessToken);
                editor.putString(AppPrefs.KEY_USER_PHONE, inputUserPhone);
                AlertKeyManager.getOrCreateSharedAlertKey(sharedPreferences);
                editor.apply();
                Toast.makeText(MainActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                launchNextActivity();
        });

        scanSettingsQrButton.setOnClickListener(v -> scanSettingsQr());
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
        if (requestCode == AppConfig.REQUEST_CAMERA) {
            if (allPermissionsGranted(grantResults)) {
                launchQrScanner();
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (requestCode == REQUEST_APP_RUNTIME_PERMISSIONS || requestCode == AppConfig.REQUEST_POST_NOTIFICATIONS) {
            if (allPermissionsGranted(grantResults)) {
                launchNextActivity();
            } else {
                Toast.makeText(this, R.string.app_permissions_required, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchNextActivity() {
        String[] missingPermissions = getMissingRuntimePermissions();
        if (missingPermissions.length > 0) {
            ActivityCompat.requestPermissions(this, missingPermissions, REQUEST_APP_RUNTIME_PERMISSIONS);
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

    private String[] getMissingRuntimePermissions() {
        List<String> missing = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECEIVE_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        return missing.toArray(new String[0]);
    }

    private boolean allPermissionsGranted(int[] grantResults) {
        if (grantResults == null || grantResults.length == 0) {
            return false;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void scanSettingsQr() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, AppConfig.REQUEST_CAMERA);
            return;
        }
        launchQrScanner();
    }

    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt(getString(R.string.scan_settings_qr_prompt));
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        qrScanLauncher.launch(options);
    }

    private void applyScannedSettings(SettingsQrCodec.ParsedSettings parsed) {
        SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        SettingsSyncHelper.applyParsedSettings(prefs, parsed);
        nightscoutUrlEditText.setText(parsed.nightscoutUrl);
        apiTokenEditText.setText(parsed.apiToken);
        accessTokenEditText.setText(parsed.accessToken);
        Toast.makeText(this, R.string.settings_imported, Toast.LENGTH_LONG).show();
    }
}
