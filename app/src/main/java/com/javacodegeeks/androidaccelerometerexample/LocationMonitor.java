package com.javacodegeeks.androidaccelerometerexample;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

public class LocationMonitor implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final String TAG = "MyAwesomeApp";

    public GoogleApiClient mGoogleApiClient;

    public LocationMonitor(Activity activity) {
        Log.d("mine", "making location monitor");
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("mine", "connected brah");

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
        Log.d("mine", "Location received: " + location.toString());
        new MyAsyncTask().execute(location);
    }

    private class MyAsyncTask extends AsyncTask<Location, Integer, String> {

        @Override
        protected String doInBackground(Location... params) {
            postData(params[0]);
            return null;
        }

        public void postData(Location loc) {
            HttpClient httpClient = new DefaultHttpClient();
            try {
                HttpPost request = new HttpPost("http://chasegps.meteor.com/add_coords/" + loc.getLatitude() + "/" + loc.getLongitude() + "/" + System.currentTimeMillis());
                httpClient.execute(request);
                Log.d("mine", "SUCCESS request");
            }catch (Exception ex) {
                Log.d("mine", "FAILED request");
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }
    }

    public void gpsOn() {
        mGoogleApiClient.connect();
    }
    public void gpsOff() {
        mGoogleApiClient.disconnect();
    }

}
