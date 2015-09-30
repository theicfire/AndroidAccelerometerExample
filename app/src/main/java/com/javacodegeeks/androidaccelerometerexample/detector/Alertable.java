package com.javacodegeeks.androidaccelerometerexample.detector;

/**
 * Created by chase on 3/25/15.
 *
 */
public interface Alertable {
    public enum AlertStatus{
        UNTRIGGERED,
        FIRST_ALERT,
        SECOND_ALERT,
        EXCESSIVE_ALERT
    };

    void noMoveAlert();
    void moveAlert();
    void firstMoveAlert();
    void secondMoveAlert();
    void excessiveMoveAlert();
    void resetAlertStatus();
    AlertStatus getAlertStatus();
}
