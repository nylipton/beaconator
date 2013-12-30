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
}
