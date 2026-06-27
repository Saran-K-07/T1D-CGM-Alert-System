package com.example.t1dalert.Utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import com.example.t1dalert.Core.AppPrefs;
import com.example.t1dalert.Preferences.PreferencesRepository;

import java.util.List;
import java.util.Locale;

public final class CountryResolver {

    private CountryResolver() {
    }

    public static String resolveCountryIso(Context context, SharedPreferences prefs) {
        String fromLocation = resolveFromLocation(context);
        if (!fromLocation.isEmpty()) {
            prefs.edit().putString(AppPrefs.KEY_LAST_DETECTED_COUNTRY, fromLocation).apply();
            return fromLocation;
        }

        String fromTelephony = resolveFromTelephony(context);
        if (!fromTelephony.isEmpty()) {
            prefs.edit().putString(AppPrefs.KEY_LAST_DETECTED_COUNTRY, fromTelephony).apply();
            return fromTelephony;
        }

        String cached = normalizeIso(prefs.getString(AppPrefs.KEY_LAST_DETECTED_COUNTRY, ""));
        if (!cached.isEmpty()) {
            return cached;
        }

        return normalizeIso(Locale.getDefault().getCountry());
    }

    public static String resolveCountryIso(Context context, PreferencesRepository prefs) {
        String fromLocation = resolveFromLocation(context);
        if (!fromLocation.isEmpty()) {
            prefs.edit().putString(AppPrefs.KEY_LAST_DETECTED_COUNTRY, fromLocation).apply();
            return fromLocation;
        }

        String fromTelephony = resolveFromTelephony(context);
        if (!fromTelephony.isEmpty()) {
            prefs.edit().putString(AppPrefs.KEY_LAST_DETECTED_COUNTRY, fromTelephony).apply();
            return fromTelephony;
        }

        String cached = normalizeIso(prefs.getString(AppPrefs.KEY_LAST_DETECTED_COUNTRY, ""));
        if (!cached.isEmpty()) {
            return cached;
        }

        return normalizeIso(Locale.getDefault().getCountry());
    }

    private static String resolveFromLocation(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }

        Location best = null;
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                return "";
            }
            for (String provider : locationManager.getAllProviders()) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location == null) {
                    continue;
                }
                if (best == null || location.getTime() > best.getTime()) {
                    best = location;
                }
            }
        } catch (Exception ignored) {
            return "";
        }

        if (best == null || !Geocoder.isPresent()) {
            return "";
        }

        try {
            Geocoder geocoder = new Geocoder(context, Locale.US);
            List<Address> addresses = geocoder.getFromLocation(best.getLatitude(), best.getLongitude(), 1);
            if (addresses == null || addresses.isEmpty()) {
                return "";
            }
            String countryCode = addresses.get(0).getCountryCode();
            return normalizeIso(countryCode);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String resolveFromTelephony(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                return "";
            }
            String networkCountry = normalizeIso(telephonyManager.getNetworkCountryIso());
            if (!networkCountry.isEmpty()) {
                return networkCountry;
            }
            return normalizeIso(telephonyManager.getSimCountryIso());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String normalizeIso(String raw) {
        if (raw == null) {
            return "";
        }
        String iso = raw.trim().toUpperCase(Locale.US);
        return iso.length() == 2 ? iso : "";
    }
}
