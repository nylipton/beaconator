package com.example.bluetoothanalyzer;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

public class OldMainActivity 
extends ListActivity
implements IBeaconConsumer
{
	public static final int REQUEST_ENABLE_BT = 1 ;
	private BluetoothManager bluetoothManager ;
	private BluetoothAdapter mBluetoothAdapter ;
	private BTListener btlsListener ;
	/** scheduler to run non-UI items (like BT scan) */
	private ExecutorService scheduler = Executors.newSingleThreadExecutor( ) ;
	private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication( this ) ;
	/** adapter of devices */
	private BluetoothDeviceAdapter deviceAdapter ;
	private ActionFoundReceiver actionFoundReceiver = new ActionFoundReceiver( ) ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if( BuildConfig.DEBUG ) 
			Log.i( Consts.LOG, "onCreate called: starting analyzer" ) ;
		deviceAdapter = new BluetoothDeviceAdapter( this, R.layout.list_mobile ) ;
		setListAdapter( deviceAdapter ) ;
		
//		setContentView( android.R.layout.simple_list_item_1 );
		//Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter( BluetoothDevice.ACTION_FOUND );
		filter.addAction( BluetoothDevice.ACTION_UUID );
		filter.addAction( BluetoothAdapter.ACTION_DISCOVERY_STARTED );
		filter.addAction( BluetoothAdapter.ACTION_DISCOVERY_FINISHED );
		registerReceiver( actionFoundReceiver, filter ); 
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter( ) ;
	    checkBTState( );
//		configureBTAdapter( );
		iBeaconManager.bind( this );
	}
	
	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		super.onActivityResult( requestCode, resultCode, data );
		if (requestCode == REQUEST_ENABLE_BT)
		      checkBTState();
	}
	
	private void checkBTState() {
	    // Check for Bluetooth support and then check to make sure it is turned on
	    // If it isn't request to turn it on
	    // List paired devices
	    // Emulator doesn't support Bluetooth and will return null
	    if(mBluetoothAdapter==null) { 
	      Log.i( Consts.LOG, "\nBluetooth NOT supported. Aborting.");
	      return;
	    } else {
	      if (mBluetoothAdapter.isEnabled()) {
	        Log.i(Consts.LOG, "\nBluetooth is enabled...");
	         
	        // Starting the device discovery
	        mBluetoothAdapter.startDiscovery();
	      } else {
	        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	      }
	    }
	  }
	
	private class ActionFoundReceiver 
	extends BroadcastReceiver
	{
	     
	    @Override
	    public void onReceive(Context context, Intent intent) 
	    {
	     String action = intent.getAction();
	     if(BluetoothDevice.ACTION_FOUND.equals(action)) 
	     {
	       BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	       Log.i(  Consts.LOG, "\n  Device: " + device.getName() + ", " + device);
//	       btDeviceList.add(device);
	     } else {
	       if(BluetoothDevice.ACTION_UUID.equals(action)) {
	         BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	         Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
	         if( uuidExtra != null )
	        	 for (int i=0; i<uuidExtra.length; i++) {
	        		 Log.i(Consts.LOG, "\n  Device: " + device.getName() + ", " + device + ", Service: " + uuidExtra[i].toString());
	         }
	       } else {
	         if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
	           Log.i(Consts.LOG, "\nDiscovery Started...");
	         } else {
	           if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
	             Log.i(Consts.LOG, "\nDiscovery Finished");
//	             Iterator<bluetoothdevice> itr = btDeviceList.iterator();
//	             while (itr.hasNext()) {
//	               // Get Services for paired devices
//	               BluetoothDevice device = itr.next();
//	               out.append("\nGetting Services for " + device.getName() + ", " + device);
//	               if(!device.fetchUuidsWithSdp()) {
//	                 out.append("\nSDP Failed for " + device.getName());
//	               }
//	                
//	             }
	           }
	         }
	       }
	      }
	    }
	  };

	/**
	 * Sets up the Bluetooth listener, manager, and adapter. Doesn't start scanning.
	 */
	private void configureBTAdapter()
	{
		btlsListener = new BTListener( ) ;

		scheduler.submit( new Runnable( )
		{
			public void run()
			{
				bluetoothManager = ( BluetoothManager ) getSystemService( Context.BLUETOOTH_SERVICE );
				if ( bluetoothManager != null )
				{
					mBluetoothAdapter = bluetoothManager.getAdapter( );
					if ( mBluetoothAdapter == null )
					{
						if ( BuildConfig.DEBUG )
							Log.i( Consts.LOG, "Couldn't get a bluetooth adapter" );
						Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
						startActivityForResult( enableBtIntent, OldMainActivity.REQUEST_ENABLE_BT );
					}
					else if ( !mBluetoothAdapter.isEnabled( ) )
					{
						// TODO ask the user if it's OK to enable bluetooth
						mBluetoothAdapter.enable( );
						if ( mBluetoothAdapter.isDiscovering( ) )
							mBluetoothAdapter.cancelDiscovery( );
					}
				}
				else if ( BuildConfig.DEBUG )
				{
					Log.i( Consts.LOG, "Couldn't get a bluetooth manager" );
				}
			}
		} ) ;
	}
	
	/**
	 * Will start scanning on the BT adapter 
	 * 
	 * @see	BTListener
	 */
	private void startScanning( )
	{	
		scheduler.submit( new Runnable( )
		{
			public void run()
			{
				if( mBluetoothAdapter != null )
				{
					if( BuildConfig.DEBUG ) 
						Log.i( Consts.LOG, "Starting to scan for bluetooth devices" ) ;
					
					OldMainActivity.this.runOnUiThread( new Runnable( )
					{
						public void run()
						{
							Toast.makeText( OldMainActivity.this, "Starting Bluetooth scan", Toast.LENGTH_SHORT ).show( ) ;
						}
					} );
					
					mBluetoothAdapter.startLeScan( btlsListener ) ;
				}
				else
				{
					//TODO make sure this is on the UI thread
					Log.w( Consts.LOG, "Sorry, this phone couldn't get a bluetooth adapter." ) ;
					OldMainActivity.this.runOnUiThread( new Runnable( )
					{
						public void run()
						{
							Toast.makeText( OldMainActivity.this, "Sorry, no bluetooth adapter is available", Toast.LENGTH_SHORT ).show( ) ;
						}
					} );
				}
			}
		} );
	}
	
	/**
	 * Schedules the <class>BluetoothAdapter</class> to stop scanning on a separate thread.
	 * 
	 * @param delay The number of seconds until this runs.
	 */
	private void stopScanning( )
	{
		scheduler.submit( new Runnable( )
		{
			public void run()
			{
				if( BuildConfig.DEBUG ) 
					Log.i( Consts.LOG, "Stopping BTLE scan" ) ;

					OldMainActivity.this.runOnUiThread( new Runnable( )
					{
						public void run()
						{
							Toast.makeText( OldMainActivity.this, "Stopping Bluetooth scan", Toast.LENGTH_SHORT ).show( ) ;
						}
					} ) ;
					mBluetoothAdapter.stopLeScan( btlsListener ) ;
					mBluetoothAdapter.cancelDiscovery( ) ;
				}
		} );
	}

	@Override
	protected void onStop()
	{	
		super.onStop( );
		
		// I'm not clear about whether this should or shouldn't be run on this thread
		if( mBluetoothAdapter != null )
		{
			mBluetoothAdapter.stopLeScan( btlsListener ) ;
			mBluetoothAdapter.cancelDiscovery( ) ;
		}
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy( );
		unregisterReceiver( actionFoundReceiver ) ;
		iBeaconManager.unBind( this ) ;
	}
	
	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		// TODO, just put in one dynamic menu item to start or stop scanning based on what the adapter is doing
		menu.add( R.string.start_scanning ) ;
		menu.add( R.string.stop_scanning ) ;
		menu.add( R.string.menu_legalnotices ) ;
		
		return super.onCreateOptionsMenu( menu ) ;
	}
	
	 @Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		if ( item.getTitle( ).toString( ).equals( getString( R.string.menu_legalnotices ) ) )
			displayLegal( );
		else if ( item.getTitle( ).toString( ).equals( getString( R.string.start_scanning ) ) )
			startScanning( );
		else if ( item.getTitle( ).toString( ).equals( getString( R.string.stop_scanning ) ) )
			stopScanning( );

		return super.onOptionsItemSelected( item );
	}

	 /**
	  * Really just doing this as an experiment
	  */
	 private void displayLegal( )
	 {
		String licenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo( getApplicationContext( ) );
		AlertDialog.Builder LicenseDialog = new AlertDialog.Builder( OldMainActivity.this );
		LicenseDialog.setTitle( "Legal Notices" );
		LicenseDialog.setMessage( licenseInfo );
		LicenseDialog.show( );
	 }

	@Override
	public void onIBeaconServiceConnect()
	{
		if( BuildConfig.DEBUG )
			Log.i( Consts.LOG, "iBeacon service is running" ) ;
			
		iBeaconManager.setRangeNotifier( new RangeNotifier( )
		{
			@Override
			public void didRangeBeaconsInRegion( final Collection<IBeacon> iBeacons, final Region region )
			{
				Iterator<IBeacon> iter = iBeacons.iterator( ) ;

				while( iter.hasNext( ) )
				{
					IBeacon beacon = iter.next( ) ;
					if( BuildConfig.DEBUG )
						Log.d( Consts.LOG, "I see an iBeacon " + beacon.getAccuracy( ) + " meters away."  ) ;
				}
				OldMainActivity.this.runOnUiThread( new Runnable( )
				{
					public void run()
					{
						deviceAdapter.updateBeaconStatus( iBeacons, region );
					}
				} );
			}
		} );
		
		scheduler.submit( new Runnable( )
		{
			public void run()
			{
				try {
					iBeaconManager.startRangingBeaconsInRegion( new Region( "myMonitoringUniqueId", null, null, null ) );
				} catch (RemoteException e) {   
			    	Log.e( Consts.LOG, "exception starting to range beacons", e ) ;
			    } 
			}
		} ) ;
		
//		iBeaconManager.setMonitorNotifier(new MonitorNotifier( )
//		{
//			
//			@Override
//			public void didExitRegion( Region region )
//			{
//				if( BuildConfig.DEBUG )
//					Log.i( Consts.LOG, "I no longer see an iBeacon in region " + region ) ;
//			}
//			
//			@Override
//			public void didEnterRegion( Region region )
//			{
//				if( BuildConfig.DEBUG )
//					Log.i( Consts.LOG, "I just saw an iBeacon in region " + region ) ;
//			}
//			
//			@Override
//			public void didDetermineStateForRegion( int state, Region region )
//			{
//				if( BuildConfig.DEBUG )
//					Log.i( Consts.LOG, "I just switched from seeing/not seeing an iBeacon" ) ;
//			}
//		} ) ;
//		 
//		    try {
//		        iBeaconManager.startMonitoringBeaconsInRegion( new Region( "myMonitoringUniqueId", null, null, null ) ) ;
//		    } catch (RemoteException e) {   
//		    	Log.e( Consts.LOG, e.toString( ) ) ;
//		    } 
	}
	
	/**
	 * Will create a listener for a BT adapter and upon discovery of a device will connect to the GATT profiles on those devices.
	 * 
	 * @see	GATTListener
	 * @author daniellipton
	 *
	 */
	class BTListener
	implements BluetoothAdapter.LeScanCallback
	{	
		public void onLeScan( 
				final BluetoothDevice device, 
				final int rssi,
				final byte[] scanRecord )
		{
			if( BuildConfig.DEBUG )
				Log.v( Consts.LOG, "onLeScan called" ) ;
			
			if( !deviceAdapter.isKnownDevice( device ) )
			{
				device.fetchUuidsWithSdp( ) ;
				OldMainActivity.this.runOnUiThread( new Runnable( )
				{
					public void run()
					{
						deviceAdapter.add( new FoundDevice( device ) );
					}
				} );
				
				if( BuildConfig.DEBUG ) 
				{
					Log.i( Consts.LOG, "Found Bluetooth device: " + device.getName( ) ) ;
					Log.i( Consts.LOG, "rssi=" + rssi + ". ad record=" + scanRecord ) ;
				}
				scheduler.submit( new Runnable( )
				{
					public void run()
					{
						device.connectGatt( OldMainActivity.this, false, GATTListener.getGattListener( device, deviceAdapter ) ) ;
					}
				} ) ;
			}
			else
			{
				if( BuildConfig.DEBUG )
					Log.d( Consts.LOG, "onLeScan called with a known device. rssi=" + rssi ) ;
			}
			
			// run on the UI thread because it needs to update the UI
			OldMainActivity.this.runOnUiThread( new Runnable( )
			{
				public void run()
				{
					deviceAdapter.updateDeviceStatus( device, rssi, scanRecord ) ;
				}
			} );
		}
	}
}
