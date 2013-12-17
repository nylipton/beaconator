package com.example.bluetoothanalyzer;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

public class MainActivity 
extends Activity
{
	private ArrayAdapter<String> merchantSpinnerAdapter ;
	
	/** scheduler to run non-UI items (like device scanning) */
	private ExecutorService scheduler = Executors.newSingleThreadExecutor( ) ;

	/** The serialization (saved instance state) Bundle key representing the current tab position. */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item" ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if( BuildConfig.DEBUG ) 
			Log.i( Consts.LOG, "onCreate called: starting analyzer" ) ;
		
		ActionBar actionBar = getActionBar( ) ;
		actionBar.setCustomView( R.layout.actionbar ) ;
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME ) ;
		actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS ) ;
		actionBar.addTab( actionBar.newTab( ).setText( R.string.tab_beacons ).setTabListener( new FragmentTabListener<BeaconsFragment>( this, getText( R.string.tab_beacons ).toString( ), BeaconsFragment.class ) ) ) ;
		actionBar.addTab( actionBar.newTab( ).setText( R.string.tab_events ).setTabListener( new FragmentTabListener<DummySectionFragment>( this, getText( R.string.tab_events ).toString( ), DummySectionFragment.class ) ) ) ;
		actionBar.addTab( actionBar.newTab( ).setText( R.string.tab_notifications ).setTabListener( new FragmentTabListener<DummySectionFragment>( this, getText( R.string.tab_notifications ).toString( ), DummySectionFragment.class ) ) ) ;

		Spinner merchantSpinner = ( Spinner ) actionBar.getCustomView( ).findViewById( R.id.merchantSpinner ) ;
		merchantSpinnerAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item ) ;
		merchantSpinnerAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item ) ;
		merchantSpinner.setAdapter( merchantSpinnerAdapter ) ;
		FirebaseHelper.addMerchantsListener( new MerchantsListener( ) ); // will populate the merchants spinner
		
