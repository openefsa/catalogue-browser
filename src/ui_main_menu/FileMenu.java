package ui_main_menu;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import catalogue_browser_dao.CatalogueDAO;
import catalogue_object.Catalogue;
import dcf_manager.Dcf;
import dcf_user.User;
import import_catalogue.ImportActions;
import messages.Messages;
import new_local_catalogue.CatalogueCreationActions;
import new_local_catalogue.DuplicatedCatalogueException;
import ui_main_panel.DownloadCatalogueFrom;
import ui_main_panel.FormCataloguesList;
import ui_main_panel.FormLocalCatalogueName;
import ui_main_panel.OldCatalogueReleaseDialog;
import ui_progress_bar.FormProgressBar;
import utilities.GlobalUtil;

/**
 * File menu for the main menu.
 * @author avonva
 *
 */
public class FileMenu implements MainMenuItem {

	// codes to identify the menu items (used for listeners)
	public static final int NEW_CAT_MI = 0;
	public static final int OPEN_CAT_MI = 1;
	public static final int IMPORT_CAT_MI = 2;
	public static final int DOWNLOAD_CAT_MI = 3;
	public static final int CLOSE_CAT_MI = 4;
	public static final int DELETE_CAT_MI = 5;
	public static final int EXIT_MI = 6;
	
	private MenuListener listener;
	
	private MainMenu mainMenu;
	private Shell shell;
	
	private MenuItem newMI;
	private MenuItem openMI;
	private MenuItem importCatMI;
	private MenuItem downloadMI;
	private MenuItem closeMI;
	private MenuItem deleteMI;
	private MenuItem exitMI;
	
	/**
	 * Set the listener to the file menu
	 * @param listener
	 */
	public void setListener(MenuListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Initialize the main file menu passing the main
	 * menu which contains it and the menu in which we
	 * want to create the file menu
	 * @param mainMenu
	 * @param menu
	 */
	public FileMenu( MainMenu mainMenu, Menu menu ) {
		this.mainMenu = mainMenu;
		this.shell = mainMenu.getShell();
		create( menu );
	}
	
	/**
	 * Create a file menu 
	 * @param menu
	 */
	public MenuItem create ( Menu menu ) {
		
		// create FILE Menu and its sub menu items
		Menu fileMenu = new Menu( menu );

		MenuItem fileItem = new MenuItem( menu , SWT.CASCADE );
		fileItem.setText( Messages.getString("BrowserMenu.FileMenuName") );
		fileItem.setMenu( fileMenu );

		newMI = addNewLocalCatMI ( fileMenu );  // new database, only edit

		openMI = addOpenDBMI ( fileMenu );  // open a catalogue
		
		importCatMI = addImportCatalogueMI ( fileMenu );
		
		downloadMI = addDownloadCatalogueMI ( fileMenu );  // download catalogue
		
		closeMI = addCloseCatalogueMI ( fileMenu );
		
		deleteMI = addDeleteCatalogueMI ( fileMenu ); //  delete catalogue
		
		// add separator
		new MenuItem( fileMenu , SWT.SEPARATOR );
		
		exitMI = addExitMI ( fileMenu );  // exit app
		
		fileMenu.addListener( SWT.Show, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				refresh();
			}
		});
		
