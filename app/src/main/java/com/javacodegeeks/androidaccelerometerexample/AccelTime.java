package com.javacodegeeks.androidaccelerometerexample;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by chase on 3/6/15.
 *
 */
public class AccelTime {
    private final static String TAG = AccelTime.class.getSimpleName();

    public float x;
    public float y;
    public float z;
    public long time;

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
            Log.d(TAG, "BAD JSON");
        }
        return ret;
    }
}
