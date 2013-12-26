package com.example.bluetoothanalyzer;

import android.util.Log;
import android.widget.ArrayAdapter;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;

/**
 * Listeners for when there are merchants for the spinner from the backend database
 * @author daniellipton
 *
 */
class MerchantsListener
implements ChildEventListener
{
	/**
	 * 
	 */
	private final MainActivity mainActivity;
	private final ArrayAdapter<String> merchantSpinnerAdapter ;
	
	MerchantsListener( MainActivity mainActivity, ArrayAdapter<String> adapter )
	{
		this.mainActivity = mainActivity;
		this.merchantSpinnerAdapter = adapter ;
	}
	
	@Override
	public void onCancelled( FirebaseError arg0 )
	{
		this.mainActivity.runOnUiThread( new Runnable( )
		{
			public void run()
			{
				MerchantsListener.this.merchantSpinnerAdapter.clear( );
			}
		} );
	}

	@Override
	public void onChildAdded( final DataSnapshot ds, String previousChildName )
	{
		Log.i( Consts.LOG, "added a new merchant: " + ds.getValue( String.class ) ) ;
		this.mainActivity.runOnUiThread( new Runnable( )
		{
			public void run()
			{
				MerchantsListener.this.merchantSpinnerAdapter.add( ds.getValue( String.class) );
			}
		} );
		
	}

	@Override
	public void onChildChanged( DataSnapshot arg0, String arg1 )
	{
		// TODO change the merchant name in the list
	}

	@Override
	public void onChildMoved( DataSnapshot arg0, String arg1 )
	{
		// TODO When would this even happen?
	}

	@Override
	public void onChildRemoved( final DataSnapshot ds )
	{
		this.mainActivity.runOnUiThread( new Runnable( )
		{
			public void run()
			{
				MerchantsListener.this.merchantSpinnerAdapter.remove( ds.getValue( ).toString( ) );
			}
		} );
	}
}