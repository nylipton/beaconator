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
class MerchantsFirebaseListener
implements ChildEventListener
{
	/**
	 * 
	 */
	private final MainActivity mainActivity;
	private final ArrayAdapter<String> merchantSpinnerAdapter ;
	
	MerchantsFirebaseListener( MainActivity mainActivity, ArrayAdapter<String> adapter )
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
				MerchantsFirebaseListener.this.merchantSpinnerAdapter.clear( );
			}
		} );
	}

	@Override
	public void onChildAdded( DataSnapshot ds, String previousChildName )
	{
		final String name = ds.getValue( String.class ) ;
		if( BuildConfig.DEBUG )
			Log.d( Consts.LOG, "added a new merchant: " + name ) ;
		this.mainActivity.runOnUiThread( new Runnable( )
		{
			public void run()
			{
				MerchantsFirebaseListener.this.merchantSpinnerAdapter.add( name );
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
				MerchantsFirebaseListener.this.merchantSpinnerAdapter.remove( ds.getValue( ).toString( ) );
			}
		} );
	}
}