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
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
    public MovementDetector movementDetector;
    private TextView countView, alertStatusView;
    private int count;
    private AlertStatus alertStatus;
    private TextView excessiveAlertStatusView;
    private Handler mHandler;
    private ToneGenerator toneGenerator;
    private Button btnConnectDisconnect;
    private boolean autoSiren = false;
    private final String PHONE_NUMBER = "+15125778778";

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
        countView = (TextView) findViewById(R.id.count);
        alertStatusView = (TextView) findViewById(R.id.alertStatus);
        excessiveAlertStatusView = (TextView) findViewById(R.id.excessiveAlertStatus);
        movementDetector = new MovementDetector(this);
        startExcessiveAlertStatusUpdate();
        Utils.postReqThread(Utils.METEOR_URL + "/phonestart");

        toneGenerator = new ToneGenerator();
        btnConnectDisconnect=(Button) this.findViewById(R.id.btn_select);
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toneGenerator.toggle();
            }
        });

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
            resetAlertStatus();
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

    public void setAlertStatus(AlertStatus a) {
        alertStatus = a;
        if (a != AlertStatus.UNTRIGGERED) {
            gpsMonitor.gpsOn();
        } else {
            gpsMonitor.gpsOff();
            movementDetector.reset();
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
    public void resetAlertStatus() {
        setAlertStatus(AlertStatus.UNTRIGGERED);
        movementDetector.setSensitivity((float) 1.0);
        autoSiren = false;
        Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/autoSirenOn/false");
    }

    @Override
    public void noMoveAlert() {
        toneGenerator.stop();
    }

    @Override
    public void moveAlert() {
        if (autoSiren) {
            Log.d(TAG, "play!");
            toneGenerator.play();
        }
    }

    @Override
    public void firstMoveAlert() {

        if (alertStatus == AlertStatus.UNTRIGGERED) {
            setAlertStatus(AlertStatus.MINI);
            Log.d(TAG, "Sending mini alert.");

            if (isProduction) {
                Date date = new Date();
                pbullet.send("MiniAlert: Phone moved once.", "At " + date.toString());
            } else {
                v.vibrate(50);
            }
        }
    }

    @Override
    public void excessiveMoveAlert() {
        if (AlertStatus.MINI.compareTo(alertStatus) >= 0) {
            excessiveAlertStatusView.setText("Triggered!");
            setAlertStatus(AlertStatus.EXCESSIVE);
            Log.d(TAG, "excessiveMoveAlert.");
            movementDetector.setHighSensitivity();
            autoSiren = true;
            Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/autoSirenOn/true");
            if (isProduction) {
                mMeteor.alarmTrigger();
                Date date = new Date();
                pbullet.send("Phone moved LOTS!", "At " + date.toString());
                SmsManager.getDefault().sendTextMessage(PHONE_NUMBER, null, "Phone moved LOTS -- " + date.toString(), null, null);
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
                                if (Alertable.AlertStatus.MINI == alertStatus) {
                                    long timeLeftToAlert = movementDetector.timeLeftToAlertIfAdded(System.currentTimeMillis());
                                    if (timeLeftToAlert > 0) {
                                        excessiveAlertStatusView.setText("Second bump trigger until " + timeLeftToAlert);
                                    } else if (timeLeftToAlert == -999) {
                                        excessiveAlertStatusView.setText("Need first bump");
                                        resetAlertStatus();
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
        }
    }
}
