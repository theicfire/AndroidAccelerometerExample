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
    private final float vibrateThreshold;
    private Queue<AccelTime> accelQueueDetector;
    public AccelQueue accelQueueMeteor;
    private Queue<Long> movementTimesQueue;
    private Alertable alert;

    private static final int MAX_NOTIFY_DELTA = 25 * 1000;
    private static final int MIN_NOTIFY_DELTA = 10 * 1000;

    public MovementDetector(Alertable alert) {
        this.alert = alert;
        accelQueueDetector = new ConcurrentLinkedQueue<AccelTime>();
        accelQueueMeteor = new AccelQueue();
        movementTimesQueue = new ConcurrentLinkedQueue<Long>();
        vibrateThreshold = (float) .5;
    }

    public void add(AccelTime accelTime) {
        if (Alertable.AlertStatus.MINI.compareTo(alert.getAlertStatus()) >= 0 &&
                maxAccelDifference(accelTime) > vibrateThreshold) {
            Log.d(TAG, "Diff is great enough!");
            alert.sendMiniAlert();
            if (timeLeftToAlertIfAdded(System.currentTimeMillis()) > 0) {
                alert.sendExcessiveAlert();
            }
            movementTimesQueue.add(System.currentTimeMillis());
            accelQueueDetector.clear();
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
