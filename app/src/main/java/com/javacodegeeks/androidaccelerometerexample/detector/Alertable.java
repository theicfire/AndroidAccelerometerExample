package com.javacodegeeks.androidaccelerometerexample.detector;

/**
 * Created by chase on 3/25/15.
 *
 */
public interface Alertable {
    public enum AlertStatus{
        UNTRIGGERED,
        MINI,
        EXCESSIVE
    };

    void noMoveAlert();
    void moveAlert();
    void firstMoveAlert();
    void excessiveMoveAlert();
    void resetAlertStatus();
    AlertStatus getAlertStatus();
}
