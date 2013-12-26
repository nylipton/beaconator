package com.example.bluetoothanalyzer;

import com.estimote.sdk.Utils;


/**
 * This beacon was discovered and saved in the database.<p>
 * Note that there really shouldn't be a <code>SavedBeaconDevice<code> and a
 * <code>DiscoveredBeaconDevice</code> at the same time that refer to the same device (i.e.
 * uuid/major/minor). 
 * @author daniellipton
 *
 */
public class SavedBeaconDevice 
implements Device
{
	@SuppressWarnings("unused")
	private String uuid, merchant ;
	private int  major, minor ;
	
	public SavedBeaconDevice( String uuid, int major, int minor, String merchant )
	{
		this.uuid = uuid ;
		this.major = major ;
		this.minor = minor ;
		this.merchant = merchant ;
	}
	
	@Override
	public String getTypeString()
	{
		return "iBeacon" ;
	}

	@Override
	public String getName()
	{
		return Utils.normalizeProximityUUID( uuid ) ;
	}

	@Override
	public String getLongDescription()
	{
		StringBuffer sb = new StringBuffer( ) ;
		sb.append( "major = " ).append( major ) ;
		sb.append( ", minor = " ).append( minor ) ;
		return sb.toString( ) ;
	}

	public boolean equals( Object o )
	{
		if( !( o instanceof SavedBeaconDevice ) || o == null )
			return false ;
		SavedBeaconDevice device = ( SavedBeaconDevice ) o ;

		return ( device.uuid.equals( this.uuid )
				&& ( device.major == ( this.major ) )
				&& ( device.minor == ( this.minor ) ) ) ;
	}
	
	public int hashCode( )
	{
		int result = HashCodeUtil.SEED ;
		result = HashCodeUtil.hash( result, uuid ) ;
		result = HashCodeUtil.hash( result,  major ) ;
		result = HashCodeUtil.hash( result,  minor ) ;
		
		return result ;
	}
	
	/**
	 * Is this saved beacon referring to the same device?
	 */
	public boolean isSame( DiscoveredBeaconDevice beacon )
	{
		return ( beacon != null 
				&& beacon.getBeacon( ).proximityUUID.equals( this.uuid )
				&& beacon.getBeacon( ).major == this.major
				&& beacon.getBeacon( ).minor == this.minor ) ;
	}
}
