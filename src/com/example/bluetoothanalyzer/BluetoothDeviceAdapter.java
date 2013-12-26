package com.example.bluetoothanalyzer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
extends ArrayAdapter<Device>
{
	/** Is this beacon saved in Firebase? */
	private Map<Beacon, Boolean> beaconSaved ;
	private String selectedMerchantName = null ;
	
	public BluetoothDeviceAdapter( Context context, int resource )
	{
		super( context, resource );
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
		final Device device = getItem( position ) ;
		StringBuffer sb = new StringBuffer( ) ;
		sb.append( device.getTypeString( ) ) ;
		sb.append( ": " ) ;
		sb.append( device.getName( ).toUpperCase( Locale.getDefault( ) ) ) ;
		deviceLabel.setText( sb.toString( ) ) ;
		
		// device details
		String detailsString = device.getLongDescription( ) ;

		Log.d( Consts.LOG, detailsString ) ;
		deviceDetailsLabel.setText( detailsString ) ;
		
		// list-item button
		Button button = ( Button ) rowView.findViewById( R.id.beaconButton ) ;
		if( device instanceof DiscoveredBeaconDevice )
		{
			DiscoveredBeaconDevice bd = ( DiscoveredBeaconDevice ) device ;
			button.setVisibility( Button.VISIBLE ) ;
			Boolean b = beaconSaved.get( bd.getBeacon( ) ) ;
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
				button.setOnClickListener( new BeaconButtonListener( bd ) ) ;
		}
		else
			button.setVisibility( Button.INVISIBLE );

		return rowView ;
	}

//	private String getBTDeviceLabelString( BluetoothDevice device )
//	{
//		StringBuffer sb = new StringBuffer( ) ;
//		// rssi and scan record
//		Integer rssi = deviceRssiMap.get( device ) ;
//		if( rssi != null )
//			sb.append( "rssi=" ).append(  rssi ).append( "\n" ) ;
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
//		ParcelUuid[] pUUIDArray = device.getUuids( ) ;
//		if( pUUIDArray != null )
//			for( int i = 0 ; i < pUUIDArray.length ; i++ )
//				sb.append( "uuid " ).append( i ).append(  " = " ).append(  pUUIDArray[i] ).append( "\n" ) ;
//		return sb.toString( ) ;
//	}
	
	public boolean isKnowniBeacon( Beacon beacon )
	{
		return( getPosition( new DiscoveredBeaconDevice( beacon ) ) >= 0 ) ;
	}

	/** Updates a found BT device's rssi and scan record.<p>THIS MUST BE CALLED BY THE MAIN UI THREAD */
//	public void updateDeviceStatus( BluetoothDevice device, int rssi, byte[] scanRecord )
//	{
//		int i = 0 ;
//		Byte[] bytes = new Byte[scanRecord.length] ;
//		for( byte b : scanRecord )
//			bytes[i++] = b ;
//		
//		deviceScanRecordMap.put( device, bytes ) ;
//		deviceRssiMap.put( device, Integer.valueOf( rssi ) ) ;
//		
//		notifyDataSetChanged( ) ;
//	}
	
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
			DiscoveredBeaconDevice newBeacon = new DiscoveredBeaconDevice( b ) ;
			int i = getPosition( newBeacon ) ;
			if( i >= 0 ) //i.e. this is a known iBeacon
			{
				( ( DiscoveredBeaconDevice ) getItem( i ) ).setBeacon( b ) ;
				dataChanged = true ;
			}
			else
			{
				FirebaseHelper.addBeaconValueListener( b, new BeaconFBListener( b ) ) ;
				add( newBeacon ) ;
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
		DiscoveredBeaconDevice fd ;
		
		BeaconButtonListener( DiscoveredBeaconDevice fd )
		{
			this.fd = fd ;
		}
		
		@Override
		public void onClick( View v )
		{
			Button button = ( Button ) v ;
			if( button.getText( ).equals( v.getContext( ).getText( R.string.addButtonText ) ) )
			{	
				if( selectedMerchantName != null && fd.getBeacon( ) != null )
					FirebaseHelper.addBeacon( fd.getBeacon( ), selectedMerchantName ) ;
				else
					Log.w( Consts.LOG, "Can't save beacon because something isn't set correctly." ) ;
			}
			else if( button.getText( ).equals( v.getContext( ).getText( R.string.removeButtonText ) ) )
			{
				FirebaseHelper.removeBeacon( fd.getBeacon( ) ) ;
			}
		}
	}

	public void setMerchant( String merchantName )
	{
		this.selectedMerchantName = merchantName ;
		
		// remove all of the old beacons for this merchant from this list
	}
}
