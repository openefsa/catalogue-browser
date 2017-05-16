package dcf_reserve_util;

import catalogue_object.Catalogue;
import dcf_webservice.ReserveLevel;

/**
 * This listener is used to notify the caller when a
 * catalogue is forced to enable the editing mode.
 * This listener is called only when the dcf is found
 * busy.
 * @author avonva
 *
 */
public interface ForcedEditingListener {

	/**
	 * Called when the catalogue editing is forced by the user
	 * identified by the username
	 * @param catalogue
	 * @param username
	 * @param level
	 */
	public void editingForced ( Catalogue catalogue, String username, ReserveLevel level );
}
