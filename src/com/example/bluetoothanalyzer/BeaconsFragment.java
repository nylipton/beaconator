package com.example.bluetoothanalyzer;

import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.estimote.sdk.BeaconManager.RangingListener;

/**
 * Fragment to show the list of beacons
 * 
 * @author daniellipton
 *
 */
public class BeaconsFragment
extends ListFragment
{
	private static final int REQUEST_ENABLE_BT = 1234 ;
	private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
	private static final com.estimote.sdk.Region ALL_ESTIMOTE_BEACONS = new Region( ESTIMOTE_PROXIMITY_UUID, null, null ) ;
	private BeaconManager beaconManager = null ;
	private BluetoothDeviceAdapter deviceAdapter = null ;
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{	
		deviceAdapter = new BluetoothDeviceAdapter( getActivity( ), R.layout.list_mobile ) ;
		setListAdapter( deviceAdapter ) ;
		
		beaconManager = new BeaconManager( getActivity( ) ) ;
		beaconManager.setRangingListener( new RangingListener( )
		{
			
			@Override
			public void onBeaconsDiscovered( final Region region, final List<Beacon> beacons )
			{
				for( int i = 0 ; i < beacons.size( ) ; i++ )
				{
					Beacon beacon = beacons.get( i ) ;
					if( BuildConfig.DEBUG )
						Log.d( Consts.LOG, "I see an iBeacon " + Utils.computeAccuracy( beacon ) + " meters away."  ) ;
					getActivity( ).runOnUiThread( new Runnable( )
					{
						public void run()
						{
							deviceAdapter.updateBeaconStatus( beacons, region ) ;
						}
					} );
				}
			}
		} ) ;
		
		super.onCreate( savedInstanceState );
	}
	
	public void setMerchant( String merchantName )
	{
		deviceAdapter.setMerchant( merchantName ) ;
	}

	@Override
	public void onStart()
	{
		super.onStart( );

		// Check if device supports Bluetooth Low Energy.
		if ( !beaconManager.hasBluetooth( ) )
		{
			Toast.makeText( getActivity( ), "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG ).show( );
			return;
		}

		// If Bluetooth is not enabled, let user enable it.
		if ( !beaconManager.isBluetoothEnabled( ) )
		{
			Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
			startActivityForResult( enableBtIntent, REQUEST_ENABLE_BT );
		}
		else
		{
			connectToService( ) ;
		}
	}
	
	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		if ( requestCode == REQUEST_ENABLE_BT )
		{
			if ( resultCode == Activity.RESULT_OK )
			{
				connectToService( );
			}
			else
			{
				Toast.makeText( getActivity( ), "Bluetooth not enabled", Toast.LENGTH_LONG ).show( );
			}
		}
		super.onActivityResult( requestCode, resultCode, data );
	}
	
	 private void connectToService()
	{
		beaconManager.connect( new BeaconManager.ServiceReadyCallback( )
		{
			
			@Override
			public void onServiceReady()
			{
				try
				{
					beaconManager.startRanging( ALL_ESTIMOTE_BEACONS );
				}
				catch( RemoteException e )
				{
					Toast.makeText(getActivity( ), "Cannot start ranging, something terrible happened", Toast.LENGTH_LONG).show();
					if( BuildConfig.DEBUG)
						Log.e(Consts.LOG, "Cannot start ranging", e);
				}
				
			}
		} );
		
	}

	@Override
	 public void onStop( ) 
	 {
	    try {
	      beaconManager.stopRanging( ALL_ESTIMOTE_BEACONS ) ;
	    } catch (RemoteException e) {
	    	if( BuildConfig.DEBUG )
	    		Log.d(Consts.LOG, "Error while stopping ranging", e ) ;
	    }

	    super.onStop();
	  }

	@Override
	public void onDestroy()
	{
		beaconManager.disconnect( ) ;
		super.onDestroy( );
	}
}