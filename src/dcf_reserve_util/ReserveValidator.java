package dcf_reserve_util;

import ui_progress_bar.FormProgressBar;

/**
 * This thread starts in background a
 * pending reserve process to retrieve its log document
 * from the dcf and to assess if the reserve request
 * succeeded or not. All the events
 * are captured by the {@link #reserveListener}
 * @author avonva
 *
 */
public class ReserveValidator extends Thread {

	private PendingReserve pendingReserve;
	
	private FormProgressBar progressBar;
	private ReserveListener listener;
	
	/**
	 * Initialize the reserve validator.
	 * @param pendingReserve
	 */
	public ReserveValidator( PendingReserve pendingReserve, 
			ReserveListener listener ) {
		
		this.pendingReserve = pendingReserve;
		this.listener = listener;
	}

	@Override
	public void run() {
		
		pendingReserve.setListener( listener );
		pendingReserve.setProgressBar( progressBar );
		
		// start the pending reserve process
		pendingReserve.start();
	}
	
	/**
	 * Set the progress bar if we want to show it
	 * in the possible import process of new versions
	 * of the catalogue
	 * @return
	 */
	public void setProgressBar( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}
}
