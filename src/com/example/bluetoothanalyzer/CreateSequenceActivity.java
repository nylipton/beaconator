package com.example.bluetoothanalyzer;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity that will allow users to create new sequences of beacon events (regions) which can be tied to notifications
 * (or theoretically other events).
 * 
 * @author daniellipton
 *
 */
public class CreateSequenceActivity 
extends ListActivity
{
	private String merchant ;
	private TextView sequenceTV ;
	private EditText sequenceNameTV ;
	private Button saveButton, newButton ;
	private List<ProximityEvent> proximityEvents ;
	private ButtonClickListener buttonClickListener = new ButtonClickListener( ) ;
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		this.merchant = getIntent( ).getStringExtra( ( String ) getText( R.string.MERCHANT_KEY ) ) ;
		SequenceDeviceAdapater adapter = new SequenceDeviceAdapater( this, R.layout.sequence_list_item, this ) ;
		adapter.setMerchant( merchant ) ;
		setListAdapter( adapter ) ;
		proximityEvents = new ArrayList<ProximityEvent>( ) ;
		// Show the Up button in the action bar.
		setupActionBar( );
		
		setContentView( R.layout.sequence_frag ) ;
		saveButton = ( Button ) findViewById( R.id.saveButton ) ;
		saveButton.setOnClickListener( buttonClickListener );
		newButton = ( Button ) findViewById( R.id.newButton ) ;
		newButton.setOnClickListener( buttonClickListener );
		sequenceTV = ( TextView ) findViewById( R.id.sequenceDesc ) ;
		sequenceNameTV = ( EditText ) findViewById( R.id.sequenceNameField ) ;
		sequenceNameTV.addTextChangedListener( new TextWatcher( )
		{
			public void onTextChanged( CharSequence s, int start, int before, int count ) {}
			public void beforeTextChanged( CharSequence s, int start, int count, int after ) {}
			
			public void afterTextChanged( Editable s )
			{
				determineSaveNewButtonStates( ) ;
			}
		} );
		sequenceNameTV.setOnFocusChangeListener( new OnFocusChangeListener( )
		{ // this is so the keyboard goes away after the edittext loses focus - shouldn't it do this by default?
			public void onFocusChange( View v, boolean hasFocus )
			{
				if( v.getId( ) == R.id.sequenceNameField && !hasFocus )
				{
					InputMethodManager imm =  (InputMethodManager) getSystemService( Context.INPUT_METHOD_SERVICE);
		            imm.hideSoftInputFromWindow( v.getWindowToken( ), 0 ) ;
				}
			}
		} ) ;
		
		super.onCreate( savedInstanceState );
	}
	
	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar()
	{
		getActionBar( ).setDisplayHomeAsUpEnabled( true );
		getActionBar( ).setTitle( getText( R.string.sequencesNewLink ) );
	}
	
	/** Will set the save and new buttons to enabled or disabled. Make sure to call this on the main ui thread */
	protected void determineSaveNewButtonStates( )
	{
		Editable s = sequenceNameTV.getText( ) ;
		boolean saveEnable = ( s.length( ) > 0 ) && ( proximityEvents.size( ) > 0 ) ;
		saveButton.setEnabled( saveEnable ) ;
		
		boolean newEnable = ( s.length( ) > 0 ) || ( proximityEvents.size( ) > 0 ) ;
		newButton.setEnabled( newEnable ) ;
	}
	
	public void saveSequence( )
	{
		String sequenceName = sequenceNameTV.getText( ).toString( ) ;
		FirebaseHelper.addSequence( sequenceName, proximityEvents ) ;
		
		runOnUiThread( new Runnable( )
		{
			public void run()
			{
				Toast toast = Toast.makeText( CreateSequenceActivity.this, R.string.saveSequenceToast, Toast.LENGTH_SHORT ) ;
				toast.show( ) ;
			}
		} ) ;
		
		finish( ) ;
	}
	
	public void newSequence( )
	{
		runOnUiThread( new Runnable( )
		{
			public void run()
			{
				sequenceTV.setText( "" ) ;
				sequenceNameTV.setText( "" ) ;
				proximityEvents.clear( ) ;
				sequenceTV.setVisibility( View.INVISIBLE );
				determineSaveNewButtonStates( ) ;
			}
		} ) ;
	}
	
	public void addEvent( final ProximityEvent evt )
	{
		runOnUiThread( new Runnable( )
		{
			public void run()
			{
				sequenceTV.setVisibility( View.VISIBLE );
				
				if( sequenceTV.getText( ).length( ) > 0 )
					sequenceTV.append( "\u2192" ) ;
				sequenceTV.append( evt.toString( ) ) ;
				
				proximityEvents.add( evt ) ;
				determineSaveNewButtonStates( ) ;
			}
		} );
	}
	
	/** Listener for the save and new buttons */
	class ButtonClickListener
	implements OnClickListener
	{
		public void onClick( View v )
		{
			if( v == saveButton )
				CreateSequenceActivity.this.saveSequence( ) ;
			else if( v == newButton )
				CreateSequenceActivity.this.newSequence( ); 
		}
		
	}
}
