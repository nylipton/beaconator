package com.example.bluetoothanalyzer;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Lists all of the sequences and allows the user to make new ones
 * @author daniellipton
 *
 */
public class SequencesFragment 
extends ListFragment 
implements MerchantListener
{
	private String selectedMerchant = null ;
	private ArrayAdapter<Sequence> adapter ;
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		adapter = new ArrayAdapter<Sequence>( getActivity(), android.R.layout.simple_list_item_1 ) ;
		setListAdapter( adapter ) ;
		
		super.onCreate( savedInstanceState );
	}
	
	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		View view = inflater.inflate( R.layout.notifications_frag, container, false ) ;
		Button newButton = ( Button ) view.findViewById( R.id.newButton ) ;
		newButton.setText( R.string.sequencesNewLink ) ;
		newButton.setOnClickListener( new OnClickListener( )
		{
			public void onClick( View v )
			{
				SequencesFragment.this.createNewSequence( ) ;
			}
		} );
		
		return view ;
	}
	
	@Override
	public void onResume( )
	{
		Spinner merchantSpinner = ( Spinner ) getActivity( ).findViewById( R.id.merchantSpinner ) ;
		String merchant = ( String ) merchantSpinner.getSelectedItem( ) ;
		if( merchant != null )
			selectedMerchantChanged( merchant ) ;
		
		super.onResume( ) ;
	}
	
	protected void createNewSequence( )
	{
		if( selectedMerchant != null )
		{
			Intent intent = new Intent( getActivity( ), CreateSequenceActivity.class ) ;
			intent.putExtra( ( String ) getActivity( ).getText( R.string.MERCHANT_KEY ), ( selectedMerchant ) ) ;
			getActivity( ).startActivity( intent ) ;
		}
		else
		{
			Toast.makeText( getActivity( ), "Please select a merchant", Toast.LENGTH_LONG ).show( ) ;
		}
	}

	@Override
	public void selectedMerchantChanged( String merchantName )
	{
		this.selectedMerchant = merchantName ;
		SequenceFBListener listener = new SequenceFBListener( merchantName ) ;
		FirebaseHelper.addSequenceParentListener( merchantName, listener );
		
		getActivity( ).runOnUiThread( new Runnable( )
		{ 
			public void run()
			{
				adapter.clear( ) ;
			}
		} );
	}

	class SequenceFBListener
	implements ChildEventListener
	{
		String merchant ;
		
		SequenceFBListener( String merchant )
		{
			this.merchant = merchant ;
		}
		
		@Override
		public void onChildAdded( DataSnapshot snapshot, String previousChildName )
		{
			final Sequence seq = new Sequence( ) ;
			seq.setName( snapshot.getName( ) );
			FirebaseHelper.addEventsListener( snapshot, new EventFBListener( merchant, seq ) ) ;
			
			SequencesFragment.this.getActivity( ).runOnUiThread( new Runnable( )
			{ 
				public void run( )
				{
					SequencesFragment.this.adapter.add( seq ) ;
				}
			} );
		}

		//TODO implement these
		public void onCancelled( FirebaseError arg0 ) {}
		public void onChildChanged( DataSnapshot arg0, String arg1 ){}
		public void onChildMoved( DataSnapshot arg0, String arg1 ){}
		public void onChildRemoved( DataSnapshot arg0 ){}
	}
	
	class EventFBListener
	implements ChildEventListener
	{
		private Sequence seq ;
		private String merchant ;
		
		public EventFBListener( String merchant, Sequence seq )
		{
			this.seq = seq ;
			this.merchant = merchant ;
		}
		
		@Override
		public void onChildAdded( DataSnapshot snapshot, String previousChildName )
		{
//			String userName = snapshot.getName( ) ;
	        ProximityEvent evt = FirebaseHelper.convertDataSnapshotToEvent( merchant, snapshot ) ;
	        seq.add( evt ) ;
	        FirebaseHelper.addBeaconValueListener( 
	        		evt.getBeacon( ).getUUID( ), 
	        		evt.getBeacon( ).getMajor( ), 
	        		evt.getBeacon( ).getMinor( ), 
	        		new BeaconFBListener( evt ) ) ; // this is to find out the beacon name
	        SequencesFragment.this.getActivity( ).runOnUiThread( new Runnable( )
			{ 
				public void run( )
				{
					SequencesFragment.this.adapter.notifyDataSetChanged( ) ;
				}
			} );
//	        String where = previousChildName == null ? "at the beginning" : "after " + previousChildName;
//	        System.out.println("User " + userName + " has entered the chat and should appear " + where);
		}

		//TODO implement these
		public void onCancelled( FirebaseError arg0 ) {}
		public void onChildChanged( DataSnapshot arg0, String arg1 ){}
		public void onChildMoved( DataSnapshot arg0, String arg1 ){}
		public void onChildRemoved( DataSnapshot arg0 ){}
	}
	
	class BeaconFBListener
	implements ValueEventListener
	{
		ProximityEvent evt ;
		BeaconFBListener( ProximityEvent evt )
		{
			this.evt = evt ;
		}

		public void onCancelled( FirebaseError arg0 ) {}
		
		@Override
		public void onDataChange( DataSnapshot ds )
		{
			evt.getBeacon( ).setName( FirebaseHelper.getBeaconName( ds ) ) ;
			SequencesFragment.this.getActivity( ).runOnUiThread( new Runnable( )
			{ 
				public void run( )
				{
					SequencesFragment.this.adapter.notifyDataSetChanged( ) ;
				}
			} );
		}
	}
}
