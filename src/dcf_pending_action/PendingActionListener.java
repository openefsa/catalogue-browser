package dcf_pending_action;

import catalogue.Catalogue;
import dcf_webservice.DcfResponse;

/**
 * Listener used for web service operations related to
 * pending actions. In particular, we use this listener
 * to notify the user and to make actions based on the
 * pending action status and response.
 * @author avonva
 *
 */
public interface PendingActionListener {
	
	/**
	 * Called when there are connection problems during
	 * the creation and dispatch of pending actions.
	 * @param catalogue the catalogue which is implied into
	 * the pending action that failed
	 */
	public void connectionFailed( Catalogue catalogue );
	
	/**
	 * Called when the request is prepared and
	 * it is ready to be sent.
	 * @param catalogue the catalogue which is implied into
	 * the pending action that failed
	 */
	public void requestPrepared( Catalogue catalogue );
	
	/**
	 * Called when the reserve request was successfully sent
	 * to the dcf and the log code of the response is retrieved
	 * @param pendingAction the pending action which generated the
	 * log code
	 * @param logCode the log code which was found in the dcf
	 * soap response
	 */
	public void requestSent ( PendingAction pendingAction, 
			String logCode );
	
	/**
	 * Called when a pending reserve is completed
	 * @param pendingAction the completed pending action
	 * @param response the dcf response regarding the pending operation
	 * contained in the pending action object
	 */
	public void responseReceived( PendingAction pendingAction, DcfResponse response );
	
	/**
	 * Called when a pending action status changed. For example,
	 * if we find that we are using and old internal version of
	 * the catalogue, the new status would be {@link PendingActionStatus#OLD_VERSION}
	 * @param pendingAction the pending action which changed
	 * its status
	 */
	public void statusChanged( PendingAction pendingAction, 
			PendingActionStatus status );
}
