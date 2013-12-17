package com.example.bluetoothanalyzer;

import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import com.radiusnetworks.ibeacon.IBeacon;

/**
 * Represents either an iBeacon or a BluetoothDevice. Frankly I would do a better job abstracting this from those two except that
 * the responsability for doing that right belongs to the iBeacon class, so screw it.
 * 
 * @author daniellipton
 *
 */
@SuppressLint("DefaultLocale")
public class FoundDevice
{
	private IBeacon iBeacon = null ;
	private BluetoothDevice btDevice = null ;
	private DeviceType type ;
	private static int testCountId = 0 ;
	
	public enum DeviceType {
		IBEACON( "iBeacon" ), BT( "Bluetooth LE" ), TEST( "Test device" ) ;
		
		private String s ;
		DeviceType( String s )
		{
			this.s = s ;
		}
		
		public String toString( )
		{
			return s ;
		}
	}
	
	/** Use this if you need a test instance of this class */
	public static synchronized FoundDevice getTestDevice( )
	{
		return new TestDevice( testCountId++ ) ;
	}
	
	private FoundDevice( )
	{
		this.type = DeviceType.TEST ;
	}
	
	public FoundDevice( IBeacon iBeacon )
	{
		this.setiBeacon( iBeacon ) ;
		this.type = DeviceType.IBEACON ;
	}
	
	public FoundDevice( BluetoothDevice btDevice )
	{
		this.setBtDevice( btDevice ) ;
		this.type = DeviceType.BT ;
	}
	
	public String getName( )
	{
		if( type == DeviceType.BT )
			return getBtDevice().getName( ) ;
		else
		{
			String formattedUuidString = "", uuidString = getiBeacon().getProximityUuid( ) ;
			try {
				UUID uuid = UUID.fromString( uuidString ) ;
				formattedUuidString = uuid.toString( ) ;
			} catch( IllegalArgumentException e ) {
				formattedUuidString = "illegal formatted uuid" ;
			}
			return formattedUuidString ;
		}
	}
	
	public DeviceType getType( )
	{
		return type ;
	}
	
	public String getTypeString( )
	{
		return type.toString( ) ;
	}

	public IBeacon getiBeacon()
	{
		return iBeacon;
	}

	public void setiBeacon( IBeacon iBeacon )
	{
		this.iBeacon = iBeacon;
	}

	public BluetoothDevice getBtDevice()
	{
		return btDevice;
	}

	public void setBtDevice( BluetoothDevice btDevice )
	{
		this.btDevice = btDevice;
	}
	
	@Override
	/**
	 * Simply looks at the <code>equals</code> method of the underlying device
	 */
	public boolean equals( Object o )
	{
		if( !( o instanceof FoundDevice ) )
			return false ;
		FoundDevice fd = ( FoundDevice ) o ;
		if( fd.getType( ) != this.getType( ) )
			return false ;
		
		boolean same ;
		if( fd.getType( ).equals( DeviceType.IBEACON ) )
			same = fd.getiBeacon( ).equals( this.getiBeacon( ) ) ;
		else
			same = fd.getBtDevice( ).equals( this.getBtDevice( ) ) ;
		return same ;
	}
	
	@Override
	/**
	 * returns the hashcode of the underlying device
	 */
	public int hashCode( )
	{
		if( getType( ) == DeviceType.IBEACON )
			return getiBeacon( ).hashCode( ) ;
		else
			return getBtDevice( ).hashCode( ) ;
	}
	
	public static class TestDevice
	extends FoundDevice
	{
		int id ;
		
		public TestDevice( int id )
		{
			super( ) ;
			this.id = id ;
		}
		
		@Override
		public String getName()
		{
			return getTypeString( ) + " " + id ;
		}
		
		@Override
		public boolean equals( Object o )
		{
			if( !( o instanceof TestDevice ) )
				return false ;
			return( this.id == ( ( TestDevice ) o ).id ) ;
		}
	}
}
