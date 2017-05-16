package dcf_reserve_util;

/**
 * Listener called when a reserve operation
 * successfully starts.
 * @author avonva
 *
 */
public interface ReserveStartedListener {
	/**
	 * Called when a reserve operation starts.
	 */
	public void reserveStarted ( ReserveResult reserveLog );
}
