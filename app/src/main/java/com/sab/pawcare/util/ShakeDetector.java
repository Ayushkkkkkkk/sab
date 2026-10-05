package com.sab.pawcare.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeDetector implements SensorEventListener {
    private static final float THRESHOLD = 13f;
    private static final int MIN_INTERVAL_MS = 1200;
    private long lastShake;
    private final Runnable onShake;

    public ShakeDetector(Runnable onShake) {
        this.onShake = onShake;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float g = (float) Math.sqrt(x * x + y * y + z * z);
        if (g > THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastShake > MIN_INTERVAL_MS) {
                lastShake = now;
                onShake.run();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void register(SensorManager sm) {
        Sensor accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accel != null) {
            sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void unregister(SensorManager sm) {
        sm.unregisterListener(this);
    }
}
