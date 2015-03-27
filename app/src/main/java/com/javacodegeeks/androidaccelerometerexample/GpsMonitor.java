package com.javacodegeeks.androidaccelerometerexample;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class GpsMonitor implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final static String TAG = GpsMonitor.class.getSimpleName();


    public GoogleApiClient mGoogleApiClient;

    public GpsMonitor(Activity activity) {
        Log.d(TAG, "making location monitor");
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "connected brah");

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location received: " + location.toString());
        Utils.postReqThread("http://oligps.meteor.com/add_coords/" + location.getLatitude() + "/" + location.getLongitude() + "/" + System.currentTimeMillis());
    }

    public void gpsOn() {
        mGoogleApiClient.connect();
        Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/gpsOn/true");
    }
    public void gpsOff() {
        mGoogleApiClient.disconnect();
        Utils.postReqThread(Utils.METEOR_URL + "/setGlobalState/gpsOn/false");
    }

}
