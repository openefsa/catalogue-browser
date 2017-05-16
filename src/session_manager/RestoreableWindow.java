package session_manager;

import org.eclipse.swt.widgets.Shell;

/**
 * Interface to be used in order to save the shell
 * dimensions on the database. You can use the
 * {@link WindowPreference} class to save and restore
 * the window settings of a {@link RestoreableWindow} class.
 * Use {@link WindowPreference#restore} to 
 * restore the settings of a previously saved window and use
 * {@link WindowPreference#saveOnClosure(RestoreableWindow)} to
 * save the settings of a window when it is closed
 * @author avonva
 *
 */
public interface RestoreableWindow {

	/**
	 * Code to identify the window. Note that
	 * if two windows have the same code, some
	 * overwrites can happen.
	 * @return
	 */
	public String getWindowCode();
	
	/**
	 * Return the shell we want to save.
	 * @return
	 */
	public Shell getWindowShell();
}
