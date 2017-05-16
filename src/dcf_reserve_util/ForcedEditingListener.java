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
	
	/**
	 * Called when the editing is removed
	 * @param catalogue
	 * @param username
	 * @param level the new reserveLevel of the catalogue, if the catalogue was not
	 * reserved then this will be NONE, otherwise we get the reserve level required
	 */
	public void editingRemoved ( Catalogue catalogue, String username, ReserveLevel level );
}
