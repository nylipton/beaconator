package com.example.bluetoothanalyzer;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.firebase.client.ChildEventListener;
import com.firebase.client.Firebase;
import com.firebase.client.ValueEventListener;
import com.radiusnetworks.ibeacon.IBeacon;

public class FirebaseHelper
{
	/** 
	 * Date format for the datetime this beacon was added into Firebase
	 */
	public final static DateFormat dateFormat = DateFormat.getDateTimeInstance( ) ;
	
	private final static String FIREBASE_URL = "https://beacon-test.firebaseio.com/" ;
	private final static String MERCHANT_NODE = "merchants" ;
	private final static String BEACONS_NODE = "beacons" ;
	private final static String MERCHANT_KEY = "merchant" ;
	private final static String CREATED_KEY = "created" ;
	
	public static Firebase getBeaconNode( IBeacon beacon )
	{
		if( beacon == null )
			throw new IllegalArgumentException( "can't pass null beacon" ) ;
		Firebase fbBeaconsRef = new Firebase( FIREBASE_URL ) ;
		return fbBeaconsRef.child( BEACONS_NODE ).child( beacon.getProximityUuid( ) ).child( Integer.toString( beacon.getMajor( ) ) ).child( Integer.toString( beacon.getMinor( ) ) ) ;
	}
	
	/**
	 * Sticks the beacon into Firebase as <code>beacons/proximityUUID/major/minor</code> with the value of a timestamp
	 * 
	 * @param beacon the beacon to be added
	 */
	public static void addBeacon( IBeacon beacon, String merchantName )
	{
		Map<String, String> beaconEntry = new HashMap<String, String>( ) ;
		beaconEntry.put( MERCHANT_KEY, merchantName ) ;
		beaconEntry.put( CREATED_KEY, dateFormat.format( new Date( ) ) ) ;
		getBeaconNode( beacon ).setValue( beaconEntry );
	}
	
	public static void addBeaconValueListener( IBeacon beacon, ValueEventListener listener )
	{
		Firebase beaconRef = getBeaconNode( beacon ) ;
		beaconRef.addValueEventListener( listener ) ;
	}

	public static void removeBeacon( IBeacon beacon )
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

	public static void addMerchantsListener( String merchantName )
	{
		Firebase fbBaseRef = new Firebase( FIREBASE_URL ) ;
		Firebase newRef = fbBaseRef.child( MERCHANT_NODE ).push( ) ;
		newRef.setValue( merchantName ) ;
		//TODO add more data and a structure around this, like the added time.
	}
}
