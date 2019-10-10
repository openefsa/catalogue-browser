package ui_main_panel;

import org.eclipse.swt.widgets.Shell;

/**
 * A class which implements this interface
 * is a UI class which should have a Shell 
 * and a method to update the UI
 * @author avonva
 *
 */
public interface UpdateableUI {

	/**
	 * Get the shell of the class
	 * @return
	 */
	public Shell getShell();
	
	/**
	 * Update the user interface
	 * @param data additional data passed by the caller
	 */
	public void updateUI( Object data );
}
