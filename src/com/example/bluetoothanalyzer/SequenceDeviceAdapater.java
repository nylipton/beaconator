package com.example.bluetoothanalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;

public class SequenceDeviceAdapater 
extends ArrayAdapter<SavedBeaconDevice>
{
	/** map of beacons keyed by merchant name */
	private Map<String, List<SavedBeaconDevice>> beaconMap ;
	private Semaphore merchantSemaphore = new Semaphore( 1 ) ;
	private String selectedMerchant = null ; 
	private SequencesFragment sequencesFragment ; //TODO is it worth abstracting this so there's a listener for new ProximityEvents instead of directly calling the SequencesFragment?
	
	public SequenceDeviceAdapater( Context context, int resource, SequencesFragment fragment )
	{
		super( context, resource );
		this.sequencesFragment = fragment ;
		FirebaseHelper.addBeaconParentListener( new BeaconsListener( ) );
		beaconMap = new ConcurrentHashMap<String, List<SavedBeaconDevice>>( ) ;
	}

	 @Override
	public View getView( int position, View convertView, ViewGroup parent )
	{
		TextView beaconName = null, beaconDetails = null ;
		View rowView = null ;
		if( convertView != null )
		{
			rowView = convertView ;
		}
		else
		{
			LayoutInflater inflater = ( LayoutInflater ) getContext( ).getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			rowView = inflater.inflate( R.layout.sequence_list_item, parent, false ) ;
		}
		beaconName = ( TextView ) rowView.findViewById( R.id.beaconName ) ;
		beaconDetails = ( TextView ) rowView.findViewById( R.id.beaconDetails ) ;
		SavedBeaconDevice beacon = super.getItem( position ) ;
		beaconName.setText( beacon.getName( ) ) ;
		beaconDetails.setText( beacon.getLongDescription( ) ) ;
		
		Button nearBtn = ( Button ) rowView.findViewById( R.id.nearBtn ) ;
		Button immediateBtn = ( Button ) rowView.findViewById( R.id.immediateBtn ) ;
		Button farBtn = ( Button ) rowView.findViewById( R.id.farBtn ) ;
		if( !nearBtn.hasOnClickListeners( ) ) nearBtn.setOnClickListener( new ProximityButtonListener( ProximityEvent.Proximity.NEAR, beacon ) );
		if( !immediateBtn.hasOnClickListeners( ) ) immediateBtn.setOnClickListener( new ProximityButtonListener( ProximityEvent.Proximity.IMMEDIATE, beacon ) );
		if( !farBtn.hasOnClickListeners( ) ) farBtn.setOnClickListener( new ProximityButtonListener( ProximityEvent.Proximity.FAR, beacon ) );
		return rowView ;
	}

	public void setMerchant( String merchantName )
	{
		try
		{
			merchantSemaphore.acquire( ) ;
			boolean changed = ( merchantName != this.selectedMerchant ) ;
			this.selectedMerchant = merchantName ;
			if( changed )
			{
				super.clear( ) ;
				List<SavedBeaconDevice> beacons = beaconMap.get( merchantName ) ;
				if( beacons != null )
					super.addAll( beacons );
				updateList( ) ;
			}
		}
		catch( InterruptedException e ) { }
		finally {
			merchantSemaphore.release( ) ;
		}
	}
	
	/** Notifies this adapter that data has changed; performs this later on the main thread so this can be called anywhere */
	private void updateList( )
	{
		new Handler( Looper.getMainLooper( ) ).post( new Runnable( ) {
			public void run()
			{
				SequenceDeviceAdapater.this.notifyDataSetChanged( ) ;
			}
		} ) ;
	}
	
	/**
	 * Listens for when beacons are added or removed from the database
	 */
	class BeaconsListener
	implements ChildEventListener
	{
		
		public void onChildAdded( DataSnapshot ds, String previousChildName )
		{
			try {
				merchantSemaphore.acquire( );
				List<SavedBeaconDevice> beacons = FirebaseHelper.convertDataSnapshotToBeacons( ds ) ;
				for( int i = 0 ; i < beacons.size( ) ; i++ )
				{
					SavedBeaconDevice beacon = beacons.get( i ) ;
					String merchant = beacon.getMerchant( ) ;
					List<SavedBeaconDevice> merchantBeacons = beaconMap.get( merchant ) ;
					if( merchantBeacons == null )
					{
						merchantBeacons = new ArrayList<SavedBeaconDevice>( ) ;
						beaconMap.put( merchant, merchantBeacons ) ;
					}
					merchantBeacons.add( beacon ) ;
					
					if( selectedMerchant != null && selectedMerchant.equals( merchant ) )
						add( beacon );
				}
			} catch( InterruptedException e ) { }
			finally {
				merchantSemaphore.release( ) ;
			}
			
			updateList( ) ;
		}

		public void onChildChanged( DataSnapshot arg0, String arg1 )
		{}

		public void onChildMoved( DataSnapshot arg0, String arg1 )
		{}

		public void onChildRemoved( DataSnapshot arg0 )
		{}

		public void onCancelled( FirebaseError arg0 )
		{}
	}
	
	class ProximityButtonListener
	implements OnClickListener
	{
		ProximityEvent.Proximity prox ;
		SavedBeaconDevice beacon ;
		
		ProximityButtonListener( ProximityEvent.Proximity prox, SavedBeaconDevice beacon )
		{
			this.prox = prox ;
			this.beacon = beacon ;
		}
		
		public void onClick( View v )
		{
			ProximityEvent evt = new ProximityEvent( beacon, prox ) ;
			sequencesFragment.addEvent( evt ) ;
		}
	}

}
