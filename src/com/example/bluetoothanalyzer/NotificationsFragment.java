package com.example.bluetoothanalyzer;

import android.app.ListFragment;
import android.app.Notification;
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
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
//		NotificationsAdapater adapter = new NotificationsAdapater( getActivity( ), R.layout.notification_list_item ) ;
		ArrayAdapter<Notification> adapter = new ArrayAdapter<Notification>( getActivity(), android.R.layout.list_content ) ;
		setListAdapter( adapter ) ;
		super.onCreate( savedInstanceState ) ;
		//TODO load the data from the database
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
	}
}
