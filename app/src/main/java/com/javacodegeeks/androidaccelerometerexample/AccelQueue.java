package com.javacodegeeks.androidaccelerometerexample;

import android.util.Log;

import org.json.JSONArray;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by chase on 3/9/15.
 */
public class AccelQueue {
    public LinkedBlockingQueue<AccelTime> accelsToSend;

    public AccelQueue() {
        accelsToSend = new LinkedBlockingQueue<AccelTime>();
    }

    public String accelsToJSON() {
        JSONArray ret = new JSONArray();
        AccelTime cur;
        try {
            // Need to block at the start, if there's no elements to take
            ret.put(accelsToSend.take().toJSON());
        } catch (InterruptedException e) {
            Log.e("mine", "EEEEKInterrupted Exception!!!");
        }
        while (! accelsToSend.isEmpty()) {
            ret.put(accelsToSend.poll().toJSON());
        }
        return ret.toString();
    }
}
