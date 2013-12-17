package com.example.bluetoothanalyzer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Listens for BTLE GATT profile call backs. Then explores the services and prints out what it finds. <p>
 * Note that this service isn't thread-safe. It assumes that only one thread will be making the calls to the listener methods.
 * 
 * @author daniellipton
 *
 */
class GATTListener
extends BluetoothGattCallback
{
	BluetoothDevice device ;
	BluetoothDeviceAdapter adapter ;
	/** list of services that have already been discovered */
	private Set<BluetoothGattService> knownServices ;
	/** list of profiles that have already been discovered */
	private Set<BluetoothGatt> knownProfiles ;
	/** map of GATT profiles to their current state */
	private Map<BluetoothGatt, Integer> profileStateMap ;
	private static final Map<BluetoothDevice, GATTListener> instances = new HashMap<BluetoothDevice, GATTListener>( ) ;
	/** handler for the UI thread */
	private Handler handler = new Handler( Looper.getMainLooper( ) ) ;
	
	/** Use this to get an instance */
	static synchronized GATTListener getGattListener( BluetoothDevice device, BluetoothDeviceAdapter adapter )
	{
		GATTListener listener = instances.get( device ) ;
		if( listener == null )
		{
			listener = new GATTListener( device, adapter ) ;
			instances.put( device, listener ) ;
		}
		return listener ;
	}
	
	private GATTListener( BluetoothDevice device, BluetoothDeviceAdapter adapter )
	{
		if( BuildConfig.DEBUG ) 
			Log.i( Consts.LOG, "Creating a GATTListener" ) ;

		this.device = device ;
		this.adapter = adapter ;
		
		knownServices = new HashSet<BluetoothGattService>( ) ;
		knownProfiles = new HashSet<BluetoothGatt>( ) ;
		profileStateMap = new ConcurrentHashMap<BluetoothGatt, Integer>( ) ;
	}
	
	@Override
	public void onCharacteristicChanged( 
			BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic )
	{
		if( BuildConfig.DEBUG ) 
			Log.i( Consts.LOG, device.getName( ) + ": characteristic changed on GATT service: " + gatt + ", characteristic = " + characteristic ) ;
		
		super.onCharacteristicChanged( gatt, characteristic );
	}
	
	@Override
	public void onServicesDiscovered( 
			BluetoothGatt gatt, 
			int status )
	{
		super.onServicesDiscovered( gatt, status ) ;
		
		if( status == BluetoothGatt.GATT_SUCCESS ) 
		{
			for( BluetoothGattService service : gatt.getServices( )  )
			{
				synchronized( knownServices )
				{
					if( !knownServices.contains( service ) )
					{
						knownServices.add( service ) ;
						if( BuildConfig.DEBUG )
						{
							Log.i( Consts.LOG, "---" ) ;
							Log.i( Consts.LOG, device.getName( ) + ": GATT service discovered, status = " + ((status==BluetoothGatt.GATT_SUCCESS)?"success":"failure") ) ;

							final StringBuffer sb = new StringBuffer( );
							sb.append( "device : " );
							sb.append( device.getName( ) );
							sb.append( ( service.getType( ) == BluetoothGattService.SERVICE_TYPE_PRIMARY ) ? " this is a primary service"
									: " this is a secondary service" );
							Log.i( Consts.LOG, sb.toString( ) ) ;
						}
	
						// final String uuid = service.getUuid( ).toString( );
//						for ( BluetoothGattCharacteristic gattCharacteristic : service.getCharacteristics( ) )
//						{
//							for ( BluetoothGattDescriptor descriptor : gattCharacteristic.getDescriptors( ) )
//							{
//								boolean read = gatt.readDescriptor( descriptor );
//								if( BuildConfig.DEBUG )
//									Log.i( Consts.LOG, "Read characteristic descriptor = " + read );
//								if ( read )
//								{
//									final byte[] dVal = descriptor.getValue( );
//									
//									if( BuildConfig.DEBUG )
//										Log.i( Consts.LOG, "descriptor= " + dVal );
//								}
//							}
//						}
						if( BuildConfig.DEBUG )
							Log.i( Consts.LOG, "---" );
					}
				}
			}
		}
		else if( BuildConfig.DEBUG) 
			Log.i( Consts.LOG, "onServicesDiscovered called but status wasn't success: " + status ) ;
	}
	
	@Override
    public void onConnectionStateChange(
    		BluetoothGatt gatt, 
    		int status,
            int newState) 
	{
		if( BuildConfig.DEBUG )
			Log.i( Consts.LOG,
				"Status change for GATT profile on " 
					+ device.getName( ) 
					+ " : state = " 
					+ ((newState == BluetoothProfile.STATE_CONNECTED)?"connected":"disconnected" ) ) ;
		
		super.onConnectionStateChange( gatt, status, newState ) ;
		
		profileStateMap.put( gatt, Integer.valueOf( newState ) ) ;
		if( newState == BluetoothProfile.STATE_CONNECTED )
		{
			if( BuildConfig.DEBUG )
				Log.i( Consts.LOG, "Attempting to discover services on " + device.getName( ) + "..." ) ;
			gatt.discoverServices( ) ;
		}
		
		synchronized( getKnownProfiles() )
		{
    		if( !getKnownProfiles( ).contains( gatt ) )
    		{
    			getKnownProfiles( ).add( gatt ) ;
    			handler.post( new Runnable( ) {
						public void run()
						{
							adapter.notifyDataSetChanged( ) ; 
						}
    				}
    			) ;
    		}
		}
    }
	
	/**
	 * @see BluetoothProfile#STATE_CONNECTED
	 */
	public int getProfileState( BluetoothGatt gatt )
	throws IllegalArgumentException
	{
		Integer s = profileStateMap.get( gatt ) ;
		if( s == null )
			throw new IllegalArgumentException( "unknown gatt profile: " + gatt ) ;
		
		return s.intValue( ) ;
	}
	
	@Override
	public void onDescriptorRead( 
			BluetoothGatt gatt,
			BluetoothGattDescriptor descriptor, 
			int status )
	{
		if( BuildConfig.DEBUG )
			Log.i( Consts.LOG,"desecriptor read = " + descriptor ) ;
		super.onDescriptorRead( gatt, descriptor, status );
	}

	/**
	 * TODO return a copy to avoid threading issues
	 * @return the set of known GATT profiles for this device
	 */
	protected Set<BluetoothGatt> getKnownProfiles()
	{
		return knownProfiles;
	}

}
