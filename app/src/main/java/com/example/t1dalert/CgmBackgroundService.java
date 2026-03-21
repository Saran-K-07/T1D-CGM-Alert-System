package com.example.t1dalert;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;

public class CgmBackgroundService extends Service {

    private static final long WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L;
    private static final String DEFAULT_UNCONSCIOUS_ESCALATION_NUMBER = "8144225325";
    private RequestQueue requestQueue;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastSgv = "---";
    private String lastTrend = "X";
    private PowerManager.WakeLock wakeLock;
    private CgmRepository cgmRepository;

    private final Runnable cgmDataRefresher = new Runnable() {
        @Override
        public void run() {
            fetchCgmData();
            final int REFRESH_INTERVAL = 15000; // 15 seconds
            handler.postDelayed(this, REFRESH_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "T1DAlert:BackgroundService");
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
        NotificationHelper.createNotificationChannel(this);
        Notification notification = NotificationHelper.buildNotification(this, lastSgv, lastTrend).build();
        startForeground(NotificationHelper.NOTIFICATION_ID, notification);
        requestQueue = Volley.newRequestQueue(this);
        cgmRepository = new CgmRepository(this, requestQueue);
        handler.post(cgmDataRefresher);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void fetchCgmData() {
        SharedPreferences sharedPreferences = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        if (sharedPreferences.getString(AppPrefs.KEY_NIGHTSCOUT_URL, "").trim().isEmpty()) {
            NotificationHelper.updateNotification(this, getString(R.string.live_cgm_missing_url_short), "?");
            return;
        }

        int finalLowSgv = CgmUtils.parseIntOrDefault(
                sharedPreferences.getString(AppPrefs.KEY_LOW_SGV, String.valueOf(AppConfig.DEFAULT_LOW_SGV)),
                AppConfig.DEFAULT_LOW_SGV
        );

        cgmRepository.fetchLatest(new CgmRepository.Listener() {
            @Override
            public void onSuccess(CgmData data) {
                int sgv = data.sgv;
                String trend = CgmUtils.getTrendArrow(data.direction);
                lastSgv = String.valueOf(sgv);
                lastTrend = trend;

                if (sgv < finalLowSgv) {
                    startFallDetectionService();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    long currentTime = System.currentTimeMillis();
                    long startTime = sharedPreferences.getLong(AppPrefs.KEY_LOW_ALERT_START_TIME, 0);
                    boolean smsSent = sharedPreferences.getBoolean(AppPrefs.KEY_SMS_SENT, false);
                    int lastAlertedSgv = sharedPreferences.getInt(AppPrefs.KEY_LAST_ALERTED_SGV, Integer.MAX_VALUE);
                    if (lastAlertedSgv == Integer.MAX_VALUE) {
                        editor.putLong(AppPrefs.KEY_LOW_ALERT_START_TIME, currentTime);
                        editor.putBoolean(AppPrefs.KEY_OVERLAY_ACTIVE, true);
                        editor.putBoolean(AppPrefs.KEY_SMS_SENT, false);
                        editor.putInt(AppPrefs.KEY_LAST_ALERTED_SGV, sgv);
                        editor.apply();
                        launchLowAlert(lastSgv, lastTrend);
                    } else {
                        boolean condition1 = (currentTime - startTime > AppConfig.LOW_ALERT_ESCALATION_MS) && !smsSent;
                        boolean condition2 = (lastAlertedSgv - sgv) > AppConfig.LOW_DROP_DELTA_FOR_RE_ALERT;
                        if (condition1 || condition2) {
                            if (condition1) {
                                boolean sent = sendEmergencySms(lastSgv, lastTrend);
                                editor.putBoolean(AppPrefs.KEY_SMS_SENT, sent);
                            } else {
                                launchLowAlert(lastSgv, lastTrend);
                            }
                            editor.putInt(AppPrefs.KEY_LAST_ALERTED_SGV, sgv);
                            editor.apply();
                        }
                    }
                } else {
                    stopFallDetectionService();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(AppPrefs.KEY_LAST_ALERTED_SGV, Integer.MAX_VALUE);
                    editor.putLong(AppPrefs.KEY_LOW_ALERT_START_TIME, 0);
                    editor.putBoolean(AppPrefs.KEY_OVERLAY_ACTIVE, false);
                    editor.putBoolean(AppPrefs.KEY_SMS_SENT, false);
                    editor.putBoolean(AppPrefs.KEY_FALL_DETECTED, false);
                    editor.putLong(AppPrefs.KEY_FALL_DETECTED_AT, 0);
                    editor.putBoolean(AppPrefs.KEY_UNCONSCIOUS_LIKELY, false);
                    editor.putFloat(AppPrefs.KEY_UNCONSCIOUS_CONFIDENCE, 0f);
                    editor.putString(AppPrefs.KEY_UNCONSCIOUS_TELEMETRY, "");
                    editor.putString(AppPrefs.KEY_LAST_LOCATION, "");
                    editor.putLong(AppPrefs.KEY_LAST_LOCATION_AT, 0L);
                    editor.apply();
                }

                NotificationHelper.updateNotification(CgmBackgroundService.this, lastSgv, lastTrend);
            }

            @Override
            public void onSchemaError() {
                NotificationHelper.updateNotification(CgmBackgroundService.this, getString(R.string.live_cgm_format_short), "?");
            }

            @Override
            public void onNetworkError() {
                NotificationHelper.updateNotification(CgmBackgroundService.this, getString(R.string.live_cgm_loading_failed_short), getString(R.string.live_cgm_trend_unavailable_short));
            }
        });
    }

    private void launchLowAlert(String sgv, String trend) {
        Intent intent = new Intent(this, LowAlertOverlayService.class);
        intent.putExtra("sgv", sgv);
        intent.putExtra("trend", trend);
        startService(intent);
    }

    private boolean sendEmergencySms(String sgv, String trend) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.sms_permission_required, Toast.LENGTH_LONG).show();
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.receive_sms_permission_missing_hint, Toast.LENGTH_LONG).show();
        }

        SharedPreferences sharedPreferences = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        String[] contacts = new String[AppPrefs.CONTACT_KEYS.length];
        for (int i = 0; i < AppPrefs.CONTACT_KEYS.length; i++) {
            contacts[i] = sharedPreferences.getString(AppPrefs.CONTACT_KEYS[i], "");
        }

        boolean fallDetected = sharedPreferences.getBoolean(AppPrefs.KEY_FALL_DETECTED, false);
        long fallDetectedAt = sharedPreferences.getLong(AppPrefs.KEY_FALL_DETECTED_AT, 0L);
        boolean unconsciousLikely = sharedPreferences.getBoolean(AppPrefs.KEY_UNCONSCIOUS_LIKELY, false);
        float unconsciousConfidence = sharedPreferences.getFloat(AppPrefs.KEY_UNCONSCIOUS_CONFIDENCE, 0f);
        String lastLocation = sharedPreferences.getString(AppPrefs.KEY_LAST_LOCATION, "");

        String message = EmergencyMessageFormatter.build(
                sgv,
                trend,
                fallDetected,
                fallDetectedAt,
                unconsciousLikely,
                unconsciousConfidence,
                lastLocation
        );
        String encryptedMessage = SecureAlertMessageCodec.encrypt(sharedPreferences, message);

        SmsManager smsManager = getSmsManager();
        if (smsManager == null) {
            Toast.makeText(this, R.string.sms_not_available, Toast.LENGTH_LONG).show();
            AppMetrics.increment(this, AppPrefs.KEY_METRIC_SMS_SEND_FAILURE);
            return false;
        }

        int attemptedCount = 0;
        int sentCount = 0;
        int failedCount = 0;
        StringBuilder encryptedRecipientsForFallback = new StringBuilder();

        for (String contact : contacts) {
            String normalizedContact = sanitizePhoneNumber(contact);
            if (!normalizedContact.isEmpty()) {
                attemptedCount++;
                if (encryptedRecipientsForFallback.length() > 0) {
                    encryptedRecipientsForFallback.append(';');
                }
                encryptedRecipientsForFallback.append(normalizedContact);
                try {
                    sendSmsMessage(smsManager, normalizedContact, encryptedMessage);
                    sentCount++;
                } catch (Exception e) {
                    failedCount++;
                    AppMetrics.increment(this, AppPrefs.KEY_METRIC_SMS_SEND_FAILURE);
                }
            }
        }

        String escalationRecipientForFallback = "";
        if (unconsciousLikely) {
            String escalationNumber = sanitizePhoneNumber(getEscalationNumber(sharedPreferences));
            if (!escalationNumber.isEmpty()) {
                attemptedCount++;
                escalationRecipientForFallback = escalationNumber;
                try {
                    sendSmsMessage(smsManager, escalationNumber, message);
                    sentCount++;
                } catch (Exception e) {
                    failedCount++;
                    AppMetrics.increment(this, AppPrefs.KEY_METRIC_SMS_SEND_FAILURE);
                }
            }
        }

        if (attemptedCount == 0) {
            Toast.makeText(this, R.string.no_emergency_contacts, Toast.LENGTH_LONG).show();
            return false;
        }

        boolean fallbackOpened = false;
        if (sentCount == 0 && !TextUtils.isEmpty(encryptedRecipientsForFallback.toString())) {
            try {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + encryptedRecipientsForFallback));
                smsIntent.putExtra("sms_body", encryptedMessage);
                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(smsIntent);
                fallbackOpened = true;
            } catch (Exception e) {
                failedCount++;
                AppMetrics.increment(this, AppPrefs.KEY_METRIC_SMS_SEND_FAILURE);
            }
        }

        if (sentCount == 0 && !fallbackOpened && !TextUtils.isEmpty(escalationRecipientForFallback)) {
            try {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + escalationRecipientForFallback));
                smsIntent.putExtra("sms_body", message);
                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(smsIntent);
                fallbackOpened = true;
            } catch (Exception e) {
                failedCount++;
                AppMetrics.increment(this, AppPrefs.KEY_METRIC_SMS_SEND_FAILURE);
            }
        }

        if (sentCount > 0) {
            Toast.makeText(this, getString(R.string.emergency_sms_sent_count, sentCount), Toast.LENGTH_LONG).show();
        } else if (fallbackOpened) {
            Toast.makeText(this, R.string.sms_fallback_opened, Toast.LENGTH_LONG).show();
        } else if (failedCount > 0) {
            Toast.makeText(this, R.string.sms_send_failed, Toast.LENGTH_LONG).show();
        }

        return sentCount > 0 || fallbackOpened;
    }

    private SmsManager getSmsManager() {
        try {
            int subscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId();
            if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                return SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
            }
        } catch (Exception ignored) {
        }
        return SmsManager.getDefault();
    }

    private void sendSmsMessage(SmsManager smsManager, String recipient, String body) {
        ArrayList<String> messageParts = smsManager.divideMessage(body);
        if (messageParts.size() > 1) {
            smsManager.sendMultipartTextMessage(recipient, null, messageParts, null, null);
        } else {
            smsManager.sendTextMessage(recipient, null, body, null, null);
        }
    }

    private void startFallDetectionService() {
        Intent fallIntent = new Intent(this, FallDetectionService.class);
        ContextCompat.startForegroundService(this, fallIntent);
    }

    private void stopFallDetectionService() {
        Intent fallIntent = new Intent(this, FallDetectionService.class);
        stopService(fallIntent);
    }

    private String sanitizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String trimmed = phoneNumber.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String cleaned = trimmed.replaceAll("[^\\d+]", "");
        if (cleaned.startsWith("+")) {
            return "+" + cleaned.substring(1).replace("+", "");
        }
        return cleaned.replace("+", "");
    }

    private String getEscalationNumber(SharedPreferences prefs) {
        return prefs.getString(AppPrefs.KEY_ESCALATION_NUMBER, DEFAULT_UNCONSCIOUS_ESCALATION_NUMBER);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopFallDetectionService();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        handler.removeCallbacks(cgmDataRefresher);
    }
}
