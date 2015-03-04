package com.javacodegeeks.androidaccelerometerexample;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class AndroidAccelerometerExample extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	private Sensor accelerometer;

    private static final int MAX_NOTIFY_DELTA = 20 * 1000;
    private static final int MIN_NOTIFY_DELTA = 5 * 1000;
    private static final int TWO_MINUTES = 40 * 1000;
    public long last_notify = 0;
    private Queue<float[]> sensorEventQueue;
    public Queue<Long> movementTimesQueue; // TODO make private

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


    }

    private String timeTillSMSAllowed() {
        if (System.currentTimeMillis() - last_notify > TWO_MINUTES) {
            return "0";
        }
        return Long.toString((TWO_MINUTES - (System.currentTimeMillis() - last_notify)) / 1000);
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
                                Log.d("mine", "come very 1 second");
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
        count += 1;
        countView.setText(Integer.toString(count));
        float[] current = {event.values[0], event.values[1], event.values[2]};
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
            if (millis - last_notify > TWO_MINUTES) {
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
}
