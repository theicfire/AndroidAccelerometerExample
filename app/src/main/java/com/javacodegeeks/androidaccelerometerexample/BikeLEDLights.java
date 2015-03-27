package com.javacodegeeks.androidaccelerometerexample;

import android.util.Log;

import com.javacodegeeks.androidaccelerometerexample.ble.UartService;

import java.io.UnsupportedEncodingException;

/**
 * Created by chase on 3/25/15.
 *
 */
public class BikeLEDLights {
    private final static String TAG = BikeLEDLights.class.getSimpleName();

    public static void turnOn(UartService mService) {
        if (mService != null) {
            try {
                mService.writeRXCharacteristic("lon".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Bluetooth not on");
        }
    }

    public static void turnOff(UartService mService) {
        if (mService != null) {
            try {
                mService.writeRXCharacteristic("loff".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Bluetooth not on");
        }
    }

    public static void turnOffCallback() {
        Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/lightsOn/false");
    }

    public static void turnOnCallback() {
        Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/lightsOn/true");
    }
}
