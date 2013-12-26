package com.example.bluetoothanalyzer;

/**
 * Some kind of device, like an iBeacon, which can be displayed in the list
 * @author daniellipton
 *
 */
public interface Device
{
	/** describes what kind of device this is */
	public String getTypeString( ) ;
	
	/** the name of this specific device */
	public String getName( ) ;
	
	/** A long description which may wrap multiple lines */
	public String getLongDescription( ) ;
}
