package com.example.bluetoothanalyzer;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.example.bluetoothanalyzer.FoundDevice.DeviceType;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

/**
 * Adapter that stores a list a BT devices. Frankly this should really create its own model instead of delving
 * into the GATTListener
 * 
 * @author daniellipton
 */
public class BluetoothDeviceAdapter 
extends ArrayAdapter<FoundDevice>
{
	private Map<BluetoothDevice, Integer> deviceRssiMap ;
	private Map<BluetoothDevice, Byte[]> deviceScanRecordMap ;
	/** Is this beacon saved in Firebase? */
	private Map<Beacon, Boolean> beaconSaved ;
	private String selectedMerchantName = null ;
	
	public BluetoothDeviceAdapter( Context context, int resource )
	{
		super( context, resource );
		
		deviceRssiMap = new ConcurrentHashMap<BluetoothDevice, Integer>( ) ;
		deviceScanRecordMap = new ConcurrentHashMap<BluetoothDevice, Byte[]>( ) ;
		beaconSaved = new ConcurrentHashMap<Beacon, Boolean>( ) ;
	}
	
	@Override
	public View getView( int position, View convertView, ViewGroup parent )
	{
		TextView deviceLabel = null ;
		TextView deviceDetailsLabel = null ;
		View rowView = null ;
		if( convertView != null )
		{
			rowView = convertView ;
		}
		else
		{
			LayoutInflater inflater = ( LayoutInflater ) getContext( ).getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			rowView = inflater.inflate( R.layout.list_mobile, parent, false ) ;
		}
//		Log.d( Consts.LOG, "View is of type " + rowView.getClass( ).getSimpleName( ) ) ;
//		Log.d( Consts.LOG, "mainLabel is of type " + deviceLabel ) ;
		deviceLabel = ( TextView ) rowView.findViewById( R.id.device_name ) ;
		deviceDetailsLabel = ( TextView ) rowView.findViewById( R.id.device_details ) ;
		
		
		// device name
		final FoundDevice device = getItem( position ) ;
		StringBuffer sb = new StringBuffer( ) ;
		sb.append( device.getTypeString( ) ) ;
		sb.append( ": " ) ;
		sb.append( device.getName( ).toUpperCase( Locale.getDefault( ) ) ) ;
		deviceLabel.setText( sb.toString( ) ) ;
		
		// device details
		String detailsString = null ;
		if( device.getType( ) == DeviceType.BT )
			detailsString = getBTDeviceLabelString( device.getBtDevice( ) );
		else if( device.getType( ) == DeviceType.IBEACON )
			detailsString = getBeaconLabelString( device.getiBeacon( ) ) ;
		else
			detailsString = "This is a test device" ;
		Log.d( Consts.LOG, detailsString ) ;
		deviceDetailsLabel.setText( detailsString ) ;
		
		// list-item button
		Button button = ( Button ) rowView.findViewById( R.id.beaconButton ) ;
		if( device.getType( ) == DeviceType.IBEACON )
		{
			button.setVisibility( Button.VISIBLE ) ;
			Boolean b = beaconSaved.get( device.getiBeacon( ) ) ;
			if( b == null ) 
			{
				button.setText( getContext( ).getString( R.string.loadingButtonText ) );
				button.setEnabled( false );
			}
			else
			{
				button.setEnabled( true ) ;
				if( b.booleanValue( ) ) button.setText( getContext( ).getString( R.string.removeButtonText ) ) ;
				else button.setText( getContext( ).getString( R.string.addButtonText ) ) ;
			}
			
			if( !button.hasOnClickListeners( ) )
				button.setOnClickListener( new BeaconButtonListener( device) ) ;
		}
		else
			button.setVisibility( Button.INVISIBLE );

		return rowView ;
	}

	private String getBeaconLabelString( Beacon iBeacon )
	{
		StringBuffer sb = new StringBuffer( ) ;
		sb.append( "major = " ).append( iBeacon.major ) ;
		sb.append( ", minor = " ).append( iBeacon.minor ) ;
		NumberFormat doubleFormat = NumberFormat.getNumberInstance( ) ;
		doubleFormat.setMaximumFractionDigits( 2 ) ;
		sb.append( "\napproximately " ).append( doubleFormat.format( Utils.computeAccuracy( iBeacon ) ) ).append( " meters away") ;
		sb.append( "\nproximity category = ") ;
		switch( Utils.computeProximity( iBeacon ) )
		{
			case FAR:
				sb.append( "far" ) ; break ;
			case IMMEDIATE:
				sb.append( "immediate" ) ; break ;
			case NEAR:
				sb.append( "near" ) ; break ;
			default:
				sb.append(  "unknown" ) ;
		}
		sb.append( "\nrssi = " ).append( iBeacon.rssi ) ;
		sb.append( ", tx power=" ).append( iBeacon.measuredPower ) ;
		
		return sb.toString( ) ;
	}

	private String getBTDeviceLabelString( BluetoothDevice device )
	{
		StringBuffer sb = new StringBuffer( ) ;
		// rssi and scan record
		Integer rssi = deviceRssiMap.get( device ) ;
		if( rssi != null )
			sb.append( "rssi=" ).append(  rssi ).append( "\n" ) ;
//		Byte[] scanRecordArray = deviceScanRecordMap.get( device ) ;
//		if( scanRecordArray != null )
//		{
//			int i = 0 ; byte[] scanRecord = new byte[scanRecordArray.length] ;
//			for( Byte b : scanRecordArray)
//				scanRecord[i++] = b.byteValue( ) ;
//			sb.append( "scan record=" ).append( scanRecord ).append( "\n" ) ;
//		}
		
		// profiles
//		GATTListener listener = GATTListener.getGattListener( device, this ) ;
//		Iterator<BluetoothGatt> pIter = listener.getKnownProfiles( ).iterator( ) ;
//		int i = 0 ;
//		while( pIter.hasNext( ) )
//		{
//			i++ ;
//			BluetoothGatt gattProfile = pIter.next( ) ;
//			sb.append( "profile #" ).append( i ).append( ": " ) ;
//			int state = listener.getProfileState( gattProfile ) ;
//			switch( state )
//			{
//				case BluetoothProfile.STATE_CONNECTED:
//					sb.append( "connected" ) ;
//					break ;
//				case BluetoothProfile.STATE_DISCONNECTED:
//					sb.append(  "disconnected" ) ;
//					break ;
//				default:
//					sb.append( "unknown state!" ) ;
//				
//			}
//			if( pIter.hasNext( ) )
//				sb.append( "\n" ) ;
//		}
		ParcelUuid[] pUUIDArray = device.getUuids( ) ;
		if( pUUIDArray != null )
			for( int i = 0 ; i < pUUIDArray.length ; i++ )
				sb.append( "uuid " ).append( i ).append(  " = " ).append(  pUUIDArray[i] ).append( "\n" ) ;
		return sb.toString( ) ;
	}

	/** Is this a known BluetoothDevice? */
	public boolean isKnownDevice( BluetoothDevice device )
	{
		return( getPosition( new FoundDevice( device ) ) >= 0 ) ;
	}
	
	public boolean isKnowniBeacon( Beacon beacon )
	{
		return( getPosition( new FoundDevice( beacon ) ) >= 0 ) ;
	}

	/** Updates a found BT device's rssi and scan record.<p>THIS MUST BE CALLED BY THE MAIN UI THREAD */
	public void updateDeviceStatus( BluetoothDevice device, int rssi, byte[] scanRecord )
	{
		int i = 0 ;
		Byte[] bytes = new Byte[scanRecord.length] ;
		for( byte b : scanRecord )
			bytes[i++] = b ;
		
		deviceScanRecordMap.put( device, bytes ) ;
		deviceRssiMap.put( device, Integer.valueOf( rssi ) ) ;
		
		notifyDataSetChanged( ) ;
	}
	
	/** 
	 * Will update a discovered iBeacon's status or just add it to the list of it's not there yet.<p>
	 * ALWAYS CALL THIS ON THE UI THREAD
	 * @param iBeacons
	 * @param region
	 */
	public void updateBeaconStatus( Collection<Beacon> iBeacons, Region region )
	{
		Iterator<Beacon> iter = iBeacons.iterator( ) ;
		boolean dataChanged = false ;
		while( iter.hasNext( ) )
		{
			// add this device to the adapter's list if it isn't already there
			Beacon b = iter.next( ) ;
			FoundDevice newFD = new FoundDevice( b ) ;
			int i = getPosition( newFD ) ;
			if( i >= 0 ) //i.e. this is a known iBeacon
			{
				getItem( i ).setiBeacon( b );
				dataChanged = true ;
			}
			else
			{
				FirebaseHelper.addBeaconValueListener( b, new BeaconFBListener( b ) ) ;
				add( newFD ) ;
			}
		}
		if( dataChanged )
			notifyDataSetChanged( ) ;
	}
	
	/**
	 * Listens for when the database's values have changed
	 * @author daniellipton
	 */
	class BeaconFBListener
	implements ValueEventListener
	{
		private Beacon beacon ;
		BeaconFBListener( Beacon b )
		{
			this.beacon = b ;
		}
		
		@Override
		public void onCancelled( FirebaseError arg0 )
		{
			beaconSaved.put( beacon, null ) ;
			updateList( ) ;
		}

		@Override
		public void onDataChange( DataSnapshot snapshot )
		{
			Object s = snapshot.getValue( ) ; 
			if( s == null )
			{
				Log.i( Consts.LOG, "no value for beacon " + beacon ) ;
				beaconSaved.put( beacon, Boolean.FALSE ) ;
			}
			else
			{
				Log.i( Consts.LOG, "beacon was put in the database" ) ;
				beaconSaved.put( beacon, Boolean.TRUE ) ;
			}
			
			updateList( ) ;
		}
		
		private void updateList( )
		{
			new Handler( Looper.getMainLooper( ) ).post( new Runnable( ) {
				public void run()
				{
					BluetoothDeviceAdapter.this.notifyDataSetChanged( ) ;
				}
			} ) ;
		}
	}
	
	/** Listener for buttons on beacon list items */
	class BeaconButtonListener
	implements OnClickListener
	{
		FoundDevice fd ;
		
		BeaconButtonListener( FoundDevice fd )
		{
			this.fd = fd ;
		}
		
		@Override
		public void onClick( View v )
		{
			Button button = ( Button ) v ;
			if( button.getText( ).equals( v.getContext( ).getText( R.string.addButtonText ) ) )
			{	
				if( selectedMerchantName != null && fd.getiBeacon( ) != null )
					FirebaseHelper.addBeacon( fd.getiBeacon( ), selectedMerchantName ) ;
				else
					Log.w( Consts.LOG, "Can't save beacon because something isn't set correctly." ) ;
			}
			else if( button.getText( ).equals( v.getContext( ).getText( R.string.removeButtonText ) ) )
			{
				FirebaseHelper.removeBeacon( fd.getiBeacon( ) ) ;
			}
		}
	}

	public void setMerchant( String merchantName )
	{
		this.selectedMerchantName = merchantName ;
	}
}
