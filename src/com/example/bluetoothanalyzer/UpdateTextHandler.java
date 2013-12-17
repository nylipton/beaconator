package com.example.bluetoothanalyzer;

import android.widget.TextView;

/**
 * Wrapper to display text in a <class>TextView</class>. This should be a thread-safe way to send text to a view which keeps adding text to new lines. It can be called from any thread. 
 * It should only block momentarily to add the text to an inner queue.
 * 
 * @author daniellipton
 *
 */
public class UpdateTextHandler
{
	TextView tv ;
	
	/**
	 * 
	 * @param activity
	 * @param tv	The <class>TextView</class> which will display the text.
	 */
	protected UpdateTextHandler( TextView tv )
	{
		this.tv = tv ;
	}
	
	/**
	 * Adds text to the <class>TextView</class>
	 * 
	 * @param s	String that you want to display
	 * @return	if this succeeded
	 */
	protected void addText( String s )
	{
		if( s != null )
			addTextOrNull( s ) ;
		else
			throw new IllegalArgumentException( "can't write null to TextView" ) ;

	}
	
	/**
	 * Clears the <class>TextView</class>
	 * 
	 * @return if this succeeded
	 */
	protected void clearText( )
	{
		addTextOrNull( null ) ;
	}
	
	/**
	 * Will write to the end of the <class>TextView</class> either the string or if <code>null</code> will clear the contents of it.
	 * 
	 * @param s The string to add or will clear if <class>null</class>
	 */
	private void addTextOrNull( final String s )
	{
		tv.post( new Runnable( )
		{
			
			@Override
			public void run()
			{
				if( s == null ) 
					tv.setText( "" ) ;
				else 
					tv.setText( tv.getText( ) + "\n" + s ) ;
			}
		} ) ;
	}
}