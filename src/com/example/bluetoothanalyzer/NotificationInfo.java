package com.example.bluetoothanalyzer;

import android.app.Notification;

/**
 * All of the data needed for a notification. I would normally never expose fields publicly, but since this is a
 * demo screw it.
 * @author daniellipton
 *
 */
public class NotificationInfo
{
	public String name, title, message, largeIconURL, merchant, sequenceName, linkURL ;
	
	/** this may be null */
	public Notification notif ;
}
