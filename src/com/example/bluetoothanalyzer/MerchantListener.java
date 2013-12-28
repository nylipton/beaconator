package com.example.bluetoothanalyzer;

/**
 * Listener for when the selected merchant is changed
 * @author daniellipton
 *
 */
public interface MerchantListener
{
	/** called when the selected merchant has changed. 
	 * 
	 * @param merchantName The selected merchant or <code>null</code> if none is currently selected
	 */
	public void selectedMerchantChanged( String merchantName ) ;
}
