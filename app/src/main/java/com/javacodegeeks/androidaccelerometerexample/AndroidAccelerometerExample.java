package com.javacodegeeks.androidaccelerometerexample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.telephony.SmsManager;

import com.javacodegeeks.androidaccelerometerexample.push.PushNotifications;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;


public class AndroidAccelerometerExample extends Activity implements SensorEventListener, TextToSpeech.OnInitListener {

	private SensorManager sensorManager;
	private Sensor accelerometer;

    public long last_notify = 0;
    private Queue<float[]> sensorEventQueue;

    private float vibrateThreshold = 0;
    private Handler mHandler;

    private int count;

	private TextView countView, notifyTimeRangeView;

	public Vibrator v;
    TextToSpeech ttobj;

    private MyMeteor mMeteor;

    public AccelQueue accelQueue;

    private LocationMonitor locationMonitor;

    private boolean isProduction = false;
    private boolean alarmTriggered = false;

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

        setupTimeUpdate();

        accelQueue = new AccelQueue();

        ttobj = new TextToSpeech(getApplicationContext(), this);

        mMeteor = new MyMeteor(this);

        Log.d("mine", "Push creating, at the start");
        PushNotifications pusher = new PushNotifications(getApplicationContext(), this);


        locationMonitor = new LocationMonitor(this);

        ttobj = new TextToSpeech(getApplicationContext(), this);

    }

    @Override
    public void onInit(int status) {
        if(status != TextToSpeech.ERROR){
            ttobj.setLanguage(Locale.UK);
        }
    }

    private void sendTTSReceived() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                //code to do the HTTP request
                HttpClient httpClient = new DefaultHttpClient();
                try {
                    Log.d("mine", "Sending tts received");
                    HttpPost request = new HttpPost("http://chaselambda.com:3000/tts-received");
                    request.addHeader("content-type", "application/json");
                    httpClient.execute(request);
                } catch (Exception ex) {
                    // handle exception here
                    Log.d("mine", "FAILED request");
                } finally {
                    httpClient.getConnectionManager().shutdown();
                }
            }
        });
        thread.start();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
//        setIntent(intent);//must store the new intent unless getIntent() will return the old one
        String intentText = intent.getStringExtra(Intent.EXTRA_TEXT);
        Log.d("mine", "Got intent text: " + intentText);
        if (intentText.equals("gps-off")) {
            locationMonitor.gpsOff();
            Log.d("mine", "GPS OFF");
        } else if (intentText.equals("gps-on")) {
            locationMonitor.gpsOn();
            Log.d("mine", "GPS ON");
        } else if (intentText.equals("prod")) {
            isProduction = true;
            Log.d("mine", "PRODUCTION ON");
        } else if (intentText.equals("debug")) {
            isProduction = false;
            Log.d("mine", "PRODUCTION OFF");
        } else if (intentText.equals("alarm-reset")) {
            alarmTriggered = false;
            sensorEventQueue.clear();
            updateNotifyTimeRangeView();
            locationMonitor.gpsOff();
        } else {
            ttobj.speak(intentText, TextToSpeech.QUEUE_FLUSH, null);
        }
        sendTTSReceived();
    }

    private void updateNotifyTimeRangeView() {
        if (alarmTriggered) {
            notifyTimeRangeView.setText("Alarm triggered!");
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
        if (alarmTriggered) {
            accelQueue.accelsToSend.add(new AccelTime(event.values[0], event.values[1], event.values[2], System.currentTimeMillis()));
        }

        count += 1;
//        if (count % 10 == 0) {
//            Log.d("mine", accelsToJSON());
//            Log.d("mine", "Attempt to send at count " + count);
////            Arrays.fill(accelsToSend, (float) 0);
//        }
        countView.setText(Integer.toString(count));
		maybeVibrate(current); // TODO bring back
	}


        // if the change in the accelerometer value is big enough, then vibrate!
	// our threshold is MaxValue/2
	public void maybeVibrate(float[] current) {
        if (maxAccelDifference(current) > vibrateThreshold) {
            long curTime = System.currentTimeMillis();
            if (!alarmTriggered) {
                alarmTriggered = true;
                Log.d("mine", "Actual notify. Sending sms!");
                last_notify = curTime;
                if (isProduction) {
                    ttobj.speak("Welcome to the lock free bike. If you would like this moved, please call the number located on the handlebars.", TextToSpeech.QUEUE_FLUSH, null);
                    Date date = new Date();
                    SmsManager.getDefault().sendTextMessage("5125778778", null, "Phone moved -- " + date.toString(), null,null);
                    Toast.makeText(getApplicationContext(), "Sending SMS!", Toast.LENGTH_SHORT).show();
                } else {
                    v.vibrate(50);
                }
                locationMonitor.gpsOn();
            }
            updateNotifyTimeRangeView();


            Log.d("mine", "Diff is great enough!");
            sensorEventQueue.clear();
        }
        sensorEventQueue.add(current);
        if (sensorEventQueue.size() > 10000) {
            sensorEventQueue.remove();
        }

	}
//
//    private void startSendingServerData() {
//        Thread thread = new Thread(new Runnable(){
//            @Override
//            public void run(){
//                //code to do the HTTP request
//                while (true) {
//                    HttpClient httpClient = new DefaultHttpClient();
//                    try {
//
//                        HttpPost request = new HttpPost("http://chaselambda.com:3000/multi_accels/");
//                        Log.d("mine", "get JSON");
//                        StringEntity params = new StringEntity(accelQueue.accelsToJSON());
//                        Log.d("mine", "got JSON");
//                        request.addHeader("content-type", "application/json");
//                        request.setEntity(params);
//                        Log.d("mine", "Attempt to send for count" + count);
//                        HttpResponse response = httpClient.execute(request);
////                response.getStatusLine().getStatusCode();
//                        Log.d("mine", "SUCCESS request for count " + count);
//
//                    } catch (Exception ex) {
//                        // handle exception here
//                        Log.d("mine", "FAILED request");
//                    } finally {
//                        httpClient.getConnectionManager().shutdown();
//                    }
//                }
//            }
//        });
//        thread.start();
//    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        Log.d("mine", "onstart called");
//        locationMonitor.mGoogleApiClient.connect();
//        Log.d("mine", "THEN CALLED connect()");
//    }

//    @Override
//    protected void onStop() {
//        super.onStop();
////        locationMonitor.mGoogleApiClient.purposefulDisconnect(); // DON't stop, we want to run this in the background
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMeteor.mMeteor.disconnect();
    }


}
