package com.javacodegeeks.androidaccelerometerexample;

import android.util.Log;

import com.javacodegeeks.androidaccelerometerexample.detector.Alertable;

import java.util.HashMap;
import java.util.Map;

import im.delight.android.ddp.Meteor;
import im.delight.android.ddp.MeteorCallback;

/**
 * Created by chase on 3/9/15.
 *
 */
public class MeteorConnection implements MeteorCallback {
    private final static String TAG = MeteorConnection.class.getSimpleName();
    public Meteor mMeteor;
    MainActivity activity;
    private boolean reconnecting = false;
    private boolean meteorSenderStarted = false;

    public MeteorConnection(MainActivity activity) {
        this.activity = activity;
        mMeteor = new Meteor(Utils.METEOR_URL_WS);
        mMeteor.setCallback(this);
    }

    @Override
    public void onConnect() {
        Log.d(TAG, "Connected");
        Log.d(TAG, "onConnect");
        reconnecting = false;
        if (!meteorSenderStarted) {
            meteorSender();
            meteorSenderStarted = true;
        }
    }

    @Override
    public void onDisconnect(int code, String reason) {
        Log.d(TAG, "Disconnected");

        if (!reconnecting) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    Log.d(TAG, "Try to reconnect");
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
        Log.d(TAG, "Data added to <"+collectionName+"> in document <"+documentID+">");
        Log.d(TAG, "    Added: "+fieldsJson);
    }

    @Override
    public void onDataChanged(String collectionName, String documentID, String updatedValuesJson, String removedValuesJson) {
        Log.d(TAG, "Data changed in <"+collectionName+"> in document <"+documentID+">");
        Log.d(TAG, "    Updated: "+updatedValuesJson);
        Log.d(TAG, "    Removed: "+removedValuesJson);
    }

    @Override
    public void onDataRemoved(String collectionName, String documentID) {
        Log.d(TAG, "Data removed from <"+collectionName+"> in document <"+documentID+">");
    }

    @Override
    public void onException(Exception e) {
        Log.d(TAG, "Exception");
        if (e != null) {
            e.printStackTrace();
        }
    }

    public void alarmTrigger() {
        Utils.postReqThread(Utils.METEOR_URL + "/triggerAlarm");
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
                    if (activity.getAlertStatus() != Alertable.AlertStatus.UNTRIGGERED) {
                        if (mMeteor.isConnected()) {
                            Log.d(TAG, "connected, sending data");
                            Map<String, Object> insertValues = new HashMap<String, Object>();
                            // Potentially blocking! Nonzero JSON entries guaranteed.
                            Log.d(TAG, "Size to send is " + activity.movementDetector.accelQueueMeteor.accelsToSend.size());
                            insertValues.put("accelsJson", activity.movementDetector.accelQueueMeteor.accelsToJSON());
                            insertValues.put("createdAt", System.currentTimeMillis());
                            Log.d(TAG, "Meteor insert!");
                            mMeteor.insert("batchAccels", insertValues);

                            Log.d(TAG, "Meteor insert done!");
                        } else {
                            Log.w(TAG, "Clearing accelsToSend, because meteor is not connected");
                            activity.movementDetector.accelQueueMeteor.accelsToSend.clear();
                        }
                    }
                }
            }
        });
        thread.start();
    }
}

