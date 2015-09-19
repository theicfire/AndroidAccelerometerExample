package com.javacodegeeks.androidaccelerometerexample;

import android.os.AsyncTask;
import android.util.Log;

import com.javacodegeeks.androidaccelerometerexample.detector.Alertable;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Created by chase on 3/23/15.
 *
 */
public class Utils {
    private final static String TAG = Utils.class.getSimpleName();
    public final static String METEOR_URL = "http://biker.chaselambda.com"; // IP address of DO server
    public final static String METEOR_URL_WS = "ws://biker.chaselambda.com/websocket";

//    public final static String METEOR_URL = "http://10.1.10.37:3000";
//    public final static String METEOR_URL_WS = "ws://10.1.10.37:3000/websocket";

    public static void postReqThread(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpClient httpClient = new DefaultHttpClient();
                try {
                    Log.d(TAG, "SENDING request " + url);
                    HttpPost request = new HttpPost(url);
                    httpClient.execute(request);
                }catch (Exception ex) {
                    Log.e(TAG, "FAILED request");
                } finally {
                    httpClient.getConnectionManager().shutdown();
                }
            }
        }).start();
    }

    public static void postReq(String url) {
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost request = new HttpPost(url);
            httpClient.execute(request);
        }catch (Exception ex) {
            Log.e(TAG, "FAILED request");
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}
