package com.example.t1dalert.Alert;

import android.content.SharedPreferences;

import com.example.t1dalert.Core.AppPrefs;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;

public final class TrustedSenderMapper {

    private TrustedSenderMapper() {
    }

    public static String buildTrustedSendersForQr(SharedPreferences prefs) {
        LinkedHashSet<String> merged = parseCsvPhones(prefs.getString(AppPrefs.KEY_TRUSTED_SENDERS, ""));
        for (String key : AppPrefs.CONTACT_KEYS) {
            String contact = normalizePhone(prefs.getString(key, ""));
            if (!contact.isEmpty()) {
                merged.add(contact);
            }
        }
        return String.join(",", merged);
    }

    public static int addTrustedSendersToEmergencyContacts(SharedPreferences prefs, String trustedSendersCsv) {
        return addTrustedSendersToEmergencyContacts(prefs, trustedSendersCsv, Collections.emptyMap());
    }

    public static int addTrustedSendersToEmergencyContacts(SharedPreferences prefs, String trustedSendersCsv, Map<String, String> trustedReceiverNames) {
        LinkedHashSet<String> trusted = parseCsvPhones(trustedSendersCsv);
        if (trusted.isEmpty()) {
            return 0;
        }

        LinkedHashSet<String> existing = new LinkedHashSet<>();
        String[] workingContacts = new String[AppPrefs.CONTACT_KEYS.length];
        for (String key : AppPrefs.CONTACT_KEYS) {
            String current = normalizePhone(prefs.getString(key, ""));
            if (!current.isEmpty()) {
                existing.add(current);
            }
        }
        for (int i = 0; i < AppPrefs.CONTACT_KEYS.length; i++) {
            workingContacts[i] = normalizePhone(prefs.getString(AppPrefs.CONTACT_KEYS[i], ""));
        }

        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;
        int added = 0;
        for (String phone : trusted) {
            int existingIndex = findExistingContactIndex(workingContacts, phone);
            if (existingIndex >= 0) {
                String preferredName = preferredNameFor(trustedReceiverNames, phone);
                String existingName = safe(prefs.getString(AppPrefs.CONTACT_NAME_KEYS[existingIndex], ""));
                if (!preferredName.isEmpty() && existingName.isEmpty()) {
                    editor.putString(AppPrefs.CONTACT_NAME_KEYS[existingIndex], preferredName);
                    changed = true;
                }
                continue;
            }
            int emptyIndex = findEmptyContactSlot(workingContacts);
            if (emptyIndex < 0) {
                break;
            }
            editor.putString(AppPrefs.CONTACT_KEYS[emptyIndex], phone);
            String preferredName = preferredNameFor(trustedReceiverNames, phone);
            editor.putString(AppPrefs.CONTACT_NAME_KEYS[emptyIndex], preferredName.isEmpty() ? "QR Trusted Sender" : preferredName);
            workingContacts[emptyIndex] = phone;
            existing.add(phone);
            added++;
            changed = true;
        }
        if (changed) {
            editor.apply();
        }
        return added;
    }

    private static int findEmptyContactSlot(String[] workingContacts) {
        for (int i = 0; i < workingContacts.length; i++) {
            if (normalizePhone(workingContacts[i]).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static int findExistingContactIndex(String[] workingContacts, String normalizedPhone) {
        for (int i = 0; i < workingContacts.length; i++) {
            if (normalizedPhone.equals(normalizePhone(workingContacts[i]))) {
                return i;
            }
        }
        return -1;
    }

    private static String preferredNameFor(Map<String, String> trustedReceiverNames, String normalizedPhone) {
        if (trustedReceiverNames == null || trustedReceiverNames.isEmpty()) {
            return "";
        }
        for (Map.Entry<String, String> entry : trustedReceiverNames.entrySet()) {
            if (normalizedPhone.equals(normalizePhone(entry.getKey()))) {
                return safe(entry.getValue());
            }
        }
        return "";
    }

    private static LinkedHashSet<String> parseCsvPhones(String csv) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (csv == null || csv.trim().isEmpty()) {
            return set;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            String normalized = normalizePhone(part);
            if (!normalized.isEmpty()) {
                set.add(normalized);
            }
        }
        return set;
    }

    private static String normalizePhone(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim().replaceAll("[^\\d+]", "");
        if (cleaned.startsWith("+")) {
            return "+" + cleaned.substring(1).replace("+", "");
        }
        return cleaned.replace("+", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
