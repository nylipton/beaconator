package com.example.bluetoothanalyzer;

import java.util.Iterator;

import android.app.ListFragment;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;

/**
 * Shows a list of the notifications in the database and allows the user to create new ones
 * 
 * @author daniellipton
 *
 */
public class NotificationsFragment 
extends ListFragment 
implements MerchantListener
{
	private String selectedMerchant = null ;
	private NotificationsAdapater adapter ;
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		adapter = new NotificationsAdapater( getActivity( ), R.layout.notification_list_item ) ;
//		ArrayAdapter<Notification> adapter = new ArrayAdapter<Notification>( getActivity(), android.R.layout.list_content ) ;
		setListAdapter( adapter ) ;
		
		super.onCreate( savedInstanceState ) ;
	}
	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		View view = inflater.inflate( R.layout.notifications_frag, container, false ) ;
		Button newNotificationTV = ( Button ) view.findViewById( R.id.notificationLabel ) ;
		newNotificationTV.setOnClickListener( new OnClickListener( )
		{
			public void onClick( View v )
			{
				NotificationsFragment.this.createNewNotification( ) ;
			}
		} );
		
		return view ;
	}
	
	@Override
	public void onResume()
	{
		super.onResume( ) ;
		
		Spinner merchantSpinner = ( Spinner ) getActivity( ).findViewById( R.id.merchantSpinner ) ;
		String merchant = ( String ) merchantSpinner.getSelectedItem( ) ;
		if( merchant != null )
			selectedMerchantChanged( merchant ) ;
		
		getListView( ).setOnItemClickListener( new OnItemClickListener( )
		{ 
			public void onItemClick( AdapterView<?> parent, View view, int position, long id )
			{
				NotificationInfo notifInfo = ( NotificationInfo ) parent.getItemAtPosition( position ) ;
				if( notifInfo.notif == null )
					new DisplayNotificationTask( getActivity( ) ).execute( notifInfo ) ;
				else
					new DisplayNotificationTask( getActivity() ).onPostExecute( notifInfo.notif ) ; // just call it directly
			}
		} );
		
	}
	
	protected void createNewNotification()
	{
		if( selectedMerchant != null )
		{
			Intent intent = new Intent( getActivity( ), CreateNotificationActivity.class ) ;
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
		
		getActivity( ).runOnUiThread( new Runnable( )
		{ 
			public void run()
			{
				adapter.clear( ) ;
			}
		} );
		
		
		// load all of the notifications for this merchant
		NotificationFBListener listener = new NotificationFBListener( ) ;
		FirebaseHelper.addNotificationParentListener( merchantName, listener ) ;
	}
	
	class NotificationFBListener
	implements ChildEventListener
	{

		@Override
		public void onCancelled( FirebaseError arg0 )
		{
			//TODO clear the contents of the list adapter and warn the user
		}

		@Override
		public void onChildAdded( DataSnapshot ds, String arg1 )
		{
			Iterator<DataSnapshot> notifNodes = ds.getChildren( ).iterator( ) ;
			while( notifNodes.hasNext( ) )
			{
				DataSnapshot notifNode = notifNodes.next( ) ;
				if( BuildConfig.DEBUG )
					Log.d( Consts.LOG, "Found a notification in the database named " + notifNode.getName( ) ) ;
				NotificationInfo notifInfo = FirebaseHelper.convertDataSnapshotToNotification( notifNode ) ;
				new AddNotificationTask( notifInfo, NotificationsFragment.this.getActivity( ) ).execute( notifInfo ) ;
			}
		}

		public void onChildChanged( DataSnapshot arg0, String arg1 ) {}
		public void onChildMoved( DataSnapshot arg0, String arg1 ) {}
		public void onChildRemoved( DataSnapshot ds ) {}
	}
	
	/**
	 * Will attach a constructed {@link Notification} to this {@link NotificationInfo}
	 * @author daniellipton
	 *
	 */
	class AddNotificationTask
	extends DisplayNotificationTask
	{
		private NotificationInfo notifInfo ;
		
		public AddNotificationTask( NotificationInfo notifInfo, Context context )
		{
			super( context );
			this.notifInfo = notifInfo ;
		}
		
		@Override
		protected void onPostExecute( Notification result )
		{
			notifInfo.notif = result ;
			adapter.add( notifInfo ) ;
		}
	}
}
