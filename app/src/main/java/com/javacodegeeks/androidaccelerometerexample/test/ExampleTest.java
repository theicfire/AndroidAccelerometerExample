package com.javacodegeeks.androidaccelerometerexample.test;

import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityUnitTestCase;
import android.test.InstrumentationTestCase;
import android.widget.TextView;

import com.javacodegeeks.androidaccelerometerexample.AndroidAccelerometerExample;
import com.javacodegeeks.androidaccelerometerexample.R;

import java.util.LinkedList;

/**
 * Created by chase on 3/3/15.
 */
//public class ExampleTest extends InstrumentationTestCase {
//    public void test() throws Exception {
//        final int expected = 1;
//        final int reality = 5;
//        assertEquals(expected, reality);
//    }
//}

public class ExampleTest
        extends ActivityInstrumentationTestCase2<AndroidAccelerometerExample> {

    private AndroidAccelerometerExample activity;
    private static final int SECONDS_TO_MILLIS = 1000;

    public ExampleTest() {
        super(AndroidAccelerometerExample.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();

    }

    public void testPreconditions() {
        assertNotNull("activity is null", activity);
        assertEquals(0, activity.last_notify);
    }

    public void testShouldNotify_wait2Minutes() {
        activity.movementTimesQueue = new LinkedList<Long>();
        assertEquals(false, activity.shouldNotify(1000000 + 0));
        assertEquals(0, activity.last_notify);

        assertEquals(true, activity.shouldNotify(1000000 + 10 * SECONDS_TO_MILLIS));
        // Nothing until after 2 minutes
        assertEquals(false, activity.shouldNotify(1000000 + 11 * SECONDS_TO_MILLIS));
        assertEquals(false, activity.shouldNotify(1000000 + 300 * SECONDS_TO_MILLIS));
        assertEquals(false, activity.shouldNotify(1000000 + 301 * SECONDS_TO_MILLIS));
        assertEquals(1000000 + 10 * SECONDS_TO_MILLIS, activity.last_notify);
        assertEquals(true, activity.shouldNotify(1000000 + 306 * SECONDS_TO_MILLIS));
        assertEquals(1000000 + 306 * SECONDS_TO_MILLIS, activity.last_notify);
    }

    public void testShouldNotify_tooLongBetweenEvents() {
        activity.movementTimesQueue = new LinkedList<Long>();
        assertEquals(false, activity.shouldNotify(1000000 + 0));
        assertEquals(0, activity.last_notify);

        assertEquals(false, activity.shouldNotify(1000000 + 100 * SECONDS_TO_MILLIS));
    }
}
