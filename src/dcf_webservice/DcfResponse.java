package dcf_webservice;

/**
 * Enumerator to track the dcf response to web service requests.
 * @author avonva
 *
 */
public enum DcfResponse {
	
	OK,     // all ok

	AP,     // the dcf received the request but it was rejected

	FORBIDDEN,  // if the current action is forbidden
	
	ERROR;  // operation failed due to connection problems or similar
}
