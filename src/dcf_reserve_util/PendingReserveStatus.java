package dcf_reserve_util;

import dcf_manager.Dcf;

/**
 * Return type for {@link Dcf#reserve(catalogue_object.Catalogue, dcf_webservice.ReserveLevel, String, ReserveFinishedListener)}
 * @author avonva
 *
 */
public enum PendingReserveStatus {
	
	STARTED,               // if the pending reserve is just started
	SENDING,               // if we are sending the pending reserve
	COMPLETED,             // if the pending reserve was completed
	NOT_RESERVING,         // if we are not reserving (we are unreserving)
	CORRECT_VERSION,       // if the catalogue we are working with is the up to date version
	OLD_VERSION,           // if the catalogue we are working with is an older version
	MINOR_FORBIDDEN,       // if we ask for a reserve minor but this action is forbidden
	ERROR                  // general error
}
