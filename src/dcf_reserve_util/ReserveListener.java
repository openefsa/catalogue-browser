package dcf_reserve_util;

import catalogue_object.Catalogue;
import dcf_webservice.DcfResponse;

/**
 * Listener used for reserve operations with
 * pending reserves.
 * @author avonva
 *
 */
public interface ReserveListener {
	
	/**
	 * Called when the reserve request is prepared and
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
	public void requestSent ( PendingReserve pendingReserve, 
			String logCode );
	
	/**
	 * Called when a pending reserve is completed
	 * @param pendingReserve the completed pending reserve
	 * @param response the dcf response regarding the reserve operation
	 * contained in the pending reserve object
	 */
	public void responseReceived( PendingReserve pendingReserve, DcfResponse response );
	
	/**
	 * Called when a pending reserve status changed. For example,
	 * if we find that we are using and old internal version of
	 * the catalogue, the new status would be {@link PendingReserveStatus#OLD_VERSION}
	 * @param pendingReserve the pending reserve which changed
	 * its status
	 */
	public void statusChanged( PendingReserve pendingReserve, 
			PendingReserveStatus status );
	
	/**
	 * Called when the dcf is busy after having
	 * tried a pending reserve with HIGH priority.
	 * The pending reserve was queued and will be
	 * completed in other times.
	 * If this method is called, then the pending
	 * reserve priority was set to LOW and the log retrieval
	 * process was restarted with this new priority.
	 * @param pendingReserve
	 */
	public void queued ( PendingReserve pendingReserve );
	
	/**
	 * Called when a new version of the catalogue is downloaded
	 * @param pendingReserve the pending reserve which caused the
	 * download of the new catalogue version (the new catalogue version
	 * is also contained in this object see {@link PendingReserve#getCatalogue()}
	 * @param newVersion the new catalogue version which was downloaded
	 */
	public void internalVersionChanged ( PendingReserve pendingReserve, 
			Catalogue newVersion );
}
