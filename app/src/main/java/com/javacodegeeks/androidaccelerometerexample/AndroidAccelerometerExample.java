package com.javacodegeeks.androidaccelerometerexample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Map;
import java.util.HashMap;

import com.javacodegeeks.androidaccelerometerexample.test.AccelTime;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import im.delight.android.ddp.Meteor;
import im.delight.android.ddp.MeteorCallback;


public class AndroidAccelerometerExample extends Activity implements SensorEventListener, MeteorCallback {

	private SensorManager sensorManager;
	private Sensor accelerometer;

    private static int MAX_NOTIFY_DELTA = 20 * 1000;
    private static int MIN_NOTIFY_DELTA = 5 * 1000;
    private static int MIN_SMS_DELAY = 40 * 1000;
    public long last_notify = 0;
    private Queue<float[]> sensorEventQueue;
    public Queue<Long> movementTimesQueue; // TODO make private

    private ConcurrentLinkedQueue<AccelTime> accelsToSend;

    EditText minNotifyView;
    EditText maxNotifyView;
    EditText minSMSDelay;


    private float vibrateThreshold = 0;
    private Handler mHandler;

    private int count;

	private TextView countView, timeTillSMSAllowedView, notifyTimeRangeView;

	public Vibrator v;
    TextToSpeech ttobj;
    private Meteor mMeteor;

    private boolean meteorConnected = false;


    @Override
	public void onCreate(Bundle savedInstanceState) {
        sensorEventQueue = new LinkedList<float[]>();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initializeViews();

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			// success! we have an accelerometer

			accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
			vibrateThreshold = accelerometer.getMaximumRange() / 50;
		} else {
			// fail! we dont have an accelerometer!
		}

		//initialize vibration
		v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        count = 0;
        movementTimesQueue = new LinkedList<Long>();

        setupTimeUpdate();
        setupSettings();


        accelsToSend = new ConcurrentLinkedQueue<AccelTime>();

        ttobj=new TextToSpeech(getApplicationContext(),
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR){
                            ttobj.setLanguage(Locale.UK);
                        }
                    }
                });
        startSendingServerData();
//            startReadingTTS();

        mMeteor = new Meteor("ws://chasetodo.meteor.com/websocket");

        // register the callback that will handle events and receive messages
        mMeteor.setCallback(this);
    }

    public void speakText(String toSpeak){

        Toast.makeText(getApplicationContext(), toSpeak,
                Toast.LENGTH_SHORT).show();
        ttobj.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

    }

    private void setupSettings() {
        minNotifyView = (EditText) findViewById(R.id.minNotifyView);
        minNotifyView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    updateSettings();
                }
            }
        });

        maxNotifyView = (EditText) findViewById(R.id.maxNotifyView);
        maxNotifyView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    updateSettings();
                }
            }
        });
        minSMSDelay = (EditText) findViewById(R.id.minSMSDelay);
        minSMSDelay.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    updateSettings();
                }
            }
        });
        updateSettings();
    }

    private void updateSettings() {
        MIN_NOTIFY_DELTA = Integer.valueOf(minNotifyView.getText().toString()) * 1000;
        Log.d("mine", "min notify is" + MIN_NOTIFY_DELTA);
        MAX_NOTIFY_DELTA = Integer.valueOf(maxNotifyView.getText().toString()) * 1000;
        Log.d("mine", "max" + MAX_NOTIFY_DELTA);

        MIN_SMS_DELAY = Integer.valueOf(minSMSDelay.getText().toString()) * 1000;
        Log.d("mine", "sms is " + MIN_SMS_DELAY);

    }

    private String timeTillSMSAllowed() {
        if (System.currentTimeMillis() - last_notify > MIN_SMS_DELAY) {
            return "0";
        }
        return Long.toString((MIN_SMS_DELAY - (System.currentTimeMillis() - last_notify)) / 1000);
    }

    private void updateNotifyTimeRangeView() {
        if (shouldNotifyIfAdded(System.currentTimeMillis())) {
            notifyTimeRangeView.setText("Notification on next bump!");
        } else {
            notifyTimeRangeView.setText("Notification are sleeping");
        }
    }

    private void setupTimeUpdate() {
        mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                timeTillSMSAllowedView.setText(timeTillSMSAllowed());
                                updateNotifyTimeRangeView();


                            }
                        });
                    } catch (Exception e) {
                        Log.e("mine", "TODO something bad here, not sure what to do");
                    }
                }
            }
        }).start();
    }

    private float maxAccelDifference(float[] current) {
        float ret = 0;
        for (float[] arr : sensorEventQueue) {
            ret = Math.max(ret, (float) Math.sqrt(
                        Math.pow(arr[0] - current[0], 2) +
                        Math.pow(arr[1] - current[1], 2) +
                        Math.pow(arr[2] - current[2], 2)));
        }
        return ret;
    }

	public void initializeViews() {
        countView = (TextView) findViewById(R.id.count);
        timeTillSMSAllowedView = (TextView) findViewById(R.id.timeTillSMSAllowed);
        notifyTimeRangeView = (TextView) findViewById(R.id.notifyTimeRange);


    }

	////onResume() register the accelerometer for listening the events
	//protected void onResume() {
		//super.onResume();
		//sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	//}

	////onPause() unregister the accelerometer for stop listening the events
	//protected void onPause() {
		//super.onPause();
		//sensorManager.unregisterListener(this);
	//}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
        float[] current = {event.values[0], event.values[1], event.values[2]};
        accelsToSend.add(new AccelTime(event.values[0], event.values[1], event.values[2], System.currentTimeMillis()));

        count += 1;
