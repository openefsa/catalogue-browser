package ui_main_menu;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public interface MainMenuItem {

	/**
	 * Create the menu item
	 * @param menu
	 * @return
	 */
	public MenuItem create( Menu menu );
	
	/**
	 * Refresh the menu item
	 */
	public void refresh();
}
