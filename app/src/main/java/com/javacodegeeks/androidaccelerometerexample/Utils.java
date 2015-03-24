package com.javacodegeeks.androidaccelerometerexample;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Created by chase on 3/23/15.
 */
public class Utils {
    public static void postReqTask(String url) {
        new PostTask().execute(url);
    }

    public static void postReq(String url) {
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost request = new HttpPost(url);
            httpClient.execute(request);
        }catch (Exception ex) {
            Log.e("mine", "FAILED request");
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public static class PostTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            postData(params[0]);
            return null;
        }

        public void postData(String url) {
            postReq(url);
        }
    }
}
