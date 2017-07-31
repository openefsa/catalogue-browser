package catalogue_generator;

import javax.xml.soap.SOAPException;

import catalogue.Catalogue;
import messages.Messages;
import ui_progress_bar.FormProgressBar;
import ui_progress_bar.IProgressBar;

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
	private IProgressBar progressBar;
	private Catalogue catalogue;
	private boolean finished;
	
	/**
	 * Download and import in the application database 
	 * the selected {@code catalogue}
	 * @param catalogue
	 */
	public CatalogueDownloader( Catalogue catalogue ) {
		this.catalogue = catalogue;
		finished = false;
	}
	
	@Override
	public void run() {
		try {
			downloadAndImport();
		} catch (SOAPException e) {
			stop ( ThreadFinishedListener.EXCEPTION );
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
		boolean ok = catalogue.downloadAndImport( progressBar, 90,
				new ThreadFinishedListener() {
			
			@Override
			public void finished(Thread thread, int code) {
				callListener ( code );
				finished = true;
			}
		});
		
		// if file not found
		if ( !ok ) {
			stop ( ThreadFinishedListener.ERROR );
			return;
		}
	}
	
	/**
	 * Stop the process
	 * @param code
	 */
	private void stop( int code ) {
		progressBar.stop( Messages.getString( "DCDownload.MissingAttachment" ) );
		callListener ( code );
		finished = true;
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
	public void setProgressBar ( IProgressBar progressBar ) {
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
	 * Check if the thread has finished its work or not
	 * @return
	 */
	public boolean isFinished() {
		return finished;
	}
	
	/**
	 * Get the catalogue which is being downloaded
	 * @return
	 */
	public Catalogue getCatalogue() {
		return catalogue;
	};
}