//		setContentView( R.layout.activity_main ) ;
	}
	
	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		MenuInflater menuInflator = getMenuInflater( ) ;
		menuInflator.inflate( R.menu.main_activity_actions, menu ) ;
		return super.onCreateOptionsMenu( menu ) ;
	}
	 
	 /** Callback on the menu item to add a new merchant */
	 public void addMerchant( MenuItem menuItem )
	 {
		final AlertDialog.Builder builder = new AlertDialog.Builder( this ) ;
		
		builder.setView( getLayoutInflater( ).inflate( R.layout.merchant_entry_dialog, null ) ) ;
		builder.setTitle( getString( R.string.merchantDialogTitle ) ) ;
		builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() 
		{
		    public void onClick(DialogInterface dialog, int id) 
		    {
		    	AlertDialog alertDialog = ( AlertDialog ) dialog ;
		    	EditText merchantInput = ( EditText ) alertDialog.findViewById( R.id.merchantSpinnerDialogField ) ;
		    	Object o = merchantInput.getText( ) ;
		        final String merchantName = o.toString( ) ;
		        if( merchantName != null && merchantName.length( ) > 0 )
		        {
			        // TODO make sure this merchant doesn't already exist
			        scheduler.submit( new Runnable( )
					{
						public void run()
						{
							FirebaseHelper.addMerchantsListener(  merchantName ) ;
						}
					} ) ;
		        }
		    }
		});
		builder.setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener( )
		{
			public void onClick( DialogInterface dialog, int which )
			{
				dialog.cancel( ); 
			}
		} ) ;

		builder.show( ) ;
	 }

	
	/**
	 * Listeners for when there are merchants for the spinner from the backend database
	 * @author daniellipton
	 *
	 */
	class MerchantsListener
	implements ChildEventListener
	{

		@Override
		public void onCancelled( FirebaseError arg0 )
		{
			MainActivity.this.runOnUiThread( new Runnable( )
			{
				public void run()
				{
					merchantSpinnerAdapter.clear( );
				}
			} );
		}

		@Override
		public void onChildAdded( final DataSnapshot ds, String previousChildName )
		{
			Log.i( Consts.LOG, "added a new merchant: " + ds.getValue( String.class ) ) ;
			MainActivity.this.runOnUiThread( new Runnable( )
			{
				public void run()
				{
					merchantSpinnerAdapter.add( ds.getValue( String.class) );
				}
			} );
			
		}

		@Override
		public void onChildChanged( DataSnapshot arg0, String arg1 )
		{
			// TODO Auto-generated method stub
		}

		@Override
		public void onChildMoved( DataSnapshot arg0, String arg1 )
		{
			// TODO Auto-generated method stub
		}

		@Override
		public void onChildRemoved( final DataSnapshot ds )
		{
			MainActivity.this.runOnUiThread( new Runnable( )
			{
				public void run()
				{
					merchantSpinnerAdapter.remove( ds.getValue( ).toString( ) );
				}
			} );
		}
	}

	@Override
	protected void onRestoreInstanceState( Bundle savedInstanceState ) 
	{
		// Restore the previously serialized current tab position.
		if ( savedInstanceState.containsKey( STATE_SELECTED_NAVIGATION_ITEM ) )
			getActionBar( ).setSelectedNavigationItem( savedInstanceState.getInt( STATE_SELECTED_NAVIGATION_ITEM ) ) ;
	}
	
	@Override
	protected void onSaveInstanceState( Bundle outState )
	{
		// Save the selected tab
		outState.putInt( STATE_SELECTED_NAVIGATION_ITEM, getActionBar( ).getSelectedNavigationIndex( ) );
	}
	
	public static class BeaconsFragment
	extends ListFragment
	implements IBeaconConsumer
	{
		/** scheduler to run non-UI items (like device scanning) */
		private ExecutorService scheduler = Executors.newSingleThreadExecutor( ) ;
		private IBeaconManager iBeaconManager ;
		/** adapter of devices */
		private BluetoothDeviceAdapter deviceAdapter ;
		
		@Override
		public void onCreate( Bundle savedInstanceState )
		{
			iBeaconManager = IBeaconManager.getInstanceForApplication( getActivity( ) ) ;
			
			deviceAdapter = new BluetoothDeviceAdapter( getActivity( ), R.layout.list_mobile ) ;
			deviceAdapter.add( FoundDevice.getTestDevice( ) ) ; //TODO remove this
			deviceAdapter.add( FoundDevice.getTestDevice( ) ) ; //TODO remove this
			setListAdapter( deviceAdapter ) ;
			
			iBeaconManager.bind( this );
			super.onCreate( savedInstanceState );
		}

		@Override
		public void onDestroy()
		{
			iBeaconManager.unBind( this ) ;
			super.onDestroy( );
		}
		
		@Override
		public void onIBeaconServiceConnect()
		{
			if( BuildConfig.DEBUG )
				Log.i( Consts.LOG, "iBeacon service is running" ) ;
				
			iBeaconManager.setRangeNotifier( new RangeNotifier( )
			{
				@Override
				public void didRangeBeaconsInRegion( final Collection<IBeacon> iBeacons, final Region region )
				{
					Iterator<IBeacon> iter = iBeacons.iterator( ) ;

					while( iter.hasNext( ) )
					{
						IBeacon beacon = iter.next( ) ;
						if( BuildConfig.DEBUG )
							Log.d( Consts.LOG, "I see an iBeacon " + beacon.getAccuracy( ) + " meters away."  ) ;
					}
					getActivity( ).runOnUiThread( new Runnable( )
					{
						public void run()
						{
							deviceAdapter.updateBeaconStatus( iBeacons, region );
						}
					} );
				}
			} );
			
			scheduler.submit( new Runnable( )
			{
				public void run()
				{
					try {
						iBeaconManager.startRangingBeaconsInRegion( new Region( "myMonitoringUniqueId", null, null, null ) );
					} catch (RemoteException e) {   
				    	Log.e( Consts.LOG, "exception starting to range beacons", e ) ;
				    } 
				}
			} ) ;
			
//			iBeaconManager.setMonitorNotifier(new MonitorNotifier( )
//			{
//				
//				@Override
//				public void didExitRegion( Region region )
//				{
//					if( BuildConfig.DEBUG )
//						Log.i( Consts.LOG, "I no longer see an iBeacon in region " + region ) ;
//				}
//				
//				@Override
//				public void didEnterRegion( Region region )
//				{
//					if( BuildConfig.DEBUG )
//						Log.i( Consts.LOG, "I just saw an iBeacon in region " + region ) ;
//				}
//				
//				@Override
//				public void didDetermineStateForRegion( int state, Region region )
//				{
//					if( BuildConfig.DEBUG )
//						Log.i( Consts.LOG, "I just switched from seeing/not seeing an iBeacon" ) ;
//				}
//			} ) ;
//			 
//			    try {
//			        iBeaconManager.startMonitoringBeaconsInRegion( new Region( "myMonitoringUniqueId", null, null, null ) ) ;
//			    } catch (RemoteException e) {   
//			    	Log.e( Consts.LOG, e.toString( ) ) ;
//			    } 
		}

		@Override
		public Context getApplicationContext()
		{
			return getActivity( ).getApplicationContext( ) ;
		}

		@Override
		public void unbindService( ServiceConnection connection )
		{
			getActivity( ).unbindService( connection );
		}

		@Override
		public boolean bindService( Intent intent, ServiceConnection connection, int mode )
		{
			return getActivity( ).bindService( intent, connection, mode ) ;
		}
	}
	
	public static class DummySectionFragment extends Fragment 
	{
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	    {
	      TextView textView = new TextView(getActivity());
	      textView.setGravity(Gravity.CENTER);
	      textView.setText( "placeholder_text" ) ;
	      return textView;
	    }
	  }
}
