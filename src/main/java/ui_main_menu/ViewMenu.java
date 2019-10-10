package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import dcf_user.User;
import i18n_messages.CBMessages;

/**
 * View menu in the main menu
 * 
 * @author avonva
 *
 */
public class ViewMenu implements MainMenuItem {

	public static final int EXPAND_MI = 0;
	public static final int COLLAPSE_NODE_MI = 1;
	public static final int COLLAPSE_TREE_MI = 2;

	private MenuListener listener;

	private MainMenu mainMenu;
	private MenuItem viewItem; // the button which shows the view menu

	public ViewMenu(MainMenu mainMenu, Menu menu) {
		this.mainMenu = mainMenu;
		mainMenu.getShell();
		create(menu);
	}

	/**
	 * Set the listener to the menu buttons
	 * 
	 * @param listener
	 */
	public void setListener(MenuListener listener) {
		this.listener = listener;
	}

	/**
	 * Add the view menu
	 * 
	 * @param menu
	 */
	public MenuItem create(Menu menu) {

		viewItem = new MenuItem(menu, SWT.CASCADE);
		viewItem.setText(CBMessages.getString("BrowserMenu.ViewMenuName"));

		Menu editMenu = new Menu(menu);

		addExpandMI(editMenu);
		addCollapseSingleNodeMI(editMenu);
		addCollapseAllMI(editMenu);

		if (User.getInstance().isCatManager())
			addConsoleMI(editMenu);

		viewItem.setMenu(editMenu);

		viewItem.setEnabled(false);

		return viewItem;
	}

	private MenuItem addConsoleMI(Menu menu) {
		MenuItem show = new MenuItem(menu, SWT.NONE);
		show.setText(CBMessages.getString("view.show.user.console"));
		show.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				mainMenu.mainPanel.openUserConsole();
			}
		});

		return show;
	}

	/**
	 * Add menu item which allows to expand a single node and its children
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addExpandMI(Menu menu) {

		final MenuItem expandItem = new MenuItem(menu, SWT.NONE);
		expandItem.setText(CBMessages.getString("BrowserMenu.ExpandNodeCmd"));
		expandItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				// call the button listener if it was set
				if (listener != null)
					listener.buttonPressed(expandItem, EXPAND_MI, null);
			}
		});

		return expandItem;
	}

	/**
	 * Menu item which allows collapsing a single node
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addCollapseSingleNodeMI(Menu menu) {

		final MenuItem collapseSingleItem = new MenuItem(menu, SWT.NONE);
		collapseSingleItem.setText(CBMessages.getString("BrowserMenu.CollapseNodeCmd"));
		collapseSingleItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				// call the button listener if it was set
				if (listener != null)
					listener.buttonPressed(collapseSingleItem, COLLAPSE_NODE_MI, null);
			}
		});

		return collapseSingleItem;
	}

	/**
	 * add a menu item which allows collapsing the entire tree
	 * 
	 * @param menu
	 */
	private MenuItem addCollapseAllMI(Menu menu) {

		final MenuItem collapseItem = new MenuItem(menu, SWT.NONE);
		collapseItem.setText(CBMessages.getString("BrowserMenu.CollapseTreeCmd"));

		collapseItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				// call the button listener if it was set
				if (listener != null)
					listener.buttonPressed(collapseItem, COLLAPSE_TREE_MI, null);
			}
		});

		return collapseItem;
	}

	/**
	 * Refresh the menu
	 */
	public void refresh() {

		if (viewItem.isDisposed())
			return;

		// enable the edit menu only if there is a catalogue open and it is not empty
		viewItem.setEnabled(mainMenu.getCatalogue() != null);
	}
}
