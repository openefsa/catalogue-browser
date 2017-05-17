package dcf_reserve_util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.swt.widgets.Listener;

import dcf_log_util.LogRetriever;

/**
 * Builder to hide the complexity of the {@link LogRetriever} thread
 * to the user. Set all the required listeners and then use
 * {@link RetryReserveBuilder#build()} to start the log retrieval processes.
 * @author avonva
 *
 */
public class RetryReserveBuilder {

	private Collection<LogRetriever> logRetrievers;
	
	/**
	 * Initialize the builder with a single pending reserve
	 * @param pendingReserve
	 * @see #RetryReserveBuilder(Collection)
	 */
	public RetryReserveBuilder( PendingReserve pendingReserve ) {
		this ( Arrays.asList(pendingReserve) );
	}
	
	/**
	 * Initialize the builder to start the process which will retrieve
	 * the log document related to the pending reserve
	 * request.
	 * @param pendingReserves a list of pending reserves which need
	 * to be restarted
	 */
	public RetryReserveBuilder( Collection<PendingReserve> pendingReserves ) {
		
		logRetrievers = new ArrayList<>();
		
		// create a thread for each pending reserve
		for ( PendingReserve pr : pendingReserves )
			logRetrievers.add( new LogRetriever( pr ) );
	}
	
	/**
	 * Start the retry processes to retrieve the log
	 */
	public void build() {
		for ( LogRetriever logRetriever : logRetrievers )
			logRetriever.start();
	}
	
	/**
	 * Register to be notified just before reserving
	 * the catalogue (only for successful operations)
	 * @param listener
	 */
	public void setStartReserveListener ( Listener listener ) {
		for ( LogRetriever logRetriever : logRetrievers )
			logRetriever.setStartReserveListener( listener );
	}
	
	/**
	 * Register to be notified when the force editing is enabled
	 * @param listener
	 */
	public void setForceEditListener ( ForcedEditingListener listener ) {
		for ( LogRetriever logRetriever : logRetrievers )
			logRetriever.setForceEditListener( listener );
	}
	
	/**
	 * Register to be notified if a new internal version
	 * was downloaded (it happens if we are not using
	 * the last internal version of the catalogue)
	 * @param listener
	 */
	public void setNewVersionListener ( Listener listener ) {
		for ( LogRetriever logRetriever : logRetrievers )
			logRetriever.setNewVersionListener( listener );
	}
	
	/**
	 * Register to be notified when the log document is
	 * retrieved and the catalogue is possibly reserved
	 * @param listener
	 */
	public void setFinishListener ( ReserveFinishedListener listener ) {
		for ( LogRetriever logRetriever : logRetrievers )
			logRetriever.setFinishListener( listener );
	}
}
