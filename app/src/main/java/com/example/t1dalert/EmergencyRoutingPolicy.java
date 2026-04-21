package com.example.t1dalert;

import android.content.Context;
import android.content.SharedPreferences;

public final class EmergencyRoutingPolicy {

    private EmergencyRoutingPolicy() {
    }

    public static Decision decide(Context context, SharedPreferences prefs) {
        boolean optIn = prefs.getBoolean(AppPrefs.KEY_EMERGENCY_SERVICE_SMS_OPT_IN, false);
        String manualCountry = prefs.getString(AppPrefs.KEY_MANUAL_EMERGENCY_COUNTRY, "");

        if (!optIn) {
            EmergencyNumberDirectory.Entry manualEntry = EmergencyNumberDirectory.find(context, manualCountry);
            boolean shouldSendManual = manualEntry != null;
            String manualNumber = manualEntry == null ? "" : manualEntry.emergencyNumber;
            String manualIso = manualEntry == null ? "" : manualEntry.countryIso;
            return new Decision(manualIso, manualNumber, shouldSendManual, true);
        }

        String countryIso = CountryResolver.resolveCountryIso(context, prefs);
        EmergencyNumberDirectory.Entry entry = EmergencyNumberDirectory.find(context, countryIso);

        boolean shouldSendEmergencyServiceSms = optIn && entry != null && entry.smsSupported;
        String emergencyNumber = entry == null ? "" : entry.emergencyNumber;
        return new Decision(countryIso, emergencyNumber, shouldSendEmergencyServiceSms, false);
    }

    public static final class Decision {
        public final String countryIso;
        public final String emergencyNumber;
        public final boolean shouldSendEmergencyServiceSms;
        public final boolean manualCountryMode;

        Decision(String countryIso, String emergencyNumber, boolean shouldSendEmergencyServiceSms, boolean manualCountryMode) {
            this.countryIso = countryIso;
            this.emergencyNumber = emergencyNumber;
            this.shouldSendEmergencyServiceSms = shouldSendEmergencyServiceSms;
            this.manualCountryMode = manualCountryMode;
        }
    }
}
