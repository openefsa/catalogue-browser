package ui_main_panel;

import java.io.File;
import java.io.IOException;

import javax.xml.soap.SOAPException;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.DOMException;

import catalogue.Catalogue;
import dcf_manager.Dcf;
import import_catalogue.ImportCatalogueThread;
import import_catalogue.ImportCatalogueThread.ImportFileFormat;
import messages.Messages;
import ui_progress_bar.FormProgressBar;
import utilities.GlobalUtil;

/**
 * Class to manage the download action of a catalogue
 * @author avonva
 *
 */
public class DownloadCatalogueFrom {

	private Listener doneListener;
	private FormProgressBar progressBar;

	/**
	 * Listener called when an operation of this class is finished
	 * @param doneListener
	 */
	public void addDoneListener ( Listener doneListener ) {
		this.doneListener = doneListener;
	}

	/**
	 * Add a progress bar to the process
	 * @param progressBar
	 */
	public void setProgressBar ( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}


	/**
	 * Open the catalogue list dialog in order to decide which catalogue we want to download
	 * @param shell
	 * @throws DOMException
	 * @throws Exception
	 */
	public void display ( final Shell shell ) throws DOMException, Exception {
		
		// Open the catalogue form to visualize the available catalogues and to select
		// which one has to be downloaded
		FormCataloguesList catalogueForm = new FormCataloguesList ( shell, 
				Messages.getString("BrowserMenu.DownloadCatalogueListTitle"),
				Dcf.getDownloadableCat(), false );

		// open the catalogue form with the following columns
		catalogueForm.display( new String[] { "label", "version", "status", 
				"valid_from", "scopenote" });

		// set the listener which is called if catalogues are correctly chosen
		catalogueForm.addListener( new Listener() {

			@Override
			public void handleEvent( final Event event ) {

				// thread to allow refreshing the progress bar
				new Thread ( new Runnable() {

					@Override
					public void run() {

						// get the selected catalogue from the form
						final Catalogue selectedCat = (Catalogue) event.data;

						if ( !downloadCatalogue( selectedCat ) ) {
							
							shell.getDisplay().asyncExec( new Runnable() {
								
								@Override
								public void run() {
									if ( progressBar != null )
										progressBar.close();
									
									GlobalUtil.showErrorDialog(shell, 
											Messages.getString("ExportCatalogue.ErrorTitle"), 
											Messages.getString("ExportCatalogue.ErrorMessage"));
								}
							});
						}
					}
				} ).start();

				// TODO manage the import threads! => for now we allow only one catalogue at a time
				// for each selected catalogue create the record into the master table
				// and create its related database
			}
		});
	}

	/**
	 * Get the catalogue. add the metadata to the master table and create the db related to the catalogue.
	 * The catalogue data are downloaded from the dcf. The downloaded catalogue is in xml format and
	 * it will be converted into xlsx format to be imported
	 * @param catalogue
	 */
	private boolean downloadCatalogue ( final Catalogue catalogue ) {

		// Download the catalogue data

		// show the progress bar
		if ( progressBar != null ) {

			progressBar.setLabel( Messages.getString( "DownloadCatalogue.ProgressBarDownload" ) );
			progressBar.addProgress( 10 );
		}

		System.out.println ( "Downloading " + catalogue );

		// filename of the xml catalogue
		final String catalogueXmlFilename = catalogue.getCode() + ".xml";

		// ask for exporting catalogue to the dcf
		// export the catalogue and save its attachment into an xml file
		Dcf dcf = new Dcf();

		try {
			dcf.exportCatalogue( catalogue, 
					catalogueXmlFilename);
		} catch (SOAPException e) {
			e.printStackTrace();
			return false;
		}
		
		// set the catalogue type according to the dcf one
		catalogue.setCatalogueType( Dcf.dcfType );
		
		ImportCatalogueThread importCat = 
				new ImportCatalogueThread( catalogueXmlFilename, 
						ImportFileFormat.XML );

		if ( progressBar != null )
			importCat.setProgressBar( progressBar );
		
		// set the listener
		importCat.addDoneListener( new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {

				try {
					GlobalUtil.deleteFileCascade( new File( catalogueXmlFilename ) );
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if ( doneListener != null )
					doneListener.handleEvent( arg0 );
			}
		} );
		
		importCat.start();
		
		return true;
	}
}
