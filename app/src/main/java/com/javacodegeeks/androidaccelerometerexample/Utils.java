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
    public final static String METEOR_URL = "http://104.131.138.229";
    public final static String METEOR_URL_WS = "ws://104.131.138.229/websocket";

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
