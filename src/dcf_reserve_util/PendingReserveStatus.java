package dcf_reserve_util;

/**
 * Enumerator to save the status of a {@link PendingReserve}
 * object.
 * @author avonva
 *
 */
public enum PendingReserveStatus {
	
	STARTED,                // if the pending reserve is just started
	SENDING,                // if we are sending the pending reserve
	RESERVING,          	// if we are reserving the catalogue in the application db
	UNRESERVING,            // if we are unreserving the catalogue in the application db
	IMPORTING_LAST_VERSION, // if we are importing the last internal version of the catalogue
	FORCING_EDITING,        // if we are forcing the catalogue editing mode
	QUEUED,                 // if the pending reserve was queued in the dcf (busy dcf)
	INVALIDATED,            // if the catalogue related to the pending reserve was invalidated
	COMPLETED,              // if the pending reserve was completed
	ERROR                   // if error occurred
}
