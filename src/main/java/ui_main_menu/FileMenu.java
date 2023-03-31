package ui_main_menu;

import java.io.IOException;

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

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import catalogue_generator.ThreadFinishedListener;
import data_collection.DCDAO;
import data_collection.DCTableConfig;
import dcf_manager.Dcf;
import dcf_user.User;
import i18n_messages.CBMessages;
import utilities.GlobalUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * File menu for the main menu.
 * 
 * @author avonva
 *
 */
public class FileMenu implements MainMenuItem {
	
	private static final Logger LOGGER = LogManager.getLogger(FileMenu.class);

	// codes to identify the menu items (used for listeners)
	public static final int NEW_CAT_MI = 0;
	public static final int OPEN_CAT_MI = 1;
	public static final int OPEN_DC_MI = 7;
	public static final int IMPORT_CAT_MI = 2;
	public static final int DOWNLOAD_CAT_MI = 3;
	public static final int DOWNLOAD_DC_MI = 8;
	public static final int CLOSE_CAT_MI = 4;
	public static final int DELETE_CAT_MI = 5;
	public static final int EXIT_MI = 6;

	private MenuListener listener;

	private MainMenu mainMenu;
	private Shell shell;

	private MenuItem openMI;
	private MenuItem openDcMI;
	private MenuItem downloadDcMI;
	private MenuItem importCatMI;
	private MenuItem downloadMI;
	private MenuItem closeMI;
	private MenuItem deleteMI;
	private MenuItem forceRemoveMI;
	private FileActions action;

	/**
	 * Set the listener to the file menu
	 * 
	 * @param listener
	 */
	public void setListener(MenuListener listener) {
		this.listener = listener;
	}

	/**
	 * Initialize the main file menu passing the main menu which contains it and the
	 * menu in which we want to create the file menu
	 * 
	 * @param mainMenu
	 * @param menu
	 */
	public FileMenu(MainMenu mainMenu, Menu menu) {
		this.mainMenu = mainMenu;
		this.shell = mainMenu.getShell();
		create(menu);
	}

