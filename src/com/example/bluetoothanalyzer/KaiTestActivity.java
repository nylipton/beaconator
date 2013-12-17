package com.example.bluetoothanalyzer;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;


public class KaiTestActivity extends Activity {

//    private static final String TAG = MainActivity.class.getSimpleName();
//    private static final MainLog mLog = MainLog.INSTANCE;

    // private BluetoothHandler mBluetoothHandler;
    private KaiRadioScanner mRadioScanner;

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // mBluetoothHandler = new BluetoothHandler(this);
        mRadioScanner = new KaiRadioScanner(this, mScanCallback);
        mRadioScanner.startup();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
//        mLog.v(TAG, "onResume()");

        super.onResume();

        // mBluetoothHandler.startAttributeScan();
        // mBluetoothHandler.startRssiScan();
        mRadioScanner.startBluetoothLeScan();
        mRadioScanner.startWifiScan();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause() {
//        mLog.v(TAG, "onPause()");

        // mBluetoothHandler.stopAttributeScan();
        // mBluetoothHandler.stopRssiScan();
        mRadioScanner.stopBluetoothLeScan();
        mRadioScanner.stopWifiScan();

        super.onPause();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
//        mLog.v(TAG, "onDestroy()");

        // mBluetoothHandler.destruct();
        mRadioScanner.shutdown();

        // Flush the log file so that we can look at what's in it
//        mLog.flush();

        super.onDestroy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onNewIntent(android.content.Intent)
     */
    @Override
    public void onNewIntent(Intent intent) {
//        mLog.v(TAG, "onNewIntent()");
//        mLog.d(TAG, "Intent: " + intent);
    }

    private final KaiRadioScanner.ScanCallback mScanCallback = new KaiRadioScanner.ScanCallback() {

        @Override
        public void onScan(String protocol, String address, int rssi, Location location) {
//            String loc = location == null ? "null" : location.toString();
//            mLog.i(TAG, "Protocol: " + protocol + " Address: " + address + " RSSI: " + rssi
//                    + " Location: " + loc);
        }
    };

}

