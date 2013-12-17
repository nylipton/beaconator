

package com.example.bluetoothanalyzer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;

//import com.paywithisis.otapiccapp.LocationHandler;
//import com.paywithisis.otapiccapp.MainLog;

/**
 * This class scans Bluetooth LE, Wi-Fi and Cellular radios for nearby transmitters and reports the
 * received signal strength indicator of the transmitters.
 * <p>
 * To use this class, implement the RadioScanner.ScanCallback interface then create a new instances
 * of RadioScanner with the application context and a reference to your ScanCallback implementation.
 * Call startup() to initialize the RadioScanner, e.g. from your Activity's onCreate method, and
 * make sure you call shutdown() from your Activity's onDestroy method.
 * <p>
 * If you fail to call shutdown() before your application exits, you may leave the Bluetooth LE
 * radio in an inconsistent state (thanks to poor coding by Google).
 * <p>
 * Cellular scanning begins automatically after the call to startup(). To start or stop Bluetooth LE
 * or Wi-Fi scanning, call the respective start or stop methods.
 * <p>
 * This class will automatically enable the Bluetooth radio if it is turned off. However, enabling
 * Wi-Fi or the cellular radio is up to the caller or the end user.
 * <p>
 * This class requires the following permissions in the application manifest:
 * {@code <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    }
 * 
 * @author Kai Johnson
 * 
 */
public class KaiRadioScanner {

    // Constants for logging
//    private static final String TAG = RadioScanner.class.getSimpleName();
//    private static final MainLog mLog = MainLog.INSTANCE;

    /**
     * Constant protocol name for Bluetooth LE
     */
    public static final String PROTOCOL_BLUETOOTH_LE = "Bluetooth LE";

    /**
     * Constant protocol name for Wi-Fi
     */
    public static final String PROTOCOL_WIFI = "Wi-Fi";

    /**
     * Constant protocol name for Cellular radio, including CDMA, GSM, LTE and WCDMA
     */
    public static final String PROTOCOL_CELLULAR = "Cellular";

    /**
     * Ideal scan interval for repeated asynchronous scans
     */
    private static final long SCAN_INTERVAL = 1000;

    /**
     * The application context in which the scanner is running. Ideally this should be an Activity.
     */
    private final Context mContext;

    /**
     * The callback object registered by the creator of the RadioScanner. The RadioScanner calls
     * onScan whenever it gets RSSI data for a nearby transmitter.
     */
    private final ScanCallback mCallback;

    /**
     * Reference to the system BluetoothManager
     */
    private BluetoothManager mBluetoothManager = null;

    /**
     * Reference to the system BluetoothAdapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * True if the RadioScanner turned on the Bluetooth adapter during startup(). If true, the
     * RadioScanner will turn off the Bluetooth adapter when shutdown() is called.
     */
    private boolean mDisableBluetoothOnShutdown = false;

    /**
     * Callback for results from Bluetooth LE scans
     */
    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new LeScanCallback();

    /**
     * Reference to the system WifiManager.
     */
    private WifiManager mWifiManager = null;

    /**
     * BroadcastListener for results from Wi-Fi scans.
     */
    private final WifiScanListener mWifiScanListener = new WifiScanListener();

    /**
     * Reference to the system TelephonyManager
     */
    private TelephonyManager mTelephonyManager = null;

    /**
     * Convenience object to interface with Google Play Services LocationClient.
     */
    private KaiLocationHandler mLocationHandler = null;

    /**
     * Thread-safe array of devices found by scans.
     */
    private final CopyOnWriteArrayList<RadioDevice> mDevices = new CopyOnWriteArrayList<RadioDevice>();

    /**
     * Background thread for asynchronous scanning.
     */
    private final Thread mAsyncScanThread = new Thread(new AsyncScan());

