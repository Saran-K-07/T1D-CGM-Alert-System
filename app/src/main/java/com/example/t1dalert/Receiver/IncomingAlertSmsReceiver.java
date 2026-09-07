package com.example.t1dalert.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.example.t1dalert.Alert.SecureAlertMessageCodec;
import com.example.t1dalert.Core.AppMetrics;
import com.example.t1dalert.Core.AppPrefs;
import com.example.t1dalert.Core.AppPrefsStore;
import com.example.t1dalert.LowAlertOverlayService;
import com.example.t1dalert.Notification.NotificationHelper;
import com.example.t1dalert.R;

public class IncomingAlertSmsReceiver extends BroadcastReceiver {

    private static final long INCOMING_ALERT_RATE_LIMIT_MS = 30_000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        if (!AppPrefsStore.get(context).getBoolean(AppPrefs.KEY_SMS_READER_ENABLED, true)) {
            return;
        }

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) {
            return;
        }

        StringBuilder bodyBuilder = new StringBuilder();
        String sender = context.getString(R.string.unknown_contact);
        for (SmsMessage sms : messages) {
            if (sms == null) {
                continue;
            }
            if (sender.equals(context.getString(R.string.unknown_contact))) {
                String displayAddress = sms.getDisplayOriginatingAddress();
                String rawAddress = sms.getOriginatingAddress();
                if (displayAddress != null && !displayAddress.trim().isEmpty()) {
                    sender = displayAddress;
                } else if (rawAddress != null && !rawAddress.trim().isEmpty()) {
                    sender = rawAddress;
                }
            }
            String part = sms.getMessageBody();
            if (part != null) {
                bodyBuilder.append(part);
            }
        }

        String messageBody = bodyBuilder.toString().trim();
        if (messageBody.isEmpty()) {
            return;
        }

        if (!isTrustedSender(context, sender)) {
            AppMetrics.increment(context, AppPrefs.KEY_METRIC_UNTRUSTED_SENDER);
            return;
        }

        SecureAlertMessageCodec.DecodeResult decodeResult = SecureAlertMessageCodec.decrypt(
                AppPrefsStore.get(context),
                messageBody
        );
        if (!decodeResult.isValid) {
            AppMetrics.increment(context, AppPrefs.KEY_METRIC_DECRYPT_FAILURE);
            NotificationHelper.showIncomingAlertNotification(
                    context,
                    sender,
                    context.getString(R.string.alert_receive_failed_message)
            );
            return;
        }

        messageBody = decodeResult.message == null ? "" : decodeResult.message.trim();

        if (!isStrictEmergencyAlert(messageBody)) {
            return;
        }

        if (isRateLimited(context)) {
            return;
        }

        NotificationHelper.showIncomingAlertNotification(context, sender, messageBody);
        launchIncomingEmergencyFullscreenAlert(context, sender, messageBody);
    }

    private boolean isStrictEmergencyAlert(String body) {
        if (body == null) {
            return false;
        }
        String normalized = body.trim().toLowerCase();
        return normalized.startsWith("ambulance request")
                || normalized.startsWith("emergency diabetic low sugar alert")
                || normalized.startsWith("emergency: low blood sugar alert");
    }

    private boolean isTrustedSender(Context context, String senderRaw) {
        SharedPreferences prefs = AppPrefsStore.get(context);
        String sender = normalizePhone(senderRaw);
        if (sender.isEmpty()) {
            return false;
        }

        String allowlist = prefs.getString(AppPrefs.KEY_TRUSTED_SENDERS, "").trim();
        if (!allowlist.isEmpty()) {
            String[] allowed = allowlist.split(",");
            for (String value : allowed) {
                if (phoneNumbersMatch(sender, normalizePhone(value))) {
                    return true;
                }
            }
        }

        for (String key : AppPrefs.CONTACT_KEYS) {
            String contact = normalizePhone(prefs.getString(key, ""));
            if (phoneNumbersMatch(sender, contact)) {
                return true;
            }
        }
        return false;
    }

    private boolean phoneNumbersMatch(String incoming, String trusted) {
        if (incoming.isEmpty() || trusted.isEmpty()) {
            return false;
        }
        if (incoming.equals(trusted)) {
            return true;
        }

        String incomingDigits = incoming.replace("+", "");
        String trustedDigits = trusted.replace("+", "");
        int minLocalDigits = 10;
        if (incomingDigits.length() >= minLocalDigits && trustedDigits.length() >= minLocalDigits) {
            String incomingTail = incomingDigits.substring(incomingDigits.length() - minLocalDigits);
            String trustedTail = trustedDigits.substring(trustedDigits.length() - minLocalDigits);
            return incomingTail.equals(trustedTail);
        }
        return false;
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

    private boolean isRateLimited(Context context) {
        long now = System.currentTimeMillis();
        long last = AppPrefsStore.get(context).getLong(AppPrefs.KEY_LAST_INCOMING_ALERT_AT, 0L);
        if (last > 0L && (now - last) < INCOMING_ALERT_RATE_LIMIT_MS) {
            return true;
        }
        AppPrefsStore.get(context).edit().putLong(AppPrefs.KEY_LAST_INCOMING_ALERT_AT, now).apply();
        return false;
    }

    private void launchIncomingEmergencyFullscreenAlert(Context context, String sender, String message) {
        if (!Settings.canDrawOverlays(context)) {
            return;
        }
        Intent fullscreenIntent = new Intent(context, LowAlertOverlayService.class);
        fullscreenIntent.putExtra(
                LowAlertOverlayService.EXTRA_ALERT_TITLE,
                context.getString(R.string.incoming_emergency_fullscreen_title)
        );
        fullscreenIntent.putExtra(
                LowAlertOverlayService.EXTRA_ALERT_LINE_1,
                context.getString(R.string.incoming_emergency_title, sender)
        );
        fullscreenIntent.putExtra(LowAlertOverlayService.EXTRA_ALERT_LINE_2, message);
        context.startService(fullscreenIntent);
    }
}
