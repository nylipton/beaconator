package com.example.bluetoothanalyzer;

import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;

public class FragmentTabListener<T extends Fragment> 
implements TabListener
{

	private final Activity activity;
	private final String tabName;
	private final Class<T> clazz;

	public FragmentTabListener( Activity activity, String tag, Class<T> cls )
	{
		this.activity = activity;
		tabName = tag;
		clazz = cls;
	}

	@Override
	public void onTabSelected( Tab tab, FragmentTransaction ft )
	{

		Fragment myFragment = activity.getFragmentManager( ).findFragmentByTag( tabName );

		// Check if the fragment is already initialized
		if ( myFragment == null )
		{
			// If not, instantiate and add it to the activity
			myFragment = Fragment.instantiate( activity, clazz.getName( ) );
			ft.add( android.R.id.content, myFragment, tabName );
		}
		else
		{
			// If it exists, simply attach it in order to show it
			ft.attach( myFragment );
		}

	}

	@Override
	public void onTabUnselected( Tab tab, FragmentTransaction ft )
	{

		Fragment myFragment = activity.getFragmentManager( ).findFragmentByTag( tabName );

		if ( myFragment != null )
		{
			// Detach the fragment, because another one is being attached
			ft.detach( myFragment );
		}

	}

	@Override
	public void onTabReselected( Tab tab, FragmentTransaction ft )
	{
		// TODO Auto-generated method stub

	}
}