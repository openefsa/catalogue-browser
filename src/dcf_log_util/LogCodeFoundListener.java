package dcf_log_util;

/**
 * Listener called when the log code of a webservice
 * request is found.
 * @author avonva
 *
 */
public interface LogCodeFoundListener {
	public void logCodeFound( String logCode );
}
