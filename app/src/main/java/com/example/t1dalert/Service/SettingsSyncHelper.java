package com.example.t1dalert.Service;

import com.example.t1dalert.Alert.QR.SettingsQrCodec;
import com.example.t1dalert.Core.AppPrefs;
import com.example.t1dalert.Preferences.PreferencesRepository;

public final class SettingsSyncHelper {

    private SettingsSyncHelper() {
    }

    public static String buildSharePayload(
            String nightscoutUrl,
            String escalationNumber,
            String lowSgv,
            String highSgv
    ) {
        return SettingsQrCodec.toPayload(
                nightscoutUrl,
                escalationNumber,
                lowSgv,
                highSgv
        );
    }

    public static void applyParsedSettings(PreferencesRepository prefs, SettingsQrCodec.ParsedSettings parsed) {
        prefs.edit()
                .putString(AppPrefs.KEY_NIGHTSCOUT_URL, parsed.nightscoutUrl)
                .putString(AppPrefs.KEY_ESCALATION_NUMBER, parsed.escalationNumber)
                .putString(AppPrefs.KEY_LOW_SGV, parsed.lowSgv)
                .putString(AppPrefs.KEY_HIGH_SGV, parsed.highSgv)
                .apply();
    }
}
