package thread_listener;

/**
 * Listener used to notify the caller of a thread
 * when the thread has finished its work.
 * @author avonva
 *
 */
public interface ThreadFinishedListener {
	void done( final Thread thread );
}
