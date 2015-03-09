package com.javacodegeeks.androidaccelerometerexample.test;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by chase on 3/6/15.
 */
public class AccelTime {
    private float x;
    private float y;
    private float z;
    private long time;
    public AccelTime(float x, float y, float z, long time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
    }

    public JSONArray toJSON() {
        JSONArray ret = new JSONArray();
        try {
            ret.put(x);
            ret.put(y);
            ret.put(z);
            ret.put(time);
        } catch (JSONException e) {
            Log.d("mine", "BAD JSON");
        }
        return ret;
    }
}
