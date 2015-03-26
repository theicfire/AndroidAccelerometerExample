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
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;

import com.javacodegeeks.androidaccelerometerexample.ble.BleActivityComponent;
import com.javacodegeeks.androidaccelerometerexample.detector.MovementDetector;
import com.javacodegeeks.androidaccelerometerexample.detector.Alertable;
import com.javacodegeeks.androidaccelerometerexample.push.PushNotifications;

import java.util.Date;
import java.util.Locale;


public class MainActivity extends Activity implements SensorEventListener, TextToSpeech.OnInitListener, Alertable {

    public static final String TAG = "AndroidAccExample";
    private SensorManager sensorManager;
    private Vibrator v;
    private TextToSpeech ttobj;
    private MyMeteor mMeteor;
    private LocationMonitor locationMonitor;
    private boolean isProduction = false;
    private PBullet pbullet;
    private PowerManager.WakeLock mWakeLock;
    private BleActivityComponent mBle;
    public boolean alarmTriggered = false;
    public MovementDetector movementDetector;
    private TextView countView, notifyTimeRangeView;
    private int count;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            Log.e(TAG, "No Accelerometer");
            throw new AssertionError("crap");
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        registerListener();
        (new PushNotifications(getApplicationContext(), this)).runRegisterInBackground();
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

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
        initializeViews();
        movementDetector = new MovementDetector(this, (TextView) findViewById(R.id.excessiveAlertStatus));
        count = 0;
    }

    private void registerListener() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void updateNotifyTimeRangeView() {
        if (alarmTriggered) {
            notifyTimeRangeView.setText("Alarm triggered!");
        } else {
            notifyTimeRangeView.setText("Waiting for bump");
        }
    }

    private void initializeViews() {
        countView = (TextView) findViewById(R.id.count);
        notifyTimeRangeView = (TextView) findViewById(R.id.triggerStatus);
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
            setAlarmTriggered(false);
        } else if (intentText.equals("alarm-trigger")) {
            setAlarmTriggered(true);
            sendExcessiveAlert();
            ttobj.speak("Artifically triggered.", TextToSpeech.QUEUE_FLUSH, null);
        } else if (intentText.equals("bt-on")) {
            mBle.arduinoConnect();
        } else if (intentText.equals("bt-off")) {
            mBle.disconnect();
        } else if (intentText.equals("lon")) {
            LEDLights.turnOn(mBle.mService);
        } else if (intentText.equals("loff")) {
            LEDLights.turnOff(mBle.mService);
        } else if (intentText.equals("chain-on")) {
            BikeChain.turnOn(mBle.mService);
        } else {
            ttobj.speak(intentText, TextToSpeech.QUEUE_FLUSH, null);
        }
        Utils.postReqTask("http://biker.chaselambda.com/tts-received");
    }

    public void setAlarmTriggered(boolean t) {
        alarmTriggered = t;
        if (t) {
            locationMonitor.gpsOn();
        } else {
            locationMonitor.gpsOff();
            movementDetector.reset();
        }
        updateNotifyTimeRangeView();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        movementDetector.add(new AccelTime(event.values[0], event.values[1], event.values[2], System.currentTimeMillis()));
        count += 1;
        countView.setText(Integer.toString(count));
    }

    @Override
    public void sendMiniAlert() {
        if (!alarmTriggered) {
            setAlarmTriggered(true);
            Log.d("mine", "Sending mini alert.");
            if (isProduction) {
                Date date = new Date();
                ttobj.speak("Welcome to the rocket bike. It doesn't need a thick lock because it is equipped with tracking equipment, internet connectivity, and a horrendously loud siren.", TextToSpeech.QUEUE_FLUSH, null);
                pbullet.send("MiniAlert: Phone moved once.", "At " + date.toString());
            } else {
                v.vibrate(50);
            }
        }
    }

    @Override
    public void unsetMiniAlert() {
        setAlarmTriggered(false);

    }

    @Override
    public void sendExcessiveAlert() {
        Log.d("mine", "Excessive notify.");
        Date date = new Date();
        mMeteor.alarmTrigger();
        if (isProduction) {
//                    ttobj.speak("Welcome to the lock free bike. If you would like this moved, please call the number located on the handlebars.", TextToSpeech.QUEUE_FLUSH, null);
            pbullet.send("Phone moved LOTS!", "At " + date.toString());
            SmsManager.getDefault().sendTextMessage("+15125778778", null, "Phone moved LOTS -- " + date.toString(), null, null);
//            Toast.makeText(getApplicationContext(), "Sending SMS!", Toast.LENGTH_SHORT).show();
        } else {
            v.vibrate(500);
        }
    }


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