package com.javacodegeeks.androidaccelerometerexample;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import im.delight.android.ddp.Meteor;
import im.delight.android.ddp.MeteorCallback;

/**
 * Created by chase on 3/9/15.
 */
public class MyMeteor implements MeteorCallback {
    static final String TAG = "MyMeteor";
    public Meteor mMeteor;
    private boolean started = false;
    AndroidAccelerometerExample activity;
    private boolean reconnecting = false;
    private boolean meteorSenderStarted = false;
//    private Lock lock = new ReentrantLock();

    public MyMeteor(AndroidAccelerometerExample activity) {
        this.activity = activity;
        mMeteor = new Meteor("ws://biker.chaselambda.com/websocket");
        mMeteor.setCallback(this);
    }

    @Override
    public void onConnect() {
        System.out.println("Connected");
        Log.d(TAG, "onConnect");
        reconnecting = false;
        if (!meteorSenderStarted) {
            meteorSender();
            meteorSenderStarted = true;
        }
    }

    @Override
    public void onDisconnect(int code, String reason) {
        System.out.println("Disconnected");

        if (!reconnecting) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("Try to reconnect");
                    reconnecting = false;
                    mMeteor.reconnect();
                }
            });
            thread.start();
        }
        reconnecting = true;

    }

    @Override
    public void onDataAdded(String collectionName, String documentID, String fieldsJson) {
        System.out.println("Data added to <"+collectionName+"> in document <"+documentID+">");
        System.out.println("    Added: "+fieldsJson);
    }

    @Override
    public void onDataChanged(String collectionName, String documentID, String updatedValuesJson, String removedValuesJson) {
        System.out.println("Data changed in <"+collectionName+"> in document <"+documentID+">");
        System.out.println("    Updated: "+updatedValuesJson);
        System.out.println("    Removed: "+removedValuesJson);
    }

    @Override
    public void onDataRemoved(String collectionName, String documentID) {
        System.out.println("Data removed from <"+collectionName+"> in document <"+documentID+">");
    }

    @Override
    public void onException(Exception e) {
        System.out.println("Exception");
        if (e != null) {
            e.printStackTrace();
        }
    }

    public void alarmTrigger() {
//        Map<String, Object> query= new HashMap<String, Object>();
//        query.put("_id", "zXHhuCCtXNkXK6EZ");
//        Map<String, Object> update = new HashMap<String, Object>();
//        // Potentially blocking! Nonzero JSON entries guaranteed.
//        update.put("_id", "zXHhuCCtXNkXK6EZ");
//        update.put("name", "alarmSet");
//        update.put("value", false);
//        Log.d(TAG, "Meteor alert!");
//        mMeteor.update("other", query, update);
//        Log.d(TAG, "Meteor alert done");
        new MyAsyncTask().execute();
    }

    private class MyAsyncTask extends AsyncTask<Location, Integer, String> {

        @Override
        protected String doInBackground(Location... params) {
            postData();
            return null;
        }

        public void postData() {
            HttpClient httpClient = new DefaultHttpClient();
            try {
                HttpPost request = new HttpPost("http://biker.chaselambda.com/triggerAlarm");
                HttpResponse response = httpClient.execute(request);
            }catch (Exception ex) {
                // handle exception here
                Log.d("mine", "FAILED request");
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }
    }

    public void meteorSender() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                Log.d(TAG, "Running meteorSender");
                while (true) {
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    if (activity.alarmTriggered) {
                        if (mMeteor.isConnected()) {
                            Log.d(TAG, "connected, sending data");
                            Map<String, Object> insertValues = new HashMap<String, Object>();
                            // Potentially blocking! Nonzero JSON entries guaranteed.
                            insertValues.put("accelsJson", activity.accelQueue.accelsToJSON());
                            insertValues.put("createdAt", System.currentTimeMillis());
                            Log.d(TAG, "Meteor insert!");
                            mMeteor.insert("batchAccels", insertValues);

                            Log.d(TAG, "Meteor insert done!");
                        } else {
                            Log.w(TAG, "Clearing accelsToSend, because meteor is not connected");
                            activity.accelQueue.accelsToSend.clear();
                        }
                    }
                }
            }
        });
        thread.start();
    }
}