    /**
     * The RadioScanner.ScanCallback interface provides a single method that the RadioScanner will
     * call when it gets RSSI data about a nearby transmitter. This method may be called from a
     * thread outside of the main UI thread.
     * 
     * @author Kai Johnson
     * 
     */
    public interface ScanCallback {
        /**
         * This method is called whenever the RadioScanner gets RSSI data about a nearby
         * transmitter.
         * 
         * @param protocol
         *            Human-readable string identifying the protocol for the nearby transmitter.
         * @param address
         *            Network address of the nearby transmitter.
         * @param rssi
         *            RSSI of the nearby transmitter.
         * @param location
         *            Location fix of this device when the RSSI was observed. This parameter may be
         *            null if a location fix is not available.
         */
        abstract void onScan(String protocol, String address, int rssi, Location location);
    }

    /**
     * Create a new RadioScanner object
     * 
     * @param context
     *            the application context for the RadioScanner, ideally an Activity.
     * @param callback
     *            the callback object for results from scans.
     */
    public KaiRadioScanner(Context context, ScanCallback callback) {
        mContext = context;
        mCallback = callback;
    }

    /**
     * Initialize the RadioScanner. This will also initiate scanning of nearby cells on the cellular
     * network. Call this method before calling any other method of the RadioScanner.
     */
    public void startup() {
//        mLog.v(TAG, "startup");

        // Get a reference to the BluetoothManager
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);

        if (mBluetoothManager != null) {
            // Get a reference to the (default) BluetoothAdapter
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }

