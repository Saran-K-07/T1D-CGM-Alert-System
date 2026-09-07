package com.example.t1dalert;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import androidx.annotation.Nullable;

import com.example.t1dalert.Core.AppPrefs;
import com.example.t1dalert.Core.AppPrefsStore;

public class LowAlertOverlayService extends Service {

    public static final String EXTRA_ALERT_TITLE = "alert_title";
    public static final String EXTRA_ALERT_LINE_1 = "alert_line_1";
    public static final String EXTRA_ALERT_LINE_2 = "alert_line_2";
    public static final String EXTRA_ALERT_STYLE = "alert_style";
    public static final String EXTRA_ALERT_NOTIFY_ONLY = "alert_notify_only";

    public static final int STYLE_LOW = 0;
    public static final int STYLE_HIGH = 1;

    private WindowManager windowManager;
    private View overlayView;
    private Intent serviceIntent;
    private boolean overlayAdded = false;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private int previousAlarmVolume = -1;
    private ToneGenerator toneGenerator;
    private final Handler buzzerHandler = new Handler(Looper.getMainLooper());
    private final Runnable buzzerLoop = new Runnable() {
        @Override
        public void run() {
            if (toneGenerator == null) {
                return;
            }
            // Re-trigger short high-urgency buzzer tones for continuous attention.
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900);
            buzzerHandler.postDelayed(this, 750);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.serviceIntent = intent;
        if (!overlayAdded && serviceIntent != null) {
            setupOverlay();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocus(null);
            }
            if (previousAlarmVolume >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0);
            }
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        stopContinuousBuzzer();
        if (overlayView != null && overlayAdded) {
            windowManager.removeView(overlayView);
        }
    }

    private void setupOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.activity_low_glucose_alert, null);

        String sgv = serviceIntent.getStringExtra("sgv");
        String trend = serviceIntent.getStringExtra("trend");
        String alertTitleExtra = serviceIntent.getStringExtra(EXTRA_ALERT_TITLE);
        String alertLine1Extra = serviceIntent.getStringExtra(EXTRA_ALERT_LINE_1);
        String alertLine2Extra = serviceIntent.getStringExtra(EXTRA_ALERT_LINE_2);
        int alertStyle = serviceIntent.getIntExtra(EXTRA_ALERT_STYLE, STYLE_LOW);
        boolean notifyOnly = serviceIntent.getBooleanExtra(EXTRA_ALERT_NOTIFY_ONLY, false);
        String defaultTitle = alertStyle == STYLE_HIGH
                ? getString(R.string.high_glucose_alert_title)
                : getString(R.string.low_glucose_alert_title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            String alertChannelId = alertStyle == STYLE_HIGH ? "high_alert_channel" : "low_alert_channel";
            CharSequence name = alertStyle == STYLE_HIGH ? "High Glucose Alerts" : "Low Glucose Alerts";
            String description = alertStyle == STYLE_HIGH
                    ? "Emergency alerts for high blood sugar"
                    : "Critical alerts for low blood sugar";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(alertChannelId, name, importance);
            channel.setDescription(description);
            channel.setBypassDnd(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            nm.createNotificationChannel(channel);

            Intent intent = new Intent(this, LiveCgmActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            String safeTitle = (alertTitleExtra == null || alertTitleExtra.trim().isEmpty())
                    ? defaultTitle
                    : alertTitleExtra.trim();
            String safeLine1 = (alertLine1Extra == null || alertLine1Extra.trim().isEmpty())
                    ? getString(R.string.current_sgv_label, sgv == null ? "---" : sgv)
                    : alertLine1Extra.trim();
            String safeLine2;
            if (alertLine2Extra == null || alertLine2Extra.trim().isEmpty()) {
                safeLine2 = alertStyle == STYLE_HIGH
                        ? getString(R.string.high_glucose_alert_line2)
                        : getString(R.string.trend_label, trend == null ? "?" : trend);
            } else {
                safeLine2 = alertLine2Extra.trim();
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, alertChannelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(safeTitle)
                    .setContentText(safeLine1 + " | " + safeLine2)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setSilent(true)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true);

            nm.notify(2, builder.build());
        }

        TextView alertText = overlayView.findViewById(R.id.alert_text);
        TextView sgvText = overlayView.findViewById(R.id.sgv_text);
        TextView trendText = overlayView.findViewById(R.id.trend_text);

        String safeSgv = sgv == null ? "---" : sgv;
        String safeTrend = trend == null ? "?" : trend;
        String safeTitle = (alertTitleExtra == null || alertTitleExtra.trim().isEmpty())
                ? defaultTitle
                : alertTitleExtra.trim();
        String safeLine1 = (alertLine1Extra == null || alertLine1Extra.trim().isEmpty())
                ? getString(R.string.current_sgv_label, safeSgv)
                : alertLine1Extra.trim();
        String safeLine2;
        if (alertLine2Extra == null || alertLine2Extra.trim().isEmpty()) {
            safeLine2 = alertStyle == STYLE_HIGH
                    ? getString(R.string.high_glucose_alert_line2)
                    : getString(R.string.trend_label, safeTrend);
        } else {
            safeLine2 = alertLine2Extra.trim();
        }
        alertText.setText(safeTitle);
        sgvText.setText(safeLine1);
        trendText.setText(safeLine2);

        overlayView.findViewById(R.id.alert_root).setBackgroundColor(
                alertStyle == STYLE_HIGH ? Color.parseColor("#D66A00") : Color.RED
        );

        SeekBar dismissSlider = overlayView.findViewById(R.id.dismiss_slider);
        dismissSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 100) {
                    SharedPreferences sharedPreferences = AppPrefsStore.get(LowAlertOverlayService.this);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(AppPrefs.KEY_OVERLAY_ACTIVE, false);
                    editor.apply();
                    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    nm.cancel(2);
                    stopSelf();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 100) {
                    seekBar.setProgress(0);
                }
            }
        });

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP;

        if (!notifyOnly) {
            windowManager.addView(overlayView, params);
            overlayAdded = true;

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 1000}, 0));
                } else {
                    vibrator.vibrate(new long[]{0, 1000}, 0);
                }
            }

            audioManager.setStreamMute(AudioManager.STREAM_ALARM, false);
            previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
            requestAudioFocus();
            startContinuousBuzzer();
        }
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChange -> { })
                    .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
    }

    private void startContinuousBuzzer() {
        stopContinuousBuzzer();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        buzzerLoop.run();
    }

    private void stopContinuousBuzzer() {
        buzzerHandler.removeCallbacks(buzzerLoop);
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
