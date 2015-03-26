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

    public void send(String title, String msg) {
        new MyAsyncTask().execute(title, msg);
    }

    public class MyAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            postData(params[0], params[1]);
            return null;
        }

        public void postData(String title, String msg) {
            HttpClient httpClient = new DefaultHttpClient();
            try {
                HttpPost request = new HttpPost("http://pbullet.chaselambda.com/send");
                List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                pairs.add(new BasicNameValuePair("message", msg));
                pairs.add(new BasicNameValuePair("title", title));
                request.setEntity(new UrlEncodedFormEntity(pairs));
                httpClient.execute(request);

                Log.d(TAG, "SUCCESS request");
                // handle response here...
            }catch (Exception ex) {
                // handle exception here
                Log.d(TAG, "FAILED request");
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }
    }
}

