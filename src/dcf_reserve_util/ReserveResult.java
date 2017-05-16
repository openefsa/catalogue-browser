package dcf_reserve_util;

import dcf_manager.Dcf;

/**
 * Return type for {@link Dcf#reserve(catalogue_object.Catalogue, dcf_webservice.ReserveLevel, String, ReserveFinishedListener)}
 * @author avonva
 *
 */
public enum ReserveResult {
	
	ERROR,            // general error
	NOT_RESERVING,    // if we are not reserving (we are unreserving)
	CORRECT_VERSION,  // if the catalogue we are working with is the up to date version
	OLD_VERSION,      // if the catalogue we are working with is an older version
	MINOR_FORBIDDEN;  // if we ask for a reserve minor but this action is forbidden

	// the new version of the internal version
	private String version;
	
	// filename used to host the filename
	// of the new imported internal version
	private String filename;
	
	public void setVersion ( String version ) {
		this.version = version;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getFilename() {
		return filename;
	}
}
