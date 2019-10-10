package dcf_manager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * This class is a thread which in background retrieves the
 * catalogues meta-data from the dcf. When the download is
 * finished the update listener is called.
 * @author avonva
 *
 */
public class UpdatesChecker extends Thread {
	
	private static final Logger LOGGER = LogManager.getLogger(UpdatesChecker.class);
	
	// listener called when the updates are finished
	private Listener updatesListener;
	
	/**
	 * Set the listener called when the updates are finished
	 * @param updatesListener
	 */
	public void setUpdatesListener(Listener updatesListener) {
		this.updatesListener = updatesListener;
	}
	
	/**
	 * Run method of the thread
	 */
	@Override
	public void run() {
		
		try {
			
			Dcf dcf = new Dcf();
			
			// Refresh the dcf catalogues
			dcf.refreshCatalogues();
			
			// call the listener
			if ( updatesListener != null )
				updatesListener.handleEvent( new Event() );
			
			super.run();
		}
		
		catch ( Exception e ) {
			e.printStackTrace();
			LOGGER.error("Cannot get catalogues list", e);
		}
	}
}
