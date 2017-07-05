package ui_main_menu;

import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import data_collection.DCDAO;
import data_collection.DCDownloader;
import data_collection.DCTableConfig;
import data_collection.DataCollection;
import dcf_manager.Dcf;
import dcf_user.User;
import form_objects_list.FormDCTableConfigsList;
import form_objects_list.FormDataCollectionsList;
import messages.Messages;
import ui_main_panel.OldCatalogueReleaseDialog;
import ui_progress_bar.FormProgressBar;
import utilities.GlobalUtil;

public class FileActions {
	
	/**
	 * Open a catalogue from the available list of catalogues
	 * in the main user interface
	 * @param shell
	 * @param catalogue
	 * @return true if the catalogue was opened
	 */
	public static boolean openCatalogue( Shell shell, Catalogue catalogue ) {
		
		User user = User.getInstance();
		
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
				
				dialog.open();
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

		// open the catalogue when the dialog is closed
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_WAIT );

		// open the catalogue
		catalogue.open();

		GlobalUtil.setShellCursor( shell, SWT.CURSOR_ARROW );
		
		return true;
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
		downloader.setDoneListener( new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				
				// warn user in UI thread
				shell.getDisplay().asyncExec( new Runnable() {
					
					@Override
					public void run() {
						GlobalUtil.showDialog(
								shell, 
								dc.getCode(), 
								Messages.getString( Messages.getString( "DCDownload.Success" ) ), 
								SWT.ICON_INFORMATION );
					}
				});
			}
		});

		downloader.start();
	}
}
