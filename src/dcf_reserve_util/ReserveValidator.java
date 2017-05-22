package dcf_reserve_util;

import dcf_webservice.PendingAction;
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

	private PendingAction pendingAction;
	
	private FormProgressBar progressBar;
	private PendingActionListener listener;
	
	/**
	 * Initialize the reserve validator.
	 * @param pendingReserve
	 */
	public ReserveValidator( PendingAction pendingAction, 
			PendingActionListener listener ) {
		
		this.pendingAction = pendingAction;
		this.listener = listener;
	}

	@Override
	public void run() {
		
		pendingAction.setListener( listener );
		pendingAction.setProgressBar( progressBar );
		
		// start the pending reserve process
		pendingAction.start();
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