		return fileItem;
	}
	
	
	/**
	 * Add a "New..." item to the menu, 
	 * which allows to create a new local catalogue
	 * @param menu
	 */
	private MenuItem addNewLocalCatMI ( final Menu menu ) {

		final MenuItem newFileItem = new MenuItem( menu , SWT.NONE );
		newFileItem.setText( Messages.getString("BrowserMenu.NewCatalogueCmd") );
		
		// if the new local catalogue button is pressed
		newFileItem.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				FormLocalCatalogueName dialog = new FormLocalCatalogueName ( menu.getShell() );

				String catalogueCode = dialog.open();
				
				// if null the cancel button was pressed
				if ( catalogueCode == null )
					return;
				
				// set the wait cursor
				GlobalUtil.setShellCursor( menu.getShell() , SWT.CURSOR_WAIT );
				
				
				// create a database for the new catalogue
				// but if the catalogue already exists show an error dialog
				try {
					CatalogueCreationActions.newLocalCatalogue( catalogueCode );
				}
				catch ( DuplicatedCatalogueException exception ) {
					
					GlobalUtil.showErrorDialog( shell, 
							Messages.getString( "BrowserMenu.NewLocalCatErrorTitle" ),
							Messages.getString( "BrowserMenu.NewLocalCatErrorMessage" ) );
					
					GlobalUtil.setShellCursor( menu.getShell() , SWT.CURSOR_ARROW );
					
					return;
				}

				// refresh the menu items, we have opened a catalogue
				// therefore somethings have to be enabled
				mainMenu.refresh();

				// reset the standard cursor
				GlobalUtil.setShellCursor( menu.getShell() , SWT.CURSOR_ARROW );
				
				GlobalUtil.showDialog(shell, 
						Messages.getString("NewLocalCat.DoneTitle"),
						Messages.getString("NewLocalCat.DoneMessage"), 
						SWT.ICON_INFORMATION );
				
				if ( listener != null )
					listener.buttonPressed( newFileItem, NEW_CAT_MI, null );
			}
		} );
		
		return newFileItem;
	}
	

	/**
	 * Add a menu item which allows to download from the DCF a catalogue
	 * @param menu
	 * @return 
	 */
	private MenuItem addDownloadCatalogueMI ( Menu menu ) {
		
		// Item to see and load the catalogues directly from the DCF
		final MenuItem loadCatalogueItem = new MenuItem( menu, SWT.NONE );

		// if button pressed
		loadCatalogueItem.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				try {
					
					// download the catalogue
					DownloadCatalogueFrom action = new DownloadCatalogueFrom();
					
					// show a progress bar
					FormProgressBar progressBar = new FormProgressBar( shell, 
							Messages.getString( "BrowserMenu.ProgressDownloadTitle" ) );
					
					action.setProgressBar( progressBar );
					
					action.addDoneListener( new Listener() {
						
						@Override
						public void handleEvent(Event event) {
							
							GlobalUtil.showDialog(shell, 
									Messages.getString( "BrowserMenu.DownloadSuccessTitle" ),
									Messages.getString( "BrowserMenu.DownloadSuccessMessage" ),
									SWT.ICON_INFORMATION );
						}
					} );
					
					// open download form and possibly download catalogue
					action.display ( shell );

				} catch ( Exception e1 ) {

					String message = Messages.getString("BrowserMenu.DownloadCmdErrorMessage1");

					// if we are sure that there are errors in log in
					// change the message in a more specific one
					// 401 is the code for unauthorized accesses
					if ( e1.getMessage().contains( "401" ) )
						message = Messages.getString("BrowserMenu.DownloadCmdErrorMessage2");

					// call the error listener
					GlobalUtil.showErrorDialog(shell, 
							Messages.getString("BrowserMenu.DownloadCmdErrorTitle"),
							message);
				}
				
				if ( listener != null )
					listener.buttonPressed( loadCatalogueItem, 
							DOWNLOAD_CAT_MI, null );
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		
		return loadCatalogueItem;
	}



	/**
	 * Add a menu item which allows to open a .catalog file which will be
	 * loaded as database
	 * @param menu
	 */
	private MenuItem addOpenDBMI ( Menu menu ) {
		
		final MenuItem openFileItem = new MenuItem( menu , SWT.NONE );
		openFileItem.setText( Messages.getString("BrowserMenu.OpenCatalogueCmd") );

		openFileItem.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				CatalogueDAO catDao = new CatalogueDAO();
				
				ArrayList < Catalogue > myCatalogues = catDao.getLocalCatalogues();
				
				// Order the catalogues by label name to make a better visualization
				Collections.sort( myCatalogues );
				
				// open the form for selecting a catalogue (single selection)
				FormCataloguesList fcl = new FormCataloguesList ( shell,
						Messages.getString("BrowserMenu.OpenCatalogueListTitle"), myCatalogues, false );
				
				// set the ok button text
				fcl.setOkButtonText( Messages.getString("BrowserMenu.OpenSelectedCatalogueCmd") );
				
				String[] columns;
				
				final User user = User.getInstance();
				
				// display only the columns that we want
				if ( user.isCatManager() )
					columns = new String[] {"label", "version", "status", "reserve" };
				else
					columns = new String[] {"label", "version", "status" };
				
				fcl.display( columns );
				
				// listener called when a catalogue is selected
				fcl.addListener( new Listener() {
					
					@Override
					public void handleEvent( Event event ) {
						
						// get the selected catalogue from the listener event
						final Catalogue selectedCat = ( Catalogue ) event.data;
						
						// if the user is logged in we can check the updates
						if ( user.isLogged() ) {
							
							// check if there is a catalogue update
							boolean hasUpdate = selectedCat.hasUpdate();
							
							// check if the update was already downloaded
							boolean alreadyDownloaded = selectedCat.isLastReleaseAlreadyDownloaded();

							// if there is a new version and we have not downloaded it yet
							// we warn the user that a new version is available
							if ( hasUpdate && !alreadyDownloaded ) {
								
								OldCatalogueReleaseDialog dialog = 
										new OldCatalogueReleaseDialog( shell, selectedCat );
								
								dialog.open();
							}
						}
						else {  
							
							// if we are not logged in, simply warn the user that we cannot
							// be sure that this is the last release
							MessageBox mb = new MessageBox( shell, SWT.ICON_INFORMATION );
							mb.setText( Messages.getString("BrowserMenu.CatalogueReleaseInfoTitle") );
							mb.setMessage( Messages.getString("BrowserMenu.CatalogueReleaseInfoMessage") );
							mb.open();
						}
						
						// open the catalogue when the dialog is closed
						openCatalogue ( selectedCat );
						
						if ( listener != null )
							listener.buttonPressed( openFileItem, 
									OPEN_CAT_MI, event );
					}
				});
			}
		} );
		
		return openFileItem;
	}
	
	/**
	 * Add a menu item to import a catalogue database from a ecf file
	 * @param menu
	 * @return
	 */
	private MenuItem addImportCatalogueMI ( final Menu menu ) {
		
		final MenuItem importCatMI = new MenuItem( menu, SWT.PUSH );
		importCatMI.setText( Messages.getString( "BrowserMenu.ImportCatalogueCmd" ) );
		
		importCatMI.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {

				// ask the file to the user
				String filename = GlobalUtil.showFileDialog( menu.getShell(), 
						Messages.getString("BrowserMenu.ImportCatalogueCmd"), 
						new String[] { "*.ecf" } );

				if ( filename == null || filename.isEmpty() )
					return;

				
				// ask for final confirmation
				MessageBox alertBox = new MessageBox( shell, SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION );
				alertBox.setText( Messages.getString("EcfImport.WarnTitle") );
				alertBox.setMessage( Messages.getString( "EcfImport.WarnMessage" ) );
				int val = alertBox.open();

				// return if cancel was pressed
				if ( val == SWT.CANCEL )
					return;
				
				ImportActions importAction = new ImportActions();
				importAction.setProgressBar( new FormProgressBar( shell, "") );
				
				// start the import from the ecf file
				// we save the db where the excel files says (i.e. in the official folder
				// we create a folder with the catalogue code and version which are read from
				// the excel sheet of the catalogue
				importAction.importEcf( null, filename, true, new Listener() {
					
					@Override
					public void handleEvent(Event event) {
						
						// refresh menu items when the import is 
						// finished (needed to refresh open and delete buttons)
						mainMenu.refresh();
						
						GlobalUtil.showDialog(shell, 
								Messages.getString("BrowserMenu.ImportSuccessTitle"),
								Messages.getString( "BrowserMenu.ImportSuccessMessage" ),
								SWT.ICON_INFORMATION );
						
						if ( listener != null )
							listener.buttonPressed( importCatMI, 
									IMPORT_CAT_MI, event );
					}
				} );
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		importCatMI.setEnabled( false );
		
		return importCatMI;
	}
	
	/**
	 * Close the current catalogue (if one is opened)
	 * @param menu
	 * @return
	 */
	private MenuItem addCloseCatalogueMI ( Menu menu ) {
		
		final MenuItem closeCatMI = new MenuItem( menu , SWT.NONE );
		closeCatMI.setText( Messages.getString("BrowserMenu.CloseCatalogueCmd") );
		
		closeCatMI.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				// close the catalogue
				closeCatalogue();
				
				if ( listener != null )
					listener.buttonPressed( closeCatMI, 
							CLOSE_CAT_MI, null );
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		return closeCatMI;
	}
	
	
	/**
	 * Add a menu item which allows to open a .catalog file which will be
	 * loaded as database
	 * @param menu
	 */
	private MenuItem addDeleteCatalogueMI ( Menu menu ) {
		
		final MenuItem deleteCatMI = new MenuItem( menu , SWT.NONE );
		deleteCatMI.setText( Messages.getString("BrowserMenu.DeleteCatalogueCmd") );

		deleteCatMI.addSelectionListener( new SelectionAdapter() {
			
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				final CatalogueDAO catDao = new CatalogueDAO();
				
				ArrayList < Catalogue > myCatalogues = catDao.getLocalCatalogues();
				
				// Order the catalogues by label name to make a better visualization
				Collections.sort( myCatalogues );
				
				FormCataloguesList fcl = new FormCataloguesList ( shell, 
						Messages.getString("BrowserMenu.DeleteCatalogueListTitle"), myCatalogues, true );
				
				// set the ok button text
				fcl.setOkButtonText( Messages.getString("BrowserMenu.DeleteSelectedCatalogueCmd") );
				
				// display only the columns that we want
				fcl.display( new String[] {"label", "version", "status"} );
				
				fcl.addListener( new Listener() {
					
					@Override
					public void handleEvent(Event event) {
						
						@SuppressWarnings("unchecked")
						ArrayList<Catalogue> selectedCats = (ArrayList<Catalogue>) event.data;
						
						boolean problems = false;
						
						// remove the catalogues from the database
						for ( Catalogue catalogue : selectedCats ) {
							
							if ( catalogue.isReserved() || catalogue.isRequestingAction() ) {
								problems = true;
								continue; 
							}
							
							System.out.println ( "Deleting catalogue " + catalogue.getCode() );
							catDao.delete( catalogue );
						}
						
						if ( problems ) {
							GlobalUtil.showDialog( shell, 
									Messages.getString( "Delete.ErrorTitle" ), 
									Messages.getString( "Delete.ErrorMessage" ), 
									SWT.ICON_WARNING );
						}
						
						
						if ( listener != null )
							listener.buttonPressed( deleteCatMI, 
									DELETE_CAT_MI, event );
					}
				});
			}
		} );
		
		return deleteCatMI;
	}

	
	/**
	 * Add a menu item which allows exiting the application
	 * @param menu
	 */
	private MenuItem addExitMI ( Menu menu ) {
		
		final MenuItem exitItem = new MenuItem( menu , SWT.NONE );
		exitItem.setText( Messages.getString("BrowserMenu.ExitAppCmd") );
		
		exitItem.addSelectionListener( new SelectionAdapter() {

			public void widgetSelected ( SelectionEvent e ) {
				
				mainMenu.getShell().close();
				
				if ( listener != null )
					listener.buttonPressed( exitItem, 
							EXIT_MI, null );
			}

		} );
		
		return exitItem;
	}
	
	/**
	 * Open the selected catalogue ( the catalogue dao will open the db )
	 * @param catalogue
	 */
	private void openCatalogue ( final Catalogue catalogue ) {
		
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_WAIT );
		
		// open the catalogue in a separate thread
		// since it is time consuming
		shell.getDisplay().syncExec( new Runnable() {
			
			@Override
			public void run() {
				
				// close the previous catalogue
				closeCatalogue();
				
				// open the catalogue
				catalogue.open();
			}
		});

		// refresh menu items
		mainMenu.refresh();
		
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_ARROW );
	}
	
	/**
	 * Open the currently open catalogue (if there is one)
	 */
	private void closeCatalogue () {
		
		if ( mainMenu.getCatalogue() == null )
			return;
		
		mainMenu.getCatalogue().close();
		
		// refresh UI
		mainMenu.refresh();
	}
	
	/**
	 * Refresh all the menu items of the file menu
	 */
	public void refresh () {

		CatalogueDAO catDao = new CatalogueDAO();
		
		// get all the catalogues I have downloaded before and get the size
		boolean hasCatalogues = catDao.getLocalCatalogues().size() > 0;

		// check if there is at least one catalogue available from the 
		// catalogue master table. If not => open disabled
		// can open only if we are not getting updates and we have at least one catalogue downloaded
		openMI.setEnabled( hasCatalogues && !Dcf.isGettingUpdates() && 
				catDao.getLocalCatalogues().size() > 0 );

		// allow import only if no catalogue is opened
		importCatMI.setEnabled( mainMenu.getCatalogue() == null );
		
		User user = User.getInstance();
		
		// we can download only if we know the user access level and
		// if there are some catalogues which can be downloaded
		// we avoid the possibility to download a catalogue while
		// we are checking the user access level since there may be
		// database conflicts!
		boolean canDownload = user.isUserLevelDefined() && 
				Dcf.getDownloadableCat() != null;

		downloadMI.setEnabled ( canDownload );
		
		// enable close only if there is an open catalogue
		closeMI.setEnabled( mainMenu.getCatalogue() != null );

		// enable delete only if we have at least one catalogue downloaded and we
		// have not an open catalogue (to avoid deleting the open catalogue)
		deleteMI.setEnabled( hasCatalogues && mainMenu.getCatalogue() == null );

		// if we are retrieving the catalogues
		if ( Dcf.isGettingUpdates() ) {
			downloadMI.setText( Messages.getString( "BrowserMenu.DownloadingUpdatesCmd" ) );
			openMI.setText( Messages.getString( "BrowserMenu.DownloadingUpdatesCmd" ) );
		}
		// if we are getting the user level
		else if ( user.isGettingUserLevel() ) {
			downloadMI.setText( Messages.getString( "BrowserMenu.GettingUserLevelCmd" ) );
			openMI.setText( Messages.getString( "BrowserMenu.OpenCatalogueCmd" ) );
		}
		else {
			downloadMI.setText( Messages.getString( "BrowserMenu.DownloadCatalogueCmd" ) );
			openMI.setText( Messages.getString( "BrowserMenu.OpenCatalogueCmd" ) );
		}
	}
}
