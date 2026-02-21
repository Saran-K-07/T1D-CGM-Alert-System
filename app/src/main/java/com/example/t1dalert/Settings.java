package com.example.t1dalert;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

public class Settings extends AppCompatActivity {

    private TextInputEditText nightscoutUrlEditText;
    private TextInputEditText apiTokenEditText;
    private TextInputEditText accessTokenEditText;
    private TextInputEditText lowSgvEditText;
    private TextInputEditText highSgvEditText;
    private Button addContactButton;
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

    private static final int CONTACT_PICK_REQUEST = 1;
    private static final int READ_CONTACTS_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        nightscoutUrlEditText = findViewById(R.id.nightscout_url);
        apiTokenEditText = findViewById(R.id.api_token);
        accessTokenEditText = findViewById(R.id.access_token);
        lowSgvEditText = findViewById(R.id.low_sgv);
        highSgvEditText = findViewById(R.id.high_sgv);
        addContactButton = findViewById(R.id.add_contact_button);
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

        loadSettings();
        updateContactDisplay();

        addContactButton.setOnClickListener(v -> addContact());

        removeContact1Button.setOnClickListener(v -> removeContact(0));
        removeContact2Button.setOnClickListener(v -> removeContact(1));
        removeContact3Button.setOnClickListener(v -> removeContact(2));
        removeContact4Button.setOnClickListener(v -> removeContact(3));
        removeContact5Button.setOnClickListener(v -> removeContact(4));

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

    private void addContact() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS_REQUEST);
        } else {
            pickContact();
        }
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, CONTACT_PICK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONTACT_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            getContactDetails(contactUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_CONTACTS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickContact();
            } else {
                Toast.makeText(this, "Contacts permission required to add emergency contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getContactDetails(Uri contactUri) {
        Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            if (idIndex >= 0) {
                String contactId = cursor.getString(idIndex);
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                String name = nameIndex >= 0 ? cursor.getString(nameIndex) : "Unknown";
                cursor.close();

                Cursor phoneCursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{contactId},
                    null
                );

                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    int phoneIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    if (phoneIndex >= 0) {
                        String phoneNumber = phoneCursor.getString(phoneIndex);
                        addContactToList(name, phoneNumber);
                    }
                    phoneCursor.close();
                }
            } else {
                cursor.close();
            }
        }
    }

    private void addContactToList(String name, String phoneNumber) {
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);

        // Normalize phone numbers: remove non-digits
        String normalizedPhone = phoneNumber.replaceAll("\\D", "");

        String[] keys = {MainActivity.KEY_CONTACT_1, MainActivity.KEY_CONTACT_2, MainActivity.KEY_CONTACT_3, MainActivity.KEY_CONTACT_4, MainActivity.KEY_CONTACT_5};

        // Check for duplicates
        for (int i = 0; i < keys.length; i++) {
            String existingPhone = sharedPreferences.getString(keys[i], "");
            String normalizedExisting = existingPhone.replaceAll("\\D", "");
            if (normalizedExisting.equals(normalizedPhone)) {
                Toast.makeText(this, "Contact already added", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        String[] nameKeys = {MainActivity.KEY_CONTACT_1_NAME, MainActivity.KEY_CONTACT_2_NAME, MainActivity.KEY_CONTACT_3_NAME, MainActivity.KEY_CONTACT_4_NAME, MainActivity.KEY_CONTACT_5_NAME};

        for (int i = 0; i < keys.length; i++) {
            if (sharedPreferences.getString(keys[i], "").isEmpty()) {
                editor.putString(keys[i], phoneNumber);
                editor.putString(nameKeys[i], name);
                editor.apply();
                updateContactDisplay();
                Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, "Maximum 5 contacts allowed", Toast.LENGTH_SHORT).show();
    }

    private void removeContact(int index) {
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String[] keys = {MainActivity.KEY_CONTACT_1, MainActivity.KEY_CONTACT_2, MainActivity.KEY_CONTACT_3, MainActivity.KEY_CONTACT_4, MainActivity.KEY_CONTACT_5};
        String[] nameKeys = {MainActivity.KEY_CONTACT_1_NAME, MainActivity.KEY_CONTACT_2_NAME, MainActivity.KEY_CONTACT_3_NAME, MainActivity.KEY_CONTACT_4_NAME, MainActivity.KEY_CONTACT_5_NAME};

        editor.putString(keys[index], "");
        editor.putString(nameKeys[index], "");
        editor.apply();

        updateContactDisplay();
        Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show();
    }

    private void updateContactDisplay() {
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String[] nameKeys = {MainActivity.KEY_CONTACT_1_NAME, MainActivity.KEY_CONTACT_2_NAME, MainActivity.KEY_CONTACT_3_NAME, MainActivity.KEY_CONTACT_4_NAME, MainActivity.KEY_CONTACT_5_NAME};
        TextView[] textViews = {contact1TextView, contact2TextView, contact3TextView, contact4TextView, contact5TextView};
        Button[] buttons = {removeContact1Button, removeContact2Button, removeContact3Button, removeContact4Button, removeContact5Button};

        for (int i = 0; i < nameKeys.length; i++) {
            String name = sharedPreferences.getString(nameKeys[i], "");
            if (name.isEmpty()) {
                textViews[i].setText("Not selected");
                buttons[i].setVisibility(View.GONE);
            } else {
                textViews[i].setText(name);
                buttons[i].setVisibility(View.VISIBLE);
            }
        }
    }
}