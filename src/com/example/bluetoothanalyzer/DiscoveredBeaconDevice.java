package com.example.bluetoothanalyzer;

import java.text.NumberFormat;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;

/**
 * An iBeacon which has been discovered.
 * 
 * @author daniellipton
 * @see Beacon
 */
public class DiscoveredBeaconDevice 
implements Device
{
	private Beacon beacon ;
	
	public DiscoveredBeaconDevice( Beacon beacon )
	{
		this.beacon = beacon ;
	}

	@Override
	public String getTypeString()
	{
		return "iBeacon" ;
	}

	@Override
	public String getName()
	{
		return Utils.normalizeProximityUUID( beacon.proximityUUID ) ;
	}

	@Override
	public String getLongDescription()
	{
		StringBuffer sb = new StringBuffer( ) ;
		sb.append( "major = " ).append( beacon.major ) ;
		sb.append( ", minor = " ).append( beacon.minor ) ;
		NumberFormat doubleFormat = NumberFormat.getNumberInstance( ) ;
		doubleFormat.setMaximumFractionDigits( 2 ) ;
		sb.append( "\napproximately " ).append( doubleFormat.format( Utils.computeAccuracy( beacon ) ) ).append( " meters away") ;
		sb.append( "\nproximity category = ") ;
		switch( Utils.computeProximity( beacon ) )
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
		sb.append( "\nrssi = " ).append( beacon.rssi ) ;
		sb.append( ", tx power=" ).append( beacon.measuredPower ) ;
		
		return sb.toString( ) ;
	}

	public Beacon getBeacon( )
	{
		return beacon ;
	}
	
	public void setBeacon( Beacon beacon )
	{
		this.beacon = beacon ;
	}
	
	@Override
	public boolean equals( Object o )
	{
		if( !( o instanceof DiscoveredBeaconDevice ) )
			return false ;
		
		return ( ( ( DiscoveredBeaconDevice ) o ).getBeacon( ).equals( getBeacon( ) ) ) ;
	}
	
	@Override
	public int hashCode()
	{
		return getBeacon( ).hashCode( ) ;
	}
}
