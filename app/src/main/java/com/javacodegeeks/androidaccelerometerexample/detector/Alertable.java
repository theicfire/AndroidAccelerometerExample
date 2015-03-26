package com.javacodegeeks.androidaccelerometerexample.detector;

/**
 * Created by chase on 3/25/15.
 *
 */
public interface Alertable {
    public enum AlertStatus{
        UNTRIGGERED,
        MINI,
        EXCESSIVE,
        CHAIN_CUT
    };

    void sendMiniAlert();
    void unsetMiniAlert();
    void sendExcessiveAlert();
    AlertStatus getAlertStatus();
}
