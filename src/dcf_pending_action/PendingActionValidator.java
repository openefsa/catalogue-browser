package dcf_pending_action;

import javax.xml.soap.SOAPException;

import ui_progress_bar.FormProgressBar;

/**
 * This thread starts in background a
 * pending action process to retrieve its log document
 * from the dcf and to assess if the request
 * succeeded or not. All the events
 * are captured by the {@link #listener}
 * @author avonva
 *
 */
public class PendingActionValidator extends Thread {

	private PendingAction pendingAction;
	private PendingActionListener listener;
	private FormProgressBar bar;
	
	/**
	 * Initialize the reserve validator.
	 * @param pendingReserve
	 */
	public PendingActionValidator( PendingAction pendingAction, 
			PendingActionListener listener ) {
		
		this.pendingAction = pendingAction;
		this.listener = listener;
	}

	@Override
	public void run() {
		
		pendingAction.setListener( listener );
		pendingAction.setProgressBar( bar );
		
		boolean notify = true;
		boolean started = false;
		
		// do until the process is successful
		while ( !started ) {
			
			// start the pending reserve process
			try {
				
				pendingAction.start( notify );

				// success => started!
				started = true;

			} catch (SOAPException e) {

				notify = false;
				
				// bad connection, wait connection
				System.err.println( "Bad internet connection. The " 
						+ pendingAction + " waits one minute to restart" );
				
				// wait one minute
				try {
					Thread.sleep( 60000 );
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	public void setProgressBar(FormProgressBar bar) {
		this.bar = bar;
	}
}
