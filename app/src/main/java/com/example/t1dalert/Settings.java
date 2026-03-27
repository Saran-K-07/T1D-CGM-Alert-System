package com.example.t1dalert;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.EnumMap;
import java.util.Map;

public class Settings extends AppCompatActivity {

    private TextInputEditText nightscoutUrlEditText;
    private TextInputEditText apiTokenEditText;
    private TextInputEditText accessTokenEditText;
    private TextInputEditText lowSgvEditText;
    private TextInputEditText highSgvEditText;
    private TextInputEditText userPhoneEditText;
    private TextView contact1TextView;
    private TextView contact2TextView;
    private TextView contact3TextView;
    private TextView contact4TextView;
    private TextView contact5TextView;
    private Button removeContact1Button;
    private Button removeContact2Button;
    private Button removeContact3Button;
    private Button removeContact4Button;
    private Button removeContact5Button;
    private Button saveButton;
    private Button showSettingsQrButton;
    private Button scanSettingsQrButton;
    private Button showContactsQrButton;
    private Button scanContactsQrButton;

    private static final int CAMERA_PERMISSION_REQUEST = AppConfig.REQUEST_CAMERA;
    private int pendingScanPromptRes = R.string.scan_settings_qr_prompt;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> qrScanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result == null || result.getContents() == null || result.getContents().trim().isEmpty()) {
                    return;
                }

                String scanned = result.getContents().trim();

                SettingsQrCodec.ParsedSettings parsedSettings = SettingsQrCodec.fromPayload(scanned);
                if (parsedSettings != null) {
                    applyScannedSettings(parsedSettings);
                    return;
                }

                ContactShareQrCodec.ParsedContactShare parsedContactShare = ContactShareQrCodec.fromPayload(scanned);
                if (parsedContactShare != null) {
                    applyScannedContactShare(parsedContactShare);
                    return;
                }

                {
                    Toast.makeText(this, R.string.invalid_settings_qr, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        nightscoutUrlEditText = findViewById(R.id.nightscout_url);
        apiTokenEditText = findViewById(R.id.api_token);
        accessTokenEditText = findViewById(R.id.access_token);
        lowSgvEditText = findViewById(R.id.low_sgv);
        highSgvEditText = findViewById(R.id.high_sgv);
        userPhoneEditText = findViewById(R.id.user_phone);
        contact1TextView = findViewById(R.id.contact_1_text);
        contact2TextView = findViewById(R.id.contact_2_text);
        contact3TextView = findViewById(R.id.contact_3_text);
        contact4TextView = findViewById(R.id.contact_4_text);
        contact5TextView = findViewById(R.id.contact_5_text);
        removeContact1Button = findViewById(R.id.remove_contact_1);
        removeContact2Button = findViewById(R.id.remove_contact_2);
        removeContact3Button = findViewById(R.id.remove_contact_3);
        removeContact4Button = findViewById(R.id.remove_contact_4);
        removeContact5Button = findViewById(R.id.remove_contact_5);
        saveButton = findViewById(R.id.save_button);
        showSettingsQrButton = findViewById(R.id.show_settings_qr_button);
        scanSettingsQrButton = findViewById(R.id.scan_settings_qr_button);
        showContactsQrButton = findViewById(R.id.show_contacts_qr_button);
        scanContactsQrButton = findViewById(R.id.scan_contacts_qr_button);

        loadSettings();
        updateContactDisplay();

        removeContact1Button.setOnClickListener(v -> removeContact(0));
        removeContact2Button.setOnClickListener(v -> removeContact(1));
        removeContact3Button.setOnClickListener(v -> removeContact(2));
        removeContact4Button.setOnClickListener(v -> removeContact(3));
        removeContact5Button.setOnClickListener(v -> removeContact(4));

        saveButton.setOnClickListener(v -> {
            saveSettings();
            finish();
        });

        showSettingsQrButton.setOnClickListener(v -> showSettingsQr());
        scanSettingsQrButton.setOnClickListener(v -> scanQr(R.string.scan_settings_qr_prompt));
        showContactsQrButton.setOnClickListener(v -> showContactsQr());
        scanContactsQrButton.setOnClickListener(v -> scanQr(R.string.scan_contacts_qr_prompt));
    }

    private void loadSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        AlertKeyManager.getOrCreateSharedAlertKey(sharedPreferences);
        String nightscoutUrl = sharedPreferences.getString(AppPrefs.KEY_NIGHTSCOUT_URL, "");
        String apiToken = sharedPreferences.getString(AppPrefs.KEY_API_TOKEN, "");
        String accessToken = sharedPreferences.getString(AppPrefs.KEY_ACCESS_TOKEN, "");
        String lowSgv = sharedPreferences.getString(AppPrefs.KEY_LOW_SGV, String.valueOf(AppConfig.DEFAULT_LOW_SGV));
        String highSgv = sharedPreferences.getString(AppPrefs.KEY_HIGH_SGV, String.valueOf(AppConfig.DEFAULT_HIGH_SGV));
        String userPhone = sharedPreferences.getString(AppPrefs.KEY_USER_PHONE, "");
        nightscoutUrlEditText.setText(nightscoutUrl);
        apiTokenEditText.setText(apiToken);
        accessTokenEditText.setText(accessToken);
        lowSgvEditText.setText(lowSgv);
        highSgvEditText.setText(highSgv);
        userPhoneEditText.setText(userPhone);
    }

    private void saveSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String nightscoutUrl = nightscoutUrlEditText.getText().toString().trim();
        String apiToken = apiTokenEditText.getText().toString().trim();
        String accessToken = accessTokenEditText.getText().toString().trim();
        String lowSgvString = lowSgvEditText.getText().toString().trim();
        String highSgvString = highSgvEditText.getText().toString().trim();
        String userPhone = userPhoneEditText.getText().toString().trim();
        editor.putString(AppPrefs.KEY_NIGHTSCOUT_URL, nightscoutUrl);
        editor.putString(AppPrefs.KEY_API_TOKEN, apiToken);
        editor.putString(AppPrefs.KEY_ACCESS_TOKEN, accessToken);
        editor.putString(AppPrefs.KEY_LOW_SGV, lowSgvString);
        editor.putString(AppPrefs.KEY_HIGH_SGV, highSgvString);
        editor.putString(AppPrefs.KEY_USER_PHONE, userPhone);

        editor.apply();

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanQr(pendingScanPromptRes);
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void removeContact(int index) {
        SharedPreferences sharedPreferences = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(AppPrefs.CONTACT_KEYS[index], "");
        editor.putString(AppPrefs.CONTACT_NAME_KEYS[index], "");
        editor.apply();

        updateContactDisplay();
        Toast.makeText(this, R.string.contact_removed, Toast.LENGTH_SHORT).show();
    }

    private void updateContactDisplay() {
        SharedPreferences sharedPreferences = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        TextView[] textViews = {contact1TextView, contact2TextView, contact3TextView, contact4TextView, contact5TextView};
        Button[] buttons = {removeContact1Button, removeContact2Button, removeContact3Button, removeContact4Button, removeContact5Button};

        for (int i = 0; i < AppPrefs.CONTACT_NAME_KEYS.length; i++) {
            String name = sharedPreferences.getString(AppPrefs.CONTACT_NAME_KEYS[i], "");
            if (name == null || name.isEmpty()) {
                textViews[i].setText(R.string.contact_not_selected);
                buttons[i].setVisibility(View.GONE);
            } else {
                textViews[i].setText(name);
                buttons[i].setVisibility(View.VISIBLE);
            }
        }
    }

    private void showSettingsQr() {
        String payload = SettingsSyncHelper.buildSharePayload(
                textOf(nightscoutUrlEditText),
                textOf(apiTokenEditText),
                textOf(accessTokenEditText),
                AlertKeyManager.getOrCreateSharedAlertKey(getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)),
                getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
                        .getString(AppPrefs.KEY_ESCALATION_NUMBER, ""),
                textOf(lowSgvEditText),
                textOf(highSgvEditText)
        );

        showQrDialog(payload, R.string.settings_qr_title, R.string.settings_qr_description);
    }

    private void showContactsQr() {
        String trustedCsv = TrustedSenderMapper.buildTrustedSendersForQr(
                getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        );
        String userPhone = textOf(userPhoneEditText);
        String payload = ContactShareQrCodec.toPayload(trustedCsv, userPhone);
        showQrDialog(payload, R.string.contacts_qr_title, R.string.contacts_qr_description);
    }

    private void showQrDialog(String payload, int titleRes, int messageRes) {
        Bitmap qrBitmap = generateQrBitmap(payload, 900);
        if (qrBitmap == null) {
            Toast.makeText(this, R.string.unable_generate_qr, Toast.LENGTH_SHORT).show();
            return;
        }

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(qrBitmap);
        imageView.setAdjustViewBounds(true);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(32, 24, 32, 8);
        container.setGravity(Gravity.CENTER);
        container.addView(imageView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setMessage(getString(messageRes))
                .setView(container)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void scanQr(int promptRes) {
        pendingScanPromptRes = promptRes;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }

        ScanOptions options = new ScanOptions();
        options.setPrompt(getString(promptRes));
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        qrScanLauncher.launch(options);
    }

    private void applyScannedSettings(SettingsQrCodec.ParsedSettings parsed) {
        nightscoutUrlEditText.setText(parsed.nightscoutUrl);
        apiTokenEditText.setText(parsed.apiToken);
        accessTokenEditText.setText(parsed.accessToken);
        lowSgvEditText.setText(parsed.lowSgv);
        highSgvEditText.setText(parsed.highSgv);

        SettingsSyncHelper.applyParsedSettings(
                getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE),
                parsed
        );
        Toast.makeText(this, R.string.settings_imported, Toast.LENGTH_LONG).show();
    }

    private void applyScannedContactShare(ContactShareQrCodec.ParsedContactShare parsed) {
        SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        String mergedTrusted = mergeTrustedSenders(parsed.trustedSenders, parsed.userPhone);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AppPrefs.KEY_TRUSTED_SENDERS, mergedTrusted);
        if (textOf(userPhoneEditText).isEmpty() && parsed.userPhone != null && !parsed.userPhone.trim().isEmpty()) {
            editor.putString(AppPrefs.KEY_USER_PHONE, parsed.userPhone.trim());
            userPhoneEditText.setText(parsed.userPhone.trim());
        }
        editor.apply();

        int addedCount = TrustedSenderMapper.addTrustedSendersToEmergencyContacts(prefs, mergedTrusted);
        updateContactDisplay();
        Toast.makeText(this, R.string.contacts_qr_imported, Toast.LENGTH_LONG).show();
        if (addedCount > 0) {
            Toast.makeText(this, getString(R.string.qr_trusted_added_contacts, addedCount), Toast.LENGTH_LONG).show();
        }

        if (parsed.userPhone != null && !parsed.userPhone.trim().isEmpty()) {
            promptForScannedContactName(parsed.userPhone.trim());
        }
    }

    private String mergeTrustedSenders(String trustedSendersCsv, String userPhone) {
        StringBuilder merged = new StringBuilder();
        if (trustedSendersCsv != null && !trustedSendersCsv.trim().isEmpty()) {
            merged.append(trustedSendersCsv.trim());
        }
        if (userPhone != null && !userPhone.trim().isEmpty()) {
            if (merged.length() > 0) {
                merged.append(",");
            }
            merged.append(userPhone.trim());
        }
        return merged.toString();
    }

    private void promptForScannedContactName(String phoneRaw) {
        String normalizedPhone = normalizePhone(phoneRaw);
        if (normalizedPhone.isEmpty()) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        int existingIndex = findContactIndexByPhone(prefs, normalizedPhone);
        String existingName = existingIndex >= 0
                ? prefs.getString(AppPrefs.CONTACT_NAME_KEYS[existingIndex], "")
                : "";

        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setPadding(24, 8, 24, 0);
        TextInputEditText nameEditText = new TextInputEditText(this);
        nameEditText.setHint(getString(R.string.contact_name_hint));
        if (existingName != null && !existingName.trim().isEmpty()) {
            nameEditText.setText(existingName.trim());
            nameEditText.setSelection(nameEditText.getText() == null ? 0 : nameEditText.getText().length());
        }
        inputLayout.addView(nameEditText);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.save_contact_name_title, normalizedPhone))
                .setView(inputLayout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String inputName = nameEditText.getText() == null ? "" : nameEditText.getText().toString().trim();
                    if (inputName.isEmpty()) {
                        Toast.makeText(this, R.string.contact_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveScannedContactName(normalizedPhone, inputName);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveScannedContactName(String normalizedPhone, String name) {
        SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        int index = findContactIndexByPhone(prefs, normalizedPhone);
        if (index < 0) {
            index = findFirstEmptyContactIndex(prefs);
        }
        if (index < 0) {
            Toast.makeText(this, R.string.max_contacts_allowed, Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit()
                .putString(AppPrefs.CONTACT_KEYS[index], normalizedPhone)
                .putString(AppPrefs.CONTACT_NAME_KEYS[index], name)
                .apply();
        updateContactDisplay();
        Toast.makeText(this, R.string.contact_added, Toast.LENGTH_SHORT).show();
    }

    private int findContactIndexByPhone(SharedPreferences prefs, String normalizedPhone) {
        for (int i = 0; i < AppPrefs.CONTACT_KEYS.length; i++) {
            String current = normalizePhone(prefs.getString(AppPrefs.CONTACT_KEYS[i], ""));
            if (normalizedPhone.equals(current)) {
                return i;
            }
        }
        return -1;
    }

    private int findFirstEmptyContactIndex(SharedPreferences prefs) {
        for (int i = 0; i < AppPrefs.CONTACT_KEYS.length; i++) {
            String current = normalizePhone(prefs.getString(AppPrefs.CONTACT_KEYS[i], ""));
            if (current.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim().replaceAll("[^\\d+]", "");
        if (cleaned.startsWith("+")) {
            return "+" + cleaned.substring(1).replace("+", "");
        }
        return cleaned.replace("+", "");
    }

    private Bitmap generateQrBitmap(String text, int sizePx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565);

            for (int y = 0; y < sizePx; y++) {
                for (int x = 0; x < sizePx; x++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
