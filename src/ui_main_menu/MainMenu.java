package ui_main_menu;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import global_manager.GlobalManager;
import ui_main_panel.MainPanel;

/**
 * Main menu of the catalogue browser. The menu bar is subdivided in
 * different sections and allows to perform several editing and non
 * editing operations on the catalogue.
 * @author avonva
 *
 */
public class MainMenu extends Observable implements Observer {

	// the shell which contains the menu
	private Shell shell;
	
	// the current catalogue
	private Catalogue catalogue;

	protected MainPanel mainPanel;
	private Menu mainMenu;         // the main menu bar
	private FileMenu file;         // file menu
	private ViewMenu view;         // view menu
	ToolsMenu tools;               // tools menu
	private AboutMenu about;       // about menu
	private LoginMenu login;       // login button
	
	MenuListener fileListener;
	MenuListener viewListener;
	MenuListener toolsListener;
	MenuListener loginListener;
	
	/**
	 * Initialize the main menu
	 * @param shell shell on which creating the menu
	 */
	public MainMenu(Shell shell, MainPanel panel) {
		this.shell = shell;
		this.mainPanel = panel;
	}
	
	public LoginMenu getLogin() {
		return login;
	}
	
	/**
	 * Create the main menu
	 * @param shell
	 * @return
	 */
	public Menu createMainMenu () {
		
		if ( shell.isDisposed() )
			return null;
		
		// Add a menu bar => MAIN MENU
		mainMenu = new Menu( shell , SWT.BAR );

		// file menu with new, open, download cat, report, exit...
		file = new FileMenu( this, mainMenu );
		file.setListener( fileListener );

		// edit menu with expand, collapse, copy, paste...
		view = new ViewMenu ( this, mainMenu );
		view.setListener( viewListener );

		// tools menu with append, import, export, pick-lists, options
		tools = new ToolsMenu ( this, mainMenu );
		tools.setListener( toolsListener );
		
		// about menu with licenses
		about = new AboutMenu ( this, mainMenu );
		
		// dcf login button
		login = new LoginMenu ( this, mainMenu );
		login.setListener( loginListener );
		
		return mainMenu;
	}
	
	/**
	 * Refresh all the menu
	 */
	public void refresh() {
		
		shell.getDisplay().asyncExec( new Runnable() {
			
			@Override
			public void run() {
				
				// refresh the state of elements
				file.refresh();
				view.refresh();
				tools.refresh();
				about.refresh();
				login.refresh();
			}
		});
	}
	
	
	/**
	 * Get the shell related to the main menu
	 * @return
	 */
	public Shell getShell() {
		return shell;
	}
	
	/**
	 * Get the current catalogue
	 * @return
	 */
	public Catalogue getCatalogue() {
		return catalogue;
	}
	
	/**
	 * Refresh the catalogue for the main menu
	 * @param catalogue
	 */
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public void update(Observable o, Object arg) {

		// update the selected catalogue if it was changed
		if ( o instanceof GlobalManager )
			this.catalogue = ((GlobalManager) o).getCurrentCatalogue();
		
		if ( arg instanceof Catalogue )
			this.catalogue = (Catalogue) arg;
	}
	
	/**
	 * Update the menu that something
	 * has changed in the sub menus
	 * @param data data to be passed to observers
	 */
	public void update ( Object data ) {
		setChanged();
		notifyObservers( data );
	}
	
	/**
	 * Update the menu that something
	 * has changed in the sub menus
	 */
	public void update () {
		update( null );
	}
	
	/**
	 * Listener to observe file menu items
	 * @param fileListener
	 */
	public void setFileListener(MenuListener fileListener) {
		this.fileListener = fileListener;
	}
	
	/**
	 * Listener to observe view menu items
	 * @param viewListener
	 */
	public void setViewListener(MenuListener viewListener) {
		this.viewListener = viewListener;
	}
	
	/**
	 * Listener to observe tools menu item
	 * @param toolsListener
	 */
	public void setToolsListener(MenuListener toolsListener) {
		this.toolsListener = toolsListener;
	}
	
	/**
	 * Listener to observe the login menu item
	 * @param loginListener
	 */
	public void setLoginListener(MenuListener loginListener) {
		this.loginListener = loginListener;
	}
}
