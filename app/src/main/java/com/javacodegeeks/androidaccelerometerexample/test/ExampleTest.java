package com.javacodegeeks.androidaccelerometerexample.test;

import android.test.ActivityInstrumentationTestCase2;

import com.javacodegeeks.androidaccelerometerexample.MainActivity;

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
        extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity activity;
    private static final int SECONDS_TO_MILLIS = 1000;

    public ExampleTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();

    }

    public void testPreconditions() {
        assertNotNull("activity is null", activity);
    }

//    public void testShouldNotify_tooLongBetweenEvents() {
//        activity.movementTimesQueue = new LinkedList<Long>();
//        assertEquals(false, activity.shouldNotify(1000000 + 0));
//        assertEquals(0, activity.last_notify);
//
//        assertEquals(false, activity.shouldNotify(1000000 + 100 * SECONDS_TO_MILLIS));
//    }
}
