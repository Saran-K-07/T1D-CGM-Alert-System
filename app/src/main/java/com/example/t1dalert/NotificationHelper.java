package com.example.t1dalert;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "cgm_channel_silent_v2";
    private static final String FALL_CHANNEL_ID = "fall_channel_silent_v2";
    private static final String INCOMING_ALERT_CHANNEL_ID = "incoming_alert_channel_silent_v2";
    public static final int NOTIFICATION_ID = 1;
    public static final int FALL_DETECTION_NOTIFICATION_ID = 3;
    public static final int INCOMING_ALERT_NOTIFICATION_ID = 4;

    public static void createNotificationChannel(Context context) {
        CharSequence name = context.getString(R.string.channel_cgm_alerts_name);
        String description = context.getString(R.string.channel_cgm_alerts_desc);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        channel.setSound(null, null);
        channel.enableVibration(false);

        CharSequence fallName = context.getString(R.string.channel_fall_name);
        String fallDescription = context.getString(R.string.channel_fall_desc);
        NotificationChannel fallChannel = new NotificationChannel(FALL_CHANNEL_ID, fallName, NotificationManager.IMPORTANCE_LOW);
        fallChannel.setDescription(fallDescription);
        fallChannel.setSound(null, null);
        fallChannel.enableVibration(false);

        CharSequence incomingName = context.getString(R.string.channel_incoming_name);
        String incomingDescription = context.getString(R.string.channel_incoming_desc);
        NotificationChannel incomingChannel = new NotificationChannel(INCOMING_ALERT_CHANNEL_ID, incomingName, NotificationManager.IMPORTANCE_LOW);
        incomingChannel.setDescription(incomingDescription);
        incomingChannel.setSound(null, null);
        incomingChannel.enableVibration(false);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        notificationManager.createNotificationChannel(fallChannel);
        notificationManager.createNotificationChannel(incomingChannel);
    }

    public static NotificationCompat.Builder buildNotification(Context context, String sgv, String trend) {
        Intent intent = new Intent(context, LiveCgmActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String title = context.getString(R.string.notification_current_glucose_title);
        String content = sgv + " " + trend;

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(pendingIntent);
    }

    public static void updateNotification(Context context, String sgv, String trend) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = buildNotification(context, sgv, trend);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public static NotificationCompat.Builder buildFallDetectionNotification(Context context) {
        Intent intent = new Intent(context, LiveCgmActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, FALL_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.fall_monitoring_active_title))
                .setContentText(context.getString(R.string.fall_monitoring_active_desc))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(pendingIntent);
    }

    public static void updateFallDetectionNotification(Context context, float impactMagnitude, float postureRatio, float confidence) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int posturePercent = Math.round(postureRatio * 100f);
        int confidencePercent = Math.round(confidence * 100f);
        String content = context.getString(
                R.string.possible_fall_content,
                Math.round(impactMagnitude),
                posturePercent,
                confidencePercent
        );

        Intent intent = new Intent(context, LiveCgmActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FALL_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.possible_fall_detected_title))
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(FALL_DETECTION_NOTIFICATION_ID, builder.build());
    }

    public static void showIncomingAlertNotification(Context context, String sender, String message) {
        createNotificationChannel(context);
        Intent intent = new Intent(context, LiveCgmActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 2, intent, PendingIntent.FLAG_IMMUTABLE);

        String title = context.getString(R.string.incoming_emergency_title, sender);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, INCOMING_ALERT_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(INCOMING_ALERT_NOTIFICATION_ID, builder.build());
    }
}
