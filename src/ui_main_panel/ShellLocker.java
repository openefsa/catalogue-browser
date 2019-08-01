package ui_main_panel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
<<<<<<< HEAD
 * This class is used to block the closure of the application if some important
 * operations are processed. Note that you cannot use the close listener of the
 * shell if you use this class
 * 
=======
 * This class is used to block the
 * closure of the application if
 * some important operations are
 * processed. Note that you cannot
 * use the close listener of the shell
 * if you use this class
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
 * @author avonva
 *
 */
public class ShellLocker {
<<<<<<< HEAD

	private static final Logger LOGGER = LogManager.getLogger(ShellLocker.class);

	/**
	 * Set the lock on the shell.
	 * 
	 * @param lock
	 */
	public static void setLock(final Shell shell, final String title, final String message) {

		// save the listener in order to be able to remove it after
		Listener listener = new Listener() {

			@Override
			public void handleEvent(Event event) {
=======
	
	private static final Logger LOGGER = LogManager.getLogger(ShellLocker.class);
	
	/**
	 * Set the lock on the shell.
	 * @param lock
	 */
	public static void setLock( final Shell shell,
			final String title, final String message ) {
		
		// save the listener in order to be able to remove it after
		Listener listener = new Listener() {
			
			@Override
			public void handleEvent( Event event ) {
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380

				// go on with the closure only if
				// the lock is set to false
				event.doit = false;
<<<<<<< HEAD

				MessageBox mb = new MessageBox(shell);
				mb.setText(title);
				mb.setMessage(message);
=======
			
				MessageBox mb = new MessageBox ( shell );
				mb.setText( title );
				mb.setMessage( message );
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
				mb.open();
			}
		};

		// add the listener
<<<<<<< HEAD
		shell.addListener(SWT.Close, listener);

		LOGGER.info("Shell lock set for " + shell);
	}

	/**
	 * Remove the lock from the shell
	 * 
	 * @param shell
	 */
	public static void removeLock(final Shell shell) {

		// remove the old listeners
		Listener[] listeners = shell.getListeners(SWT.Close);

		for (Listener l : listeners) {

			shell.removeListener(SWT.Close, l);

			LOGGER.info("Shell lock removed for " + shell);
=======
		shell.addListener( SWT.Close, listener );
		
		LOGGER.info( "Shell lock set for " + shell );
	}
	
	/**
	 * Remove the lock from the shell
	 * @param shell
	 */
	public static void removeLock ( final Shell shell ) {

		// remove the old listeners
		Listener[] listeners = shell.getListeners( SWT.Close );

		for ( Listener l : listeners ) {
			
			shell.removeListener( SWT.Close, l );

			LOGGER.info( "Shell lock removed for " + shell );
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
		}
	}
}
