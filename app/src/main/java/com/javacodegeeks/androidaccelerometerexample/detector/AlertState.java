package com.javacodegeeks.androidaccelerometerexample.detector;

import android.app.Activity;
import android.telephony.SmsManager;
import android.util.Log;

import com.javacodegeeks.androidaccelerometerexample.GpsMonitor;
import com.javacodegeeks.androidaccelerometerexample.MainActivity;
import com.javacodegeeks.androidaccelerometerexample.Utils;

import java.util.Date;

public class AlertState implements Alertable {
    private final static String TAG = MainActivity.class.getSimpleName();

    private long stateTransitionTime = 0;

    private static final int FIRST_TO_SECOND_ALERT_MIN_MS = 2 * 1000;
    private static final int FIRST_TO_SECOND_ALERT_MAX_MS = 25 * 1000;
    private static final int SECOND_TO_EXCESSIVE_ALERT_MIN_MS = 15 * 1000;
    private static final int SECOND_TO_EXCESSIVE_ALERT_MAX_MS = 180 * 1000;

    private final String CHASE_PHONE_NUMBER = "+15125778778";
    private final String OLI_PHONE_NUMBER = "+16506447811";
    private AlertStatus alertStatus;

    private MainActivity activity;

    public AlertState(MainActivity activity) {
        alertStatus = AlertStatus.UNTRIGGERED;
        this.activity = activity;
    }

    public void setAlertStatus(AlertStatus a) {
        stateTransitionTime = System.currentTimeMillis();
        activity.alertStatusView.setText(a.name());
        alertStatus = a;
        if (a != AlertStatus.UNTRIGGERED) {
            activity.gpsMonitor.gpsOn();
        } else {
            activity.gpsMonitor.gpsOff();
            activity.movementDetector.accelQueueDetector.clear();
        }
    }

    public long timeTillReset() {
        switch (getAlertStatus()) {
            case FIRST_ALERT:
                return stateTransitionTime + FIRST_TO_SECOND_ALERT_MAX_MS - System.currentTimeMillis();
            case SECOND_ALERT:
                return stateTransitionTime + SECOND_TO_EXCESSIVE_ALERT_MAX_MS - System.currentTimeMillis();
            default: // Excessive
                return -1;
        }
    }

    @Override
    public AlertStatus getAlertStatus() {
        return alertStatus;
    }

    @Override
    public void noMoveAlert() {
        activity.toneGenerator.stop();
        if ((Alertable.AlertStatus.FIRST_ALERT == getAlertStatus() &&
                stateTransitionTime + FIRST_TO_SECOND_ALERT_MAX_MS < System.currentTimeMillis())
         || (Alertable.AlertStatus.SECOND_ALERT == getAlertStatus() &&
                stateTransitionTime + SECOND_TO_EXCESSIVE_ALERT_MAX_MS < System.currentTimeMillis())) {
            resetAlertStatus();
        }
    }

    @Override
    public void moveAlert() {
        if (activity.autoSiren) {
            Log.d(TAG, "play!");
            activity.toneGenerator.play();
        }
        if (Alertable.AlertStatus.UNTRIGGERED == getAlertStatus()) {
            firstMoveAlert();
        } else if (Alertable.AlertStatus.FIRST_ALERT == getAlertStatus()) {
            if (stateTransitionTime + FIRST_TO_SECOND_ALERT_MIN_MS < System.currentTimeMillis()) {
                secondMoveAlert();
            }
        } else if (Alertable.AlertStatus.SECOND_ALERT == getAlertStatus()) {
            if (stateTransitionTime + SECOND_TO_EXCESSIVE_ALERT_MIN_MS < System.currentTimeMillis()) {
                excessiveMoveAlert();
            }
        }
    }

    @Override
    public void resetAlertStatus() {
        setAlertStatus(AlertStatus.UNTRIGGERED);
        activity.movementDetector.setSensitivity((float) 1.0);
        activity.autoSiren = false;
        Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/autoSirenOn/false");
    }

    @Override
    public void firstMoveAlert() {
        setAlertStatus(AlertStatus.FIRST_ALERT);
        Log.d(TAG, "firstMoveAlert");
        if (activity.isProduction) {
            activity.pbullet.send("firstMoveAlert: Phone moved once.", "At " + (new Date()).toString());
        } else {
            activity.v.vibrate(50);
        }
    }

    @Override
    public void secondMoveAlert() {
        setAlertStatus(AlertStatus.SECOND_ALERT);
        Log.d(TAG, "secondMoveAlert.");
        activity.movementDetector.setHighSensitivity();
        activity.autoSiren = true;
        Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/autoSirenOn/true");

        if (activity.isProduction) {
            Date date = new Date();
            activity.pbullet.send("secondMoveAlert: Phone moved second time.", "At " + date.toString());
        } else {
            activity.v.vibrate(500);
        }
    }

    @Override
    public void excessiveMoveAlert() {
        setAlertStatus(AlertStatus.EXCESSIVE_ALERT);
        Log.d(TAG, "excessiveMoveAlert.");
        if (activity.isProduction) {
            activity.mMeteor.alarmTrigger();
            activity.pbullet.send("Phone moved LOTS!", "At " + (new Date()).toString());
            SmsManager.getDefault().sendTextMessage(CHASE_PHONE_NUMBER, null, "Phone moved LOTS -- " + (new Date()).toString(), null, null);
            SmsManager.getDefault().sendTextMessage(OLI_PHONE_NUMBER, null, "Phone moved LOTS -- " + (new Date()).toString(), null, null);
        } else {
            activity.v.vibrate(500);
        }
    }
}
