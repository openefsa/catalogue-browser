package ui_pending_request_list;

import pending_request.IPendingRequest;

public interface PendingRequestTableRelaunchListener {

	/**
	 * Called if a pending request was relaunched from
	 * the table
	 * @param request
	 */
	public void relaunched(IPendingRequest request);
}
