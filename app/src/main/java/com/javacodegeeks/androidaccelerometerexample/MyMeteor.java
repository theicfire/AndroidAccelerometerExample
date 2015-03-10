package com.javacodegeeks.androidaccelerometerexample;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import im.delight.android.ddp.Meteor;
import im.delight.android.ddp.MeteorCallback;

/**
 * Created by chase on 3/9/15.
 */
public class MyMeteor implements MeteorCallback {
    private boolean meteorConnected = false;
    private Meteor mMeteor;

    public MyMeteor() {

        mMeteor = new Meteor("ws://chasetodo.meteor.com/websocket");
        mMeteor.setCallback(this);
    }

    public void startSending() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                //code to do the HTTP request
                while (true) {
                    if (meteorConnected) {
                        Log.d("mine", "connected, sending data");
                        Map<String, Object> insertValues = new HashMap<String, Object>();
//        insertValues.put("_id", "my-key");
                        insertValues.put("androidTime", System.currentTimeMillis());
                        mMeteor.insert("hitters", insertValues);
                        try {
                            Thread.sleep(500);
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        Log.d("mine", "done sending data");
                    }
//                    URL url;
//                    HttpURLConnection conn;
//                    BufferedReader rd;
//                    String line;
//                    String result = "";
//                    try {
//                        url = new URL("http://accel.chaselambda.com/");
//                        conn = (HttpURLConnection) url.openConnection();
//                        conn.setRequestMethod("GET");
//                        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                        while ((line = rd.readLine()) != null) {
//                            result += line;
//                        }
//                        rd.close();
//                        Log.d("mine", "RESULT" + result);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                }
            }
        });
        thread.start();
    }

    @Override
    public void onConnect() {
        System.out.println("Connected");

        // subscribe to data from the server
//        String subscriptionId = mMeteor.subscribe("hitters");

        // unsubscribe from data again (usually done later or not at all)
//        mMeteor.unsubscribe(subscriptionId);

        // insert data into a collection
        meteorConnected = true;

//        // update data in a collection
//        Map<String, Object> updateQuery = new HashMap<String, Object>();
//        updateQuery.put("_id", "my-key");
//        Map<String, Object> updateValues = new HashMap<String, Object>();
//        insertValues.put("_id", "my-key");
//        insertValues.put("some-number", 5);
//        mMeteor.update("my-collection", updateQuery, updateValues);
//
//        // remove data from a collection
//        mMeteor.remove("my-collection", "my-key");
//
//        // call an arbitrary method
//        mMeteor.call("/my-collection/count");
    }

    @Override
    public void onDisconnect(int code, String reason) {
        System.out.println("Disconnected");
        meteorConnected = false;
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

    public void disconnect() {
        mMeteor.disconnect();
    }
}

