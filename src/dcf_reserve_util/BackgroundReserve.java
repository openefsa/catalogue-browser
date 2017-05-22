package dcf_reserve_util;

import catalogue_object.Catalogue;
import dcf_manager.Dcf;
import dcf_webservice.Reserve;
import dcf_webservice.ReserveLevel;
import ui_progress_bar.FormProgressBar;

/**
 * This thread is used to perform a reserve request in
 * background, instead of doing it in foreground
 * calling {@link Reserve#reserve(Catalogue, ReserveLevel, String)}.
 * In practice, we always use the reserve thread, since we do not
 * want to freeze the user interface waiting for the dcf response.
 * @author avonva
 *
 */
public class BackgroundReserve extends Thread {
	
	private Catalogue catalogue;      // the catalogue we want to reserve
	private ReserveLevel level;       // the reserve level we want for the catalogue
	private String description;       // the description of the reserve request
	
	private PendingActionListener listener; // listen to reserve events
	
	private FormProgressBar progressBar;
	
	/**
	 * Initialize the a reserve parameters.
	 * @param catalogue the catalogue we want to reserve
	 * @param reserveLevel the reserve level we want
	 * @param reserveDescription the reserve description
	 */
	public BackgroundReserve( Catalogue catalogue, ReserveLevel level, 
			String description ) {

		this.catalogue = catalogue;
		this.level = level;
		this.description = description;
	}
	
	/**
	 * Set the listener for reserve events.
	 * @param listener
	 */
	public void setListener(PendingActionListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Set the progress bar which will be used if a new
	 * version of the catalogue is downloaded
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Start the reserve process in background
	 */
	@Override
	public void run() {
		
		// instantiate the reserve soap action
		Reserve reserve = new Reserve();
		
		// notify that we are ready to perform the reserve
		if ( listener != null )
			listener.requestPrepared();
		
		// start the reserve process for the catalogue
		PendingReserve pr = reserve.reserve( catalogue, level, description );
		
		// if we have successfully sent the request,
		// we can notify the caller that we have
		// the log code of the request saved in the database
		if ( listener != null && pr != null )
			listener.requestSent( pr, pr.getLogCode() );
		
		// start the pending reserve we have just created
		Dcf dcf = new Dcf();
		dcf.setProgressBar( progressBar );
		dcf.startPendingReserve( pr, listener );
	}
}
