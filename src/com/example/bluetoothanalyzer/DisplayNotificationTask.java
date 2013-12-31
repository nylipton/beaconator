package com.example.bluetoothanalyzer;

import java.io.InputStream;
import java.net.URL;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

/** 
 * Will display a notification. The reason this is in an <code>AsyncTask</code> is because it downloads the icon image.<p>
 * {@link #onPostExecute(Notification)} will display the notification. 
 * */
public class DisplayNotificationTask 
extends AsyncTask<NotificationInfo, Void, Notification>
{
	private static int notificationId = 4783516 ;
	private Context context ;
	
	public DisplayNotificationTask( Context context )
	{
		this.context = context ;
	}
	
	@Override
	protected Notification doInBackground( NotificationInfo... params )
	{
		NotificationInfo notif = params[0] ;
		Intent resultIntent = new Intent( Intent.ACTION_VIEW, Uri.parse( notif.linkURL ) ) ;
		TaskStackBuilder tsb = TaskStackBuilder.create( context ) ;
		tsb.addNextIntent( resultIntent ) ;
		PendingIntent resultPendingIntent = tsb.getPendingIntent( 0, PendingIntent.FLAG_UPDATE_CURRENT ) ;
		
		String url = notif.largeIconURL ; 
		Bitmap largeIcon = null ;
		try
		{
			InputStream iStream = ( InputStream ) new URL( url ).getContent( ) ;
			Bitmap bm = BitmapFactory.decodeStream( iStream ) ;
			int height = (int) context.getResources( ).getDimension(android.R.dimen.notification_large_icon_height);
			int width = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_width);
			Log.i( Consts.LOG, "large notification icon width="+width+" and height="+ height ) ;
			largeIcon = Bitmap.createScaledBitmap( bm, width, height, true ) ;
		}
		catch( Exception e )
		{
			Log.e( Consts.LOG, "Couldn't get large icon from: " + url, e ) ;
		}
		
		Notification.Builder builder =  new Notification.Builder( context )
		.setContentTitle( notif.title )
		.setContentText( notif.message )
		.setOngoing( false )
		.setSmallIcon( R.drawable.ic_launcher )
		.setLargeIcon( largeIcon )
		.setTicker( notif.title )
		.setContentIntent( resultPendingIntent ) ;
		
		return builder.build( ) ;
	}

	@Override
	protected void onPostExecute( Notification result )
	{
		NotificationManager mNotificationManager = ( NotificationManager ) context.getSystemService( Context.NOTIFICATION_SERVICE ) ;
		mNotificationManager.notify( notificationId, result ) ;
	}
}
