package ui_main_menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.DOMException;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
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
import form_objects_list.FormCataloguesList;
import form_objects_list.FormDCTableConfigsList;
import form_objects_list.FormDataCollectionsList;
import import_catalogue.CatalogueImporter.ImportFileFormat;
import import_catalogue.CatalogueImporterThread;
import messages.Messages;
import ui_main_panel.FormLocalCatalogueName;
import ui_main_panel.OldCatalogueReleaseDialog;
import ui_progress_bar.FormMultipleProgress;
import ui_progress_bar.FormProgressBar;
import ui_progress_bar.TableMultipleProgress.TableRow;
import utilities.GlobalUtil;

/**
 * Actions which are performed when menu items in the
 * File main menu are pressed. This was done to hide from
 * the user interface class the code details and to 
 * reuse possibly the file actions for testing purposes.
 * @author avonva
 *
 */
public class FileActions {

	/**
	 * Ask to the user the new catalogue code and create a new local catalogue.
	 * @param shell
	 */
	public static void createNewLocalCatalogue ( Shell shell ) {
		
		FormLocalCatalogueName dialog = new FormLocalCatalogueName ( shell );

		String catalogueCode = dialog.open();
		
		// if null the cancel button was pressed
		if ( catalogueCode == null )
			return;
		
		// set the wait cursor
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_WAIT );
		
		// create a database for the new catalogue
		// but if the catalogue already exists show an error dialog
		try {
			CatalogueCreator.newLocalCatalogue( catalogueCode );
		}
		catch ( DuplicatedCatalogueException exception ) {
			
			GlobalUtil.showErrorDialog( shell, 
					Messages.getString( "BrowserMenu.NewLocalCatErrorTitle" ),
					Messages.getString( "BrowserMenu.NewLocalCatErrorMessage" ) );
			
			GlobalUtil.setShellCursor( shell , SWT.CURSOR_ARROW );
			
			return;
		}