        // If Bluetooth is not enabled, turn it on
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            mDisableBluetoothOnShutdown = true;
        }

        // If the Bluetooth adapter is currently in discovery mode, stop discovery
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Get a reference to the WifiManager
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        // Get a reference to the TelephonyManager
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        // Set up a handler for the Google Play Services LocationClient
        mLocationHandler = new KaiLocationHandler((Activity) mContext);

        // Start scanning the cellular network, and be prepared to scan nearby Bluetooth LE devices
        startAsyncScan();
    }

    /**
     * Stops all scanning, disconnect from all Bluetooth LE devices, clear any records of nearby
     * transmitters, and restore the state of the BluetoothAdapter. This method must be called
     * before the application exits to ensure that the Bluetooth stack is left in a consistent
     * state.
     */
    public void shutdown() {
//        mLog.v(TAG, "shutdown");

        stopAsyncScan();

        reset();

        // If we turned on Bluetooth in startup, shut it down
        if (mDisableBluetoothOnShutdown && mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();
        }
    }

    /**
     * Clear all records of nearby transmitters
     */
    private void reset() {
//        mLog.v(TAG, "reset");

        // Stop all scans
        stopBluetoothLeScan();
        stopWifiScan();

        // Clear all identified devices
        mDevices.clear();
    }

    /**
     * Start scanning for Bluetooth LE devices.
     */
    public void startBluetoothLeScan() {
//        mLog.v(TAG, "startBluetoothLeScan");

        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    /**
     * Stop scanning for Bluetooth LE devices.
     */
    public void stopBluetoothLeScan() {
//        mLog.v(TAG, "stopBluetoothLeScan");

        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    /**
     * Start scanning for Wi-Fi devices.
     */
    public void startWifiScan() {
        // http://groups.google.com/group/android-developers/browse_thread/thread/f722d5f90cfae69
        IntentFilter i = new IntentFilter();
        i.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mContext.registerReceiver(mWifiScanListener, i);

        mWifiManager.startScan();
    }

    /**
     * Stop scanning for Wi-Fi devices.
     */
    public void stopWifiScan() {
        mContext.unregisterReceiver(mWifiScanListener);
    }

    /**
     * Start a thread for background scanning operations, especially the periodic queries of
     * Bluetooth LE devices and periodic observations of the cellular network.
     */
    private void startAsyncScan() {
//        mLog.v(TAG, "startAsyncScan");

        mLocationHandler.start();
        if (mAsyncScanThread.isAlive()) {
            stopAsyncScan();
        }
        mAsyncScanThread.start();
    }

    /**
     * Stop the background scanning thread. This method may block the calling thread for up to
     * 100ms.
     */
    private void stopAsyncScan() {
//        mLog.v(TAG, "stopAsyncScan");

        mLocationHandler.stop();
        if (mAsyncScanThread.isAlive()) {
            mAsyncScanThread.interrupt();
            try {
                mAsyncScanThread.join(100);
            } catch (InterruptedException e) {
//                mLog.e(TAG, "Exception while waiting for thread interrupt: " + e.toString());
            }
        }
    }

    /**
     * Implementation of the BluetoothAdapter.LeScanCallback interface to receive calls to
     * onLeScan() when the BluetoothAdapter finds new Bluetooth LE devices. In response, this method
     * adds the device to its list of devices for periodic scanning.
     * 
     * @author Kai Johnson
     * 
     */
    private class LeScanCallback implements BluetoothAdapter.LeScanCallback {

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.bluetooth.BluetoothAdapter.LeScanCallback#onLeScan(android.bluetooth.BluetoothDevice
         * , int, byte[])
         */
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            mLog.v(TAG, "onLeScan");

            RadioDevice radioDevice = new RadioDevice();
            radioDevice.mProtocol = PROTOCOL_BLUETOOTH_LE;
            radioDevice.mAddress = device.getAddress();
            radioDevice.mLastRssi = rssi;
            radioDevice.mLastSeen = SystemClock.elapsedRealtime();
            // radioDevice.mDrift = 0;
            radioDevice.mGatt = null;

            if (!mDevices.contains(radioDevice)) {
                mDevices.add(radioDevice);
            }

            mCallback.onScan(radioDevice.mProtocol, radioDevice.mAddress, radioDevice.mLastRssi,
                    mLocationHandler.getLocation());
        }

    }

    /**
     * BroadcastReceiver to receive the intent that is broadcast when the WifiManager completes a
     * Wi-Fi scan. This method re-initiates the Wi-Fi scan, since a scan takes a little more than a
     * second.
     * 
     * @author Kai Johnson
     * 
     */
    private class WifiScanListener extends BroadcastReceiver {

        /*
         * (non-Javadoc)
         * 
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = mWifiManager.getScanResults();

            for (ScanResult r : scanResults) {
                mCallback.onScan(PROTOCOL_WIFI, r.BSSID, r.level, mLocationHandler.getLocation());
            }

            mWifiManager.startScan();
        }
    }

    /**
     * Record of a nearby transmitter device that has been found by one of the scanning methods.
     * Right now, this is just used to track Bluetooth LE devices that need to be periodically
     * scanned.
     * 
     * @author Kai Johnson
     * 
     */
    private class RadioDevice {
        String mProtocol;
        String mAddress;
        int mLastRssi;
        long mLastSeen;
        // long mDrift;
        BluetoothGatt mGatt;

        /*
         * Implementation of the equals() method to compare RadioDevices using only the mProtocol
         * and mAddress fields and ignoring other fields.
         * 
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object o) {
            // Return true if the objects are identical.
            // (This is just an optimization, not required for correctness.)
            if (this == o) {
                return true;
            }

            // Return false if the other object has the wrong type.
            // This type may be an interface depending on the interface's specification.
            if (!(o instanceof RadioDevice)) {
                return false;
            }

            // Cast to the appropriate type.
            // This will succeed because of the instanceof, and lets us access private fields.
            RadioDevice d = (RadioDevice) o;

            // Check each field. Primitive fields, reference fields, and nullable reference
            // fields are all treated differently.
            return mProtocol.equals(d.mProtocol) && mAddress.equals(d.mAddress);
        }

        /*
         * Implementation of the hashCode() method to match the implementation of equals().
         * 
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            // Start with a non-zero constant.
            int result = 17;

            // Include a hash for each field.
            result = 31 * result + mProtocol.hashCode();

            result = 31 * result + mAddress.hashCode();

            return result;
        }

    }

    /**
     * Background scanning thread. This thread will connect to Bluetooth LE devices and periodically
     * examine the RSSI of the connections. It will also periodically collect information on nearby
     * cells on the cellular network.
     * 
     * @author Kai Johnson
     * 
     */
    private class AsyncScan implements Runnable {

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
//            mLog.v(TAG, "run");

            // Try block to catch interrupted exceptions
            try {
                // While the thread has not been interrupted
                do {
                    /*
                     * Period for the next sleep cycle. This is at most SCAN_INTERVAL milliseconds,
                     * but may be less if a Bluetooth LE device is due to be rescanned sooner than
                     * that.
                     */
                    long sleepTime = SCAN_INTERVAL;

                    // Current time for this scan run
                    long currentTime = SystemClock.elapsedRealtime();

                    // Go through all the discovered Bluetooth LE devices
                    for (RadioDevice d : mDevices) {
                        // If it's time to scan this device again
                        if (currentTime - d.mLastSeen > SCAN_INTERVAL) {
                            // If we haven't connected to this device before
                            if (d.mGatt == null) {
                                /*
                                 * Connect to the device so we can read RSSI. After the connection
                                 * is established, the callback will read RSSI
                                 */
                                GattCallback callback = new GattCallback(d);

                                d.mGatt = mBluetoothAdapter.getRemoteDevice(d.mAddress)
                                        .connectGatt(mContext, false, callback);
                            } else {
                                // Just read RSSI
                                d.mGatt.readRemoteRssi();
                            }
                        } else {
                            /*
                             * It's not time to scan this device yet. Figure out how long we need to
                             * sleep to get back to this device in about a second.
                             */
                            long deviceSleep = SCAN_INTERVAL - (currentTime - d.mLastSeen);
                            if (deviceSleep < sleepTime) {
                                sleepTime = deviceSleep;
                            }
                        }
                    }

                    // If we have a TelephonyManager to work with, get info on nearby cells
                    if (mTelephonyManager != null) {

                        /*
                         * Try using getAllCellInfo() this may not return any results on some
                         * phones.
                         */
                        List<CellInfo> allCellInfo = mTelephonyManager.getAllCellInfo();

                        // If we got some results
                        if (allCellInfo != null) {
                            // Step through each of the CellInfo objects
                            for (CellInfo c : allCellInfo) {
                                /*
                                 * Report the cell address and RSSI using the callback. Too bad
                                 * CellInfo didn't provide the interface for all these methods that
                                 * the subclasses have in common ...
                                 */
                                if (c instanceof CellInfoCdma) {
                                    CellInfoCdma cdma = (CellInfoCdma) c;
                                    mCallback.onScan(PROTOCOL_CELLULAR, cdma.getCellIdentity()
                                            .toString(), cdma.getCellSignalStrength().getDbm(),
                                            mLocationHandler.getLocation());
                                } else if (c instanceof CellInfoGsm) {
                                    CellInfoGsm gsm = (CellInfoGsm) c;
                                    mCallback.onScan(PROTOCOL_CELLULAR, gsm.getCellIdentity()
                                            .toString(), gsm.getCellSignalStrength().getDbm(),
                                            mLocationHandler.getLocation());
                                } else if (c instanceof CellInfoLte) {
                                    CellInfoLte lte = (CellInfoLte) c;
                                    mCallback.onScan(PROTOCOL_CELLULAR, lte.getCellIdentity()
                                            .toString(), lte.getCellSignalStrength().getDbm(),
                                            mLocationHandler.getLocation());
                                } else if (c instanceof CellInfoWcdma) {
                                    CellInfoWcdma wcdma = (CellInfoWcdma) c;
                                    mCallback.onScan(PROTOCOL_CELLULAR, wcdma.getCellIdentity()
                                            .toString(), wcdma.getCellSignalStrength().getDbm(),
                                            mLocationHandler.getLocation());
                                } else {
//                                    mLog.e(TAG, "Unknown CellInfo type: " + c.toString());
                                }
                            }
                        }

                        /*
                         * Since we can't count on getAllCellInfo, we need to try getting
                         * NeighboringCellInfo too. This also doesn't work on some phones, but it's
                         * the best we can do.
                         */
                        List<NeighboringCellInfo> neighboringCellInfo = mTelephonyManager
                                .getNeighboringCellInfo();

                        // If we got some results
                        if (neighboringCellInfo != null) {
                            // Report the cell address and RSSI using the callback.
                            for (NeighboringCellInfo n : neighboringCellInfo) {
                                mCallback.onScan(PROTOCOL_CELLULAR, n.toString(), n.getRssi(),
                                        mLocationHandler.getLocation());
                            }
                        }
                    }

                    // Sleep for a little while, probably less than a second
                    Thread.sleep(sleepTime);
                } while (!Thread.interrupted());
            } catch (InterruptedException e) {
                /*
                 * We'll catch this exception if stopAsyncScan is called while we're sleeping. If
                 * somehow it was called while we were processing, we'd just fall out of the
                 * do-while loop.
                 */
//                mLog.d(TAG, "Sleep interrupted.");
            }

            /*
             * The thread is exiting, so we need to disconnect from all the Bluetooth LE devices.
             * It's really important that we do this, because exiting the application will leave the
             * Bluetooth LE connections open and (sadly!) orphaned. Since we can only discover
             * unconnected Bluetooth LE devices, the next scan will never see the orphaned connected
             * devices.
             */
            for (RadioDevice d : mDevices) {
                d.mGatt.disconnect();
            }
        }
    }

    /**
     * Implementation of BluetoothGattCallback to read the RSSI of connected Bluetooth LE devices.
     * Note that there is a separate GattCallback object for each Bluetooth LE connection.
     * 
     * @author Kai Johnson
     * 
     */
    private class GattCallback extends BluetoothGattCallback {

        RadioDevice mRadioDevice;

        /**
         * Create the GattCallback object with a reference to the associated RadioDevice.
         * 
         * @param d
         */
        public GattCallback(RadioDevice d) {
            mRadioDevice = d;
        }

        /*
         * Handle new connections to Bluetooth LE devices, followed by reading the RSSI.
         * 
         * (non-Javadoc)
         * 
         * @see android.bluetooth.BluetoothGattCallback#onConnectionStateChange(android.bluetooth.
         * BluetoothGatt, int, int)
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            mLog.v(TAG, "onConnectionStateChange");

            /*
             * There's some general weirdness in the BluetoothGatt implementation, and it returns
             * status codes that have not been defined in the published documentation. For now, we
             * just log them and ignore them.
             */
            if (status != BluetoothGatt.GATT_SUCCESS) {
//                mLog.e(TAG, "onConnectionStateChange status: " + status);
            }

            // Sanity check to make sure this callback is paired with the proper RadioDevice
            if (!mRadioDevice.mAddress.equals(gatt.getDevice().getAddress())) {
//                mLog.e(TAG,
//                        "onConnectionStateChange - GATT address does not match expected device address.");
            }

            switch (newState) {
            case BluetoothProfile.STATE_DISCONNECTED:
                break;
            case BluetoothProfile.STATE_CONNECTING:
                break;
            case BluetoothProfile.STATE_CONNECTED:
                /*
                 * When a new connection is established, start reading the RSSI.
                 */
                gatt.readRemoteRssi();
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                break;
            default:
//                mLog.e(TAG, "Unknown connection state: " + newState);
                break;
            }
        }

        /*
         * Report the RSSI for the Bluetooth LE device using the callback.
         * 
         * (non-Javadoc)
         * 
         * @see
         * android.bluetooth.BluetoothGattCallback#onReadRemoteRssi(android.bluetooth.BluetoothGatt,
         * int, int)
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//            mLog.v(TAG, "onReadRemoteRssi");

            /*
             * There's some general weirdness in the BluetoothGatt implementation, and it returns
             * status codes that have not been defined in the published documentation. For now, we
             * just log them and ignore them.
             */
            if (status != BluetoothGatt.GATT_SUCCESS) {
//                mLog.e(TAG, "onReadRemoteRssi status: " + status);
            }

            // Sanity check to make sure this callback is paired with the proper RadioDevice
            if (!mRadioDevice.mAddress.equals(gatt.getDevice().getAddress())) {
//                mLog.e(TAG,
//                        "onReadRemoteRssi - GATT address does not match expected device address.");
            }

            // Update the RadioDevice fields
            mRadioDevice.mLastSeen = SystemClock.elapsedRealtime();
            mRadioDevice.mLastRssi = rssi;

            // Report the Bluetooth LE device address and RSSI using the callback.
            mCallback.onScan(PROTOCOL_BLUETOOTH_LE, mRadioDevice.mAddress, rssi,
                    mLocationHandler.getLocation());
            
        }
    }
}