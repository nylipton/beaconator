package com.example.bluetoothanalyzer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.estimote.sdk.Beacon;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.GenericTypeIndicator;
import com.firebase.client.ValueEventListener;

/**
 * A bunch of convenience methods to read and write the Firebase database. Note that while the fb methods don't seem to 
 * block so you can probably call them on the main UI thread, it's not really documented anywhere so be aware.
 * 
 * @author daniellipton
 *@see https://beacon-test.firebaseio.com/#
 */
public class FirebaseHelper
{
	/** 
	 * Date format for the datetime this beacon was added into Firebase
	 * @see #CREATED_KEYCREATED
	 */
	public final static DateFormat dateFormat = DateFormat.getDateTimeInstance( ) ;
	
	private final static String FIREBASE_URL = "https://beacon-test.firebaseio.com/" ;
	/** parent for all merchants */
	private final static String MERCHANTS_NODE = "merchants" ;
	/** parent for all sequences */
	private final static String SEQUENCES_NODE = "sequences" ;
	/** parent for all beacons for this merchant */
	private final static String BEACONS_NODE = "beacons" ;
	/** key for key/val hash that goes in a beacon under minor to represent the created timestamp */
	private final static String CREATED_KEY = "created" ;
	/** key for key/val hash that goes in a beacon under minor to represent the merchant */
	private final static String MERCHANT_KEY = "merchant" ;
	/** key for key/val hash that goes in a beacon under minor to represent the beacon's name */
	private final static String BEACON_NAME_KEY = "name" ;
	
	/** key for key/val hash that goes in a sequence under sequence node to represent the sequence's uuid */
	private final static String UUID_NAME_KEY = "uuid" ;
	/** key for key/val hash that goes in a sequence under sequence node to represent the sequence's major id */
	private final static String MAJOR_NAME_KEY = "major" ;
	/** key for key/val hash that goes in a sequence under sequence node to represent the sequence's minor id */
	private final static String MINOR_NAME_KEY = "minor" ;
	/** key for key/val hash that goes in a sequence under sequence node to represent the sequence's proximity */
	private final static String PROXIMITY_NAME_KEY = "proximity" ;
	/** child under a sequence name and parent to the random id nodes for the events */
	private final static String EVENTS_NODE = "events" ;
	
	/** key for key/val hash that goes in a notification to represent the notification's title */
	private final static String TITLE_KEY = "title" ;
	/** key for key/val hash that goes in a notification to represent the notification's message */
	private final static String MESSAGE_KEY = "message" ;
	/** key for key/val hash that goes in a notification to represent the notification's link  (i.e. where clicking the link goes) */
	private final static String LINK_KEY = "link" ;
	/** key for key/val hash that goes in a notification to represent the notification's large icon URL */
	private final static String LARGE_ICON_URL_KEY = "largeIconURL" ;
	/** child under a sequence name and parent to the sequence name. this introduces data integrity issues */
	private final static String SEQ_NAME_KEY = "sequenceName" ;
	/** child under a sequence name and parent to the notification name */
	private final static String NOTIF_NODE = "notifications" ;
	
	public static Firebase getBeaconNode( Beacon beacon )
	{
		if( beacon == null  )
			throw new IllegalArgumentException( "can't pass null beacon " ) ;
		Firebase fbRef = new Firebase( FIREBASE_URL ) ;
		return fbRef.child( BEACONS_NODE ).child( beacon.proximityUUID ).child( Integer.toString( beacon.major ) ).child( Integer.toString( beacon.minor ) ) ;
	}
	
	/** Adds a listener for all of the beacons in the database */
	public static void addBeaconParentListener( ChildEventListener listener )
	{
		Firebase fbBeaconsRef = new Firebase( FIREBASE_URL ).child( BEACONS_NODE ) ;
		fbBeaconsRef.addChildEventListener( listener ) ;
	}
	
	/** Will add a listener to get the values of this beacon */
	public static void addBeaconValueListener( String uuid, int major, int minor, ValueEventListener listener )
	{
		Firebase fbRef = new Firebase( FIREBASE_URL ) ;
		fbRef = fbRef.child( BEACONS_NODE ).child( uuid ).child( Integer.toString( major ) ).child( Integer.toString( minor ) ) ;
		fbRef.addValueEventListener( listener ) ;
	}
	
	/**
	 * Sticks the beacon into Firebase as <code>beacons/proximityUUID/major/minor</code> with the value of a timestamp and the merchant
	 * 
	 * @param beacon the beacon to be added
	 */
	public static void addBeacon( Beacon beacon, String merchantName, String beaconName )
	{
		Map<String, String> beaconEntry = new HashMap<String, String>( ) ;
		beaconEntry.put( CREATED_KEY, dateFormat.format( new Date( ) ) ) ;
		beaconEntry.put( MERCHANT_KEY, merchantName ) ;
		beaconEntry.put( BEACON_NAME_KEY, beaconName ) ;
		getBeaconNode( beacon ).setValue( beaconEntry );
	}
	
