package com.example.bluetoothanalyzer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;

public class CreateNotificationActivity 
extends Activity
{
	private String merchant ;
	private EditText nameField, titleField, messageField ;
	private ArrayAdapter<String> sequenceSpinnerAdapter ;
	private Spinner sequenceSpinner ;
	
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState ) ;
		this.merchant = getIntent( ).getStringExtra( ( String ) getText( R.string.MERCHANT_KEY ) ) ;
		setContentView( R.layout.notification_creator ) ;
		// Show the Up button in the action bar.
		setupActionBar( );
		
		messageField = ( EditText ) findViewById( R.id.notificationMessageField ) ;
		titleField = ( EditText ) findViewById( R.id.notificationTitleField ) ;
		nameField = ( EditText ) findViewById( R.id.notificationNameField ) ;
		sequenceSpinner = ( Spinner ) findViewById( R.id.notificationSequencePicker ) ;
		sequenceSpinnerAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item ) ;
		sequenceSpinnerAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item ) ;
		sequenceSpinner.setAdapter( sequenceSpinnerAdapter ) ;
		
		FirebaseHelper.addSequenceParentListener( merchant, new SequenceListener( ) );
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar()
	{
		getActionBar( ).setDisplayHomeAsUpEnabled( true );
		getActionBar( ).setTitle( merchant + " " + getText( R.string.notificationWord ) );
	}

	public void createNotification( View view )
	{
		if( nameField.getText( ).length( ) <= 0 )
			nameField.setError( getText( R.string.blank_field_error ) );
		else if( titleField.getText( ).length( ) <= 0 )
			titleField.setError( getText( R.string.blank_field_error ) );
		else if( messageField.getText( ).length( ) <= 0 )
			messageField.setError( getText( R.string.blank_field_error ) );
		else
		{
			NotificationInfo notif = new NotificationInfo( ) ;
			notif.largeIconURL = "http://www.storebrandsdecisions.com/upload/images/news_images/cvs/cvs_logo_web150.jpg" ;
			notif.title = titleField.getText( ).toString( ) ;
			notif.name = nameField.getText( ).toString( ) ;
			notif.message = messageField.getText( ).toString( ) ;
			notif.linkURL = "https://www.google.com/search?q=" + merchant ;
			notif.merchant = merchant ;
			notif.sequenceName = ( String ) sequenceSpinner.getSelectedItem( ) ;
//			new DisplayNotificationTask( this ).execute( notif ) ;
			
			FirebaseHelper.addNotification( notif ) ; // save it in the database, probably should be off the UI thread
			finish( ) ;
			
		}
	}
	
	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId( ) )
		{
			case android.R.id.home:
				// This ID represents the Home or Up button. In the case of this
				// activity, the Up button is shown. Use NavUtils to allow users
				// to navigate up one level in the application structure. For
				// more details, see the Navigation pattern on Android Design:
				//
				// http://developer.android.com/design/patterns/navigation.html#up-vs-back
				//
//				NavUtils.navigateUpFromSameTask( this );
				return true;
		}
		return super.onOptionsItemSelected( item );
	}
	
	class SequenceListener
	implements ChildEventListener
	{
		@Override
		public void onCancelled( FirebaseError arg0 )
		{
			CreateNotificationActivity.this.runOnUiThread( new Runnable( )
			{
				public void run()
				{
					CreateNotificationActivity.this.sequenceSpinnerAdapter.clear( );
					Toast.makeText( CreateNotificationActivity.this, "Lost connection to database", Toast.LENGTH_LONG ).show( ) ;
				}
			} );
		}

		@Override
		public void onChildAdded( DataSnapshot ds, String previousChildName )
		{
			final String name = ds.getName( ) ;
			if( BuildConfig.DEBUG )
				Log.d( Consts.LOG, "added a new sequence: " + name ) ;
			CreateNotificationActivity.this.runOnUiThread( new Runnable( )
			{
				public void run()
				{
					CreateNotificationActivity.this.sequenceSpinnerAdapter.add( name );
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
			CreateNotificationActivity.this.runOnUiThread( new Runnable( )
			{
				public void run()
				{
					CreateNotificationActivity.this.sequenceSpinnerAdapter.remove( ds.getValue( ).toString( ) );
				}
			} );
		}
		
	}
}
