package ui_main_menu;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import catalogue_object.Catalogue;
import dcf_webservice.DcfResponse;
import dcf_webservice.ReserveLevel;
import global_manager.GlobalManager;
import messages.Messages;
import utilities.GlobalUtil;

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

	private Menu mainMenu;         // the main menu bar
	private FileMenu file;         // file menu
	private ViewMenu view;         // view menu
	ToolsMenu tools;               // tools menu
	private AboutMenu about;       // about menu
	private LoginMenu login;       // login button
	
	Listener openListener;         // listener called when the open button is pressed
	Listener closeListener;        // listener called when the close button is pressed
	Listener updateListener;       // listener called when graphics update needed
	Listener importListener;       // listener called when an import action is finished
	Listener exportListener;       // listener called when an export action is finished
	Listener newCatListener;       // listener called when a new local catalogue is created
	Listener expandNodeListener;   // listener called when expand node button is pressed
	Listener collapseNodeListener; // listener called when collapse node button is pressed
	Listener collapseTreeListener; // listener called when collapse tree button is pressed
	Listener loginListener;        // called when the user access level is determined
	Listener reserveListener;      // called when the user clicks reserve/unreserve
	
	/**
	 * Initialize the main menu
	 * @param shell shell on which creating the menu
	 */
	public MainMenu( Shell shell ) {
		this.shell = shell;
	}
	
	/**
	 * Create the main menu
	 * @param shell
	 * @return
	 */
	public Menu createMainMenu () {
		
		// Add a menu bar => MAIN MENU
		mainMenu = new Menu( shell , SWT.BAR );

		// file menu with new, open, download cat, report, exit...
		file = new FileMenu( this, mainMenu );

		// edit menu with expand, collapse, copy, paste...
		view = new ViewMenu ( this, mainMenu );

		// tools menu with append, import, export, pick-lists, options
		tools = new ToolsMenu ( this, mainMenu );

		// about menu with licenses
		about = new AboutMenu ( this, mainMenu );
		
		// dcf login button
		login = new LoginMenu ( this, mainMenu );
		
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
	 * Call the error listener. Return true if the listener was called
	 * @param strings
	 */
	protected void handleError( String title, String message ) {
		GlobalUtil.showErrorDialog( shell, title, message );
	}
	
	/**
	 * Notify the user of some actions
	 * @param title
	 * @param message
	 */
	protected void warnUser ( String title, String message ) {
		GlobalUtil.showDialog( shell, title, message, SWT.ICON_INFORMATION );
	}
	
	/**
	 * Warn the user based on the dcf response
	 * @param catalogue additional information to show, set to null otherwise
	 * @param response
	 * @param level the reserve level we wanted, set to null if not needed
	 */
	protected void warnDcfResponse ( Catalogue catalogue, DcfResponse response, ReserveLevel level ) {
		
		String preMessage = "";
		
		if ( catalogue != null )
			preMessage = catalogue.getCode() + " - " 
					+ catalogue.getVersion() + ": ";
		
		switch ( response ) {
		
		// if wrong reserve operation notify the user
		case ERROR:
			handleError( Messages.getString( "Reserve.ErrorTitle" ),
					preMessage + Messages.getString( "Reserve.ErrorMessage" ) );
			break;
		
		case BUSY:
			
			// warn user according to the reserve level
			
			if ( level != null && level.greaterThan( ReserveLevel.NONE ) ) {
				warnUser( Messages.getString( "Reserve.BusyTitle" ),
						preMessage + Messages.getString( "Reserve.BusyMessage" ) );
			}
			else {

				if ( level != null ) {
					warnUser( Messages.getString( "Unreserve.BusyTitle" ),
							preMessage + Messages.getString( "Unreserve.BusyMessage" ) );
				}
			}
			break;
			
		// if the catalogue is already reserved
		case NO:
			handleError( Messages.getString( "Reserve.NoTitle" ),
					preMessage + Messages.getString( "Reserve.NoMessage" ) );
			break;

		// if everything went ok
		case OK:
			
			// warn the user that the operation went fine
			warnUser( Messages.getString( "Reserve.OkTitle" ),
					preMessage + Messages.getString( "Reserve.OkMessage" ) );
			
			break;
		default:
			break;
		}
	}

	@Override
	public void update(Observable o, Object arg) {

		// update the selected catalogue if it was changed
		if ( o instanceof GlobalManager )
			this.catalogue = ((GlobalManager) o).getCurrentCatalogue();
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
	 * listener called when graphics update needed
	 * @param updateListener
	 */
	public void addUpdateListener(Listener updateListener) {
		this.updateListener = updateListener;
	}
	
	/**
	 * Set the listener called when the open menu item is clicked
	 * @param openListener
	 */
	public void addOpenListener(Listener openListener) {
		this.openListener = openListener;
	}
	
	/**
	 * Set the listener called when the close catalogue menu item is clicked
	 * @param closeListener
	 */
	public void addCloseListener ( Listener closeListener ) {
		this.closeListener = closeListener;
	}
	
	/**
	 * Set the listener which is called when an import is finished
	 * @param closeListener
	 */
	public void addImportListener ( Listener importListener ) {
		this.importListener = importListener;
	}
	
	/**
	 * Set the listener which is called when an export action is finished
	 * @param exportListener
	 */
	public void addExportListener ( Listener exportListener ) {
		this.exportListener = exportListener;
	}
	
	/**
	 * Set the listener which is called when a new local catalogue is created
	 * @param openListener
	 */
	public void addNewCatListener(Listener newCatListener) {
		this.newCatListener = newCatListener;
	}
	
	/**
	 * Set the listener which is called when expand node button is pressed
	 * in the event data is present the selected term
	 * @param openListener
	 */
	public void addExpandNodeListener(Listener expandNodeListener) {
		this.expandNodeListener = expandNodeListener;
	}
	
	/**
	 * Set the listener which is called when collapse node button is pressed
	 * in the event data is present the selected term
	 * @param openListener
	 */
	public void addCollapseNodeListener(Listener collapseNodeListener) {
		this.collapseNodeListener = collapseNodeListener;
	}
	
	/**
	 * Set the listener which is called when collapse tree button is pressed
	 * @param openListener
	 */
	public void addCollapseTreeListener(Listener collapseTreeListener) {
		this.collapseTreeListener = collapseTreeListener;
	}
	
	/**
	 * Called when the login procedure is finished
	 * @param loginListener
	 */
	public void addLoginListener ( Listener loginListener ) {
		this.loginListener = loginListener;
	}
	
	/**
	 * Add a listener which is called when the user presses
	 * reserve/unreserve in the tool menu.
	 * @param reserveListener
	 */
	public void addReserveListener ( Listener reserveListener ) {
		this.reserveListener = reserveListener;
	}
}
