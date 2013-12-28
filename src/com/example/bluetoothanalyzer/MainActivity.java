package com.example.bluetoothanalyzer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


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
		actionBar.addTab( actionBar.newTab( ).setText( R.string.tab_events ).setTabListener( new FragmentTabListener<SequencesFragment>( this, getText( R.string.tab_events ).toString( ), SequencesFragment.class ) ) ) ;
		actionBar.addTab( actionBar.newTab( ).setText( R.string.tab_notifications ).setTabListener( new FragmentTabListener<DummySectionFragment>( this, getText( R.string.tab_notifications ).toString( ), DummySectionFragment.class ) ) ) ;

		final Spinner merchantSpinner = ( Spinner ) actionBar.getCustomView( ).findViewById( R.id.merchantSpinner ) ;
		merchantSpinnerAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item ) ;
		merchantSpinnerAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item ) ;
		merchantSpinner.setAdapter( merchantSpinnerAdapter ) ;
		FirebaseHelper.addMerchantsListener( new MerchantsFirebaseListener( this, merchantSpinnerAdapter) ); // will populate the merchants spinner
		merchantSpinner.setOnItemSelectedListener( new MerchantSpinnerItemSelectedListener( merchantSpinner ) );
	}
	
	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		MenuInflater menuInflator = getMenuInflater( ) ;
		menuInflator.inflate( R.menu.main_activity_actions, menu ) ;
		return super.onCreateOptionsMenu( menu ) ;
	}
	 
	 /**  add a new merchant - called by the menu overflow button */
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
							FirebaseHelper.addMerchant( merchantName ) ;
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
	
	/** Listens for when the merchant spinner has selected a new merchant */
	private final class MerchantSpinnerItemSelectedListener 
	implements AdapterView.OnItemSelectedListener
	{
		private final Spinner merchantSpinner;

		private MerchantSpinnerItemSelectedListener( Spinner merchantSpinner )
		{
			this.merchantSpinner = merchantSpinner;
		}

		@Override
		public void onItemSelected( AdapterView<?> parent, View view, int position, long id )
		{
			String merchantName = ( String ) merchantSpinner.getSelectedItem( ) ;
			Fragment frag = getFragmentManager( ).findFragmentById( android.R.id.content ) ;
			if( frag instanceof MerchantListener )
			{
				MerchantListener listener = ( MerchantListener ) frag ;
				listener.selectedMerchantChanged( merchantName );
			}
		}

		public void onNothingSelected( AdapterView<?> parent ) { } // one merchant should always be selected - hide beacons?
	}

	public static class DummySectionFragment extends Fragment 
	{
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	    {
	      TextView textView = new TextView(getActivity());
	      textView.setGravity(Gravity.CENTER);
	      textView.setText( "placeholder text" ) ;
	      return textView;
	    }
	  }
}
