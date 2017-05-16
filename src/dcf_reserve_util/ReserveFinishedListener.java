package dcf_reserve_util;

import catalogue_object.Catalogue;
import dcf_webservice.DcfResponse;

/**
 * Listener called when a reserve operation
 * finishes on a catalogue. Give the dcf response as argument.
 * @author avonva
 *
 */
public interface ReserveFinishedListener {

	/**
	 * Called when a reserve operation finished
	 * on the catalogue.
	 * @param catalogue the catalogue we want to reserve
	 * @param response the dcf response
	 */
	public void reserveFinished ( Catalogue catalogue, DcfResponse response );
}
