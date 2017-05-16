package ui_main_panel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * This class is used to block the
 * closure of the application if
 * some important operations are
 * processed.
 * @author avonva
 *
 */
public class ShellLocker {
	
	private static Listener listener;
	
	/**
	 * Set the lock on the shell.
	 * @param lock
	 */
	public static void setLock( final Shell shell,
			final String title, final String message ) {
		
		// save the listener in order to be able to remove it after
		listener = new Listener() {
			
			@Override
			public void handleEvent( Event event ) {

				// go on with the closure only if
				// the lock is set to false
				event.doit = false;
			
				MessageBox mb = new MessageBox ( shell );
				mb.setText( title );
				mb.setMessage( message );
				mb.open();
			}
		};

		// add the listener
		shell.addListener( SWT.Close, listener );
	}
	
	/**
	 * Remove the lock from the shell
	 * @param shell
	 */
	public static void removeLock ( final Shell shell ) {
		
		// remove the old listener
		if ( listener != null )
			shell.removeListener( SWT.Close, listener );
	}
}
