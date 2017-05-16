package dcf_webservice;

import dcf_reserve_util.PendingReserve;

/**
 *  enum to model the types of response related to a reserve operation
 *  Values:
 *  OK: all ok
 *  NO: the dcf received the request but is unable to accomplish 
 *  it (as if we want to reserve an already reserved catalogue)
 *  BUSY: the dcf is busy at the moment
 *  ERROR: operation failed due to connection problems or similar
 *  
 *  Note that OK and NO values are retrieved by inspecting the log
 *  of the operation, therefore the DcfResponse is also used to
 *  track the log result.
 * @author avonva
 *
 */
public enum DcfResponse {
	OK,     // all ok

	NO,     // the dcf received the request but is unable to accomplish 
	        // it (as if we want to reserve an already reserved catalogue)
	
	BUSY,   // dcf is busy
	
	ERROR;  // operation failed due to connection problems or similar
	
	/*
	 * The pending reserve object related
	 * to the dcf response. If busy we can
	 * perform a retry using the pending reserve,
	 * otherwise we can delete it if the request
	 * succeeded.
	 */
	private PendingReserve pr;
	
	/**
	 * Set the pending reserve to the enum.
	 * @param pr
	 */
	public void setPendingReserve(PendingReserve pr) {
		this.pr = pr;
	}
	/**
	 * Get the pending reserve action related to the enum.
	 * @return
	 */
	public PendingReserve getPendingReserve() {
		return pr;
	}
}
