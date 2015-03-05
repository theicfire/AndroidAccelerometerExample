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
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class AndroidAccelerometerExample extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	private Sensor accelerometer;

    private static int MAX_NOTIFY_DELTA = 20 * 1000;
    private static int MIN_NOTIFY_DELTA = 5 * 1000;
    private static int MIN_SMS_DELAY = 40 * 1000;
    public long last_notify = 0;
    private Queue<float[]> sensorEventQueue;
    public Queue<Long> movementTimesQueue; // TODO make private

    private float[][] accelsToSend;
    private long[] accelsToSendTime;
    private static final int ACCELS_TO_SEND_LENGTH = 10;

    EditText minNotifyView;
    EditText maxNotifyView;
    EditText minSMSDelay;


    private float vibrateThreshold = 0;
    private Handler mHandler;

    private int count;

	private TextView countView, timeTillSMSAllowedView, notifyTimeRangeView;

	public Vibrator v;

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


        accelsToSend = new float[10][3];
        accelsToSendTime = new long[10];

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
        accelsToSend[count % 10][0] = event.values[0];
        accelsToSend[count % 10][1] = event.values[1];
        accelsToSend[count % 10][2] = event.values[2];
        accelsToSendTime[count % 10] = System.currentTimeMillis();
//        Log.d("mine", "time" + accelsToSendTime[count % 10]);

        count += 1;
        if (count % 10 == 0) {
            Log.d("mine", accelsToJSON());
            Log.d("mine", "Attempt to send at count " + count);
            new MyAsyncTask().execute(accelsToJSON(), Integer.toString(count));
//            Arrays.fill(accelsToSend, (float) 0);
        }
        countView.setText(Integer.toString(count));
		maybeVibrate(current);
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
        for (int i = 0; i < accelsToSend.length; i++) {
            JSONArray toAdd = new JSONArray();
            for (float n : accelsToSend[i]) {
                try {
                    toAdd.put((double) n);
                } catch (JSONException e) {
                    Log.d("mine", "BAD JSON");
                }

            }
            toAdd.put(accelsToSendTime[i]);
            ret.put(toAdd);
        }
        return ret.toString();
    }

    private class MyAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            postData(params[0], params[1]);
            return null;
        }

        public void postData(String json, String count) {
            HttpClient httpClient = new DefaultHttpClient();
            try {
                HttpPost request = new HttpPost("https://paetwmjyqv.localtunnel.me/multi_accels");
                StringEntity params =new StringEntity(json);
                request.addHeader("content-type", "application/json");
                request.setEntity(params);
                HttpResponse response = httpClient.execute(request);
//                response.getStatusLine().getStatusCode();
                Log.d("mine", "SUCCESS request for count " + count);

                // handle response here...
            }catch (Exception ex) {
                // handle exception here
                Log.d("mine", "FAILED request");
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }

    }
}