	/** Adds a listener for when the values of this beacon change */
	public static void addBeaconValueListener( Beacon beacon, ValueEventListener listener )
	{
		Firebase beaconRef = getBeaconNode( beacon ) ;
		beaconRef.addValueEventListener( listener ) ;
	}

	public static void removeBeacon( Beacon beacon )
	{
		Firebase beaconRef = getBeaconNode( beacon ) ;
		beaconRef.removeValue( ) ; 
	}
	
	/**
	 * Listen to when merchants are added, removed, etc.
	 * @param listener
	 */
	public static void addMerchantsListener( ChildEventListener listener )
	{
		Firebase fbBaseRef = new Firebase( FIREBASE_URL ) ;
		fbBaseRef.child( MERCHANTS_NODE ).addChildEventListener( listener ) ;
	}

	/** Add a merchant to the database */
	public static void addMerchant( String merchantName )
	{
		Firebase fbBaseRef = new Firebase( FIREBASE_URL ) ;
		Firebase newRef = fbBaseRef.child( MERCHANTS_NODE ).push( ) ;
		newRef.setValue( merchantName ) ;
		//TODO add more data and a structure around this, like the added time.
	}
	
	/**
	 * Adds this sequence to the database. Note that every proximity event must have the same merchant or this 
	 * will throw an <code>IllegalArgumentException</code>.
	 * @param sequence
	 */
	public static void addSequence( String sequenceName, List<ProximityEvent> sequence )
	{
		if( sequence == null || sequence.size( ) == 0 ) return ;
		
		Firebase fbBaseRef = new Firebase( FIREBASE_URL ) ;
		Firebase seqRef = fbBaseRef.child( SEQUENCES_NODE ) ;
		String merchant = sequence.get( 0 ).getBeacon( ).getMerchant( ) ;
		Firebase sequenceRef = seqRef.child( merchant ).child( sequenceName ) ;
		for( int i = 0 ; i < sequence.size( ) ; i++ )
		{
			ProximityEvent evt = sequence.get( i ) ;
			if( !evt.getBeacon( ).getMerchant( ).equals( merchant ) )
				throw new IllegalArgumentException( "Sequence passed with different merchants. Make sure all ProximityEvents have the same merchant." ) ;
			Firebase evtRef = sequenceRef.child( EVENTS_NODE ).push( ) ;
			Map<String, Object> evtMap = new HashMap<String, Object>( ) ;
			evtMap.put( UUID_NAME_KEY, evt.getBeacon( ).getUUID( ) ) ;
			evtMap.put( MAJOR_NAME_KEY, evt.getBeacon( ).getMajor( ) ) ;
			evtMap.put( MINOR_NAME_KEY, evt.getBeacon( ).getMinor( ) ) ;
			evtMap.put( PROXIMITY_NAME_KEY, evt.getProximity( ) ) ;
			evtRef.setValue( evtMap, i ) ;
		}
	}
	
	/** Adds a listener for all of the sequences in the database */
	public static void addSequenceParentListener( String merchant, ChildEventListener listener )
	{
		Firebase fbRef = new Firebase( FIREBASE_URL ).child( SEQUENCES_NODE ).child( merchant ) ;
		fbRef.addChildEventListener( listener ) ;
	}
	
	/**
	 * Takes a <code>DataSnapshot</code> of a tree under the "beacons" node and then reads all of the beacons for
	 * that uuid.
	 * 
	 * @param ds	a snapshot of the UUID under the BEACONS_NODE
	 */
	public static List<SavedBeaconDevice> convertDataSnapshotToBeacons( DataSnapshot ds )
	{
		List<SavedBeaconDevice> beaconsList = new ArrayList<SavedBeaconDevice>( ) ;
		final String uuid = ds.getName( ) ;
		Iterator<DataSnapshot> majorIter = ds.getChildren( ).iterator( ) ;
		while( majorIter.hasNext( ) )
		{
			DataSnapshot majorDS = ( DataSnapshot ) majorIter.next( ) ;
			int major = Integer.parseInt( majorDS.getName( ) ) ;
			Iterator<DataSnapshot> minorIter = majorDS.getChildren( ).iterator( ) ;
			while( minorIter.hasNext( ) )
			{
				DataSnapshot minorDS = ( DataSnapshot ) minorIter.next( ) ;
				int minor = Integer.parseInt( minorDS.getName( ) ) ;
				GenericTypeIndicator<Map<String, String>> t = new GenericTypeIndicator<Map<String, String>>( ) { } ;
				Map<String, String> valMap = minorDS.getValue( t ) ;
				String merchantName = valMap.get( MERCHANT_KEY ) ;
				String name = valMap.get( BEACON_NAME_KEY ) ;
				SavedBeaconDevice beacon = new SavedBeaconDevice( uuid, major, minor, merchantName, name ) ;
				beaconsList.add( beacon ) ;
			}
		}
		
		return beaconsList ;
	}
	
