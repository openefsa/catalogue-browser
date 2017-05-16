package dcf_reserve_util;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue_object.Catalogue;
import dcf_log_util.LogCodeFoundListener;
import dcf_log_util.LogQuerist;
import dcf_webservice.DcfResponse;
import dcf_webservice.Reserve;
import dcf_webservice.ReserveLevel;
import ui_progress_bar.FormProgressBar;

/**
 * Reserver which is used to start a reserve operation from scratch,
 * that is, dcf webservice request + polling for retriving the reserve log.
 * Note that we should use this class only once per reserve request, in the
 * sense that if we need to check only the reserve log we should use the {@link LogQuerist}
 * class. In fact, that class does not send another reserve request, it
 * just checks the reserve log.
 * @author avonva
 *
 */
public class Reserver extends ReserveSkeleton {

	private FormProgressBar progressBar;
	private String reserveDescription;  // reserve description for the reserve operation
	private DcfResponse response;
	
	private ReserveStartedListener startListener;
	private Listener newVersionListener;
	private ReserveFinishedListener finishListener;
	private LogCodeFoundListener logCodeListener;
	
	// called just before reserving the catalogue in the db
	private Listener startReserveListener;
	
	/**
	 * Start a whole reserve operation from scratch, that is,
	 * we check if we can reserve the catalogue, if the requirements
	 * are met and then we send a NEW reserve request to the dcf and
	 * check its correctness.
	 * @param catalogue the catalogue we want to reserve
	 * @param reserveLevel the reserve level we want
	 * @param reserveDescription the reserve description
	 */
	public Reserver( Catalogue catalogue, ReserveLevel reserveLevel, 
			String reserveDescription ) {
		
		super( catalogue, reserveLevel );
		this.reserveDescription = reserveDescription;
	}
	
	/**
	 * Set the progress bar for the possible import process
	 * needed to import the last internal version of the catalogue
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}
	
	@Override
	public FormProgressBar getProgressBar() {
		return progressBar;
	}

	@Override
	public void newVersionDownloaded(Catalogue newVersion) {
		
		// open the new version of the catalogue
		// this also refreshes the graphics
		newVersion.open();
	}
	
	@Override
	public boolean canReserve( Catalogue catalogue, 
			ReserveLevel reserveLevel ) {
		
		// instantiate the reserve soap action
		Reserve reserve = new Reserve();
		
		if ( logCodeListener != null )
			reserve.setLogCodeListener( logCodeListener );
		
		// reserve the catalogue and save the response
		this.response = reserve.reserve( catalogue, 
				reserveLevel, reserveDescription );
		
		// we can reserve only if the reserve
		// operation went fine
		return response == DcfResponse.OK;
	}

	@Override
	public void reserveStarted( Catalogue catalogue, 
			ReserveLevel reserveLevel, ReserveResult reserveLog ) {
		
		// call the start listener if it was set
		if ( startListener != null )
			startListener.reserveStarted( reserveLog );
	}

	@Override
	public void reserveFinished(Catalogue catalogue, ReserveLevel reserveLevel) {

		// call the finish listener if it was set
		if ( finishListener != null )
			finishListener.reserveFinished( catalogue, response );

		// at the end of the process
		// if busy dcf, start the retry process
		// always in background on the current request
		// if the dcf will not get free and we close the
		// catalogue browser, the pending reserve will be
		// re-launched as soon as we will login into the dcf
		// note that this action needs to be the last one
		// since it blocks the thread until we find a log
		if ( response == DcfResponse.BUSY ) {

			// get the pending reserve from the busy response
			PendingReserve pr = response.getPendingReserve();

			// start the retry process and set the
			// finish listener to update again the
			// caller when the log is found
			// note that here we also enable the force editing
			// for the catalogue in the pr.retry() function
			LogQuerist logQuerist = new LogQuerist( pr );
			logQuerist.setFinishListener( finishListener );
			logQuerist.setNewVersionListener( newVersionListener );
			logQuerist.start();

			// note that when the new finish listener will
			// be called the response cannot be BUSY because
			// we iterate until we find the log
		}
	}

	@Override
	public void lastVersionDownloadStarted() {
		if ( newVersionListener != null )
			newVersionListener.handleEvent( new Event() );
	}

	@Override
	public void reserveIsStarting(Catalogue catalogue, ReserveLevel reserveLevel) {
		if ( startReserveListener != null )
			startReserveListener.handleEvent( new Event() );
	}
	
	
	/**
	 * Set the listener which is called when the reserve operation starts
	 * @param startListener
	 */
	public void setStartListener(ReserveStartedListener startListener) {
		this.startListener = startListener;
	}
	
	/**
	 * Set a listener which is called when a new version of the
	 * catalogue is downloaded
	 * @param newVersionListener
	 */
	public void setNewVersionListener(Listener newVersionListener) {
		this.newVersionListener = newVersionListener;
	}
	
	/**
	 * Listener called when the log code of the reserve
	 * action is retrieved.
	 * @param logCodeListener
	 */
	public void setLogCodeListener(LogCodeFoundListener logCodeListener) {
		this.logCodeListener = logCodeListener;
	}
	
	/**
	 * Set the listener which is called when the reserve operation finishes
	 * @param finishlistener
	 */
	public void setFinishlistener(ReserveFinishedListener finishListener) {
		this.finishListener = finishListener;
	}
	
	/**
	 * Set the listener called when the catalogue is being reserved
	 * (just before)
	 * @param startReserveListener
	 */
	public void setStartReserveListener(Listener startReserveListener) {
		this.startReserveListener = startReserveListener;
	}
}
