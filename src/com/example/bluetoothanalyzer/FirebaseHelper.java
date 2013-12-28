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

public class FirebaseHelper
{
	/** 
	 * Date format for the datetime this beacon was added into Firebase
	 * @see #CREATED_KEYCREATED
	 */
	public final static DateFormat dateFormat = DateFormat.getDateTimeInstance( ) ;
	
	private final static String FIREBASE_URL = "https://beacon-test.firebaseio.com/" ;
	/** parent for all merchants */
	private final static String MERCHANT_NODE = "merchants" ;
	/** parent for all beacons for this merchant */
	private final static String BEACONS_NODE = "beacons" ;
	/** key for key/val hash that goes in a beacon under minor to represent the created timestamp */
	private final static String CREATED_KEY = "created" ;
	/** key for key/val hash that goes in a beacon under minor to represent the merchant */
	private final static String MERCHANT_KEY = "merchant" ;
	/** key for key/val hash that goes in a beacon under minor to represent the beacon's name */
	private final static String BEACON_NAME_KEY = "name" ;
	
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
		fbBaseRef.child( MERCHANT_NODE ).addChildEventListener( listener ) ;
	}

	/** Add a */
	public static void addMerchantsListener( String merchantName )
	{
		Firebase fbBaseRef = new Firebase( FIREBASE_URL ) ;
		Firebase newRef = fbBaseRef.child( MERCHANT_NODE ).push( ) ;
		newRef.setValue( merchantName ) ;
		//TODO add more data and a structure around this, like the added time.
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
