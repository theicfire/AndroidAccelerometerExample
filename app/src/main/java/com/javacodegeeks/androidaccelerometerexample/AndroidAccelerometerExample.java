package com.javacodegeeks.androidaccelerometerexample;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.javacodegeeks.androidaccelerometerexample.ble.BleActivityComponent;
import com.javacodegeeks.androidaccelerometerexample.push.PushNotifications;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Date;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class AndroidAccelerometerExample extends Activity implements SensorEventListener, TextToSpeech.OnInitListener {

    public static final String TAG = "AndroidAccExample";
    private final float vibrateThreshold = (float) .5;
    private SensorManager sensorManager;
    private Queue<AccelTime> sensorEventQueue;
    private Handler mHandler;
    private int count;
    private TextView countView, notifyTimeRangeView;
    private Vibrator v;
    private TextToSpeech ttobj;
    private MyMeteor mMeteor;
    private LocationMonitor locationMonitor;
    private boolean isProduction = false;
    private PBullet pbullet;
    private PowerManager.WakeLock mWakeLock;
    private BleActivityComponent mBle;
    public boolean alarmTriggered = false;
    public AccelQueue accelQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            Log.e(TAG, "No Accelerometer");
            throw new AssertionError("crap");
        }

        sensorEventQueue = new ConcurrentLinkedQueue<AccelTime>();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initializeViews();
        registerListener();
        (new PushNotifications(getApplicationContext(), this)).runRegisterInBackground();
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        count = 0;
        setupTimeUpdate();
        accelQueue = new AccelQueue();
        ttobj = new TextToSpeech(getApplicationContext(), this);
        mMeteor = new MyMeteor(this);
        locationMonitor = new LocationMonitor(this);
        ttobj = new TextToSpeech(getApplicationContext(), this);
        pbullet = new PBullet();
        PowerManager manager =
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        // Needed to have accelerometer readings stay up to date, and not delay when the screen is off.
        mWakeLock.acquire();
        mBle = new BleActivityComponent(this);
    }

    private void registerListener() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.ERROR) {
            ttobj.setLanguage(Locale.UK);
        }
    }

    private void sendTTSReceived() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpClient httpClient = new DefaultHttpClient();
                try {
                    Log.d("mine", "Sending tts received");
                    HttpPost request = new HttpPost("http://biker.chaselambda.com/tts-received");
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
        if (intentText == null) {
            Log.e("mine", "Intent text equals null for some reason");
            return;
        }
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
        } else if (intentText.equals("alarm-trigger")) {
            alarmTriggered = true;
            mMeteor.alarmTrigger();
            ttobj.speak("Artifically triggered.", TextToSpeech.QUEUE_FLUSH, null);
            locationMonitor.gpsOn();
        } else {
            ttobj.speak(intentText, TextToSpeech.QUEUE_FLUSH, null);
        }
        sendTTSReceived();
    }

    private void updateNotifyTimeRangeView() {
        if (alarmTriggered) {
            notifyTimeRangeView.setText("Alarm triggered!");
        } else {
            notifyTimeRangeView.setText("Waiting for bump");
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

    private float maxAccelDifference(AccelTime current) {
        float ret = 0;
        for (AccelTime arr : sensorEventQueue) {
            ret = Math.max(ret, (float) Math.sqrt(
                    Math.pow(arr.x - current.x, 2) +
                            Math.pow(arr.y - current.y, 2) +
                            Math.pow(arr.z - current.z, 2)));
        }
        return ret;
    }

    public void initializeViews() {
        countView = (TextView) findViewById(R.id.count);
        notifyTimeRangeView = (TextView) findViewById(R.id.notifyTimeRange);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        AccelTime accelTime = new AccelTime(event.values[0], event.values[1], event.values[2], System.currentTimeMillis());
        accelQueue.accelsToSend.add(accelTime);
        count += 1;
        countView.setText(Integer.toString(count));
        maybeVibrate(accelTime);
    }

    // if the change in the accelerometer value is big enough, then vibrate!
    // our threshold is MaxValue/2
    public void maybeVibrate(AccelTime accelTime) {
        if (maxAccelDifference(accelTime) > vibrateThreshold) {
            if (!alarmTriggered) {
                alarmTriggered = true;
                Log.d("mine", "Actual notify. Sending sms!");
                Date date = new Date();
                if (isProduction) {
                    ttobj.speak("Welcome to the lock free bike. If you would like this moved, please call the number located on the handlebars.", TextToSpeech.QUEUE_FLUSH, null);
                    pbullet.send("Phone moved!", "At " + date.toString());
                    SmsManager.getDefault().sendTextMessage("+15125778778", null, "Phone moved -- " + date.toString(), null, null);
                    Toast.makeText(getApplicationContext(), "Sending SMS!", Toast.LENGTH_SHORT).show();
                } else {
                    v.vibrate(50);
                }
                mMeteor.alarmTrigger();
                locationMonitor.gpsOn();
            }
            updateNotifyTimeRangeView();


            Log.d("mine", "Diff is great enough!");
            sensorEventQueue.clear();
        }
        sensorEventQueue.add(accelTime);
        if (sensorEventQueue.size() > 100) {
            sensorEventQueue.poll();
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
//                        HttpPost request = new HttpPost("http://biker.chaselambda.com/multi_accels/");
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
//        Log.d(TAG, "STOP!");
//        mWakeLock.release();
//        mMeteor.mMeteor.disconnect();
//        unregisterListener();
////        locationMonitor.mGoogleApiClient.purposefulDisconnect(); // DON't stop, we want to run this in the background
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DESTROY!");
        mWakeLock.release();
        mMeteor.mMeteor.disconnect();
        unregisterListener();

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBle.UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mBle.mServiceConnection);
        mBle.mService.stopSelf();
        mBle.mService = null;
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBle.mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableIntent, BleActivityComponent.REQUEST_ENABLE_BT);
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mBle.onActivityResult(requestCode, resultCode, data);
    }
}