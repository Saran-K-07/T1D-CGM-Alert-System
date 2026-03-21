package com.example.t1dalert;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class FallDetectionService extends Service implements SensorEventListener {

    private static final String TAG = "FallDetectionService";
    private static final long WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L;

    private static final float FREE_FALL_THRESHOLD = 3.0f;
    private static final float IMPACT_THRESHOLD = 22.0f;
    private static final float LYING_THRESHOLD = 6.0f;
    private static final long IMPACT_WINDOW_MS = 2500;

    private static final long TELEMETRY_WINDOW_MS = 30000;
    private static final float STILLNESS_STDDEV_THRESHOLD = 0.75f;
    private static final float LINEAR_STILLNESS_STDDEV_THRESHOLD = 0.6f;
    private static final float GYRO_AVG_THRESHOLD = 0.6f;
    private static final float LYING_RATIO_THRESHOLD = 0.7f;

    private static final float CONFIDENCE_STILLNESS_WEIGHT = 0.45f;
    private static final float CONFIDENCE_POSTURE_WEIGHT = 0.25f;
    private static final float CONFIDENCE_STEPS_WEIGHT = 0.20f;
    private static final float CONFIDENCE_LINEAR_WEIGHT = 0.10f;
    private static final float UNCONSCIOUS_CONFIDENCE_THRESHOLD = 0.72f;

    private SensorManager sensorManager;
    private Sensor accelSensor;
    private Sensor gravitySensor;
    private Sensor linearAccelSensor;
    private Sensor gyroSensor;
    private Sensor stepDetectorSensor;

    private boolean freeFallDetected;
    private long freeFallTimestamp;
    private long impactTimestamp;
    private long telemetryWindowEnd;
    private float gravityZ = 9.8f;
    private float impactG;
    private int stepCountSinceImpact;

    private final Deque<Sample> accelSamples = new ArrayDeque<>();
    private final Deque<Sample> linearAccelSamples = new ArrayDeque<>();
    private final Deque<Sample> gyroSamples = new ArrayDeque<>();
    private int postureLyingSamples;
    private int postureTotalSamples;

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "T1DAlert:FallDetectionService");
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);

        NotificationHelper.createNotificationChannel(this);
        Notification notification = NotificationHelper.buildFallDetectionNotification(this).build();
        startForeground(NotificationHelper.FALL_DETECTION_NOTIFICATION_ID, notification);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            Log.e(TAG, "SensorManager unavailable");
            AppMetrics.increment(this, AppPrefs.KEY_METRIC_FALL_SENSOR_UNAVAILABLE);
            stopSelf();
            return;
        }

        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (accelSensor == null) {
            Log.e(TAG, "Accelerometer unavailable; cannot run fall detection");
            AppMetrics.increment(this, AppPrefs.KEY_METRIC_FALL_SENSOR_UNAVAILABLE);
            stopSelf();
            return;
        }

        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
        if (gravitySensor != null) {
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (linearAccelSensor != null) {
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroSensor != null) {
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravityZ = event.values[2];
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            if (impactTimestamp > 0) {
                stepCountSinceImpact++;
            }
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float magnitude = magnitude(event.values[0], event.values[1], event.values[2]);
            recordSample(linearAccelSamples, magnitude, now);
            trimWindow(linearAccelSamples, now);
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float magnitude = magnitude(event.values[0], event.values[1], event.values[2]);
            recordSample(gyroSamples, magnitude, now);
            trimWindow(gyroSamples, now);
            return;
        }

        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        float magnitude = magnitude(event.values[0], event.values[1], event.values[2]);

        if (magnitude < FREE_FALL_THRESHOLD) {
            freeFallDetected = true;
            freeFallTimestamp = now;
        }

        if (freeFallDetected && magnitude > IMPACT_THRESHOLD && (now - freeFallTimestamp) <= IMPACT_WINDOW_MS) {
            startTelemetryWindow(now, magnitude);
            Log.w(TAG, "Impact detected, collecting telemetry for unconsciousness inference");
            return;
        }

        if (impactTimestamp > 0) {
            recordSample(accelSamples, magnitude, now);
            trimWindow(accelSamples, now);
            collectPostureSample();

            if (now >= telemetryWindowEnd) {
                evaluateUnconsciousnessAndPersist();
                resetImpactState();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void startTelemetryWindow(long now, float impactMagnitude) {
        impactTimestamp = now;
        telemetryWindowEnd = now + TELEMETRY_WINDOW_MS;
        impactG = impactMagnitude;
        freeFallDetected = false;

        accelSamples.clear();
        linearAccelSamples.clear();
        gyroSamples.clear();
        postureLyingSamples = 0;
        postureTotalSamples = 0;
        stepCountSinceImpact = 0;
    }

    private void collectPostureSample() {
        postureTotalSamples++;
        if (Math.abs(gravityZ) < LYING_THRESHOLD) {
            postureLyingSamples++;
        }
    }

    private void evaluateUnconsciousnessAndPersist() {
        float accelStdDev = stdDev(accelSamples);
        float linearStdDev = stdDev(linearAccelSamples);
        float gyroAvg = average(gyroSamples);
        float postureRatio = postureTotalSamples == 0 ? 0f : (float) postureLyingSamples / postureTotalSamples;

        boolean stillByAccel = accelStdDev <= STILLNESS_STDDEV_THRESHOLD;
        boolean stillByLinear = linearStdDev <= LINEAR_STILLNESS_STDDEV_THRESHOLD || linearAccelSamples.isEmpty();
        boolean stillByGyro = gyroAvg <= GYRO_AVG_THRESHOLD || gyroSamples.isEmpty();
        boolean noSteps = stepCountSinceImpact == 0;
        boolean postureLying = postureRatio >= LYING_RATIO_THRESHOLD;

        float stillnessComponent = (stillByAccel ? 0.5f : 0f) + (stillByGyro ? 0.3f : 0f) + (stillByLinear ? 0.2f : 0f);
        float postureComponent = postureLying ? 1f : 0f;
        float stepsComponent = noSteps ? 1f : 0f;
        float linearComponent = stillByLinear ? 1f : 0f;

        float confidence = (stillnessComponent * CONFIDENCE_STILLNESS_WEIGHT)
                + (postureComponent * CONFIDENCE_POSTURE_WEIGHT)
                + (stepsComponent * CONFIDENCE_STEPS_WEIGHT)
                + (linearComponent * CONFIDENCE_LINEAR_WEIGHT);

        boolean unconsciousLikely = confidence >= UNCONSCIOUS_CONFIDENCE_THRESHOLD;

        String telemetry = String.format(
                Locale.US,
                "impact=%.2f accelStd=%.3f linearStd=%.3f gyroAvg=%.3f postureRatio=%.2f steps=%d conf=%.2f wearable=NA crashSensor=NA",
                impactG,
                accelStdDev,
                linearStdDev,
                gyroAvg,
                postureRatio,
                stepCountSinceImpact,
                confidence
        );

        SharedPreferences sharedPreferences = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(AppPrefs.KEY_FALL_DETECTED, unconsciousLikely);
        editor.putLong(AppPrefs.KEY_FALL_DETECTED_AT, unconsciousLikely ? System.currentTimeMillis() : 0L);
        editor.putBoolean(AppPrefs.KEY_UNCONSCIOUS_LIKELY, unconsciousLikely);
        editor.putFloat(AppPrefs.KEY_UNCONSCIOUS_CONFIDENCE, confidence);
        editor.putString(AppPrefs.KEY_UNCONSCIOUS_TELEMETRY, telemetry);
        editor.apply();

        if (unconsciousLikely) {
            saveLastKnownLocation();
            NotificationHelper.updateFallDetectionNotification(this, impactG, postureRatio, confidence);
            Log.e(TAG, "Unconsciousness likely: " + telemetry);
        } else {
            Log.d(TAG, "Fall not classified as unconscious: " + telemetry);
        }
    }

    private void resetImpactState() {
        impactTimestamp = 0L;
        telemetryWindowEnd = 0L;
        impactG = 0f;
        accelSamples.clear();
        linearAccelSamples.clear();
        gyroSamples.clear();
        postureLyingSamples = 0;
        postureTotalSamples = 0;
        stepCountSinceImpact = 0;
    }

    private void recordSample(Deque<Sample> target, float value, long timestamp) {
        target.addLast(new Sample(timestamp, value));
    }

    private void trimWindow(Deque<Sample> target, long now) {
        long threshold = now - TELEMETRY_WINDOW_MS;
        while (!target.isEmpty() && target.peekFirst().timestamp < threshold) {
            target.removeFirst();
        }
    }

    private float stdDev(Deque<Sample> samples) {
        if (samples.isEmpty()) {
            return 0f;
        }

        float mean = average(samples);
        float sum = 0f;
        for (Sample sample : samples) {
            float delta = sample.value - mean;
            sum += delta * delta;
        }
        return (float) Math.sqrt(sum / samples.size());
    }

    private float average(Deque<Sample> samples) {
        if (samples.isEmpty()) {
            return 0f;
        }
        float sum = 0f;
        for (Sample sample : samples) {
            sum += sample.value;
        }
        return sum / samples.size();
    }

    private float magnitude(float x, float y, float z) {
        return (float) Math.sqrt((x * x) + (y * y) + (z * z));
    }

    private void saveLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission unavailable during unconscious detection");
            return;
        }

        Context attributedContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            attributedContext = createAttributionContext("fall_detection_location");
        } else {
            attributedContext = this;
        }
        LocationManager locationManager = (LocationManager) attributedContext.getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            Log.w(TAG, "LocationManager unavailable");
            return;
        }

        Location bestLocation = null;
        try {
            for (String provider : locationManager.getProviders(true)) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location == null) {
                    continue;
                }
                if (bestLocation == null || location.getTime() > bestLocation.getTime()) {
                    bestLocation = location;
                }
            }
        } catch (SecurityException se) {
            Log.w(TAG, "Location security exception", se);
            return;
        }

        if (bestLocation == null) {
            Log.w(TAG, "No last known location available");
            return;
        }

        String locationString = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f",
                bestLocation.getLatitude(), bestLocation.getLongitude());

        SharedPreferences sharedPreferences = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(AppPrefs.KEY_LAST_LOCATION, locationString);
        editor.putLong(AppPrefs.KEY_LAST_LOCATION_AT, bestLocation.getTime());
        editor.apply();
    }

    private static class Sample {
        final long timestamp;
        final float value;

        Sample(long timestamp, float value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}
