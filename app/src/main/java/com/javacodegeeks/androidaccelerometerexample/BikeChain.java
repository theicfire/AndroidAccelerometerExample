package com.javacodegeeks.androidaccelerometerexample;

import com.javacodegeeks.androidaccelerometerexample.ble.UartService;

import java.io.UnsupportedEncodingException;

/**
 * Created by chase on 3/25/15.
 */
public class BikeChain {
    public static void turnOn(UartService mService) {
        try {
            mService.writeRXCharacteristic("chainon".getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void turnOnCallback() {
        Utils.postReqTask("http://biker.chaselambda.com/chain/on");
    }

    public static void turnOffCallback() {
        Utils.postReqTask("http://biker.chaselambda.com/chain/off");
    }

}
