package com.example.bluetoothanalyzer;

import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NotificationsAdapater 
extends ArrayAdapter<NotificationInfo>
{
	private DateFormat df = DateFormat.getTimeInstance( DateFormat.SHORT ) ; 
	
	public NotificationsAdapater( Context context, int resource )
	{
		super( context, resource );
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent )
	{
		NotificationInfo notif = getItem( position ) ;
		
		View rowView = null ;
		TextView titleView, textView, text2View, infoView, timeView ;
		ImageView largeIconView, smallIconView ;
		if( convertView != null )
		{
			rowView = convertView ;
		}
		else
		{
			LayoutInflater inflater = ( LayoutInflater ) getContext( ).getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			rowView = inflater.inflate( R.layout.notification_list_item, parent, false ) ;
		}
		titleView = ( TextView ) rowView.findViewById( R.id.title ) ;
		textView = ( TextView ) rowView.findViewById( R.id.text ) ;
		text2View = ( TextView ) rowView.findViewById( R.id.text2 ) ;
		infoView = ( TextView ) rowView.findViewById( R.id.info ) ;
		largeIconView = ( ImageView ) rowView.findViewById( R.id.notificationLargeIcon ) ;
		smallIconView = ( ImageView ) rowView.findViewById( R.id.notificationSmallIcon ) ;
		View v = rowView.findViewById( R.id.time ) ;
		if( v instanceof ViewStub )
			timeView = ( TextView ) ( ( ViewStub ) v ).inflate( ) ;
		else
			timeView = ( TextView ) v ;
		
		titleView.setText( notif.title ) ;
		textView.setText( notif.message ) ;
		text2View.setText( "text2" ) ; // not used (for now)
		infoView.setText( " " ) ; // not used (for now)
		largeIconView.setImageBitmap( notif.notif.largeIcon ) ;
		smallIconView.setImageResource( notif.notif.icon ) ;
		smallIconView.setVisibility( View.VISIBLE );
		timeView.setText( df.format( new Date( notif.notif.when ) ) ) ;
		return rowView ;
	}
}