//        if (count % 10 == 0) {
//            Log.d("mine", accelsToJSON());
//            Log.d("mine", "Attempt to send at count " + count);
////            Arrays.fill(accelsToSend, (float) 0);
//        }
        countView.setText(Integer.toString(count));
//		maybeVibrate(current); // TODO bring back
	}

    public boolean shouldNotify(long millis) {
        movementTimesQueue.add(millis);
        if (shouldNotifyIfAdded(millis)) {
            last_notify = millis;
            return true;
        }
        return false;
    }

    public boolean shouldNotifyIfAdded(long millis) {
        // clear queue
        while (movementTimesQueue.peek() != null && movementTimesQueue.peek() < millis - MAX_NOTIFY_DELTA) {
            movementTimesQueue.remove();
        }

        if (movementTimesQueue.peek() != null && movementTimesQueue.peek() < millis - MIN_NOTIFY_DELTA) {
            if (millis - last_notify > MIN_SMS_DELAY) {
                return true;
            }
        }
        return false;
    }

        // if the change in the accelerometer value is big enough, then vibrate!
	// our threshold is MaxValue/2
	public void maybeVibrate(float[] current) {


        if (maxAccelDifference(current) > vibrateThreshold) {
            if (shouldNotify(System.currentTimeMillis())) {
                Log.d("mine", "Actual notify. Sending sms!");
                speakText("Welcome to the lock free bike. If you would like this moved, please call the number located on the handlebars.");
                Date date = new Date();

                SmsManager.getDefault().sendTextMessage("5125778778", null, "Phone moved -- " + date.toString(), null,null);
                Toast.makeText(getApplicationContext(), "Sending SMS!", Toast.LENGTH_SHORT).show();
            }
            updateNotifyTimeRangeView();

//            v.vibrate(50);
            Log.d("mine", "Diff is great enough!");
            sensorEventQueue.clear();
        }
        sensorEventQueue.add(current);
        if (sensorEventQueue.size() > 100) {
            sensorEventQueue.remove();
        }

	}

    private String accelsToJSON() {
        JSONArray ret = new JSONArray();
        while (! accelsToSend.isEmpty()) {
            ret.put(accelsToSend.poll().toJSON());
        }
        return ret.toString();
    }

    private void startSendingServerData() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                //code to do the HTTP request
                while (true) {
                    HttpClient httpClient = new DefaultHttpClient();
                    try {

                        HttpPost request = new HttpPost("http://chasetodo.meteor.com/multi_accels/");
                        Log.d("mine", "get JSON");
                        StringEntity params = new StringEntity(accelsToJSON());
                        Log.d("mine", "got JSON");
                        request.addHeader("content-type", "application/json");
                        request.setEntity(params);
                        Log.d("mine", "Attempt to send for count" + count);
                        HttpResponse response = httpClient.execute(request);
//                response.getStatusLine().getStatusCode();
                        Log.d("mine", "SUCCESS request for count " + count);

                    } catch (Exception ex) {
                        // handle exception here
                        Log.d("mine", "FAILED request");
                    } finally {
                        httpClient.getConnectionManager().shutdown();
                    }
                }
            }
        });
        thread.start();
    }

    private void startReadingTTS() {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMeteor.disconnect();
    }
}
