package com.example.bluetoothanalyzer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
	private Map<DiscoveredBeaconDevice, Boolean> beaconSaved ;
	private String selectedMerchantName = null ;
	
	public BluetoothDeviceAdapter( Context context, int resource )
	{
		super( context, resource );
		beaconSaved = new ConcurrentHashMap<DiscoveredBeaconDevice, Boolean>( ) ;
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

		if( BuildConfig.DEBUG )
			Log.v( Consts.LOG, detailsString ) ;
		deviceDetailsLabel.setText( detailsString ) ;
		
		// list-item button
		Button button = ( Button ) rowView.findViewById( R.id.beaconButton ) ;
		if( device instanceof DiscoveredBeaconDevice )
		{
			DiscoveredBeaconDevice bd = ( DiscoveredBeaconDevice ) device ;
			button.setVisibility( Button.VISIBLE ) ;
			Boolean b = beaconSaved.get( bd ) ;
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
				BeaconFBListener listener = new BeaconFBListener( newBeacon ) ;
				FirebaseHelper.addBeaconValueListener( b, listener )  ;
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
		private DiscoveredBeaconDevice beacon ;
		
		BeaconFBListener( DiscoveredBeaconDevice b )
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
				Log.i( Consts.LOG, "no value for beacon - it wasn't in the database"  ) ;
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
					showBeaconNameDialog( fd.getBeacon( ), selectedMerchantName ) ;
				else
					Log.w( Consts.LOG, "Can't save beacon because something isn't set correctly." ) ;
			}
			else if( button.getText( ).equals( v.getContext( ).getText( R.string.removeButtonText ) ) )
			{
				FirebaseHelper.removeBeacon( fd.getBeacon( ) ) ;
			}
		}
		
		/**  add a new merchant - called by the menu overflow button */
		 public void showBeaconNameDialog( final Beacon beacon, final String selectedMerchantName )
		 {
			 Context context = BluetoothDeviceAdapter.this.getContext( ) ;
			final AlertDialog.Builder builder = new AlertDialog.Builder( context ) ;
			
			LayoutInflater inflater = ( LayoutInflater ) getContext( ).getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			builder.setView( inflater.inflate( R.layout.beacon_name_entry_dialog, null ) ) ;
			builder.setTitle( context.getString( R.string.beaconNameDialogTitle ) ) ;
			builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() 
			{
			    public void onClick( DialogInterface dialog, int id ) 
			    {
			    	AlertDialog alertDialog = ( AlertDialog ) dialog ;
			    	EditText beaconInput = ( EditText ) alertDialog.findViewById( R.id.beaconNameDialogField ) ;
			    	Object o = beaconInput.getText( ) ;
			        final String beaconName = o.toString( ) ;
			        if( beaconName != null && beaconName.length( ) > 0 )
			        {
				        //TODO get this off the GUI thread
			        	FirebaseHelper.addBeacon( beacon, selectedMerchantName, beaconName ) ;
			        }
			    }
			});
			builder.setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener( )
			{
				public void onClick( DialogInterface dialog, int which )
				{
					dialog.cancel( ); 
				}
			} ) ;

			builder.show( ) ;
		 }
	}

	public void setMerchant( final String merchantName )
	{
		this.selectedMerchantName = merchantName ;
		
		// remove all of the old beacons for this merchant from this list
	}
}
