package dcf_log_util;

/**
 * Listener called when we ask for a log to the
 * dcf and it is busy.
 * @author avonva
 *
 */
public interface BusyDcfListener {
	/**
	 * Called when the dcf is busy and we have asked
	 * for a log
	 * @param logCode the log we have asked
	 */
	public void dcfIsBusy( String logCode );
}
