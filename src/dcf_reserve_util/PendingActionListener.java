package dcf_reserve_util;

import dcf_webservice.DcfResponse;
import dcf_webservice.PendingAction;

/**
 * Listener used for web service operations with
 * pending actions.
 * @author avonva
 *
 */
public interface PendingActionListener {
	
	/**
	 * Called when the request is prepared and
	 * it is ready to be sent.
	 */
	public void requestPrepared ();
	
	/**
	 * Called when the reserve request was successfully sent
	 * to the dcf and the log code of the response is retrieved
	 * @param pendingReserve the pending reserve related to the
	 * log code
	 * @param logCode the log code which was found in the dcf
	 * soap response
	 */
	public void requestSent ( PendingAction pendingAction, 
			String logCode );
	
	/**
	 * Called when a pending reserve is completed
	 * @param pendingReserve the completed pending reserve
	 * @param response the dcf response regarding the reserve operation
	 * contained in the pending reserve object
	 */
	public void responseReceived( PendingAction pendingAction, DcfResponse response );
	
	/**
	 * Called when a pending reserve status changed. For example,
	 * if we find that we are using and old internal version of
	 * the catalogue, the new status would be {@link PendingReserveStatus#OLD_VERSION}
	 * @param pendingReserve the pending reserve which changed
	 * its status
	 */
	public void statusChanged( PendingAction pendingAction, 
			PendingReserveStatus status );
}
