package dcf_pending_action;

/**
 * Enumerator to save the status of a {@link PendingReserve}
 * object.
 * @author avonva
 *
 */
public enum PendingActionStatus {
	
	STARTED,                // if the pending reserve is just started
	SENDING,                // if we are sending the pending reserve
	IMPORTING_LAST_VERSION, // if we are importing the last internal version of the catalogue
	FORCING_EDITING,        // if we are forcing the catalogue editing mode
	QUEUED,                 // if the pending reserve was queued in the dcf (busy dcf)
	INVALID_VERSION,        // if the catalogue was forced to edit, but the catalogue is not the last internal version
	INVALID_RESPONSE,       // if the catalogue was forced to edit, but no reserve can be obtained
	COMPLETED,              // if the pending reserve was completed
	ERROR                   // if error occurred
}
