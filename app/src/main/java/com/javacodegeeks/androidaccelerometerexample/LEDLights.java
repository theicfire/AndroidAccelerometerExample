package com.javacodegeeks.androidaccelerometerexample;

import com.javacodegeeks.androidaccelerometerexample.ble.UartService;

import java.io.UnsupportedEncodingException;

/**
 * Created by chase on 3/25/15.
 */
public class LEDLights {
    public static void turnOn( UartService mService) {
        try {
            mService.writeRXCharacteristic("lon".getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void turnOff(UartService mService) {
        try {
            mService.writeRXCharacteristic("loff".getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void turnOffCallback() {
        Utils.postReqTask("http://biker.chaselambda.com/lights/off");
    }

    public static void turnOnCallback() {
        Utils.postReqTask("http://biker.chaselambda.com/lights/on");
    }
}