	/**
	 * Create a file menu
	 * 
	 * @param menu
	 */
	public MenuItem create(Menu menu) {

		// initialize the action class
		action = new FileActions();
		action.addCloseCatalogueListener(new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				closeCatalogue();
			}
		});

		// create FILE Menu and its sub menu items
		Menu fileMenu = new Menu(menu);

		MenuItem fileItem = new MenuItem(menu, SWT.CASCADE);
		fileItem.setText(CBMessages.getString("BrowserMenu.FileMenuName"));
		fileItem.setMenu(fileMenu);

		// create cat
		addNewLocalCatMI(fileMenu);
		// open cat
		openMI = addOpenDBMI(fileMenu);
		// download cat
		downloadMI = addDownloadCatalogueMI(fileMenu);
		// import cat
		importCatMI = addImportCatalogueMI(fileMenu);
		// close cat
		closeMI = addCloseCatalogueMI(fileMenu);
		// delete cat
		deleteMI = addDeleteCatalogueMI(fileMenu);
		Catalogue cat =  mainMenu.getCatalogue();
		// force remove cat
		if (cat != null && User.getInstance().isCatManagerOf(cat)
				&& !cat.hasUpdate()) {
			forceRemoveMI = addForceRemoveMI(fileMenu);
		}

		// add separator
		new MenuItem(fileMenu, SWT.SEPARATOR);

		addDcMI(fileMenu);

		// add separator
		new MenuItem(fileMenu, SWT.SEPARATOR);

		// add exit option
		addExitMI(fileMenu);

		fileMenu.addListener(SWT.Show, new Listener() {

			@Override
			public void handleEvent(Event event) {
				refresh();
			}
		});

		return fileItem;
	}

	/**
	 * Add a "New..." item to the menu, which allows to create a new local catalogue
	 * 
	 * @param menu
	 */
	private MenuItem addNewLocalCatMI(final Menu menu) {

		final MenuItem newFileItem = new MenuItem(menu, SWT.NONE);
		newFileItem.setText(CBMessages.getString("BrowserMenu.NewCatalogueCmd"));

		// if the new local catalogue button is pressed
		newFileItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {

				// create a new local catalogue
				action.createNewLocalCatalogue(mainMenu.getShell());

				if (listener != null)
					listener.buttonPressed(newFileItem, NEW_CAT_MI, null);
			}
		});

		return newFileItem;
	}

	/**
	 * Add a menu item which allows to download from the DCF a catalogue
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addDownloadCatalogueMI(Menu menu) {

		// Item to see and load the catalogues directly from the DCF
		final MenuItem loadCatalogueItem = new MenuItem(menu, SWT.NONE);

		// if button pressed
		loadCatalogueItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				// initialize the class and download the catalogue
				action.downloadCatalogue(shell);

				if (listener != null)
					listener.buttonPressed(loadCatalogueItem, DOWNLOAD_CAT_MI, null);

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		return loadCatalogueItem;
	}

	/**
	 * Add a menu item which allows to open a .catalog file which will be loaded as
	 * database
	 * 
	 * @param menu
	 */
	private MenuItem addOpenDBMI(Menu menu) {

		final MenuItem openFileItem = new MenuItem(menu, SWT.NONE);
		openFileItem.setText(CBMessages.getString("BrowserMenu.OpenCatalogueCmd"));

		openFileItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {

				action.openCatalogue(shell, new Listener() {

					@Override
					public void handleEvent(Event arg0) {
						// refresh main menu
						mainMenu.refresh();

						if (listener != null) {
							listener.buttonPressed(openFileItem, OPEN_CAT_MI, arg0);
						}
					}
				});
			}
		});

		return openFileItem;
	}

	/**
	 * Add DC cascade menu
	 * 
	 * @author shahaal
	 * @param menu
	 * @return
	 */
	private MenuItem addDcMI(Menu menu) {

		final MenuItem dc = new MenuItem(menu, SWT.CASCADE);
		dc.setText(CBMessages.getString("BrowserMenu.DataCollectionCmd"));

		// Initialize the menu
		final Menu selectDcMenu = new Menu(shell, SWT.DROP_DOWN);

		// add the menu
		dc.setMenu(selectDcMenu);

		// open dc
		openDcMI = addOpenDcMI(selectDcMenu);
		// download dc
		downloadDcMI = addDownloadDcMI(selectDcMenu);

		return dc;
	}

	/**
	 * Open data collection menu item
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addOpenDcMI(Menu menu) {

		final MenuItem openDc = new MenuItem(menu, SWT.NONE);
		openDc.setText(CBMessages.getString("BrowserMenu.OpenDcCmd"));

		openDc.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				// open a data collection and possibly open
				// a configuration
				DCTableConfig config = action.openDC(shell);

				if (config == null)
					return;

				if (listener != null) {
					Event event = new Event();
					event.data = config;
					listener.buttonPressed(openDc, OPEN_DC_MI, event);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		return openDc;
	}

	/**
	 * 
	 * @param menu
	 */
	private MenuItem addDownloadDcMI(Menu menu) {

		final MenuItem downloadDc = new MenuItem(menu, SWT.NONE);
		downloadDc.setText(CBMessages.getString("BrowserMenu.DownloadDcCmd"));

		downloadDc.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {

				// warn the user if logged with openapi
				if (User.getInstance().isLoggedInOpenAPI()) {

					// if user doesn't want to continue return
					MessageBox mb = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					mb.setText(CBMessages.getString("FormDCList.Title"));
					mb.setMessage(CBMessages.getString("FormDCList.WarnOpenapiUser"));

					if (mb.open() != SWT.YES)
						return;
				}
				// open the data collection selection list
				action.downloadDC(shell);
			}
		});

		return downloadDc;
	}

	/**
	 * Add a menu item to import a catalogue database from a ecf file
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addImportCatalogueMI(final Menu menu) {

		final MenuItem importCatMI = new MenuItem(menu, SWT.PUSH);
		importCatMI.setText(CBMessages.getString("BrowserMenu.ImportCatalogueCmd"));

		importCatMI.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				if (listener != null)
					listener.buttonPressed(importCatMI, IMPORT_CAT_MI, null);

				action.importCatalogue(shell, new ThreadFinishedListener() {

					@Override
					public void finished(Thread thread, final int code, Exception e) {

						// refresh menu items when the import is
						// finished (needed to refresh open and delete buttons)
						mainMenu.refresh();

						mainMenu.getShell().getDisplay().asyncExec(new Runnable() {

							@Override
							public void run() {

								String title;
								String msg;
								int icon;

								if (code == ThreadFinishedListener.OK) {
									title = CBMessages.getString("EcfImport.ImportSuccessTitle");
									msg = CBMessages.getString("EcfImport.ImportSuccessMessage");
									icon = SWT.ICON_INFORMATION;
								} else {
									title = CBMessages.getString("EcfImport.ImportErrorTitle");
									msg = CBMessages.getString("EcfImport.ImportErrorMessage");
									icon = SWT.ICON_ERROR;
								}

								GlobalUtil.showDialog(shell, title, msg, icon);
							}
						});
					}
				});
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		importCatMI.setEnabled(false);

		return importCatMI;
	}

	/**
	 * Close the current catalogue (if one is opened)
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addCloseCatalogueMI(Menu menu) {

		final MenuItem closeCatMI = new MenuItem(menu, SWT.NONE);
		closeCatMI.setText(CBMessages.getString("BrowserMenu.CloseCatalogueCmd"));

		closeCatMI.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				closeCatalogue();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		return closeCatMI;
	}

	/**
	 * method used for closing a catalogue
	 * 
	 * @author shahaal
	 * @param closeCatMI
	 */
	protected void closeCatalogue() {

		// return if current cat is null
		if (mainMenu.getCatalogue() == null)
			return;

		// close the catalogue
		mainMenu.getCatalogue().close();

		// refresh UI
		mainMenu.refresh();

		// update the main UI
		if (listener != null)
			listener.buttonPressed(null, CLOSE_CAT_MI, null);

	}

	/**
	 * Add a menu item which allows to open a .catalog file which will be loaded as
	 * database
	 * 
	 * @param menu
	 */
	private MenuItem addDeleteCatalogueMI(Menu menu) {

		final MenuItem deleteCatMI = new MenuItem(menu, SWT.NONE);
		deleteCatMI.setText(CBMessages.getString("BrowserMenu.DeleteCatalogueCmd"));

		deleteCatMI.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {

				// ask and delete catalogues
				action.deleteCatalogues(shell);

				if (listener != null)
					listener.buttonPressed(deleteCatMI, DELETE_CAT_MI, null);
			}
		});

		return deleteCatMI;
	}

	/**
	 * Add the force remove mi, which allows to remove a catalogue from the
	 * database. Note that this button is enabled only for internal version of
	 * catalogues which were reserved!
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addForceRemoveMI(Menu menu) {

		final MenuItem frMI = new MenuItem(menu, SWT.PUSH);

		frMI.setText(CBMessages.getString("BrowserMenu.ForceRemove"));
		frMI.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				// ask for confirmation
				int val = GlobalUtil.showDialog(shell, CBMessages.getString("ForceRemove.ConfirmTitle"),
						CBMessages.getString("ForceRemove.ConfirmMessage"), SWT.YES | SWT.NO);

				if (val == SWT.NO)
					return;

				// reset the catalogue data to the previous version
				try {
					// delete on disk
					Catalogue cat = mainMenu.getCatalogue();
					// close catalogue
					closeCatalogue();
					// delete on disk
					DatabaseManager.deleteDb(cat);
					// delete record on main db
					final CatalogueDAO catDao = new CatalogueDAO();
					catDao.delete(cat);
				} catch (IOException e) {
					LOGGER.error("Error during reset process of catalogue ", e);
					
					GlobalUtil.showErrorDialog(shell, CBMessages.getString("ForceRemove.ErrorTitle"),
							CBMessages.getString("ForceRemove.ErrorMessage"));
				}

				if (listener != null)
					listener.buttonPressed(forceRemoveMI, DELETE_CAT_MI, null);
			}
		});

		frMI.setEnabled(false);

		return frMI;
	}

	/**
	 * Add a menu item which allows exiting the application
	 * 
	 * @param menu
	 */
	private MenuItem addExitMI(Menu menu) {

		final MenuItem exitItem = new MenuItem(menu, SWT.NONE);
		exitItem.setText(CBMessages.getString("BrowserMenu.ExitAppCmd"));

		exitItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				mainMenu.getShell().close();

				if (listener != null)
					listener.buttonPressed(exitItem, EXIT_MI, null);
			}

		});

		return exitItem;
	}

	/**
	 * Refresh all the menu items of the file menu
	 */
	public void refresh() {

		CatalogueDAO catDao = new CatalogueDAO();

		// get all the catalogues I have downloaded before and get the size
		boolean hasCatalogues = catDao.getMyCatalogues(Dcf.dcfType).size() > 0;

		// Return if widget disposed
		if (openMI.isDisposed())
			return;

		// get current catalogue
		Catalogue cat = mainMenu.getCatalogue();
		
		// check if there is at least one catalogue available from the
		// catalogue master table. If not => open disabled
		// can open only if we are not getting updates and we have at least one
		// catalogue downloaded
		openMI.setEnabled(hasCatalogues && !Dcf.isGettingUpdates() && catDao.getMyCatalogues(Dcf.dcfType).size() > 0);

		// allow import only if no catalogue is opened
		importCatMI.setEnabled(cat == null);

		User user = User.getInstance();

		// we can download only if we know the user access level and
		// if there are some catalogues which can be downloaded
		// we avoid the possibility to download a catalogue while
		// we are checking the user access level since there may be
		// database conflicts!
		boolean canDownload = user.isUserLevelDefined() && Dcf.getDownloadableCat() != null;

		downloadMI.setEnabled(canDownload);

		// the openapi users cannot download all catalogues at once (too many server
		// requests)
		downloadDcMI.setEnabled(canDownload); // && !user.isLoggedInOpenAPI() );

		// enable close only if there is an open catalogue
		closeMI.setEnabled(cat != null);

		DCDAO dcDao = new DCDAO();
		openDcMI.setEnabled(!dcDao.getAll().isEmpty());

		// enable delete only if we have at least one catalogue downloaded and we
		// have not an open catalogue (to avoid deleting the open catalogue)
		deleteMI.setEnabled(hasCatalogues && cat == null);

		// if user is cat manager of the catalogue and it is reserved allow force remove
		if (cat != null && user.isCatManagerOf(cat)) {
			// web service request
			if (user.hasPendingRequestsFor(cat)) {
				// editing, we leave these buttons enabled
				if (cat != null && user.hasForcedReserveOf(cat) == null) {
					if (forceRemoveMI != null)
						forceRemoveMI.setEnabled(false);
				}
			} else if (user.canEdit(cat)) {
				// enable forceRemoveMI
				if (forceRemoveMI != null) {
					forceRemoveMI.setEnabled(user.hasReserved(cat.getCode()));
				}
			}
		}

		// if we are retrieving the catalogues
		if (Dcf.isGettingUpdates()) {
			downloadMI.setText(CBMessages.getString("BrowserMenu.DownloadingUpdatesCmd"));
			openMI.setText(CBMessages.getString("BrowserMenu.DownloadingUpdatesCmd"));
			downloadDcMI.setText(CBMessages.getString("BrowserMenu.DownloadingUpdatesCmd"));
		}
		// if we are getting the user level
		else if (user.isGettingUserLevel()) {
			downloadMI.setText(CBMessages.getString("BrowserMenu.GettingUserLevelCmd"));
			openMI.setText(CBMessages.getString("BrowserMenu.OpenCatalogueCmd"));
			downloadDcMI.setText(CBMessages.getString("BrowserMenu.GettingUserLevelCmd"));
		} else {
			downloadDcMI.setText(CBMessages.getString("BrowserMenu.DownloadDcCmd"));
			downloadMI.setText(CBMessages.getString("BrowserMenu.DownloadCatalogueCmd"));
			openMI.setText(CBMessages.getString("BrowserMenu.OpenCatalogueCmd"));
		}
	}
}
