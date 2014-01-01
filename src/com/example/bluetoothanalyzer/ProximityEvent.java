package com.example.bluetoothanalyzer;

public class ProximityEvent
{
	private SavedBeaconDevice beacon ;
	private Proximity prox ;
	
	public enum Proximity 
	{
		NEAR( "NEA" ), IMMEDIATE( "IMD" ), FAR( "FAR" ) ;
		private String name ;
		Proximity( String name )
		{
			this.name = name ;
		}
		
		public String toString( )
		{
			return name ;
		}
	} ;
	
	public ProximityEvent( SavedBeaconDevice beacon, Proximity prox )
	{
		this.prox = prox ;
		this.beacon = beacon ;
	}
	
	public SavedBeaconDevice getBeacon( )
	{
		return beacon ;
	}
	
	public Proximity getProximity( )
	{
		return prox ;
	}
	
	public String toString( )
	{
		return beacon.getName( ) + ":" + prox ;
	}
}
