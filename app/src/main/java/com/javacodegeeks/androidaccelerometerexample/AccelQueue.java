package com.javacodegeeks.androidaccelerometerexample;

import org.json.JSONArray;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by chase on 3/9/15.
 */
public class AccelQueue {
    public ConcurrentLinkedQueue<AccelTime> accelsToSend;

    public AccelQueue() {
        accelsToSend = new ConcurrentLinkedQueue<AccelTime>();
    }

    public String accelsToJSON() {
//        return "{\"time\":" + System.currentTimeMillis() + "}";
        JSONArray ret = new JSONArray();
        while (! accelsToSend.isEmpty()) {
            ret.put(accelsToSend.poll().toJSON());
        }
        return ret.toString();
    }
}
