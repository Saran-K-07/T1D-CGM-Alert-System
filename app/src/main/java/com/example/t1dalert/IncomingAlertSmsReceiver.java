package com.example.t1dalert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.SmsMessage;

import java.util.Locale;

public class IncomingAlertSmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
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
            if (sender.equals(context.getString(R.string.unknown_contact)) && sms.getDisplayOriginatingAddress() != null) {
                sender = sms.getDisplayOriginatingAddress();
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
                context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE),
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

        messageBody = decodeResult.message;

        if (!isStrictEmergencyAlert(messageBody)) {
            return;
        }

        NotificationHelper.showIncomingAlertNotification(context, sender, messageBody);
        speakMessage(context, messageBody);
    }

    private boolean isStrictEmergencyAlert(String body) {
        return body.startsWith("AMBULANCE REQUEST")
                || body.startsWith("Emergency diabetic low sugar alert")
                || body.startsWith("Emergency: Low blood sugar alert");
    }

    private boolean isTrustedSender(Context context, String senderRaw) {
        String allowlist = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(AppPrefs.KEY_TRUSTED_SENDERS, "")
                .trim();
        if (allowlist.isEmpty()) {
            return true;
        }

        String sender = normalizePhone(senderRaw);
        if (sender.isEmpty()) {
            return false;
        }

        String[] allowed = allowlist.split(",");
        for (String value : allowed) {
            if (sender.equals(normalizePhone(value))) {
                return true;
            }
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

    private void speakMessage(Context context, String message) {
        final TextToSpeech[] ttsHolder = new TextToSpeech[1];
        ttsHolder[0] = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsHolder[0].setLanguage(Locale.US);
                ttsHolder[0].setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        ttsHolder[0].stop();
                        ttsHolder[0].shutdown();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        ttsHolder[0].stop();
                        ttsHolder[0].shutdown();
                    }
                });
                ttsHolder[0].speak(message, TextToSpeech.QUEUE_FLUSH, null, "incoming_alert_sms");
            }
        });
    }
}
