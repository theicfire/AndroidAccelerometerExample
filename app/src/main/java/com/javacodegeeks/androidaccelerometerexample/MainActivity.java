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
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.javacodegeeks.androidaccelerometerexample.detector.AlertState;
import com.javacodegeeks.androidaccelerometerexample.detector.Alertable;
import com.javacodegeeks.androidaccelerometerexample.detector.MovementDetector;
import com.javacodegeeks.androidaccelerometerexample.push.PushNotifications;

import java.util.Locale;


public class MainActivity extends Activity implements SensorEventListener, TextToSpeech.OnInitListener {
    private final static String TAG = MainActivity.class.getSimpleName();
    private SensorManager sensorManager;
    public Vibrator v;
    private TextToSpeech ttsobj;
    public MeteorConnection mMeteor;
    public GpsMonitor gpsMonitor;
    public boolean isProduction = false;
    public PBullet pbullet;
    private PowerManager.WakeLock mWakeLock;
    public MovementDetector movementDetector;
    public TextView countView, alertStatusView;
    private int count;
    public AlertState alertState;

    private TextView excessiveAlertStatusView;
    private Handler mHandler;
    public ToneGenerator toneGenerator;
    private Button coolButton;
    public boolean autoSiren = false;

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

        alertState = new AlertState(this);
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
        countView = (TextView) findViewById(R.id.count);
        alertStatusView = (TextView) findViewById(R.id.alertStatus);
        excessiveAlertStatusView = (TextView) findViewById(R.id.excessiveAlertStatus);
        movementDetector = new MovementDetector(alertState);
        startExcessiveAlertStatusUpdate();
        Utils.postReqThread(Utils.METEOR_URL + "/phonestart");

        toneGenerator = new ToneGenerator();
        coolButton=(Button) this.findViewById(R.id.btn_select);
        coolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "clicked");
            }
        });

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
            alertState.resetAlertStatus();
        } else if (intentText.equals("auto-siren-on")) {
            autoSiren = true;
            Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/autoSirenOn/true");
        } else if (intentText.equals("auto-siren-off")) {
            autoSiren = false;
            Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/autoSirenOn/false");
        } else if (intentText.toLowerCase().startsWith("sensitivity")) {
            float sensitivity = Float.parseFloat(intentText.substring("sensitivity".length()));
            movementDetector.setSensitivity(sensitivity);
        } else {
//            ttsobj.speak(intentText, TextToSpeech.QUEUE_FLUSH, null);
        }
        Utils.postReqThread(Utils.METEOR_URL + "/tts-received"); // TODO + "/" + intentText .. read that in meteor
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
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DESTROY!");
        mWakeLock.release();
        mMeteor.mMeteor.disconnect();
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                                if (alertState.getAlertStatus() == Alertable.AlertStatus.UNTRIGGERED) {
                                    excessiveAlertStatusView.setText("Already untriggered");
                                } else if (alertState.getAlertStatus() == Alertable.AlertStatus.EXCESSIVE_ALERT) {
                                    excessiveAlertStatusView.setText("Manual reset needed in EXCESSIVE_ALERT");
                                } else {
                                    excessiveAlertStatusView.setText("" + alertState.timeTillReset());
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
}
