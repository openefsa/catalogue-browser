package catalogue_generator;

import javax.xml.soap.SOAPException;

import catalogue.Catalogue;
import messages.Messages;
import ui_progress_bar.FormProgressBar;

/**
 * Thread used to download a catalogue in background. If needed,
 * a progress bar can be set using {@link #setProgressBar(FormProgressBar)}.
 * If you need to perform actions when the download is finished, specify them
 * in the {@link ThreadFinishedListener} using the
 * {@link #setDoneListener(ThreadFinishedListener)} method.
 * @author avonva
 *
 */
public class CatalogueDownloader extends Thread {

	private ThreadFinishedListener doneListener;
	private FormProgressBar progressBar;
	private Catalogue catalogue;
	
	/**
	 * Download and import in the application database 
	 * the selected {@code catalogue}
	 * @param catalogue
	 */
	public CatalogueDownloader( Catalogue catalogue ) {
		this.catalogue = catalogue;
	}
	
	@Override
	public void run() {
		try {
			downloadAndImport();
		} catch (SOAPException e) {
			callListener ( ThreadFinishedListener.EXCEPTION );
		}
	}
	
	/**
	 * Get the catalogue. add the metadata to the master table and 
	 * create the db related to the catalogue.
	 * The catalogue data are downloaded from the dcf. 
	 * The downloaded catalogue is in xml format and
	 * it will be converted into xlsx format to be imported
	 * @param catalogue
	 * @throws SOAPException 
	 */
	private void downloadAndImport () throws SOAPException {

		// show the progress bar
		if ( progressBar != null ) {
			progressBar.setLabel( Messages.getString( "DownloadCatalogue.ProgressBarDownload" ) );
			progressBar.addProgress( 10 );
		}

		// download and import the catalogue
		boolean ok = catalogue.downloadAndImport( progressBar, 
				new ThreadFinishedListener() {
			
			@Override
			public void finished(Thread thread, int code) {
				callListener ( ThreadFinishedListener.OK );
			}
		});
		
		// if file not found
		if ( !ok ) {
			callListener ( ThreadFinishedListener.ERROR );
			return;
		}
	}
	
	/**
	 * Call the done listener ({@link #doneListener})
	 * @param correct
	 */
	private void callListener ( int code ) {
		if ( doneListener != null )
			doneListener.finished( this, code );
	}
	
	/**
	 * Add a progress bar to the process
	 * @param progressBar
	 */
	public void setProgressBar ( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Listener called when the thread finishes its work
	 * @param doneListener
	 */
	public void setDoneListener(ThreadFinishedListener doneListener) {
		this.doneListener = doneListener;
	}
	
	/**
	 * Get the catalogue which is being downloaded
	 * @return
	 */
	public Catalogue getCatalogue() {
		return catalogue;
	};
}
