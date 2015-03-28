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

import com.javacodegeeks.androidaccelerometerexample.ble.BleActivityComponent;
import com.javacodegeeks.androidaccelerometerexample.detector.Alertable;
import com.javacodegeeks.androidaccelerometerexample.detector.MovementDetector;
import com.javacodegeeks.androidaccelerometerexample.push.PushNotifications;

import java.util.Date;
import java.util.Locale;


public class MainActivity extends Activity implements SensorEventListener, TextToSpeech.OnInitListener, Alertable {

    private final static String TAG = MainActivity.class.getSimpleName();
    private SensorManager sensorManager;
    private Vibrator v;
    private TextToSpeech ttsobj;
    private MeteorConnection mMeteor;
    private GpsMonitor gpsMonitor;
    private boolean isProduction = false;
    private PBullet pbullet;
    private PowerManager.WakeLock mWakeLock;
    private BleActivityComponent mBle;
    public MovementDetector movementDetector;
    private TextView countView, alertStatusView;
    private int count;
    private AlertStatus alertStatus;
    private TextView excessiveAlertStatusView;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            Log.e(TAG, "No Accelerometer");
            throw new AssertionError("crap");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        (new PushNotifications(getApplicationContext(), this)).runRegisterInBackground();

        count = 0;
        alertStatus = AlertStatus.UNTRIGGERED;
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        ttsobj = new TextToSpeech(getApplicationContext(), this);
        mMeteor = new MeteorConnection(this);
        gpsMonitor = new GpsMonitor(this);
        ttsobj = new TextToSpeech(getApplicationContext(), this);
        pbullet = new PBullet();
        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        // Needed to have accelerometer readings stay up to date, and not delay when the screen is off.
        mWakeLock.acquire();
        mBle = new BleActivityComponent(this);
        countView = (TextView) findViewById(R.id.count);
        alertStatusView = (TextView) findViewById(R.id.alertStatus);
        excessiveAlertStatusView = (TextView) findViewById(R.id.excessiveAlertStatus);
        movementDetector = new MovementDetector(this);
        startExcessiveAlertStatusUpdate();
        Utils.postReqThread(Utils.METEOR_URL + "/phonestart");
    }

    @Override
    public AlertStatus getAlertStatus() {
        return alertStatus;
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.ERROR) {
            ttsobj.setLanguage(Locale.UK);
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
//        setIntent(intent);//must store the new intent unless getIntent() will return the old one
        String intentText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (intentText == null) {
            Log.e(TAG, "Intent text equals null for some reason");
            return;
        }
        Log.d(TAG, "Got intent text: " + intentText);
        if (intentText.equals("gps-off")) {
            gpsMonitor.gpsOff();
            Log.d(TAG, "GPS OFF");
        } else if (intentText.equals("gps-on")) {
            gpsMonitor.gpsOn();
            Log.d(TAG, "GPS ON");
        } else if (intentText.equals("prod")) {
            isProduction = true;
            Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/prodOn/true");
            Log.d(TAG, "PRODUCTION ON");
        } else if (intentText.equals("debug")) {
            isProduction = false;
            Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/prodOn/false");
            Log.d(TAG, "PRODUCTION OFF");
        } else if (intentText.equals("alarm-reset")) {
            setAlertStatus(AlertStatus.UNTRIGGERED);
        } else if (intentText.equals("alarm-trigger")) {
            setAlertStatus(AlertStatus.MINI);
            sendExcessiveAlert();
            ttsobj.speak("Artifically triggered.", TextToSpeech.QUEUE_FLUSH, null);
        } else if (intentText.equals("bt-on")) {
            mBle.arduinoConnect();
        } else if (intentText.equals("bt-off")) {
            mBle.disconnect();
        } else if (intentText.equals("lon")) {
            BikeLEDLights.turnOn(mBle.mService);
        } else if (intentText.equals("loff")) {
            BikeLEDLights.turnOff(mBle.mService);
        } else if (intentText.equals("chain-on")) {
            BikeChain.turnOn(mBle.mService);
        } else if (intentText.equals("siren-short")) {
            BikeSiren.onShort(mBle.mService);
        } else if (intentText.equals("siren-medium")) {
            BikeSiren.onMedium(mBle.mService);
        } else if (intentText.equals("siren-forever")) {
            BikeSiren.onForever(mBle.mService);
        } else {
            ttsobj.speak(intentText, TextToSpeech.QUEUE_FLUSH, null);
        }
        Utils.postReqThread(Utils.METEOR_URL + "/tts-received");
    }

    public void setAlertStatus(AlertStatus a) {
        alertStatus = a;
        if (a != AlertStatus.UNTRIGGERED) {
            gpsMonitor.gpsOn();
        } else {
            gpsMonitor.gpsOff();
            movementDetector.reset();
            BikeLEDLights.turnOff(mBle.mService);
            excessiveAlertStatusView.setText("Need first bump");
        }
        updateAlertStatusView();
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
    public void unsetMiniAlert() {
        setAlertStatus(AlertStatus.UNTRIGGERED);
    }

    @Override
    public void sendMiniAlert() {
        if (alertStatus == AlertStatus.UNTRIGGERED) {
            setAlertStatus(AlertStatus.MINI);
            Log.d(TAG, "Sending mini alert.");
            if (isProduction) {
                Date date = new Date();
                ttsobj.speak("Welcome to the smart bike. If you'd like this moved, please call the number on the handlebars.", TextToSpeech.QUEUE_FLUSH, null);
                BikeLEDLights.turnOn(mBle.mService);
                pbullet.send("MiniAlert: Phone moved once.", "At " + date.toString());
            } else {
                v.vibrate(50);
            }
        }
    }

    @Override
    public void sendExcessiveAlert() {
        if (AlertStatus.MINI.compareTo(alertStatus) >= 0) {
            excessiveAlertStatusView.setText("Triggered!");
            setAlertStatus(AlertStatus.EXCESSIVE);
            Log.d(TAG, "sendExcessiveAlert.");
            if (isProduction) {
                mMeteor.alarmTrigger();
                Date date = new Date();
                BikeSiren.onShort(mBle.mService);
                ttsobj.speak("This doesn't need a thick lock because it's GPS tracked and constantly monitored", TextToSpeech.QUEUE_FLUSH, null);
                pbullet.send("Phone moved LOTS!", "At " + date.toString());
                SmsManager.getDefault().sendTextMessage("+16506447811", null, "Phone moved LOTS -- " + date.toString(), null, null);
            } else {
                v.vibrate(500);
            }
        }
    }

    public void sendChainAlert() {
        if (AlertStatus.EXCESSIVE.compareTo(alertStatus) >= 0) {
            excessiveAlertStatusView.setText("Chain Cut!");
            setAlertStatus(AlertStatus.CHAIN_CUT);
            Log.d(TAG, "sendChainAlert.");
            if (isProduction) {
                mMeteor.alarmTrigger();
                Date date = new Date();
                ttsobj.speak("The lock has been cut. Palo Alto authorities notified.", TextToSpeech.QUEUE_FLUSH, null);
                pbullet.send("Chain cut!", "At " + date.toString());
                SmsManager.getDefault().sendTextMessage("+16506447811", null, "Chain cut! -- " + date.toString(), null, null);
            } else {
                v.vibrate(500);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DESTROY!");
        mWakeLock.release();
        mMeteor.mMeteor.disconnect();
        sensorManager.unregisterListener(this);
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

    private void startExcessiveAlertStatusUpdate() {
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
                                if (Alertable.AlertStatus.MINI == alertStatus) {
                                    long timeLeftToAlert = movementDetector.timeLeftToAlertIfAdded(System.currentTimeMillis());
                                    if (timeLeftToAlert > 0) {
                                        excessiveAlertStatusView.setText("Second bump trigger until " + timeLeftToAlert);
                                    } else if (timeLeftToAlert == -999) {
                                        excessiveAlertStatusView.setText("Need first bump");
                                        unsetMiniAlert();
                                    } else {
                                        excessiveAlertStatusView.setText("Require second bump in " + timeLeftToAlert);
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "TODO something bad here, not sure what to do");
                    }
                }
            }
        }).start();
    }

    private void updateAlertStatusView() {
        switch (alertStatus) {
            case UNTRIGGERED:
                alertStatusView.setText("Untriggered");
                break;
            case MINI:
                alertStatusView.setText("Small amount of movement");
                break;
            case EXCESSIVE:
                alertStatusView.setText("Excessive movement");
                break;
            case CHAIN_CUT:
                alertStatusView.setText("Chain cut!");
                break;
        }
    }
}