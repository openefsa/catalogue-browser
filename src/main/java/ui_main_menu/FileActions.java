package ui_main_menu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.DOMException;

import catalogue.AttachmentNotFoundException;
import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import catalogue_generator.CatalogueCreator;
import catalogue_generator.CatalogueDestroyer;
import catalogue_generator.CatalogueDownloader;
import catalogue_generator.CatalogueDownloaderManager;
import catalogue_generator.DuplicatedCatalogueException;
import catalogue_generator.ThreadFinishedListener;
import data_collection.DCDAO;
import data_collection.DCDownloader;
import data_collection.DCTableConfig;
import data_collection.DataCollection;
import dcf_manager.Dcf;
import dcf_user.User;
import export_catalogue.ExportActions;
import form_objects_list.FormCataloguesList;
import form_objects_list.FormDCTableConfigsList;
import form_objects_list.FormDataCollectionsList;
import global_manager.GlobalManager;
import i18n_messages.CBMessages;
import import_catalogue.CatalogueImporter.ImportFileFormat;
import import_catalogue.CatalogueImporterThread;
import progress_bar.FormMultipleProgress;
import progress_bar.FormProgressBar;
import progress_bar.IProgressBar;
import progress_bar.TableMultipleProgress.TableRow;
import soap.DetailedSOAPException;
import ui_main_panel.FormLocalCatalogueName;
import ui_main_panel.OldCatalogueReleaseDialog;
import utilities.GlobalUtil;

/**
 * Actions which are performed when menu items in the File main menu are
 * pressed. This was done to hide from the user interface class the code details
 * and to reuse possibly the file actions for testing purposes.
 * 
 * @author avonva
 * @author shahaal
 */
public class FileActions {

	private static final Logger LOGGER = LogManager.getLogger(FileActions.class);

	private final int minimumMemory = 400;

	private Listener listener;

