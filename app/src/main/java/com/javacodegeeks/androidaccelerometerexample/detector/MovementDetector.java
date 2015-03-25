package com.javacodegeeks.androidaccelerometerexample.detector;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.javacodegeeks.androidaccelerometerexample.AccelQueue;
import com.javacodegeeks.androidaccelerometerexample.AccelTime;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by chase on 3/25/15.
 */
public class MovementDetector {
    private final static String TAG = MovementDetector.class.getSimpleName();
    private final float vibrateThreshold = (float) .5;
    private Queue<AccelTime> accelQueueDetector;
    public AccelQueue accelQueueMeteor;
    private Queue<Long> movementTimesQueue;
    private Alertable alert;
    private Handler mHandler;
    private static final int MAX_NOTIFY_DELTA = 12 * 1000;
    private static final int MIN_NOTIFY_DELTA = 5 * 1000;
    private TextView excessiveAlertStatus;
    private boolean excessiveAlertTriggered;

    public MovementDetector(Alertable alert, TextView excessiveAlertStatus) {
        this.alert = alert;
        this.excessiveAlertStatus = excessiveAlertStatus;
        excessiveAlertTriggered = false;
        startTimeRangeViewUpdate();
        accelQueueDetector = new ConcurrentLinkedQueue<AccelTime>();
        accelQueueMeteor = new AccelQueue();
        movementTimesQueue = new ConcurrentLinkedQueue<Long>();

    }

    public void add(AccelTime accelTime) {
        if (!excessiveAlertTriggered && maxAccelDifference(accelTime) > vibrateThreshold) {
            Log.d(TAG, "Diff is great enough!");
            alert.sendMiniAlert();
            if (timeLeftToAlertIfAdded(System.currentTimeMillis()) > 0) {
                alert.sendExcessiveAlert();
                excessiveAlertStatus.setText("Triggered!");
                excessiveAlertTriggered = true;
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
        excessiveAlertStatus.setText("Need first bump");
        excessiveAlertTriggered = false;
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

    private void startTimeRangeViewUpdate() {
        mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!excessiveAlertTriggered) {
                                    long timeLeftToAlert = timeLeftToAlertIfAdded(System.currentTimeMillis());
                                    if (timeLeftToAlert > 0) {
                                        excessiveAlertStatus.setText("Second bump trigger until " + timeLeftToAlert);
                                    } else if (timeLeftToAlert == -999) {
                                        excessiveAlertStatus.setText("Need first bump");
                                        alert.unsetMiniAlert();
                                    } else {
                                        excessiveAlertStatus.setText("Require second bump in " + timeLeftToAlert);
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e("mine", "TODO something bad here, not sure what to do");
                    }
                }
            }
        }).start();
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
