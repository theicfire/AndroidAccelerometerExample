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

    void noMoveAlert();
    void moveAlert();
    void firstMoveAlert();
    void unsetFirstMoveAlert();
    void excessiveMoveAlert();
    AlertStatus getAlertStatus();
}
