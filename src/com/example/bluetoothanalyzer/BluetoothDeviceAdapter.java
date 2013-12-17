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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.bluetoothanalyzer.FoundDevice.DeviceType;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.Region;

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
	/** Is this beacon saved in Firebase?*/
	private Map<IBeacon, Boolean> beaconSaved ;
	
	public BluetoothDeviceAdapter( Context context, int resource )
	{
		super( context, resource );
		
		deviceRssiMap = new ConcurrentHashMap<BluetoothDevice, Integer>( ) ;
		deviceScanRecordMap = new ConcurrentHashMap<BluetoothDevice, Byte[]>( ) ;
		beaconSaved = new ConcurrentHashMap<IBeacon, Boolean>( ) ;
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
		}
		else
			button.setVisibility( Button.INVISIBLE );

		return rowView ;
	}

	private String getBeaconLabelString( IBeacon iBeacon )
	{
		StringBuffer sb = new StringBuffer( ) ;
		sb.append( "major = " ).append( iBeacon.getMajor( ) ) ;
		sb.append( ", minor = " ).append( iBeacon.getMinor( ) ) ;
		NumberFormat doubleFormat = NumberFormat.getNumberInstance( ) ;
		doubleFormat.setMaximumFractionDigits( 2 ) ;
		sb.append( "\napproximately " ).append( doubleFormat.format( iBeacon.getAccuracy( ) ) ).append( " feet away") ;
		sb.append( "\nproximity category = ") ;
		switch( iBeacon.getProximity( ) )
		{
			case IBeacon.PROXIMITY_FAR:
				sb.append( "far" ) ; break ;
			case IBeacon.PROXIMITY_IMMEDIATE:
				sb.append( "immediate" ) ; break ;
			case IBeacon.PROXIMITY_NEAR:
				sb.append( "near" ) ; break ;
			default:
				sb.append(  "unknown" ) ;
		}
		sb.append( "\nrssi = " ).append( iBeacon.getRssi( ) ) ;
		sb.append( ", tx power=" ).append( iBeacon.getTxPower( ) ) ;
		
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
	
	public boolean isKnowniBeacon( IBeacon beacon )
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
	public void updateBeaconStatus( Collection<IBeacon> iBeacons, Region region )
	{
		Iterator<IBeacon> iter = iBeacons.iterator( ) ;
		boolean dataChanged = false ;
		while( iter.hasNext( ) )
		{
			// add this device to the adapter's list if it isn't already there
			IBeacon b = iter.next( ) ;
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
	 *
	 */
	class BeaconFBListener
	implements ValueEventListener
	{
		private IBeacon b ;
		BeaconFBListener( IBeacon b )
		{
			this.b = b ;
		}
		
		@Override
		public void onCancelled( FirebaseError arg0 )
		{
			beaconSaved.put( b, null ) ;
			updateList( ) ;
		}

		@Override
		public void onDataChange( DataSnapshot snapshot )
		{
			String s = snapshot.getValue( String.class ) ; 
			if( s == null )
			{
				Log.i( Consts.LOG, "no value for beacon " + b ) ;
				beaconSaved.put( b, Boolean.FALSE ) ;
			}
			else
			{
				Log.i( Consts.LOG, "beacon was put in the database" ) ;
				beaconSaved.put( b, Boolean.TRUE ) ;
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
}
