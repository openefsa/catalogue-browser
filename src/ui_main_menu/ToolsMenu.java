package ui_main_menu;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import already_described_terms.Picklist;
import already_described_terms.PicklistDAO;
import already_described_terms.PicklistParser;
import already_described_terms.PicklistTerm;
import catalogue.Catalogue;
import catalogue_browser_dao.DatabaseManager;
import catalogue_generator.ThreadFinishedListener;
import catalogue_object.Term;
import dcf_user.User;
import export_catalogue.ExportActions;
import ict_add_on.ICTInstaller;
import ict_add_on.ICTDownloader;
import import_catalogue.CatalogueImporter.ImportFileFormat;
import import_catalogue.CatalogueImporterThread;
import import_catalogue.ImportException;
import messages.Messages;
import progress_bar.FormProgressBar;
import sas_remote_procedures.XmlUpdateFile;
import sas_remote_procedures.XmlUpdateFileDAO;
import sas_remote_procedures.XmlUpdatesFactory;
import soap.DetailedSOAPException;
import soap.UploadCatalogueFileImpl.PublishLevel;
import soap.UploadCatalogueFileImpl.ReserveLevel;
import ui_console.ConsoleMessage;
import ui_console.ConsoleMessageFactory;
import ui_general_graphics.DialogSingleText;
import ui_main_panel.AttributeEditor;
import ui_main_panel.HierarchyEditor;
import user_preferences.CataloguePreferenceDAO;
import user_preferences.FormSearchOptions;
import user_preferences.FormUserPreferences;
import utilities.GlobalUtil;

/**
 * create the main menu for tools
 * 
 * @author shahaal
 * @author avonva
 *
 */
public class ToolsMenu implements MainMenuItem {

	public static final int RESERVE_CAT_MI = 0;
	public static final int UNRESERVE_CAT_MI = 1;
	public static final int UPLOAD_DATA_MI = 2;
	public static final int PUBLISH_CAT_MI = 3;
	public static final int RESET_CAT_MI = 4;
	public static final int IMPORT_CAT_MI = 5;
	public static final int EXPORT_CAT_MI = 6;
	public static final int IMPORT_PICKLIST_MI = 7;
	public static final int FAV_PICKLIST_MI = 8;
	public static final int HIER_EDITOR_MI = 9;
	public static final int ATTR_EDITOR_MI = 10;
	public static final int SEARCH_OPT_MI = 11;
	public static final int USER_PREF_MI = 12;
	public static final int CREATE_XML_MI = 13;
	public static final int DELETE_PICKLIST_MI = 14;
	public static final int INSTALL_ICT = 15;

	private MenuListener listener;

	private MainMenu mainMenu;
	private Shell shell;

	// tools menu items
	private MenuItem toolsItem; // tools menu
	private MenuItem reserveMI; // reserve catalogue
	private MenuItem unreserveMI; // unreserve catalogue
	private MenuItem uploadDataMI; // upload changes of the catalogue
	private MenuItem createXmlMI;
	private MenuItem publishMI; // publish a draft catalogue
	private MenuItem resetMI; // reset the catalogues data to the previous version
	private MenuItem importMI;
	private MenuItem exportMI;

	private MenuItem importPicklistMI;
	private MenuItem favouritePicklistMI;
	private MenuItem removePicklistMI;
	private MenuItem hierarchyEditMI;
	private MenuItem attributeEditMI;
	private MenuItem searchOptMI;
	private MenuItem userPrefMI;

	private MenuItem installIctMI; // install the ict tool
	private MenuItem launchIctMI; // launch ICT tool

