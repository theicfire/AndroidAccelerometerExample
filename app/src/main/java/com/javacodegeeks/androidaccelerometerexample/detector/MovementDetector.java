package com.javacodegeeks.androidaccelerometerexample.detector;

import android.util.Log;

import com.javacodegeeks.androidaccelerometerexample.AccelQueue;
import com.javacodegeeks.androidaccelerometerexample.AccelTime;
import com.javacodegeeks.androidaccelerometerexample.Utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by chase on 3/25/15.
 *
 */
public class MovementDetector {
    private final static String TAG = MovementDetector.class.getSimpleName();
    private float sensitivity;
    public Queue<AccelTime> accelQueueDetector;
    public AccelQueue accelQueueMeteor;
    private Alertable alert;

    private long lastAlertTime = 0;

    private static final int MS_TO_OFF_ALERT = 250;

    private static final float HIGH_SENSITIVITY = (float) .4;
    private static final float NORMAL_SENSITIVITY = (float) 1.0;

    public MovementDetector(Alertable alert) {
        this.alert = alert;
        accelQueueDetector = new ConcurrentLinkedQueue<AccelTime>();
        accelQueueMeteor = new AccelQueue();
        setSensitivity(NORMAL_SENSITIVITY);
    }

    public void setSensitivity(float sensitivity) {
        Log.d(TAG, "Set sensitivity to " + sensitivity);
        this.sensitivity = sensitivity;
        Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/sensitivityLevel/" + sensitivity);
    }

    public void setHighSensitivity() {
        setSensitivity(HIGH_SENSITIVITY);
    }

    public void add(AccelTime accelTime) {
        if (maxAccelDifference(accelTime) > sensitivity) {
            alert.moveAlert();
            accelQueueDetector.clear();
            lastAlertTime = System.currentTimeMillis();
        } else {
            if (lastAlertTime + MS_TO_OFF_ALERT < System.currentTimeMillis()) {
                alert.noMoveAlert();
            }
        }
        accelQueueDetector.add(accelTime);
        accelQueueMeteor.accelsToSend.add(accelTime);

        if (accelQueueDetector.size() > 100) {
            accelQueueDetector.poll();
        }
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
