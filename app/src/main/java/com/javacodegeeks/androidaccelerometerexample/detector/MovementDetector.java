package com.javacodegeeks.androidaccelerometerexample.detector;

import android.util.Log;

import com.javacodegeeks.androidaccelerometerexample.AccelQueue;
import com.javacodegeeks.androidaccelerometerexample.AccelTime;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by chase on 3/25/15.
 *
 */
public class MovementDetector {
    private final static String TAG = MovementDetector.class.getSimpleName();
    public float sensitivity;
    private Queue<AccelTime> accelQueueDetector;
    public AccelQueue accelQueueMeteor;
    private Queue<Long> movementTimesQueue;
    private Alertable alert;

    private long lastAlertTime = 0;

    private static final int MAX_NOTIFY_DELTA = 25 * 1000;
    private static final int MIN_NOTIFY_DELTA = 10 * 1000;
    private static final int TIME_TO_OFF_ALERT = 250;

    private static final float HIGH_SENSITIVITY = (float) .4;
    private static final float NORMAL_SENSITIVITY = (float) 1.0;

    public MovementDetector(Alertable alert) {
        this.alert = alert;
        accelQueueDetector = new ConcurrentLinkedQueue<AccelTime>();
        accelQueueMeteor = new AccelQueue();
        movementTimesQueue = new ConcurrentLinkedQueue<Long>();
        sensitivity = NORMAL_SENSITIVITY;
    }

    public void setSensitivity(float sensitivity) {
        Log.d(TAG, "Set senstivity to " + sensitivity);
        this.sensitivity = sensitivity;
    }

    public void setHighSensitivity() {
        setSensitivity(HIGH_SENSITIVITY);
    }

    public void add(AccelTime accelTime) {
        if (maxAccelDifference(accelTime) > sensitivity) {
            alert.moveAlert();
            accelQueueDetector.clear();
            lastAlertTime = System.currentTimeMillis();
            if (Alertable.AlertStatus.MINI.compareTo(alert.getAlertStatus()) >= 0) {
                Log.d(TAG, "Diff is great enough!");
                alert.firstMoveAlert();
                if (timeLeftToAlertIfAdded(System.currentTimeMillis()) > 0) {
                    alert.excessiveMoveAlert();
                }
                movementTimesQueue.add(System.currentTimeMillis());
            }
        } else {
            if (lastAlertTime + TIME_TO_OFF_ALERT < System.currentTimeMillis()) {
                alert.noMoveAlert();
            }
        }
        accelQueueDetector.add(accelTime);
        accelQueueMeteor.accelsToSend.add(accelTime);

        if (accelQueueDetector.size() > 100) {
            accelQueueDetector.poll();
        }
    }

    public void reset() {
        accelQueueDetector.clear();
        movementTimesQueue.clear();
    }

    public long timeLeftToAlertIfAdded(long millis) {
        while (movementTimesQueue.peek() != null && movementTimesQueue.peek() < millis - MAX_NOTIFY_DELTA) {
            movementTimesQueue.remove();
        }

        if (movementTimesQueue.peek() == null) {
            return -999;
        }
        if (movementTimesQueue.peek() < millis - MIN_NOTIFY_DELTA) {
            long ret = movementTimesQueue.peek() - (millis - MAX_NOTIFY_DELTA);
            assert ret >= 0;
            return ret;
        }
        long ret = - (movementTimesQueue.peek() - (millis - MIN_NOTIFY_DELTA));
        assert ret <= 0;
        return ret;
    }



    private float maxAccelDifference(AccelTime current) {
        float ret = 0;
        for (AccelTime arr : accelQueueDetector) {
            ret = Math.max(ret, (float) Math.sqrt(
                    Math.pow(arr.x - current.x, 2) +
                            Math.pow(arr.y - current.y, 2) +
                            Math.pow(arr.z - current.z, 2)));
        }
        return ret;
    }
}
