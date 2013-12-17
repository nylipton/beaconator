package com.example.bluetoothanalyzer;


import android.app.Activity;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

/**
 * @author Kai Johnson
 * 
 */
public class KaiLocationHandler implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

//    private static final String TAG = LocationHandler.class.getSimpleName();
//    private static final MainLog mLog = MainLog.INSTANCE;

    private final LocationClient mLocationClient;

    private final Activity mActivity;

    private boolean connected;

    public KaiLocationHandler(Activity activity) {

        /*
         * Create a new location client, using the enclosing class to handle callbacks.
         */
        mLocationClient = new LocationClient(activity, this, this);

        mActivity = activity;

        connected = false;
    }

    public void start() {
//        mLog.v(TAG, "start()");
        // Connect the client.
        mLocationClient.connect();
    }

    public void stop() {
//        mLog.v(TAG, "stop()");
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
    }

    public Location getLocation() {
//        mLog.v(TAG, "getLocation()");
        if (connected) {
            return mLocationClient.getLastLocation();
        } else {
            return null;
        }
    }

    //TODO: Find out if getting periodic location updates will improve accuracy
    
    /*
     * Define a request code to send to Google Play services. This code is returned in
     * Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    public void onConnectionFailed(ConnectionResult result) {
//        mLog.v(TAG, "onConnectionFailed()");

        /*
         * Google Play services can resolve some errors it detects. If the error has a resolution,
         * try sending an Intent to start a Google Play services activity that can resolve error.
         */
        if (result.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                result.startResolutionForResult(mActivity, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                /*
                 * Thrown if Google Play services canceled the original PendingIntent
                 */
                // Log the error
//                mLog.e(TAG, e.toString());
            }
        } else {
            /*
             * If no resolution is available, log the error.
             */
//            mLog.e(TAG, "Google Play Services connection failed: " + result.getErrorCode());
        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {
//        mLog.v(TAG, "onConnected()");
        connected = true;
    }

    @Override
    public void onDisconnected() {
//        mLog.v(TAG, "onDisconnected()");
        connected = false;
    }
}

