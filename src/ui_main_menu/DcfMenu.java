package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import dcf_user.User;
import messages.Messages;
import ui_main_panel.FormDCFLogin;
import utilities.GlobalUtil;

/**
 * class that allows to login and logout from DCF
 * 
 * @author shahaal
 *
 */
public class DcfMenu implements MainMenuItem {

	public static final int DCF_LOGIN_MI = 0;
	public static final int DCF_LOGOUT_MI = 1;
	
	private MenuListener listener;

	private MenuItem loginItem; // login
	private MenuItem logoutItem;// logout

	private Shell shell;
	private MainMenu mainMenu;

	public DcfMenu(MainMenu mainMenu, Menu menu) {
		this.mainMenu = mainMenu;
		this.shell = mainMenu.getShell();
		create(menu);
	}

	/**
	 * Listener called when a button of the menu is pressed
	 * 
	 * @param listener
	 */
	public void setListener(MenuListener listener) {
		this.listener = listener;
	}

	/**
	 * Create the DCF menu in the main menu and its sub menu items
	 * 
	 * @param menu
	 */
	public MenuItem create(Menu menu) {

		Menu dcfMenu = new Menu(menu);
		
		MenuItem dcfItem = new MenuItem(menu, SWT.CASCADE);
		dcfItem.setText(Messages.getString("BrowserMenu.DCFMenuName"));
		dcfItem.setMenu(dcfMenu);

		loginItem = addLoginMI(dcfMenu);
		logoutItem = addLogoutMI(dcfMenu);

		dcfMenu.addListener(SWT.Show, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				refresh();
			}
		});

		return dcfItem;
	}

	/**
	 * Add a menu item which allows seeing the derby licence
	 * 
	 * @param menu
	 */
	private MenuItem addLoginMI(Menu menu) {

		final MenuItem loginItem = new MenuItem(menu, SWT.NONE);
		loginItem.setText(Messages.getString("BrowserMenu.LoginMenuName"));

		loginItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				FormDCFLogin login = new FormDCFLogin(shell, Messages.getString("BrowserMenu.DCFLoginWindowTitle"));

				login.display();
				
				// if wrong credentials return
				if (!login.isValid())
					return;
				
				DcfActions.startLoginThreads(shell, null);

				// disable tools menu until we have
				// obtained the user access level
				// (avoid concurrence editing in db)
				mainMenu.tools.setEnabled(false);

				if (listener != null)
					listener.buttonPressed(loginItem, DCF_LOGIN_MI, null);
			}

		});

		return loginItem;
	}

	/**
	 * Add a menu item which allows seeing the licence of the foodex browser
	 * 
	 * @param menu
	 */
	private MenuItem addLogoutMI(Menu menu) {

		final MenuItem logoutItem = new MenuItem(menu, SWT.NONE);
		logoutItem.setText(Messages.getString("BrowserMenu.LogoutMenuName"));

		logoutItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				// logout the user
				User.getInstance().logout();
				// remove the credentials from the db
				User.getInstance().deleteCredentials();
				// set the instance at null
				User.getInstance().removeUser();
				
				String title=Messages.getString("BrowserMenu.DCFLogoutWindowTitle");
				String msg=Messages.getString("BrowserMenu.DCFLogout.message");
				
				GlobalUtil.showDialog(shell, title, msg, SWT.ICON_INFORMATION);

				if (listener != null)
					listener.buttonPressed(logoutItem, DCF_LOGOUT_MI, null);
			}
		});
		
		return logoutItem;
	}

	/**
	 * Refresh all the menu items contained in the tool menu
	 */
	public void refresh() {
		
		// check if the current catalogue is not empty (has data in it)
		boolean isLoggedIn = User.getInstance().isLoggedIn();
		
		loginItem.setEnabled(!isLoggedIn);
		logoutItem.setEnabled(isLoggedIn);
		
	}
}