	/**
	 * Ask to the user the new catalogue code and create a new local catalogue.
	 * 
	 * @param shell
	 */
	public void createNewLocalCatalogue(Shell shell) {

		FormLocalCatalogueName dialog = new FormLocalCatalogueName(shell);

		String catalogueCode = dialog.open();

		// if null the cancel button was pressed
		if (catalogueCode == null)
			return;

		// set the wait cursor
		GlobalUtil.setShellCursor(shell, SWT.CURSOR_WAIT);

		// create a database for the new catalogue
		// but if the catalogue already exists show an error dialog
		try {
			CatalogueCreator.newLocalCatalogue(catalogueCode);
		} catch (DuplicatedCatalogueException exception) {
			LOGGER.error("The catalogue already exists ", exception);
			GlobalUtil.showErrorDialog(shell, CBMessages.getString("BrowserMenu.NewLocalCatErrorTitle"),
					CBMessages.getString("BrowserMenu.NewLocalCatErrorMessage"));

			GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);

			return;
		} catch (IOException e) {
			LOGGER.error("Cannot create new local catalogue with code=" + catalogueCode, e);
			e.printStackTrace();
			GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);
			return;
		} finally {
			GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);
		}

		// reset the standard cursor
		GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);

		// warn user
		GlobalUtil.showDialog(shell, CBMessages.getString("NewLocalCat.DoneTitle"),
				CBMessages.getString("NewLocalCat.DoneMessage"), SWT.ICON_INFORMATION);
	}

	/**
	 * Open a catalogue from the available list of catalogues in the main user
	 * interface
	 * 
	 * @param shell
	 * @param catalogue
	 * @return true if the catalogue was opened
	 */
	public void openCatalogue(final Shell shell, final Listener listener) {

		// get all the catalogues downloaded in the pc
		CatalogueDAO catDao = new CatalogueDAO();
		ArrayList<Catalogue> myCatalogues = catDao.getMyCatalogues(Dcf.dcfType);

		// Order the catalogues by label name to make a better visualization
		Collections.sort(myCatalogues);

		// show columns based on permissions
		final User user = User.getInstance();

		String[] columns;
		// display only the columns that we want
		if (user.isCatManager())
			columns = new String[] { "label", "version", "status", "reserve" };
		else
			columns = new String[] { "label", "version", "status" };

		// ask a catalogue
		Catalogue catalogue = chooseCatalogue(shell, CBMessages.getString("FormCataloguesList.OpenTitle"), myCatalogues,
				columns, CBMessages.getString("FormCataloguesList.OpenCmd"));

		// return if no catalogue selected
		if (catalogue == null)
			return;

		int val1 = openCatalogue(shell, catalogue);

		if (val1 == SWT.NO)
			return;

		Event e = new Event();
		e.data = catalogue;
		listener.handleEvent(e);
	}

	public static int openCatalogue(Shell shell, Catalogue catalogue) {

		int val1 = warnDeprecatedCatalogue(shell, catalogue);

		if (val1 == SWT.NO)
			return SWT.NO;

		warnPossibleOldCatalogue(shell, catalogue);

		// open the catalogue when the dialog is closed
		GlobalUtil.setShellCursor(shell, SWT.CURSOR_WAIT);

		// open the catalogue
		catalogue.open();

		GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);

		return SWT.YES;
	}

	/**
	 * Download the last version of a catalogue, update ict db if MTX, then open the
	 * catalogue
	 * 
	 * @author shahaal
	 * 
	 * @param shell
	 * @param catalogue
	 * @param listener
	 */
	public void downloadLastVersion(final Shell shell, Catalogue catalogue, final Listener listener) {

		final Catalogue lastRelease = catalogue.getLastRelease();

		downloadSingleCat(shell, lastRelease, new Listener() {

			@Override
			public void handleEvent(Event arg0) {

				// open the catalogue when the dialog is closed
				GlobalUtil.setShellCursor(shell, SWT.CURSOR_WAIT);

				Catalogue lastReleaseImported = (Catalogue) arg0.data;

				// return if the catalogue is empty
				if (lastReleaseImported == null)
					return;

				// open the new catalogue
				lastReleaseImported.open();

				// update also the interpreting tool db (ONLY MTX)
				if (lastRelease.isMTXCatalogue() && GlobalUtil.isIctInstalled())
					updatedICTDatabase(shell, lastReleaseImported);

				GlobalUtil.setShellCursor(shell, SWT.CURSOR_ARROW);

				if (listener != null)
					listener.handleEvent(arg0);
			}
		});

	}

	/**
	 * the method update the ict db when a new update of the mtx is available
	 * 
	 * @author shahaal
	 * @param shell
	 * @param catalogue
	 */
	private static void updatedICTDatabase(Shell shell, Catalogue catalogue) {

		// export the catalogue (changed the main class so to know that the call is
		// coming from a different export button
		ExportActions export = new ExportActions();

		// set the progress bar
		export.setProgressBar(new FormProgressBar(shell, CBMessages.getString("Export.ProgressBarTitle")));

		// export the opened catalogue
		export.exportAsync(catalogue, GlobalUtil.ICT_FOODEX2_FILE_PATH, false, new ThreadFinishedListener() {

			@Override
			public void finished(Thread thread, final int code, Exception e) {

				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						String title = "ICT database updated";
						String msg;
						int icon;

						if (code == ThreadFinishedListener.OK) {
							msg = CBMessages.getString("Export.DoneMessage");
							icon = SWT.ICON_INFORMATION;
						} else {
							msg = CBMessages.getString("Export.ErrorMessage");
							icon = SWT.ICON_ERROR;
						}

						// warn the user that everything went ok
						GlobalUtil.showDialog(shell, title, msg, icon);
					}
				});
			}
		});

	}

	public static int warnDeprecatedCatalogue(Shell shell, Catalogue catalogue) {

		// check if the catalogue is deprecated
		if (catalogue.isDeprecated()) {

			int val = GlobalUtil.showDialog(shell, catalogue.getLabel(),
					CBMessages.getString("BrowserMenu.CatalogueDeprecatedMessage"),
					SWT.ICON_WARNING | SWT.YES | SWT.NO);

			return val;
		}

		return SWT.OK;
	}

	public static int warnOldRelease(Shell shell, Catalogue catalogue) {

		User user = User.getInstance();

		// cannot check if not logged in
		if (!user.isLoggedIn())
			return SWT.CANCEL;

		// check if there is a catalogue update
		boolean hasUpdate = catalogue.hasUpdate();

		// check if the update was already downloaded
		boolean alreadyDownloaded = catalogue.isLastReleaseAlreadyDownloaded();

		// if there is a new version and we have not downloaded it yet
		// we warn the user that a new version is available
		if (hasUpdate && !alreadyDownloaded) {

			OldCatalogueReleaseDialog dialog = new OldCatalogueReleaseDialog(shell, catalogue);

			dialog.open();

			int val = dialog.getButtonPressed();

			return val;
		}

		return SWT.CANCEL;
	}

	public static void warnPossibleOldCatalogue(Shell shell, Catalogue catalogue) {

		User user = User.getInstance();
		if (user.isLoggedIn() || user.isLoggedInOpenAPI() || catalogue.isLocal() || catalogue.isDeprecated())
			return;

		// if we are not logged in, simply warn the user that we cannot
		// be sure that this is the last release
		MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION);
		mb.setText(CBMessages.getString("BrowserMenu.CatalogueReleaseInfoTitle"));
		mb.setMessage(CBMessages.getString("BrowserMenu.CatalogueReleaseInfoMessage"));
		mb.open();
	}

	/**
	 * Make opening checks on the catalogue and warn the user accordingly
	 * 
	 * @param shell
	 * @param catalogue
	 * @return
	 */
	public static int performCatalogueChecks(Shell shell, Catalogue catalogue) {

		User user = User.getInstance();

		// check if the catalogue is deprecated
		if (catalogue.isDeprecated()) {

			int val = GlobalUtil.showDialog(shell, catalogue.getLabel(),
					CBMessages.getString("BrowserMenu.CatalogueDeprecatedMessage"),
					SWT.ICON_WARNING | SWT.YES | SWT.NO);

			return val;
		}

		// if the user is logged in we can check the updates
		else if (user.isLoggedIn() || user.isLoggedInOpenAPI()) {

			// check if there is a catalogue update
			boolean hasUpdate = catalogue.hasUpdate();

			// check if the update was already downloaded
			boolean alreadyDownloaded = catalogue.isLastReleaseAlreadyDownloaded();

			// if there is a new version and we have not downloaded it yet
			// we warn the user that a new version is available
			if (hasUpdate && !alreadyDownloaded) {

				OldCatalogueReleaseDialog dialog = new OldCatalogueReleaseDialog(shell, catalogue);

				int val = dialog.open();

				return val;
			}
		} else {

			// only for official catalogues
			if (!catalogue.isLocal()) {
				// if we are not logged in, simply warn the user that we cannot
				// be sure that this is the last release
				MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION);
				mb.setText(CBMessages.getString("BrowserMenu.CatalogueReleaseInfoTitle"));
				mb.setMessage(CBMessages.getString("BrowserMenu.CatalogueReleaseInfoMessage"));
				mb.open();
			}
		}

		return SWT.OK;
	}

	/**
	 * Ask to the user to select a catalogue from the {@code input} list. if the
	 * catalogue is already opened warn the user and avoid to re-open
	 * 
	 * @author shahaal
	 * @param shell
	 * @param title
	 * @param input
	 * @param columns
	 * @param okText
	 * @return
	 */
	private static Catalogue chooseCatalogue(Shell shell, String title, Collection<Catalogue> input, String[] columns,
			String okText) {

		Collection<Catalogue> objs = chooseCatalogues(shell, title, input, false, columns, okText);

		if (objs == null || objs.isEmpty())
			return null;

		// get the selected catalogue
		Catalogue selectedCat = objs.iterator().next();
		// warn user if current catalogue is equal to selected one
		Catalogue currentCat = GlobalManager.getInstance().getCurrentCatalogue();
		if (currentCat != null && selectedCat.sameAs(currentCat)) {
			String msg = CBMessages.getString("BrowserMenu.OpenCatalogueWarningMsg");
			GlobalUtil.showDialog(shell, title, msg, SWT.ICON_WARNING);
			return null;
		}

		return selectedCat;
	}

	/**
	 * Get a list of chosen catalogues
	 * 
	 * @param shell
	 * @param title
	 * @param input
	 * @param multiSel
	 * @param columns
	 * @param okText
	 * @return
	 */
	private static Collection<Catalogue> chooseCatalogues(Shell shell, String title, Collection<Catalogue> input,
			boolean multiSel, String[] columns, String okText) {

		// Open the catalogue form to visualize the available catalogues and to select
		// which one has to be downloaded
		FormCataloguesList list = new FormCataloguesList(shell, title, input, multiSel);

		list.setOkButtonText(okText);

		// open the catalogue form with the following columns
		list.display(columns);

		return list.getSelection();
	}

	/**
	 * Ask to the user to select one or more catalogues and then download them.
	 * 
	 * @param shell
	 * @throws DOMException
	 * @throws Exception
	 */
	public void downloadCatalogue(final Shell shell) {

		// get a catalogue from the dcf ones
		String[] columns = { "label", "version", "status", "valid_from", "scopenote" };

		// ask a catalogue
		Collection<Catalogue> selectedCats = chooseCatalogues(shell,
				CBMessages.getString("FormCatalogueList.DownloadTitle"), Dcf.getDownloadableCat(), true, columns,
				CBMessages.getString("FormCataloguesList.DownloadCmd"));

		// no selection return
		if (selectedCats == null || selectedCats.isEmpty())
			return;

		if (selectedCats.size() == 1)
			downloadSingleCat(shell, selectedCats.iterator().next(), null);
		else
			downloadCatalogues(shell, CBMessages.getString("Download.MultiSuccessTitle"),
					CBMessages.getString("Download.MultiSuccessMessage"), selectedCats);

	}

	/**
	 * Download a single catalogue
	 * 
	 * @param shell
	 * @param catalogue
	 */
	private void downloadSingleCat(final Shell shell, final Catalogue catalogue, final Listener listener) {

		// show a progress bar
		final IProgressBar progressBar = new FormProgressBar(shell,
				CBMessages.getString("Download.ProgressDownloadTitle"));

		Runtime run = Runtime.getRuntime();
		// calculate the used memory
		long used = run.totalMemory() - run.freeMemory();
		// get the available max memory of the JVM in MB
		long freeMemory = (run.maxMemory() - used) / (1024 * 1024);

		// check if there is available memory
		if (freeMemory <= minimumMemory) {
			// get title and message for dialog
			String msg = CBMessages.getString("Download.MemoryFull", freeMemory);
			String title = CBMessages.getString("Download.ProgressDownloadTitle");
			// if negative answer return
			if (!MessageDialog.openConfirm(shell, title, msg))
				return;
		}

		// warn the user if to keep only the last version and hence remove the other
		// versions (TODO to be completed)
		// removeOldVersions(shell, catalogue);

		// start downloading the catalogue
		CatalogueDownloader catDown = new CatalogueDownloader(catalogue);

		catDown.setProgressBar(progressBar);

		// when finishes warn user
		catDown.setDoneListener(new ThreadFinishedListener() {

			@Override
			public void finished(final Thread thread, final int value, Exception e) {

				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						String message = null;
						int icon;

						if (value == ThreadFinishedListener.OK) {
							message = CBMessages.getString("Download.DownloadSuccessMessage");
							icon = SWT.ICON_INFORMATION;
						} else if (value == EXCEPTION) {
							if (e instanceof AttachmentNotFoundException)
								message = CBMessages.getString("ExportCatalogue.ErrorMessage");
							else if (e instanceof DetailedSOAPException) {
								message = GlobalUtil.getSOAPWarning((DetailedSOAPException) e)[1];
							} else {
								message = CBMessages.getString("ExportCatalogue.general.error");
							}
							icon = SWT.ICON_ERROR;
							progressBar.close();
						} else {
							message = CBMessages.getString("ExportCatalogue.general.error");
							icon = SWT.ICON_ERROR;
							progressBar.close();
						}

						// title with catalogue label
						String title = ((CatalogueDownloader) thread).getCatalogue().getLabel();

						// warn user
						GlobalUtil.showDialog(shell, title, message, icon);

						CatalogueDAO dao = new CatalogueDAO();
						Catalogue catalogueWithId = dao.getCatalogue(catalogue.getCode(), catalogue.getVersion(),
								catalogue.getCatalogueType());

						if (listener != null) {
							Event event = new Event();
							event.data = catalogueWithId;
							listener.handleEvent(event);
						}
					}
				});
			}
		});

		catDown.start();

	}

	/**
	 * method used for removing all old versions of a catalogue
	 * 
	 * @author shahaal
	 */
	@SuppressWarnings("unused")
	private void removeOldVersions(final Shell shell, Catalogue catalogue) {

		// get the production folder
		File[] productionFolder = new File(DatabaseManager.PRODUCTION_CAT_DB_FOLDER).listFiles();
		// iterate the folders
		for (File file : productionFolder) {
			// if there is a already a version of the catalogue to download
			if (file.isDirectory() && file.getName().equals("CAT_" + catalogue.getName() + "_DB")) {
				String title = "Replace catalogues";
				String msg = "Do you want to replace the exsitings catalogues with the latest version?";
				// if negative answer return
				if (!MessageDialog.openConfirm(shell, title, msg))
					return;

				// close the current catalogue using the exsisting close cat function
				if (listener != null)
					listener.handleEvent(null);

				// warn the user if to keep only the last version and remove the older ones
				try {
					GlobalUtil.deleteFileCascade(file);
				} catch (IOException e1) {
                    LOGGER.error("Error during delete of folders ", e1);
					e1.printStackTrace();
				}

			}
		}

	}

	/**
	 * Download a collection of catalogues
	 * 
	 * @param shell
	 * @param title
	 * @param msg
	 * @param cats
	 */
	private static void downloadCatalogues(final Shell shell, final String title, final String msg,
			final Collection<Catalogue> cats) {

		CatalogueDownloaderManager manager = new CatalogueDownloaderManager(1);

		// download all the dc catalogues
		final FormMultipleProgress dialog = new FormMultipleProgress(shell);

		// for each catalogue
		for (Catalogue cat : cats) {

			// add a progress row in the table
			final TableRow row = dialog.addRow(cat.getLabel());

			// prepare the download thread
			CatalogueDownloader downloader = new CatalogueDownloader(cat);

			// set the table bar as progress bar
			downloader.setProgressBar(row.getBar());

			// start the download of the catalogue
			manager.add(downloader);
		}

		// warn user when finished
		manager.setDoneListener(new Listener() {

			@Override
			public void handleEvent(Event arg0) {

				// warn user in the ui thread
				// and make the list of progresses closeable
				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						GlobalUtil.showDialog(shell, title, msg, SWT.ICON_INFORMATION);

						dialog.done();
					}
				});
			}
		});

		// start thread in a batch way
		manager.start();

		dialog.open();
	}

	/**
	 * Ask to the user the .ecf to import and import it. Update also ICT db
	 * 
	 * @author shahaal
	 * @param shell
	 * @param doneListener called when the import is finished
	 */
	public void importCatalogue(Shell shell, final ThreadFinishedListener doneListener) {

		// ask the file to the user
		String filename = GlobalUtil.showFileDialog(shell, CBMessages.getString("BrowserMenu.ImportCatalogueCmd"),
				new String[] { "*.ecf" }, SWT.OPEN);

		if (filename == null || filename.isEmpty())
			return;

		// ask for final confirmation
		int val = GlobalUtil.showDialog(shell, CBMessages.getString("EcfImport.WarnTitle"),
				CBMessages.getString("EcfImport.WarnMessage"), SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);

		// return if cancel was pressed
		if (val == SWT.CANCEL)
			return;

		CatalogueImporterThread importCat = new CatalogueImporterThread(filename, ImportFileFormat.ECF);

		FormProgressBar progressBar = new FormProgressBar(shell, CBMessages.getString("EcfImport.ImportEcfBarTitle"));

		importCat.setProgressBar(progressBar);

		importCat.addDoneListener(doneListener);

		importCat.start();

	}

	/**
	 * delete a specific catalogue
	 * 
	 * @param shell
	 */
	public void deleteCatalogue(final Shell shell, final Catalogue cat) {

		// create a list of the catalogue in order to call the cat destroyer
		ArrayList<Catalogue> catList = new ArrayList<Catalogue>(Arrays.asList(cat));

		// invoke deleter thread for catalogues
		CatalogueDestroyer deleter = new CatalogueDestroyer(catList);

		// when finished
		deleter.setDoneListener(new ThreadFinishedListener() {

			@Override
			public void finished(Thread thread, final int code, Exception e) {

				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						String msg;
						int icon;

						if (code == ThreadFinishedListener.OK) {
							msg = CBMessages.getString("Delete.OkMessage");
							icon = SWT.ICON_INFORMATION;
						} else {
							msg = CBMessages.getString("Delete.ErrorMessage");
							icon = SWT.ICON_WARNING;
						}

						// warn user
						GlobalUtil.showDialog(shell, CBMessages.getString("Delete.Title"), msg, icon);
					}
				});
			}
		});

		// progress bar for deleting catalogues
		final FormProgressBar progressBar = new FormProgressBar(shell,
				CBMessages.getString("FileMenu.DeleteCatalogue"));

		progressBar.open();

		deleter.setProgressBar(progressBar);

		deleter.start();
	}

	/**
	 * Ask to the user which catalogues he wants to delete and delete them.
	 * 
	 * @param shell
	 */
	public void deleteCatalogues(final Shell shell) {

		final CatalogueDAO catDao = new CatalogueDAO();

		ArrayList<Catalogue> myCatalogues = catDao.getMyCatalogues(Dcf.dcfType);

		// Order the catalogues by label name to make a better visualization
		Collections.sort(myCatalogues);

		// ask which catalogues to delete
		Collection<Catalogue> catalogues = chooseCatalogues(shell,
				CBMessages.getString("FormCatalogueList.DeleteTitle"), myCatalogues, true,
				new String[] { "label", "version", "status" }, CBMessages.getString("FormCatalogueList.DeleteCmd"));

		if (catalogues.isEmpty())
			return;

		// invoke deleter thread for catalogues
		CatalogueDestroyer deleter = new CatalogueDestroyer(catalogues);

		// when finished
		deleter.setDoneListener(new ThreadFinishedListener() {

			@Override
			public void finished(Thread thread, final int code, Exception e) {

				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						String msg;
						int icon;

						if (code == ThreadFinishedListener.OK) {
							msg = CBMessages.getString("Delete.OkMessage");
							icon = SWT.ICON_INFORMATION;
						} else {
							msg = CBMessages.getString("Delete.ErrorMessage");
							icon = SWT.ICON_WARNING;
						}

						// warn user
						GlobalUtil.showDialog(shell, CBMessages.getString("Delete.Title"), msg, icon);
					}
				});
			}
		});

		// progress bar for deleting catalogues
		final FormProgressBar progressBar = new FormProgressBar(shell,
				CBMessages.getString("FileMenu.DeleteCatalogue"));

		progressBar.open();

		deleter.setProgressBar(progressBar);

		deleter.start();
	}

	/**
	 * Ask to the user to select a data collection among the one in the
	 * {@code input}
	 * 
	 * @param shell
	 * @param title
	 * @param input
	 * @return
	 */
	private static DataCollection chooseDC(Shell shell, String title, String okText, Collection<DataCollection> input) {

		FormDataCollectionsList list = new FormDataCollectionsList(shell, title, input);

		list.setOkButtonText(okText);

		String code = FormDataCollectionsList.CODE;
		String desc = FormDataCollectionsList.DESCRIPTION;
		String activeFrom = FormDataCollectionsList.ACTIVE_FROM;
		String activeTo = FormDataCollectionsList.ACTIVE_TO;

		list.display(new String[] { code, activeFrom, activeTo, desc });

		return list.getFirstSelection();
	}

	/**
	 * Select a dc table config among the one passed in input
	 * 
	 * @param shell
	 * @param title
	 * @param input
	 * @return
	 */
	private static DCTableConfig chooseConfig(Shell shell, String title, String okText,
			Collection<DCTableConfig> input) {

		FormDCTableConfigsList list = new FormDCTableConfigsList(shell, title, input);

		list.setOkButtonText(okText);

		String name = FormDCTableConfigsList.TABLE_NAME;
		String var = FormDCTableConfigsList.VARIABLE_NAME;
		String cat = FormDCTableConfigsList.CAT_CODE;
		String hier = FormDCTableConfigsList.HIER_CODE;

		list.display(new String[] { name, var, cat, hier });

		return list.getFirstSelection();
	}

	/**
	 * Open a data collection
	 * 
	 * @param shell
	 */
	public DCTableConfig openDC(Shell shell) {

		DCDAO dcDao = new DCDAO();

		// ask for selecting a data collection
		final DataCollection dc = chooseDC(shell, CBMessages.getString("FormDCList.OpenTitle"),
				CBMessages.getString("FormDCList.OpenCmd"), dcDao.getAll());

		if (dc == null)
			return null;

		// show data collection tables and configs
		// if a data collection is selected
		DCTableConfig config = chooseConfig(shell, dc.getCode(), CBMessages.getString("FormDCList.OpenCmd"),
				dc.getTableConfigs());

		return config;
	}

	/**
	 * Download a data collection
	 * 
	 * @param shell
	 */
	public void downloadDC(final Shell shell) {

		// ask for selecting a data collection
		final DataCollection dc = chooseDC(shell, CBMessages.getString("FormDCList.Title"),
				CBMessages.getString("FormDCList.DownloadCmd"), Dcf.getDownloadableDC());

		// return if null
		if (dc == null)
			return;

		FormProgressBar progressBar = new FormProgressBar(shell, CBMessages.getString("DCDownload.ProgressBarTitle"));

		progressBar.open();

		// download the data collection
		DCDownloader downloader = new DCDownloader(dc);
		downloader.setProgressBar(progressBar);

		// when finished
		downloader.setDoneListener(new Listener() {

			@Override
			public void handleEvent(Event arg0) {

				// start downloading catalogues
				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						Collection<Catalogue> catToDownload = dc.getNewCatalogues();

						if (catToDownload.isEmpty()) {
							GlobalUtil.showDialog(shell, dc.getCode(), CBMessages.getString("DCDownload.EmptyDC"),
									SWT.ICON_INFORMATION);
							return;
						}

						// download the batch of catalogues
						downloadCatalogues(shell, dc.getCode(), CBMessages.getString("DCDownload.Success"),
								catToDownload);
					}
				});
			}
		});

		downloader.start();
	}

	/**
	 * listener called when need to close the current catalogue
	 * 
	 * @param menuListener
	 */
	public void addCloseCatalogueListener(Listener listener) {
		this.listener = listener;
	}
}