	public static NotificationInfo convertDataSnapshotToNotification( DataSnapshot ds )
	{
		GenericTypeIndicator<Map<String, String>> t = new GenericTypeIndicator<Map<String, String>>( ) { } ;
		Map<String, String> valMap = ds.getValue( t ) ;
		NotificationInfo notif = new NotificationInfo( ) ;
		notif.largeIconURL = valMap.get( LARGE_ICON_URL_KEY ) ;
		notif.linkURL = valMap.get( LINK_KEY ) ;
		notif.merchant = valMap.get( MERCHANT_KEY ) ;
		notif.message = valMap.get( MESSAGE_KEY ) ;
		notif.name = ds.getName( ) ;
		notif.sequenceName = valMap.get( SEQ_NAME_KEY ) ;
		notif.title = valMap.get( TITLE_KEY ) ;
		return notif ;
	}

	/**
	 * Saves this notification into the database 
	 */
	public static void addNotification( NotificationInfo notif )
	{
		Firebase fbBaseRef = new Firebase( FIREBASE_URL ) ; 
		Firebase fbNotifRef = fbBaseRef.child( SEQUENCES_NODE ).child( notif.merchant ).child( notif.sequenceName ).child( NOTIF_NODE ).child( notif.name ) ;
		Map<String, Object> vals = new HashMap<String, Object>( ) ;
		vals.put( TITLE_KEY, notif.title ) ;
		vals.put( MESSAGE_KEY, notif.message ) ;
		vals.put( LINK_KEY, notif.linkURL ) ;
		vals.put( LARGE_ICON_URL_KEY, notif.largeIconURL ) ;
		vals.put( SEQ_NAME_KEY, notif.sequenceName ) ;
		vals.put( CREATED_KEY, dateFormat.format( new Date( ) ) ) ;
		fbNotifRef.setValue( vals ) ;
	}
	
	/** Adds a listener for all of the notifications in the database for a specific merchant */
	public static void addNotificationParentListener( String merchant, ChildEventListener listener )
	{
		Firebase fbRef = new Firebase( FIREBASE_URL ).child( SEQUENCES_NODE ).child( merchant ) ;
		fbRef.addChildEventListener( new SequenceChildNodeListener( listener ) ) ;
	}
	
	/** These will be called when "sequences"->merchant->seq name is found */
	static class SequenceChildNodeListener
	implements ChildEventListener
	{
		ChildEventListener notificationsListener ;
		
		SequenceChildNodeListener( ChildEventListener notificationsListener )
		{
			this.notificationsListener = notificationsListener ;
		}
		
		@Override
		public void onCancelled( FirebaseError arg0 )
		{
			notificationsListener.onCancelled( arg0 ) ; 
		}

		@Override
		public void onChildAdded( DataSnapshot ds, String arg1 )
		{
			notificationsListener.onChildAdded( ds.child( NOTIF_NODE ), arg1 ); 
		}

		@Override
		public void onChildChanged( DataSnapshot ds, String arg1 )
		{
			notificationsListener.onChildChanged( ds.child( NOTIF_NODE ), arg1 ); 
		}

		@Override
		public void onChildMoved( DataSnapshot ds, String arg1 )
		{
			notificationsListener.onChildMoved( ds.child( NOTIF_NODE ), arg1 ); 
		}

		@Override
		public void onChildRemoved( DataSnapshot ds )
		{
			notificationsListener.onChildRemoved( ds.child( NOTIF_NODE ) ); 
		}
	}

	/**
	 * Adds a listener for the events under a sequence
	 * 
	 * @param snapshot	The snapshot returned by {@link FirebaseHelper#addSequenceParentListener(String, ChildEventListener)}
	 * @param eventFBListener
	 * @see FirebaseHelper#addSequenceParentListener(String, ChildEventListener)
	 */
	public static void addEventsListener( DataSnapshot snapshot, ChildEventListener listener )
	{
		snapshot.child( EVENTS_NODE ).getRef( ).addChildEventListener( listener ) ;
	}

	/**
	 * @see	FirebaseHelper#addSequenceParentListener(String, ChildEventListener)
	 */
	public static ProximityEvent convertDataSnapshotToEvent( String merchant, DataSnapshot snapshot )
	{
		GenericTypeIndicator<Map<String, Object>> t = new GenericTypeIndicator<Map<String, Object>>( ) { } ;
		Map<String, Object> userData = snapshot.getValue( t ) ;
		String uuid = ( String ) userData.get( UUID_NAME_KEY ) ;
		int major = ( ( Integer ) userData.get( MAJOR_NAME_KEY ) ).intValue( ) ;
		int minor = ( ( Integer ) userData.get( MINOR_NAME_KEY ) ).intValue( ) ;
		
		SavedBeaconDevice beacon = new SavedBeaconDevice( uuid, major, minor, merchant, null ) ;
		ProximityEvent.Proximity proximity =  ProximityEvent.Proximity.valueOf( ( String ) userData.get( PROXIMITY_NAME_KEY )) ;
		ProximityEvent evt = new ProximityEvent( beacon, proximity ) ;
		return evt ;
	}

	public static String getBeaconName( DataSnapshot ds )
	{ 
		Object val = ds.getValue( ) ;
		if( val != null )
		{
			val = ( ( Map<String, Object> ) val ).get( BEACON_NAME_KEY ) ;
		}
		return ( String ) val ;
	}
}
