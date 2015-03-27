package com.javacodegeeks.androidaccelerometerexample;

import android.util.Log;

import org.json.JSONArray;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by chase on 3/9/15.
 *
 */
public class AccelQueue {
    private final static String TAG = AccelQueue.class.getSimpleName();
    public LinkedBlockingQueue<AccelTime> accelsToSend;

    public AccelQueue() {
        accelsToSend = new LinkedBlockingQueue<AccelTime>();
    }

    public String accelsToJSON() {
        JSONArray ret = new JSONArray();
        try {
            // Block at the start, if there's no elements to take
            ret.put(accelsToSend.take().toJSON());
        } catch (InterruptedException e) {
            Log.e(TAG, "EEEEKInterrupted Exception!!!");
        }
        // Want to send a maximum of 300 data pieces. The number is arbitrary, but 300 samples
        // at even a fast accelerometer rate is still a few seconds worth of data.
        while (accelsToSend.size() > 300) {
            accelsToSend.poll();
        }
        Log.d(TAG, "Size to actually send " + accelsToSend.size());
        while (! accelsToSend.isEmpty()) {
            ret.put(accelsToSend.poll().toJSON());
        }
        return ret.toString();
    }
}
