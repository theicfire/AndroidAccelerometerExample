package com.javacodegeeks.androidaccelerometerexample;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chase on 3/17/15.
 *
 */
public class PBullet {
    private final static String TAG = PBullet.class.getSimpleName();

    public void send(final String title, final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpClient httpClient = new DefaultHttpClient();
                try {
                    Log.d(TAG, "SENDING pbullet request");
                    HttpPost request = new HttpPost("http://biker.chaselambda.com/pbullet");
                    List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                    pairs.add(new BasicNameValuePair("msg", msg));
                    pairs.add(new BasicNameValuePair("title", title));
                    request.setEntity(new UrlEncodedFormEntity(pairs));
                    httpClient.execute(request);
                }catch (Exception ex) {
                    Log.e(TAG, "FAILED request");
                } finally {
                    httpClient.getConnectionManager().shutdown();
                }
            }
        }).start();
    }
}