		// reset the standard cursor
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_ARROW );
		
		// warn user
		GlobalUtil.showDialog(shell, 
				Messages.getString("NewLocalCat.DoneTitle"),
				Messages.getString("NewLocalCat.DoneMessage"), 
				SWT.ICON_INFORMATION );
	}
	/**
	 * Open a catalogue from the available list of catalogues
	 * in the main user interface
	 * @param shell
	 * @param catalogue
	 * @return true if the catalogue was opened
	 */
	public static Catalogue openCatalogue( Shell shell ) {

		// get all the catalogues downloaded in the pc
		CatalogueDAO catDao = new CatalogueDAO();
		ArrayList <Catalogue> myCatalogues = catDao.getMyCatalogues( Dcf.dcfType );

		// Order the catalogues by label name to make a better visualization
		Collections.sort( myCatalogues );

		// show columns based on permissions
		final User user = User.getInstance();

		String[] columns;
		// display only the columns that we want
		if ( user.isCatManager() )
			columns = new String[] {"label", "version", "status", "reserve" };
		else
			columns = new String[] {"label", "version", "status" };

		// ask a catalogue
		Catalogue catalogue = chooseCatalogue( shell, 
				Messages.getString("FormCataloguesList.OpenTitle"),
				myCatalogues, columns, 
				Messages.getString("FormCataloguesList.OpenCmd") );

		// return if no catalogue selected
		if ( catalogue == null )
			return null;

		boolean ok = performCatalogueChecks ( shell, catalogue );

		if ( !ok )
			return null;
		
		// open the catalogue when the dialog is closed
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_WAIT );

		// open the catalogue
		catalogue.open();

		GlobalUtil.setShellCursor( shell, SWT.CURSOR_ARROW );

		return catalogue;
	}

	/**
	 * Make opening checks on the catalogue and warn
	 * the user accordingly
	 * @param shell
	 * @param catalogue
	 * @return
	 */
	public static boolean performCatalogueChecks( Shell shell, Catalogue catalogue ) {

		User user = User.getInstance();
		
		// check if the catalogue is deprecated
		if ( catalogue.isDeprecated() ) {

			int val = GlobalUtil.showDialog( shell, 
					catalogue.getLabel(), 
					Messages.getString("BrowserMenu.CatalogueDeprecatedMessage"), 
					SWT.ICON_WARNING | SWT.YES | SWT.NO );

			if ( val == SWT.NO )
				return false;
		}

		// if the user is logged in we can check the updates
		else if ( user.isLogged() ) {

			// check if there is a catalogue update
			boolean hasUpdate = catalogue.hasUpdate();

			// check if the update was already downloaded
			boolean alreadyDownloaded = catalogue.isLastReleaseAlreadyDownloaded();

			// if there is a new version and we have not downloaded it yet
			// we warn the user that a new version is available
			if ( hasUpdate && !alreadyDownloaded ) {

				OldCatalogueReleaseDialog dialog = 
						new OldCatalogueReleaseDialog( shell, catalogue );

				int val = dialog.open();
				
				if ( val == SWT.CANCEL )
					return false;
			}
		}
		else {

			// only for official catalogues
			if ( !catalogue.isLocal() ) {
				// if we are not logged in, simply warn the user that we cannot
				// be sure that this is the last release
				MessageBox mb = new MessageBox( shell, SWT.ICON_INFORMATION );
				mb.setText( Messages.getString("BrowserMenu.CatalogueReleaseInfoTitle") );
				mb.setMessage( Messages.getString("BrowserMenu.CatalogueReleaseInfoMessage") );
				mb.open();
			}
		}
		
		return true;
	}

	/**
	 * Ask to the user to select a catalogue from the
	 * {@code input} list.
	 * @param shell
	 * @param title title of the form
	 * @param input list of choosable catalogues
	 * @param multiSel can we select multiple catalogues?
	 * @param columns table columns to show
	 * @return
	 */
	private static Catalogue chooseCatalogue( Shell shell, String title, 
			Collection<Catalogue> input, String[] columns, String okText ) {

		Collection<Catalogue> objs = chooseCatalogues(shell, title, input, 
				false, columns, okText);
		
		if ( objs.isEmpty() )
			return null;
		
		return objs.iterator().next();
	}
	
	/**
	 * Get a list of chosen catalogues
	 * @param shell
	 * @param title
	 * @param input
	 * @param multiSel
	 * @param columns
	 * @param okText
	 * @return
	 */
	private static Collection<Catalogue> chooseCatalogues ( Shell shell, String title, 
			Collection<Catalogue> input, boolean multiSel, String[] columns, String okText ) {

		// Open the catalogue form to visualize the available catalogues and to select
		// which one has to be downloaded
		FormCataloguesList list = new FormCataloguesList ( shell, title, input, multiSel );

		list.setOkButtonText( okText );

		// open the catalogue form with the following columns
		list.display( columns );

		return list.getSelection();
	}

	/**
	 * Ask to the user to select a catalogue and then download it.
	 * @param shell
	 * @throws DOMException
	 * @throws Exception
	 */
	public static void downloadCatalogue ( final Shell shell ) {

		// get a catalogue from the dcf ones
		String[] columns = { "label", "version", "status", "valid_from", "scopenote" };

		// ask a catalogue
		Catalogue selectedCat = chooseCatalogue ( shell, 
				Messages.getString("FormCatalogueList.DownloadTitle"),
				Dcf.getDownloadableCat(), columns,
				Messages.getString("FormCataloguesList.DownloadCmd") );

		// no selection return
		if ( selectedCat == null )
			return;

		// show a progress bar
		FormProgressBar progressBar = new FormProgressBar( shell, 
				Messages.getString( "Download.ProgressDownloadTitle" ) );

		// start downloading the catalogue
		CatalogueDownloader catDown = new CatalogueDownloader( selectedCat );

		catDown.setProgressBar( progressBar );

		// when finishes warn user
		catDown.setDoneListener( new ThreadFinishedListener() {

			@Override
			public void finished(final Thread thread, final int value) {

				shell.getDisplay().asyncExec( new Runnable() {

					@Override
					public void run() {

						String message;
						int icon;

						if ( value == ThreadFinishedListener.OK ) {
							message = Messages.getString( "Download.DownloadSuccessMessage" );
							icon = SWT.ICON_INFORMATION;
						}
						else {
							message = Messages.getString("ExportCatalogue.ErrorMessage");
							icon = SWT.ICON_ERROR;
						}

						// title with catalogue label
						String title = ((CatalogueDownloader) thread).getCatalogue().getLabel();

						// warn user
						GlobalUtil.showDialog( shell, title, message, icon );
					}
				});
			}
		});

		catDown.start();
	}

	/**
	 * Ask to the user the .ecf to import and import it.
	 * @param shell
	 * @param doneListener called when the import is finished
	 */
	public static void importCatalogue( Shell shell, 
			final ThreadFinishedListener doneListener ) {
		
		// ask the file to the user
		String filename = GlobalUtil.showFileDialog( shell, 
				Messages.getString("BrowserMenu.ImportCatalogueCmd"), 
				new String[] { "*.ecf" }, SWT.OPEN );

		if ( filename == null || filename.isEmpty() )
			return;

		// ask for final confirmation
		int val = GlobalUtil.showDialog( shell, 
				Messages.getString("EcfImport.WarnTitle"), 
				Messages.getString( "EcfImport.WarnMessage" ), 
				SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION );

		// return if cancel was pressed
		if ( val == SWT.CANCEL )
			return;
		
		CatalogueImporterThread importCat = 
				new CatalogueImporterThread(
						filename, ImportFileFormat.ECF );
		
		FormProgressBar progressBar = new FormProgressBar( shell, 
				Messages.getString("EcfImport.ImportEcfBarTitle") );

		importCat.setProgressBar( progressBar );
		
		importCat.addDoneListener( doneListener );
		
		importCat.start();
	}
	
	/**
	 * Ask to the user which catalogues he wants to delete
	 * and delete them.
	 * @param shell
	 */
	public static void deleteCatalogue( final Shell shell ) {
		
		final CatalogueDAO catDao = new CatalogueDAO();
		
		ArrayList <Catalogue> myCatalogues = catDao.getMyCatalogues( Dcf.dcfType );
		
		// Order the catalogues by label name to make a better visualization
		Collections.sort( myCatalogues );
		
		// ask which catalogues to delete
		Collection<Catalogue> catalogues = chooseCatalogues( shell, 
				Messages.getString("FormCatalogueList.DeleteTitle"), 
				myCatalogues, true, new String[] {"label", "version", "status"}, 
				Messages.getString("FormCatalogueList.DeleteCmd") );
		
		if ( catalogues.isEmpty() )
			return;
		
		// invoke deleter thread for catalogues
		CatalogueDestroyer deleter = new CatalogueDestroyer ( catalogues );
		
		// when finished
		deleter.setDoneListener( new ThreadFinishedListener() {
			
			@Override
			public void finished(Thread thread, final int code) {
				
				shell.getDisplay().asyncExec( new Runnable() {
					
					@Override
					public void run() {
						
						String msg;
						int icon;
						
						if ( code == ThreadFinishedListener.OK ) {
							msg = Messages.getString( "Delete.OkMessage" );
							icon = SWT.ICON_INFORMATION;
						}
						else {
							msg = Messages.getString( "Delete.ErrorMessage" );
							icon = SWT.ICON_WARNING;
						}
						
						// warn user
						GlobalUtil.showDialog( shell, 
								Messages.getString( "Delete.Title" ), 
								msg, icon );
					}
				});
			}
		});
		
		// progress bar for deleting catalogues
		final FormProgressBar progressBar = new FormProgressBar( shell, 
				Messages.getString("FileMenu.DeleteCatalogue") );
		
		deleter.setProgressBar( progressBar );
		
		deleter.start();
	}
	
	/**
	 * Ask to the user to select a data collection among
	 * the one in the {@code input}
	 * @param shell
	 * @param title
	 * @param input
	 * @return
	 */
	private static DataCollection chooseDC ( Shell shell, 
			String title, String okText, Collection<DataCollection> input ) {

		FormDataCollectionsList list = new FormDataCollectionsList(
				shell, title, input );

		list.setOkButtonText( okText );

		String code = FormDataCollectionsList.CODE;
		String desc = FormDataCollectionsList.DESCRIPTION;
		String activeFrom = FormDataCollectionsList.ACTIVE_FROM;
		String activeTo = FormDataCollectionsList.ACTIVE_TO;

		list.display( new String[] { code, activeFrom, activeTo, desc } );

		return list.getFirstSelection();
	}

	/**
	 * Select a dc table config among the one passed in input
	 * @param shell
	 * @param title
	 * @param input
	 * @return
	 */
	private static DCTableConfig chooseConfig ( Shell shell, 
			String title, String okText, Collection<DCTableConfig> input ) {

		FormDCTableConfigsList list = new FormDCTableConfigsList( 
				shell, title, input );

		list.setOkButtonText( okText );

		String name = FormDCTableConfigsList.TABLE_NAME;
		String var = FormDCTableConfigsList.VARIABLE_NAME;
		String cat = FormDCTableConfigsList.CAT_CODE;
		String hier = FormDCTableConfigsList.HIER_CODE;

		list.display( new String[] { name, var, cat, hier } );

		return list.getFirstSelection();
	}

	/**
	 * Open a data collection
	 * @param shell
	 */
	public static DCTableConfig openDC ( Shell shell ) {

		DCDAO dcDao = new DCDAO();

		// ask for selecting a data collection
		final DataCollection dc = chooseDC ( shell, 
				Messages.getString( "FormDCList.OpenTitle" ),
				Messages.getString( "FormDCList.OpenCmd" ),
				dcDao.getAll() );

		if ( dc == null )
			return null;

		// show data collection tables and configs
		// if a data collection is selected
		DCTableConfig config = chooseConfig( shell, 
				dc.getCode(), 
				Messages.getString( "FormDCList.OpenCmd" ),
				dc.getTableConfigs() );

		return config;
	}

	/**
	 * Download a data collection
	 * @param shell
	 */
	public static void downloadDC( final Shell shell ) {

		// ask for selecting a data collection
		final DataCollection dc = chooseDC ( shell, 
				Messages.getString( "FormDCList.Title" ),
				Messages.getString( "FormDCList.DownloadCmd" ),
				Dcf.getDownloadableDC() );

		// return if null
		if ( dc == null )
			return;
		
		FormProgressBar progressBar = 
				new FormProgressBar(shell, 
						Messages.getString( "DCDownload.ProgressBarTitle" ) );

		progressBar.open();

		// download the data collection
		DCDownloader downloader = new DCDownloader( dc );
		downloader.setProgressBar( progressBar );
		
		// when finished
		downloader.setDoneListener( new Listener() {

			@Override
			public void handleEvent(Event arg0) {

				// start downloading catalogues
				shell.getDisplay().asyncExec( new Runnable() {

					@Override
					public void run() {
						
						// download all the dc catalogues
						final FormMultipleProgress dialog = new FormMultipleProgress( shell );

						CatalogueDownloaderManager manager = 
								new CatalogueDownloaderManager( 1 );
						
						Collection<Catalogue> catToDownload = dc.getNewCatalogues();
						
						if ( catToDownload.isEmpty() ) {
							GlobalUtil.showDialog(
									shell, 
									dc.getCode(), 
									Messages.getString( "DCDownload.EmptyDC" ), 
									SWT.ICON_INFORMATION );
							return;
						}
						
						// for each catalogue
						for ( Catalogue cat : catToDownload ) {
							
							// add a progress row in the table
							final TableRow row = dialog.addRow( cat.getLabel() );
							
							// prepare the download thread
							CatalogueDownloader downloader = new CatalogueDownloader( cat );
							
							// set the table bar as progress bar
							downloader.setProgressBar( row.getBar() );

							// start the download of the catalogue
							manager.add( downloader );
						}
						
						// warn user when finished
						manager.setDoneListener( new Listener() {
							
							@Override
							public void handleEvent(Event arg0) {
								
								// warn user in the ui thread
								// and make the list of progresses closeable
								shell.getDisplay().asyncExec( new Runnable() {
									
									@Override
									public void run() {

										GlobalUtil.showDialog(
												shell, 
												dc.getCode(), 
												Messages.getString( "DCDownload.Success" ), 
												SWT.ICON_INFORMATION );
										
										dialog.done();
									}
								});
							}
						});
						
						// start thread in a batch way
						manager.start();

						dialog.open();

					}
				});
			}
		});

		downloader.start();
	}
}
