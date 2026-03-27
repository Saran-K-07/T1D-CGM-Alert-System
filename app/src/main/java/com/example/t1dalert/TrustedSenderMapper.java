package com.example.t1dalert;

import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

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
        int added = 0;
        for (String phone : trusted) {
            if (existing.contains(phone)) {
                continue;
            }
            int emptyIndex = findEmptyContactSlot(workingContacts);
            if (emptyIndex < 0) {
                break;
            }
            editor.putString(AppPrefs.CONTACT_KEYS[emptyIndex], phone);
            editor.putString(AppPrefs.CONTACT_NAME_KEYS[emptyIndex], "QR Trusted Sender");
            workingContacts[emptyIndex] = phone;
            existing.add(phone);
            added++;
        }
        editor.apply();
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
}
