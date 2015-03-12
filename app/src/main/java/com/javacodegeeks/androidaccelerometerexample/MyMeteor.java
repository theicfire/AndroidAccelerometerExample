package com.javacodegeeks.androidaccelerometerexample;

import android.util.Log;

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

    private Lock lock = new ReentrantLock();
    private boolean meteorSenderRunning = false;

    public MyMeteor(AndroidAccelerometerExample activity) {
        this.activity = activity;
        mMeteor = new Meteor("ws://chaselambda.com:3000/websocket");
        mMeteor.setCallback(this);
    }

    @Override
    public void onConnect() {
        System.out.println("Connected");
        Log.d(TAG, "onConnect");
        // Let's catch up to Meteor before disconnecting
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                purposefulDisconnect();
            }
        });
        thread.start();

    }

    @Override
    public void onDisconnect(int code, String reason) {
        System.out.println("Disconnected");
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

    public void purposefulDisconnect() {
        Log.d(TAG, "Purposeful disconnect");
        // does not call onDisconnect()
        mMeteor.disconnect();
    }

    public void reconnect() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                while (!mMeteor.isConnected()) {
                    Log.d(TAG, "reconnect!!");
                    mMeteor.reconnect();
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
                meteorSender();
            }
        });
        thread.start();
    }

    public void runSender() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                lock.lock();
                Log.d(TAG, "runSender for " + meteorSenderRunning);
                if (!meteorSenderRunning) {
                    Log.d(TAG, "reconnecting");
                    reconnect();
                }
                lock.unlock();
            }
        });
        thread.start();
    }

    public void meteorSender() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                Log.d(TAG, "Running meteorSender");
                lock.lock();
                meteorSenderRunning = true;
                while (mMeteor.isConnected() && System.currentTimeMillis() - activity.last_notify < activity.MIN_SMS_DELAY) {
                    lock.unlock();
                    Log.d(TAG, "connected, sending data");
                    Map<String, Object> insertValues = new HashMap<String, Object>();
                    insertValues.put("accelsJson", activity.accelQueue.accelsToJSON());
                    mMeteor.insert("batchAccels", insertValues);
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    Log.d(TAG, "done sending data");
                    lock.lock();
                }
                meteorSenderRunning = false;
                purposefulDisconnect();
                lock.unlock();
                Log.d(TAG, "meteorSender stopping");
            }
        });
        thread.start();
    }
}

