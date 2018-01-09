package ui_pending_action_listener;

import dcf_log.DcfResponse;
import dcf_pending_action.PendingActionStatus;

public interface PendingActionMessages {

	/**
	 * Get the pending action message which will be shown to the
	 * user related to the dcf response
	 * @param response
	 * @return
	 */
	public String getResponseMessage ( DcfResponse response );
	
	/**
	 * Get the pending action message which will be shown to the
	 * user related to the current status of the pending action.
	 * @param status
	 * @return
	 */
	public String getStatusMessage ( PendingActionStatus status );
}
