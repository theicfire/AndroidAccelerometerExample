package com.javacodegeeks.androidaccelerometerexample.detector;

/**
 * Created by chase on 3/25/15.
 */
public interface Alertable {
    void sendMiniAlert();
    void unsetMiniAlert();
    void sendExcessiveAlert();
}
