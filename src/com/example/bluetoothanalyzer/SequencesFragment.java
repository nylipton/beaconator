package com.example.bluetoothanalyzer;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Fragment that will allow users to create new sequences of beacon events (regions) which can be tied to notifications
 * (or theoretically other events).
 * 
 * @author daniellipton
 *
 */
public class SequencesFragment 
extends ListFragment
implements MerchantListener
{
	private TextView sequenceTV ;
	private EditText sequenceNameTV ;
	private Button saveButton, newButton ;
	private List<ProximityEvent> proximityEvents ;
	private ButtonClickListener buttonClickListener = new ButtonClickListener( ) ;
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		SequenceDeviceAdapater adapter = new SequenceDeviceAdapater( getActivity( ), R.layout.sequence_list_item, this ) ;
		setListAdapter( adapter ) ;
		proximityEvents = new ArrayList<ProximityEvent>( ) ;
		
		super.onCreate( savedInstanceState );
	}
	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		View view = inflater.inflate( R.layout.sequence_frag, container, false ) ;
		saveButton = ( Button ) view.findViewById( R.id.saveButton ) ;
		saveButton.setOnClickListener( buttonClickListener );
		newButton = ( Button ) view.findViewById( R.id.newButton ) ;
		newButton.setOnClickListener( buttonClickListener );
		sequenceTV = ( TextView ) view.findViewById( R.id.sequenceDesc ) ;
		sequenceNameTV = ( EditText ) view.findViewById( R.id.sequenceNameField ) ;
		sequenceNameTV.addTextChangedListener( new TextWatcher( )
		{
			public void onTextChanged( CharSequence s, int start, int before, int count ) {}
			public void beforeTextChanged( CharSequence s, int start, int count, int after ) {}
			
			public void afterTextChanged( Editable s )
			{
				determineSaveNewButtonStates( ) ;
			}
		} );
		return view ;
	}
	
	/** Will set the save and new buttons to enabled or disabled. Make sure to call this on the main ui thread */
	protected void determineSaveNewButtonStates( )
	{
		Editable s = sequenceNameTV.getText( ) ;
		boolean enable = ( s.length( ) > 0 ) && ( proximityEvents.size( ) > 0 ) ;
		saveButton.setEnabled( enable ) ;
		newButton.setEnabled( enable ) ;
	}
	
	@Override
	public void onResume()
	{
		super.onResume( ) ;
		
		Spinner merchantSpinner = ( Spinner ) getActivity( ).findViewById( R.id.merchantSpinner ) ;
		String merchant = ( String ) merchantSpinner.getSelectedItem( ) ;
		if( merchant != null )
			selectedMerchantChanged( merchant ) ;
	}

	@Override
	public void selectedMerchantChanged( String merchantName )
	{
		SequenceDeviceAdapater adapter = ( SequenceDeviceAdapater ) getListAdapter( ) ;
		adapter.setMerchant( merchantName ) ;
	}
	
	public void saveSequence( )
	{
		
	}
	
	public void newSequence( )
	{
		getActivity( ).runOnUiThread( new Runnable( )
		{
			public void run()
			{
				sequenceTV.setText( "" ) ;
				sequenceNameTV.setText( "" ) ;
				proximityEvents.clear( ) ;
				sequenceTV.setVisibility( View.INVISIBLE );
			}
		} ) ;
	}
	
	public void addEvent( final ProximityEvent evt )
	{
		getActivity( ).runOnUiThread( new Runnable( )
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
				SequencesFragment.this.saveSequence( ) ;
			else if( v == newButton )
				SequencesFragment.this.newSequence( ); 
		}
		
	}
}
