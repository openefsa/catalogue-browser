package ui_main_menu;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Listener to observe the press actions of
 * menu items.
 * @author avonva
 *
 */
public interface MenuListener {
	
	/**
	 * Function called when a menu item is pressed.
	 * @param button the menu item which was pressed
	 * @param code a unique code which identifies the menuitem
	 * @param event the selection event
	 */
	public void buttonPressed( MenuItem button, int code, Event event );
}
