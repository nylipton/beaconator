package com.example.bluetoothanalyzer;

import java.util.ArrayList;

/**
 * Represents a sequence of {@link ProximityEvent}s and overrides {@link #toString()} so this can be used to show
 * what's in this sequence
 * @author daniellipton
 *
 */
public class Sequence 
extends ArrayList<ProximityEvent>
{
	private String name ;
	private static final long serialVersionUID = -1256339513529642506L ;

	@Override
	public String toString( )
	{
		StringBuffer sb = new StringBuffer( ) ;
		sb.append( getName( ) ).append( ": " ) ;
		for( int i = 0 ; i < size( ) ; i++ )
		{
			ProximityEvent evt = get( i ) ;
			sb.append( evt.toString( ) ) ;
			if( i < ( size( ) - 1 ) ) sb.append( "\u2192" ) ;
		}
		return sb.toString( ) ;
	}

	public String getName( )
	{
		return name ;
	}

	public void setName( String name )
	{
		this.name = name;
	}
	
	@Override
	public int hashCode()
	{
		int hash = super.hashCode( ) ;
		hash = HashCodeUtil.hash( hash, getName( ) ) ;
		return hash ;
	}
}
