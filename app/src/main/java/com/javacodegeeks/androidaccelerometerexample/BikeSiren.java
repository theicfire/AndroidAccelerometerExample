package com.javacodegeeks.androidaccelerometerexample;

import android.util.Log;

import com.javacodegeeks.androidaccelerometerexample.ble.UartService;

import java.io.UnsupportedEncodingException;

/**
 * Created by chase on 3/26/15.
 */
public class BikeSiren {
    private final static String TAG = BikeSiren.class.getSimpleName();

    public static void onShort(UartService mService) {
        if (mService != null) {
            try {
                mService.writeRXCharacteristic("sirshort".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Bluetooth not on");
        }
    }

    public static void onMedium(UartService mService) {
        if (mService != null) {
            try {
                mService.writeRXCharacteristic("sirmedium".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Bluetooth not on");
        }
    }

    public static void onForever(UartService mService) {
        if (mService != null) {
            try {
                mService.writeRXCharacteristic("sirforever".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Bluetooth not on");
        }
    }
}
