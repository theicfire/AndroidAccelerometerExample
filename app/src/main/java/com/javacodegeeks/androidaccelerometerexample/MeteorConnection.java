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
        Utils.postReqTask("http://biker.chaselambda.com/triggerAlarm");
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

