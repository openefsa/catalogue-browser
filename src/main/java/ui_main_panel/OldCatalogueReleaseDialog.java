package ui_main_panel;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import i18n_messages.CBMessages;

/**
 * Dialog to warn the user that a old version of the selected catalogue is used
 * @author avonva
 *
 */
public class OldCatalogueReleaseDialog {

	private Listener continueListener;   // called if the continue button is pressed
	private Listener cancelListener;     // called if the cancel button is pressed
	private MessageDialog dialog;
	
	private int buttonPressed;


	/**
	 * Set the continue listener for the continue button
	 * @param continueListener
	 */
	public void setContinueListener(Listener continueListener) {
		this.continueListener = continueListener;
	}

	/**
	 * set the cancel listener for the cancel button
	 * @param cancelListener
	 */
	public void setCancelListener(Listener cancelListener) {
		this.cancelListener = cancelListener;
	}

	/**
	 * Constructor, instantiate the message dialog and its listener
	 * @param shell
	 * @param selectedCat
	 */
	public OldCatalogueReleaseDialog( final Shell shell, final Catalogue selectedCat ) {


		// set the text elements
		String[] commands = new String[] { 
				CBMessages.getString("OldCatalogueReleaseDialog.OkCmd"), 
				CBMessages.getString("OldCatalogueReleaseDialog.CancelCmd") };
		String title = CBMessages.getString("OldCatalogueReleaseDialog.Title");
		String text = CBMessages.getString("OldCatalogueReleaseDialog.Message");

		// create the dialog
		dialog = new MessageDialog( shell, title, null, text, 
				MessageDialog.WARNING, commands, 0 ) {

			// create the listener
			protected void buttonPressed( int buttonId ) {

				setReturnCode(buttonId);

				Event e = new Event();
				e.data = selectedCat;

				// actions based on the pressed button
				switch( buttonId ) {
				case 0:
					buttonPressed = SWT.OK;
					if ( continueListener != null )
						continueListener.handleEvent( e );
					break;
				case 1:
					buttonPressed = SWT.CANCEL;
					if ( cancelListener != null )
						cancelListener.handleEvent( e );
					break;
				}

				// close the dialog
				close();
			}};
	}

	/**
	 * Open the dialog
	 * @return 
	 */
	public int open() {
		return dialog.open();
	}

	public int getButtonPressed() {
		return buttonPressed;
	};
	
	/**
	 * Close the dialog
	 */
	public void close() {
		dialog.close();
	}
}