	/**
	 * Tools menu in main menu
	 * 
	 * @param mainMenu
	 * @param menu
	 */
	public ToolsMenu(MainMenu mainMenu, Menu menu) {
		this.mainMenu = mainMenu;
		this.shell = mainMenu.getShell();
		this.toolsItem = create(menu);
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
	 * Create the tools menu with all the sub menu items
	 * 
	 * @param menu
	 */
	public MenuItem create(Menu menu) {

		MenuItem toolsItem = new MenuItem(menu, SWT.CASCADE);
		toolsItem.setText(Messages.getString("BrowserMenu.ToolsMenuName"));

		Menu toolsMenu = new Menu(menu);

		toolsItem.setMenu(toolsMenu);

		// get the current user
		User user = User.getInstance();

		// add reserve/unreserve for cm users
		if (mainMenu.getCatalogue() != null && user.isCatManagerOf(mainMenu.getCatalogue())
				&& !mainMenu.getCatalogue().hasUpdate()) {

			reserveMI = addReserveMI(toolsMenu);
			unreserveMI = addUnreserveMI(toolsMenu);
			createXmlMI = addCreateXmlMI(toolsMenu);
			uploadDataMI = addUploadDataMI(toolsMenu);
			publishMI = addPublishMI(toolsMenu);
			resetMI = addResetMI(toolsMenu);
		}

		// import operations
		importMI = addImportMI(toolsMenu);

		// export operations
		exportMI = addExportMI(toolsMenu);

		// export for ICT (just for the MTX cat)
		if (mainMenu.getCatalogue() != null && mainMenu.getCatalogue().isMTXCatalogue()) {

			// check if ict file is present
			if (GlobalUtil.isIctInstalled())
				launchIctMI = addLaunchIct(toolsMenu);
			else
				installIctMI = addInstallIct(toolsMenu);
		}

		// add import picklist
		importPicklistMI = addImportPicklistMI(toolsMenu);

		// favourite picklist
		favouritePicklistMI = addFavouritePicklistMI(toolsMenu);

		// delete picklist
		removePicklistMI = addRemovePicklistMI(toolsMenu);

		// editors only if the catalogue can be edited
		if (mainMenu.getCatalogue() != null && user.canEdit(mainMenu.getCatalogue())) {

			new MenuItem(toolsMenu, SWT.SEPARATOR);

			hierarchyEditMI = addHierarchyEditorMI(toolsMenu);

			attributeEditMI = addAttributeEditorMI(toolsMenu);
		}

		// horizontal bar to divide the menu elements
		new MenuItem(toolsMenu, SWT.SEPARATOR);

		// search preferences
		searchOptMI = addSearchOptionsMI(toolsMenu);

		// general user preferences
		userPrefMI = addUserPreferencesMI(toolsMenu);

		// called when the tools menu is shown
		toolsMenu.addListener(SWT.Show, new Listener() {

			@Override
			public void handleEvent(Event event) {

				// refresh the tool menu items
				refresh();
			}
		});

		toolsItem.setEnabled(false);

		return toolsItem;
	}

	/**
	 * Add reserve menu item (major and minor)
	 * 
	 * @param menu
	 */
	private MenuItem addReserveMI(Menu menu) {

		final MenuItem reserveMI = new MenuItem(menu, SWT.CASCADE);

		reserveMI.setText(Messages.getString("BrowserMenu.Reserve"));

		// create menu which hosts major and minor reserve
		Menu reserveOpMI = new Menu(shell, SWT.DROP_DOWN);
		reserveMI.setMenu(reserveOpMI);

		// major release
		MenuItem majorMI = new MenuItem(reserveOpMI, SWT.PUSH);
		majorMI.setText(Messages.getString("BrowserMenu.MajorCmd"));

		// minor release
		MenuItem minorMI = new MenuItem(reserveOpMI, SWT.PUSH);
		minorMI.setText(Messages.getString("BrowserMenu.MinorCmd"));

		majorMI.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				reservePressed(ReserveLevel.MAJOR);
			}
		});

		minorMI.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				reservePressed(ReserveLevel.MINOR);
			}
		});

		reserveMI.setEnabled(false);

		return reserveMI;
	}

	/**
	 * Create a button to unreserve the current catalogue
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addUnreserveMI(Menu menu) {

		final MenuItem unreserveMI = new MenuItem(menu, SWT.PUSH);

		unreserveMI.setText(Messages.getString("BrowserMenu.Unreserve"));
		unreserveMI.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				unreservePressed();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		unreserveMI.setEnabled(false);

		return unreserveMI;
	}

	/**
	 * Add the menu item used to create the xml which contains the difference
	 * between the catalogue I have in my local database and the official version of
	 * the catalogue.
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addCreateXmlMI(Menu menu) {

		final MenuItem createXmlMI = new MenuItem(menu, SWT.PUSH);
		createXmlMI.setText(Messages.getString("BrowserMenu.CreateXml"));
		createXmlMI.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				Term incorrectTerm = mainMenu.getCatalogue().isDataCorrect();
				if (incorrectTerm != null) {

					// warn the user, error occurred
					GlobalUtil.showErrorDialog(shell, incorrectTerm.getCode() + "; " + incorrectTerm.getShortName(true),
							Messages.getString("Export.DataErrorMessage"));

					return;
				}

				FormProgressBar progressBar = new FormProgressBar(shell,
						Messages.getString("CreateXml.CreateXmlBarTitle"));

				// ask for the xml creation to the sas server
				XmlUpdatesFactory xmlCreator = new XmlUpdatesFactory(new XmlUpdateFileDAO());
				xmlCreator.setProgressBar(progressBar);

				// if wrong
				xmlCreator.setAbortListener(new Listener() {

					@Override
					public void handleEvent(final Event arg0) {

						shell.getDisplay().asyncExec(new Runnable() {
							@Override
							public void run() {
								GlobalUtil.showErrorDialog(shell, Messages.getString("CreateXml.ErrorTitle"),
										(String) arg0.data);
							}
						});
					}
				});

				// if ok
				xmlCreator.setDoneListener(new Listener() {

					@Override
					public void handleEvent(Event arg0) {

						shell.getDisplay().asyncExec(new Runnable() {

							@Override
							public void run() {

								GlobalUtil.showDialog(shell, Messages.getString("CreateXml.SuccessTitle"),
										Messages.getString("CreateXml.SuccessMessage"), SWT.ICON_INFORMATION);
							}
						});
					}
				});

				// start
				xmlCreator.createXml(mainMenu.getCatalogue());

				if (listener != null)
					listener.buttonPressed(createXmlMI, CREATE_XML_MI, null);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		createXmlMI.setEnabled(false);

		return createXmlMI;
	}

	/**
	 * Add the upload data menu item to the menu. Here we upload the catalogue (in
	 * xlsx format) to a shared folder in order to start the sas procedure to upload
	 * only the real changes of the catalogue to the DCF
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addUploadDataMI(Menu menu) {

		final MenuItem uploadDataMI = new MenuItem(menu, SWT.PUSH);

		uploadDataMI.setText(Messages.getString("BrowserMenu.UploadData"));
		uploadDataMI.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				uploadDataPressed();
			}
		});

		uploadDataMI.setEnabled(true);

		return uploadDataMI;
	}

	/**
	 * Add the publish MI. If a catalogue is in draft, we can publish it using this
	 * button.
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addPublishMI(Menu menu) {

		final MenuItem publishMI = new MenuItem(menu, SWT.CASCADE);

		publishMI.setText(Messages.getString("BrowserMenu.Publish"));

		// create menu which hosts major and minor reserve
		Menu publishOpMI = new Menu(shell, SWT.DROP_DOWN);
		publishMI.setMenu(publishOpMI);

		// major release
		MenuItem majorMI = new MenuItem(publishOpMI, SWT.PUSH);
		majorMI.setText(Messages.getString("BrowserMenu.PublishMajorCmd"));

		// minor release
		MenuItem minorMI = new MenuItem(publishOpMI, SWT.PUSH);
		minorMI.setText(Messages.getString("BrowserMenu.PublishMinorCmd"));

		// publish major
		majorMI.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				publishPressed(PublishLevel.MAJOR);
			}
		});

		// publish minor
		minorMI.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				publishPressed(PublishLevel.MINOR);
			}
		});

		publishMI.setEnabled(false);

		return publishMI;
	}

	/**
	 * Add the reset mi, which allows resetting the catalogue data to the previous
	 * version. Note that this button is enabled only for internal version of
	 * catalogues which were reserved!
	 * 
	 * @param menu
	 * @return
	 */
	private MenuItem addResetMI(Menu menu) {

		final MenuItem resetMI = new MenuItem(menu, SWT.PUSH);

		resetMI.setText(Messages.getString("BrowserMenu.Reset"));
		resetMI.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				// ask for confirmation
				int val = GlobalUtil.showDialog(shell, Messages.getString("ResetChanges.ConfirmTitle"),
						Messages.getString("ResetChanges.ConfirmMessage"), SWT.YES | SWT.NO);

				if (val == SWT.NO)
					return;

				// reset the catalogue data to the previous version
				try {
					DatabaseManager.restoreBackup(mainMenu.getCatalogue());
				} catch (IOException e) {

					GlobalUtil.showErrorDialog(shell, Messages.getString("ResetChanges.ErrorTitle"),
							Messages.getString("ResetChanges.ErrorMessage"));
				}

				if (listener != null)
					listener.buttonPressed(resetMI, RESET_CAT_MI, null);
			}
		});

		resetMI.setEnabled(false);

		return resetMI;
	}

	/**
	 * Add a menu item which allows to import excels into the DB
	 * 
	 * @param menu
	 */
	private MenuItem addImportMI(Menu menu) {

		final MenuItem importItem = new MenuItem(menu, SWT.NONE);
		importItem.setText(Messages.getString("BrowserMenu.ImportCmd"));

		importItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				final String filename = GlobalUtil.showExcelFileDialog(shell,
						Messages.getString("Import.ImportWindowTitle"), SWT.OPEN);

				// return if no filename retrieved
				if (filename == null || filename.isEmpty())
					return;

				// ask for final confirmation
				int val = GlobalUtil.showDialog(shell, Messages.getString("Import.ImportWarningTitle"),
						Messages.getString("Import.ImportWarningMessage"), SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);

				// return if cancel was pressed
				if (val == SWT.CANCEL)
					return;

				CatalogueImporterThread importCat = new CatalogueImporterThread(filename, ImportFileFormat.XLSX);

				importCat.setProgressBar(new FormProgressBar(shell, Messages.getString("Import.ImportXlsxBarTitle")));

				// set the opened catalogue (importing an already existing catalogue)
				importCat.setOpenedCatalogue(mainMenu.getCatalogue());

				// set the listener
				importCat.addDoneListener(new ThreadFinishedListener() {

					@Override
					public void finished(Thread thread, final int code, final Exception e) {

						mainMenu.getShell().getDisplay().asyncExec(new Runnable() {

							@Override
							public void run() {

								String title = Messages.getString("Import.ImportErrorTitle");
								String msg = Messages.getString("Import.ImportGenericErrorMessage");
								int icon = SWT.ICON_ERROR;

								if (code == ThreadFinishedListener.OK) {
									title = Messages.getString("Import.ImportSuccessTitle");
									msg = Messages.getString("Import.ImportSuccessMessage");
									icon = SWT.ICON_INFORMATION;
								} else if (e instanceof ImportException) {

									ImportException impEx = (ImportException) e;

									switch (impEx.getCode()) {
									case "X100":
										msg = Messages.getString("Import.ImportAppendErrorMessage");
										break;
									case "X101":
										title = impEx.getData();
										msg = Messages.getString("Import.ImportStructureErrorMessage");
										break;
									case "X102":
										msg = Messages.getString("Import.ImportNoMasterErrorMessage");
										break;
									}
								}

								// load catalogue data in ram
								mainMenu.getCatalogue().refresh();
								mainMenu.getCatalogue().open();

								if (listener != null)
									listener.buttonPressed(importItem, IMPORT_CAT_MI, null);

								// show the message
								GlobalUtil.showDialog(shell, title, msg, icon);

							}
						});

					}
				});

				importCat.start();
			}
		});

		importItem.setEnabled(false);

		return importItem;
	}

	/**
	 * Add a menu item which allows to export the database into an excel file
	 * 
	 * @param menu
	 */
	private MenuItem addExportMI(Menu menu) {

		final MenuItem exportItem = new MenuItem(menu, SWT.NONE);
		exportItem.setText(Messages.getString("BrowserMenu.ExportCmd"));

		exportItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				Term incorrectTerm = mainMenu.getCatalogue().isDataCorrect();
				if (incorrectTerm != null) {

					// warn the user that everything went ok
					GlobalUtil.showErrorDialog(shell, incorrectTerm.getCode() + "; " + incorrectTerm.getShortName(true),
							Messages.getString("Export.DataErrorMessage"));

				}

				String defaultFilename = mainMenu.getCatalogue().getCode() + "_" + mainMenu.getCatalogue().getVersion()
						+ ".xlsx";

				final String filename = GlobalUtil.showExcelFileDialog(shell,
						Messages.getString("Export.FileDialogTitle"), defaultFilename, SWT.SAVE);

				// return if no filename retrieved
				if (filename == null || filename.isEmpty())
					return;

				// export the catalogue
				extractCatalogue(exportItem, filename, EXPORT_CAT_MI, true);

			}
		});

		// enable according to the operation status
		exportItem.setEnabled(false);

		return exportItem;
	}

	/**
	 * Add a menu item which allows to download and install the ICT
	 * 
	 * @author shahaal
	 * @param menu
	 */
	private MenuItem addInstallIct(Menu menu) {

		final MenuItem installItem = new MenuItem(menu, SWT.NONE);

		installItem.setText(Messages.getString("BrowserMenu.InstallICTCmd"));

		installItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {

				// ask the user before continuing the operation
				boolean confirmation = MessageDialog.openQuestion(shell, Messages.getString("ICT.Title"),
						Messages.getString("ICT.InstallationMsg"));

				if (!confirmation)
					return;

				// create first the required folders
				GlobalUtil.createIctFolders();

				// invoke the ICT downloader
				ICTDownloader downloader = new ICTDownloader();

				// instantiate the progress bar
				FormProgressBar progressbar = new FormProgressBar(shell, Messages.getString("ICT.Title"));
				downloader.setProgressBar(progressbar);

				// when finished
				downloader.setDoneListener(new ThreadFinishedListener() {

					@Override
					public void finished(Thread thread, final int code, Exception e) {

						shell.getDisplay().asyncExec(new Runnable() {

							@Override
							public void run() {

								// if download ok then continue with installation else warn the user
								if (code == ThreadFinishedListener.OK) {

									try {
										// invoke the ICT installer
										new ICTInstaller().install();
										
										// extract the foodex2 catalogue
										extractCatalogue(installItem, GlobalUtil.ICT_FOODEX2_FILE_PATH, INSTALL_ICT,
												false);

										if (listener != null)
											listener.buttonPressed(installItem, INSTALL_ICT, null);

									} catch (IOException e) {
										e.printStackTrace();
									}

								} else {
									GlobalUtil.showDialog(shell, Messages.getString("ICT.Title"),
											Messages.getString("ICT.InstallationError"), SWT.ICON_ERROR);
									e.printStackTrace();
								}

							}
						});
					}
				});

				// start the download process
				downloader.start();

			}
		});

		// enable according to the operation status
		installItem.setEnabled(false);

		return installItem;
	}

	/**
	 * Add a menu item which allows to launch the ICT tool
	 * 
	 * @author shahaal
	 * @param menu
	 */
	private MenuItem addLaunchIct(Menu menu) {

		final MenuItem launchItem = new MenuItem(menu, SWT.NONE);

		launchItem.setText(Messages.getString("BrowserMenu.LaunchICTCmd"));

		launchItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					Desktop.getDesktop().open(new File(GlobalUtil.ICT_FILE_PATH));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		// enable according to the operation status
		launchItem.setEnabled(false);

		return launchItem;
	}

	/**
	 * Add a menu item which allows selecting the favourite picklist to use in the
	 * browser
	 * 
	 * @param menu
	 */
	private MenuItem addImportPicklistMI(Menu menu) {

		// create a menu item for importing picklists
		final MenuItem picklistItem = new MenuItem(menu, SWT.CASCADE);
		picklistItem.setText(Messages.getString("BrowserMenu.ImportPicklistCmd"));

		// open a dialog to select a picklist file

		picklistItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// create dialog
				FileDialog fd = new FileDialog(shell, SWT.OPEN);

				// set dialog title
				fd.setText(Messages.getString("BrowserMenu.ImportPicklistDialogTitle"));

				// set working directory
				// get the working directory from the user preferences
				fd.setFilterPath(DatabaseManager.MAIN_CAT_DB_FOLDER);

				// select only csv files
				String[] filterExt = { "*.csv" };
				fd.setFilterExtensions(filterExt);

				// open dialog a listen to get the selected filename
				String filename = fd.open();

				if ((filename != null) && (filename.length() > 0)) {

					GlobalUtil.setShellCursor(shell, SWT.CURSOR_WAIT);

					// parse the picklist as a csv semicolon separated file
					PicklistParser parse = new PicklistParser(mainMenu.getCatalogue(), filename, ";");

					ArrayList<PicklistTerm> picklistTerms = new ArrayList<>();
					PicklistTerm currentTerm;

					// for each picklist term add it to the array list
					while ((currentTerm = parse.nextTerm()) != null)
						picklistTerms.add(currentTerm);

					// create a picklist using the filename as code
					Picklist picklist = new Picklist(filename, picklistTerms);

					PicklistDAO pickDao = new PicklistDAO(mainMenu.getCatalogue());

					// insert the new picklist if it is new
					pickDao.importPicklist(picklist);

					GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);
				}

				if (listener != null)
					listener.buttonPressed(picklistItem, IMPORT_PICKLIST_MI, null);
			}
		});

		picklistItem.setEnabled(false);

		return picklistItem;
	}

	/**
	 * Add a menu item which allows selecting the favourite picklist to use in the
	 * browser
	 * 
	 * @param menu
	 */
	private MenuItem addFavouritePicklistMI(Menu menu) {

		final MenuItem picklistItem = new MenuItem(menu, SWT.CASCADE);
		picklistItem.setText(Messages.getString("BrowserMenu.PicklistCmd"));

		// Initialize the menu
		final Menu selectPicklistMenu = new Menu(shell, SWT.DROP_DOWN);

		// add the menu
		picklistItem.setMenu(selectPicklistMenu);

		// when the menu is showed
		selectPicklistMenu.addListener(SWT.Show, new Listener() {

			@Override
			public void handleEvent(Event event) {

				// reset the item of the menu, in order to update with
				// the current picklists in the app.jar folder
				for (MenuItem item : selectPicklistMenu.getItems()) {
					item.dispose();
				}

				CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(mainMenu.getCatalogue());

				// get the favourite picklist
				Picklist currentPicklist = prefDao.getFavouritePicklist();

				PicklistDAO pickDao = new PicklistDAO(mainMenu.getCatalogue());

				// for each imported picklist we create a menu item in order to allow choosing
				// the favourite picklist
				for (Picklist picklist : pickDao.getAll()) {

					final MenuItem mi = new MenuItem(selectPicklistMenu, SWT.RADIO);
					mi.setText(picklist.getCode());

					// select the current menu item if the old selected picklist is the current one
					// if there was a favourite picklist indeed
					if (currentPicklist != null)
						mi.setSelection(picklist.equals(currentPicklist));

					// set the data for the menu item
					mi.setData(picklist);

					// actions taken when this menu item is pressed
					mi.addSelectionListener(new SelectionAdapter() {

						@Override
						public void widgetSelected(SelectionEvent e) {

							// Update the selected picklist into the preference database:

							// get the selected picklist id starting from the picklist code
							// the menu items has the picklist code in the data field
							Picklist selectedPicklist = (Picklist) mi.getData();

							CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(mainMenu.getCatalogue());

							// set the favourite picklist
							prefDao.setFavouritePicklist(selectedPicklist);

							if (listener != null)
								listener.buttonPressed(picklistItem, FAV_PICKLIST_MI, null);
						}
					});
				}
			}
		});

		picklistItem.setEnabled(false);

		return picklistItem;
	}

	private MenuItem addRemovePicklistMI(Menu menu) {

		final MenuItem picklistItem = new MenuItem(menu, SWT.CASCADE);

		picklistItem.setText(Messages.getString("BrowserMenu.DeletePicklistCmd"));

		// Initialize the menu
		final Menu selectPicklistMenu = new Menu(shell, SWT.DROP_DOWN);

		// add the menu
		picklistItem.setMenu(selectPicklistMenu);

		// when the menu is showed
		selectPicklistMenu.addListener(SWT.Show, new Listener() {

			@Override
			public void handleEvent(Event event) {

				// reset the item of the menu, in order to update with
				// the current picklists in the app.jar folder
				for (MenuItem item : selectPicklistMenu.getItems()) {
					item.dispose();
				}

				PicklistDAO pickDao = new PicklistDAO(mainMenu.getCatalogue());

				// for each imported picklist we create a menu item in order to allow choosing
				// the favourite picklist
				for (Picklist picklist : pickDao.getAll()) {

					final MenuItem mi = new MenuItem(selectPicklistMenu, SWT.PUSH);
					mi.setText(picklist.getCode());

					// set the data for the menu item
					mi.setData(picklist);

					// actions taken when this menu item is pressed
					mi.addSelectionListener(new SelectionAdapter() {

						@Override
						public void widgetSelected(SelectionEvent e) {

							// Update the selected picklist into the preference database:

							// get the selected picklist id starting from the picklist code
							// the menu items has the picklist code in the data field
							Picklist selectedPicklist = (Picklist) mi.getData();

							CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(mainMenu.getCatalogue());

							// get the favourite picklist
							Picklist favouritePicklist = prefDao.getFavouritePicklist();

							// remove favourite picklist if necessary
							if (favouritePicklist != null && selectedPicklist.equals(favouritePicklist))
								prefDao.setFavouritePicklist(null);

							PicklistDAO pickDao = new PicklistDAO(mainMenu.getCatalogue());
							// delete the selected picklist from the database
							pickDao.remove(selectedPicklist);

							if (listener != null)
								listener.buttonPressed(picklistItem, DELETE_PICKLIST_MI, null);
						}
					});
				}
			}
		});

		picklistItem.setEnabled(false);

		return picklistItem;
	}

	/**
	 * Add a menu item which allows modifying the hierarchies names
	 * 
	 * @param menu
	 */
	private MenuItem addHierarchyEditorMI(Menu menu) {

		final MenuItem hierarchyEditorItem = new MenuItem(menu, SWT.NONE);
		hierarchyEditorItem.setText(Messages.getString("BrowserMenu.HierarchyEditorCmd"));

		// Enable only if there is a catalogue open
		hierarchyEditorItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				HierarchyEditor editor = new HierarchyEditor(shell, mainMenu.getCatalogue(),
						Messages.getString("HierarchyEditor.HierarchyFacetLabel"));
				editor.display();

				if (listener != null)
					listener.buttonPressed(hierarchyEditorItem, HIER_EDITOR_MI, null);
			}
		});

		// enable according to the operation status
		hierarchyEditorItem.setEnabled(false);

		return hierarchyEditorItem;
	}

	/**
	 * Add a menu item which allows modifying the facet names
	 * 
	 * @param menu
	 */
	private MenuItem addAttributeEditorMI(Menu menu) {

		final MenuItem attributeEditorItem = new MenuItem(menu, SWT.NONE);
		attributeEditorItem.setText(Messages.getString("BrowserMenu.AttributeEditorCmd"));

		attributeEditorItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				AttributeEditor editor = new AttributeEditor(shell, mainMenu.getCatalogue(),
						Messages.getString("FormAttribute.DialogTitle"));
				editor.display();

				if (listener != null)
					listener.buttonPressed(attributeEditorItem, ATTR_EDITOR_MI, null);
			}
		});

		// enable according to the operation status
		attributeEditorItem.setEnabled(false);

		return attributeEditorItem;
	}

	/**
	 * Add a menu item which allows choosing the search preferences
	 * 
	 * @param menu
	 */
	private MenuItem addSearchOptionsMI(Menu menu) {

		// Search options form
		final MenuItem searchOptionsItem = new MenuItem(menu, SWT.PUSH);
		searchOptionsItem.setText(Messages.getString("BrowserMenu.GeneralSearchOptionsCmd"));

		// if search options is clicked
		searchOptionsItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				// open the form for choosing the search options
				FormSearchOptions sof = new FormSearchOptions(shell, Messages.getString("SearchOption.Title"),
						mainMenu.getCatalogue());

				// display the form
				sof.display();

				if (listener != null)
					listener.buttonPressed(searchOptionsItem, SEARCH_OPT_MI, null);

			}
		});

		searchOptionsItem.setEnabled(false);

		return searchOptionsItem;
	}

	/**
	 * Add a menu item which allows choosing among the user preferences
	 * 
	 * @param menu
	 */
	private MenuItem addUserPreferencesMI(Menu menu) {

		final MenuItem userPrefItem = new MenuItem(menu, SWT.NONE);

		userPrefItem.setText(Messages.getString("BrowserMenu.UserPrefCmd"));

		userPrefItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				FormUserPreferences e = new FormUserPreferences(shell, mainMenu.getCatalogue());
				e.Display();

				if (listener != null)
					listener.buttonPressed(userPrefItem, USER_PREF_MI, null);
			}
		});

		userPrefItem.setEnabled(false);

		return userPrefItem;
	}

	/**
	 * Refresh all the menu items contained in the tool menu
	 */
	public void refresh() {

		User user = User.getInstance();

		if (toolsItem.isDisposed())
			return;

		// enable the tools menu only if there is a catalogue open
		// and if we know that the user is cm or dp
		toolsItem.setEnabled(mainMenu.getCatalogue() != null && !user.isGettingUserLevel());

		if (mainMenu.getCatalogue() == null)
			return;

		PicklistDAO pickDao = new PicklistDAO(mainMenu.getCatalogue());

		// check if the db contains at least one picklist
		boolean hasPicklists = !pickDao.isEmpty();

		// check if the catalogue contains facet or not
		boolean hasFacets = mainMenu.getCatalogue().hasAttributeHierarchies();

		// check if the current user can edit the current catalogue or not
		// we can edit if we are in editing mode or if we are modifying a local
		// catalogue
		boolean canEdit = user.canEdit(mainMenu.getCatalogue());

		// check if the current catalogue is not empty (has data in it)
		boolean nonEmptyCat = !mainMenu.getCatalogue().isEmpty();

		// we can export only for non local catalogues, since if we
		// export a local catalogue we create an excel which has as
		// catalogue code a custom string, but with master hierarchy code
		// the code defined before in the excel import... And give errors!
		exportMI.setEnabled(true);

		// export for ICT (just for the MTX cat)
		if (mainMenu.getCatalogue() != null && mainMenu.getCatalogue().isMTXCatalogue()) {

			boolean ict = GlobalUtil.isIctInstalled();

			if (installIctMI != null)
				installIctMI.setEnabled(!ict);

			if (launchIctMI != null)
				launchIctMI.setEnabled(ict);
		}

		importPicklistMI.setEnabled(hasFacets);
		favouritePicklistMI.setEnabled(hasFacets && hasPicklists);
		removePicklistMI.setEnabled(hasFacets && hasPicklists);

		boolean searchPrefEnabled = nonEmptyCat
				&& (mainMenu.getCatalogue().hasTermTypes() || mainMenu.getCatalogue().hasGenericAttributes());

		searchOptMI.setEnabled(searchPrefEnabled);
		userPrefMI.setEnabled(true);

		// if editing modify also editing buttons
		if (user.canEdit(mainMenu.getCatalogue())) {

			if (hierarchyEditMI != null)
				hierarchyEditMI.setEnabled(true);

			if (attributeEditMI != null)
				attributeEditMI.setEnabled(true);
		}

		// update catalogue manager buttons
		// if the current user is enabled to
		// reserve the current catalogue
		if (user.isCatManagerOf(mainMenu.getCatalogue())) {

			boolean isReservedByCurrentUser = user.hasReserved(mainMenu.getCatalogue().getCode());

			// if we are requesting a web service
			// disable the action which can send another
			// web service request
			if (user.hasPendingRequestsFor(mainMenu.getCatalogue())) {

				if (reserveMI != null) {
					reserveMI.setText(Messages.getString("Reserve.WaitingResponse"));
					reserveMI.setEnabled(false);
				}

				if (unreserveMI != null) {
					unreserveMI.setText(Messages.getString("Reserve.WaitingResponse"));
					unreserveMI.setEnabled(false);
				}

				if (publishMI != null) {
					publishMI.setText(Messages.getString("Reserve.WaitingResponse"));
					publishMI.setEnabled(false);
				}

				if (createXmlMI != null) {
					createXmlMI.setText(Messages.getString("Reserve.WaitingResponse"));
					createXmlMI.setEnabled(false);
				}

				if (uploadDataMI != null) {
					uploadDataMI.setText(Messages.getString("Reserve.WaitingResponse"));
					uploadDataMI.setEnabled(false);
				}

				// if we are reserving but we have forced the
				// editing, we leave these buttons enabled
				if (mainMenu.getCatalogue() != null && user.hasForcedReserveOf(mainMenu.getCatalogue()) == null) {

					if (resetMI != null)
						resetMI.setEnabled(false);

					if (importMI != null)
						importMI.setEnabled(false);

					if (hierarchyEditMI != null)
						hierarchyEditMI.setEnabled(false);

					if (attributeEditMI != null)
						attributeEditMI.setEnabled(false);
				}
			} else {

				if (reserveMI != null) {
					// can reserve only if not local and catalogue not reserved
					reserveMI.setText(Messages.getString("BrowserMenu.Reserve"));
					reserveMI.setEnabled(!isReservedByCurrentUser);
				}

				if (unreserveMI != null) {
					unreserveMI.setText(Messages.getString("BrowserMenu.Unreserve"));
					unreserveMI.setEnabled(isReservedByCurrentUser);
				}

				if (uploadDataMI != null) {
					uploadDataMI.setText(Messages.getString("BrowserMenu.UploadData"));

					// check if we have already created an xml file for this catalogue
					XmlUpdateFileDAO xmlDao = new XmlUpdateFileDAO();
					XmlUpdateFile xml = xmlDao.getById(mainMenu.getCatalogue().getId());
					uploadDataMI.setEnabled(
							mainMenu.getCatalogue().isLastRelease() && isReservedByCurrentUser && xml != null);
				}

				if (createXmlMI != null) {
					createXmlMI.setText(Messages.getString("BrowserMenu.CreateXml"));
					createXmlMI.setEnabled(mainMenu.getCatalogue().isLastRelease() && isReservedByCurrentUser);
				}

				if (publishMI != null) {
					publishMI.setText(Messages.getString("BrowserMenu.Publish"));
					publishMI.setEnabled(!isReservedByCurrentUser);
				}

				if (resetMI != null && user.canEdit(mainMenu.getCatalogue())) {

					// enable resetMI only if the catalogue is an internal version
					// and if the catalogue is reserved by the current user
					resetMI.setEnabled(mainMenu.getCatalogue().isReservedBy(user)
							&& mainMenu.getCatalogue().getCatalogueVersion().isInternalVersion());
				}

				if (importMI != null)
					importMI.setEnabled(canEdit);
			}
		} else {
			if (mainMenu.getCatalogue().isLocal()) {

				if (importMI != null)
					importMI.setEnabled(true);

				if (exportMI != null)
					exportMI.setEnabled(true);

				if (installIctMI != null)
					installIctMI.setEnabled(true);

				if (launchIctMI != null)
					launchIctMI.setEnabled(true);
			}
		}
	}

	/**
	 * Set the menu as enabled or not
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		toolsItem.setEnabled(enabled);
	}

	/**
	 * Check if the reserve button can be used and warn the user if not possible to
	 * reserve
	 * 
	 * @return
	 */
	private boolean isReserveLegit(ReserveLevel level) {

		Warnings wrn = new Warnings(shell);

		Catalogue catalogue = mainMenu.getCatalogue();

		// check if we have errors
		boolean block = wrn.reserve(User.getInstance().checkCatalogue(catalogue));

		return !block;
	}

	/**
	 * Ask to the user a reservation note
	 * 
	 * @return
	 */
	private String askReserveNote() {

		// ask the reserve description
		DialogSingleText dialog = new DialogSingleText(shell, 10);
		dialog.setTitle(Messages.getString("BrowserMenu.ReserveTitle"));
		dialog.setMessage(Messages.getString("BrowserMenu.ReserveMessage"));

		return dialog.open();
	}

	/**
	 * Check if publish can be used
	 * 
	 * @return
	 */
	private boolean isPublishLegit(PublishLevel level) {

		Warnings wrn = new Warnings(shell);

		Catalogue catalogue = mainMenu.getCatalogue();

		// check if we have errors
		boolean block = wrn.publish(User.getInstance().checkCatalogue(catalogue));

		return !block;
	}

	/**
	 * Execute the reserve button actions
	 * 
	 * @param level
	 */
	private void reservePressed(ReserveLevel level) {

		boolean legit = isReserveLegit(level);

		if (!legit)
			return;

		String catCode = mainMenu.getCatalogue().getCode();

		ConsoleMessageFactory factory = new ConsoleMessageFactory(catCode);

		String levelLabel = factory.getReserveLabel(level);

		int val = GlobalUtil.showDialog(shell, Messages.getString("warning.title"),
				Messages.getString("upload.cat.file.confirmation", catCode, levelLabel),
				SWT.ICON_WARNING | SWT.YES | SWT.NO);

		if (val != SWT.YES)
			return;

		String note = askReserveNote();

		if (note == null)
			return;

		GlobalUtil.setShellCursor(shell, SWT.CURSOR_WAIT);

		String message = Messages.getString("reserve.sent", catCode, levelLabel);

		int colour = SWT.COLOR_DARK_GREEN;

		Tools tools = new Tools();
		try {
			tools.reserve(level, catCode, note);
		} catch (DetailedSOAPException e) {
			String[] warn = GlobalUtil.getSOAPWarning(e);
			message = warn[1];
			colour = SWT.COLOR_DARK_RED;
		} catch (SQLException | IOException e) {
			e.printStackTrace();
			message = Messages.getString("reserve.error", catCode, levelLabel);
			colour = SWT.COLOR_DARK_RED;
		} finally {
			GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);
		}

		mainMenu.mainPanel.addMessageToConsole(new ConsoleMessage(message, colour));

		if (listener != null) {
			listener.buttonPressed(reserveMI, RESERVE_CAT_MI, null);
		}
	}

	private void unreservePressed() {

		String catCode = mainMenu.getCatalogue().getCode();

		int val = GlobalUtil.showDialog(shell, Messages.getString("warning.title"),
				Messages.getString("upload.cat.file.confirmation", catCode, Messages.getString("unreserve.label")),
				SWT.ICON_WARNING | SWT.YES | SWT.NO);

		if (val != SWT.YES)
			return;

		GlobalUtil.setShellCursor(shell, SWT.CURSOR_WAIT);

		String message = Messages.getString("unreserve.sent", catCode);
		int colour = SWT.COLOR_DARK_GREEN;

		Tools tools = new Tools();
		// use reservation note stored in db
		try {
			tools.unreserve(catCode, mainMenu.getCatalogue().getReserveNote());
		} catch (DetailedSOAPException e) {
			String[] warn = GlobalUtil.getSOAPWarning(e);
			message = warn[1];
			colour = SWT.COLOR_DARK_RED;
		} catch (SQLException | IOException e) {
			e.printStackTrace();
			message = Messages.getString("unreserve.error", catCode);
			colour = SWT.COLOR_DARK_RED;
		} finally {
			GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);
		}

		mainMenu.mainPanel.addMessageToConsole(new ConsoleMessage(message, colour));

		if (listener != null)
			listener.buttonPressed(unreserveMI, UNRESERVE_CAT_MI, null);
	}

	/**
	 * Publish button actions
	 * 
	 * @param level
	 */
	private void publishPressed(PublishLevel level) {

		boolean legit = isPublishLegit(level);
		if (!legit)
			return;

		String catCode = mainMenu.getCatalogue().getCode();

		ConsoleMessageFactory factory = new ConsoleMessageFactory(catCode);

		String levelLabel = factory.getPublishLabel(level);

		int val = GlobalUtil.showDialog(shell, Messages.getString("warning.title"),
				Messages.getString("upload.cat.file.confirmation", catCode, levelLabel),
				SWT.ICON_WARNING | SWT.YES | SWT.NO);

		if (val != SWT.YES)
			return;

		String message = Messages.getString("publish.sent", catCode, levelLabel);
		int colour = SWT.COLOR_DARK_GREEN;

		// set wait cursor
		GlobalUtil.setShellCursor(shell, SWT.CURSOR_WAIT);

		Tools tools = new Tools();
		try {
			tools.publish(level, catCode);
		} catch (DetailedSOAPException e) {
			String[] warn = GlobalUtil.getSOAPWarning(e);
			message = warn[1];
			colour = SWT.COLOR_DARK_RED;
		} catch (SQLException | IOException e) {
			e.printStackTrace();
			message = Messages.getString("publish.error", catCode, levelLabel);
			colour = SWT.COLOR_DARK_RED;
		} finally {
			GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);
		}

		mainMenu.mainPanel.addMessageToConsole(new ConsoleMessage(message, colour));

		if (listener != null)
			listener.buttonPressed(publishMI, PUBLISH_CAT_MI, null);
	}

	private void uploadDataPressed() {

		String catCode = mainMenu.getCatalogue().getCode();

		int val = GlobalUtil
				.showDialog(
						shell, Messages.getString("warning.title"), Messages.getString("upload.cat.file.confirmation",
								catCode, Messages.getString("upload.xml.data.label")),
						SWT.ICON_WARNING | SWT.YES | SWT.NO);

		if (val != SWT.YES)
			return;

		int catId = mainMenu.getCatalogue().getId();

		String message = Messages.getString("upload.xml.sent", catCode);
		int colour = SWT.COLOR_DARK_GREEN;

		Tools tools = new Tools();
		try {
			tools.uploadXmlData(catId, catCode);
		} catch (DetailedSOAPException e) {
			String[] warn = GlobalUtil.getSOAPWarning(e);
			message = warn[1];
			colour = SWT.COLOR_DARK_RED;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			message = Messages.getString("upload.xml_not_found.error", catCode);
			colour = SWT.COLOR_DARK_RED;
		} catch (IOException e) {
			e.printStackTrace();
			message = Messages.getString("upload.xml.error", catCode);
			colour = SWT.COLOR_DARK_RED;
		} finally {
			GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);
		}

		mainMenu.mainPanel.addMessageToConsole(new ConsoleMessage(message, colour));
		if (listener != null)
			listener.buttonPressed(publishMI, CREATE_XML_MI, null);
	}

	private void extractCatalogue(MenuItem item, String filename, int action, boolean flag) {

		// export the FoodEx2 catalogue
		ExportActions export = new ExportActions();

		// set the progress bar
		export.setProgressBar(new FormProgressBar(shell, Messages.getString("Export.ProgressBarTitle")));

		// export the opened catalogue
		export.exportAsync(mainMenu.getCatalogue(), filename, flag, new ThreadFinishedListener() {

			@Override
			public void finished(Thread thread, final int code, Exception e) {

				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						String title = filename;
						String msg;
						int icon;

						if (code == ThreadFinishedListener.OK) {
							msg = Messages.getString("Export.DoneMessage");
							icon = SWT.ICON_INFORMATION;
						} else {
							msg = Messages.getString("Export.ErrorMessage");
							icon = SWT.ICON_ERROR;
						}

						// warn the user that everything went ok
						GlobalUtil.showDialog(shell, title, msg, icon);

						if (listener != null)
							listener.buttonPressed(item, action, null);
					}
				});
			}

		});
	}

}
