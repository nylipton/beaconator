package com.example.bluetoothanalyzer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ListFragment;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.BeaconManager.RangingListener;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;


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
	{
		private static final int REQUEST_ENABLE_BT = 1234 ;
		private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
		private static final com.estimote.sdk.Region ALL_ESTIMOTE_BEACONS = new Region( ESTIMOTE_PROXIMITY_UUID, null, null ) ;
		private BeaconManager beaconManager = null ;
		private BluetoothDeviceAdapter deviceAdapter = null ;
		
		@Override
		public void onCreate( Bundle savedInstanceState )
		{	
			deviceAdapter = new BluetoothDeviceAdapter( getActivity( ), R.layout.list_mobile ) ;
			setListAdapter( deviceAdapter ) ;
			
			beaconManager = new BeaconManager( getActivity( ) ) ;
			beaconManager.setRangingListener( new RangingListener( )
			{
				
				@Override
				public void onBeaconsDiscovered( final Region region, final List<Beacon> beacons )
				{
					for( int i = 0 ; i < beacons.size( ) ; i++ )
					{
						Beacon beacon = beacons.get( i ) ;
						if( BuildConfig.DEBUG )
							Log.d( Consts.LOG, "I see an iBeacon " + Utils.computeAccuracy( beacon ) + " meters away."  ) ;
						getActivity( ).runOnUiThread( new Runnable( )
						{
							public void run()
							{
								deviceAdapter.updateBeaconStatus( beacons, region ) ;
							}
						} );
					}
				}
			} ) ;
			
			super.onCreate( savedInstanceState );
		}
		
		@Override
		public void onStart()
		{
			super.onStart( );

			// Check if device supports Bluetooth Low Energy.
			if ( !beaconManager.hasBluetooth( ) )
			{
				Toast.makeText( getActivity( ), "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG ).show( );
				return;
			}

			// If Bluetooth is not enabled, let user enable it.
			if ( !beaconManager.isBluetoothEnabled( ) )
			{
				Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
				startActivityForResult( enableBtIntent, REQUEST_ENABLE_BT );
			}
			else
			{
				connectToService( ) ;
			}
		}
		
		@Override
		public void onActivityResult( int requestCode, int resultCode, Intent data )
		{
			if ( requestCode == REQUEST_ENABLE_BT )
			{
				if ( resultCode == Activity.RESULT_OK )
				{
					connectToService( );
				}
				else
				{
					Toast.makeText( getActivity( ), "Bluetooth not enabled", Toast.LENGTH_LONG ).show( );
				}
			}
			super.onActivityResult( requestCode, resultCode, data );
		}
		
		 private void connectToService()
		{
			beaconManager.connect( new BeaconManager.ServiceReadyCallback( )
			{
				
				@Override
				public void onServiceReady()
				{
					try
					{
						beaconManager.startRanging( ALL_ESTIMOTE_BEACONS );
					}
					catch( RemoteException e )
					{
						Toast.makeText(getActivity( ), "Cannot start ranging, something terrible happened", Toast.LENGTH_LONG).show();
						if( BuildConfig.DEBUG)
							Log.e(Consts.LOG, "Cannot start ranging", e);
					}
					
				}
			} );
			
		}

		@Override
		 public void onStop( ) 
		 {
		    try {
		      beaconManager.stopRanging( ALL_ESTIMOTE_BEACONS ) ;
		    } catch (RemoteException e) {
		    	if( BuildConfig.DEBUG )
		    		Log.d(Consts.LOG, "Error while stopping ranging", e ) ;
		    }

		    super.onStop();
		  }

		@Override
		public void onDestroy()
		{
			beaconManager.disconnect( ) ;
			super.onDestroy( );
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
