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
public class CoolDetector {
    private final static String TAG = CoolDetector.class.getSimpleName();
    private final float vibrateThreshold = (float) .5;
    private Queue<AccelTime> accelQueueDetector;
    public AccelQueue accelQueueMeteor;
    private Alertable alert;
    private Handler mHandler;

    public CoolDetector(Alertable alert) {
        this.alert = alert;
        startTimeRangeViewUpdate();
        accelQueueDetector = new ConcurrentLinkedQueue<AccelTime>();
        accelQueueMeteor = new AccelQueue();
    }

    public void add(AccelTime accelTime) {
        if (maxAccelDifference(accelTime) > vibrateThreshold) {
            Log.d("mine", "Diff is great enough!");
            alert.sendExcessiveAlert();
        }
        accelQueueDetector.add(accelTime);
        accelQueueMeteor.accelsToSend.add(accelTime);

        if (accelQueueDetector.size() > 100) {
            accelQueueDetector.poll();
        }
    }

    public void reset() {
        accelQueueDetector.clear();
    }

    private void startTimeRangeViewUpdate() {
        // TODO put these back..
//        mHandler = new Handler();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        Thread.sleep(1000);
//                        mHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                updateNotifyTimeRangeView();
//                            }
//                        });
//                    } catch (Exception e) {
//                        Log.e("mine", "TODO something bad here, not sure what to do");
//                    }
//                }
//            }
//        }).start();
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
