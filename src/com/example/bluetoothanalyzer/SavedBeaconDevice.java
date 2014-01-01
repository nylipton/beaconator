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
	private String uuid, merchant, name ;
	private int  major, minor ;
	
	public SavedBeaconDevice( String uuid, int major, int minor, String merchant, String name )
	{
		this.uuid = uuid ;
		this.major = major ;
		this.minor = minor ;
		this.merchant = merchant ;
		this.name = name ;
	}
	
	@Override
	public String getTypeString()
	{
		return "iBeacon" ;
	}

	@Override
	/**
	 * just the nickname
	 */
	public String getName()
	{
		return name ;
	}
	
	public void setName( String name )
	{
		this.name = name ;
	}

	@Override
	/**
	 * nickname: uuid/major/minor
	 */
	public String getLongDescription()
	{
		StringBuffer sb = new StringBuffer( ) ;
		sb.append( Utils.normalizeProximityUUID( uuid ) ) ;
		sb.append( " / major=" ).append( major ).append( " / minor=" ).append( minor ) ;
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
	
	public String getMerchant( )
	{
		return merchant ;
	}
	
	public String getUUID( )
	{
		return uuid ;
	}
	
	public int getMajor( )
	{
		return major ;
	}
	
	public int getMinor( )
	{
		return minor ;
	}
}
