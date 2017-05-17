package dcf_reserve_util;

import org.eclipse.swt.widgets.Listener;

import catalogue_object.Catalogue;
import dcf_log_util.LogCodeFoundListener;
import dcf_webservice.ReserveLevel;
import ui_progress_bar.FormProgressBar;

/**
 * This class is used to incapsulate the {@linkplain Reserver} thread
 * into a builder to hide it from the final user.
 * After having set all the listeners or the progress bar, call 
 * {@link ReserveBuilder#build()} to send the request to the
 * dcf.
 * @author avonva
 *
 */
public class ReserveBuilder {

	// the thread which makes the reserve operation
	private Reserver reserveThread;

	/**
	 * Initialize the reserve builder.
	 * @param catalogue the catalogue we want to reserve
	 * @param level the reserve level we want, set NONE to unreserve
	 * @param description the reason why we are reserving
	 */
	public ReserveBuilder( Catalogue catalogue, 
			ReserveLevel level, String description ) {
		
		// initialize the reserve thread
		this.reserveThread = new Reserver( catalogue, level, description );
	}
	
	/**
	 * Build and send the reserve request
	 */
	public void build() {
		reserveThread.start();
	}
	
	/**
	 * Register a listener in order to be notified when
	 * the reserve request starts (when it is already started)
	 * @param startListener
	 */
	public void setStartListener ( ReserveStartedListener startListener ) {
		reserveThread.setStartListener( startListener );
	}
	
	/**
	 * Register a listener in order to be notified if a new
	 * version of the catalogue was downloaded (it happens
	 * if we have an old internal version)
	 * @param startListener
	 */
	public void setNewVersionListener ( Listener startListener ) {
		reserveThread.setNewVersionListener( startListener );
	}
	
	/**
	 * Register a listener in order to be notified when
	 * the dcf log code related to our reserve request
	 * is found. When we have the log code we can try
	 * making a polling to retrieve the log itself from the dcf.
	 * @param logListener
	 */
	public void setLogCodeListener ( LogCodeFoundListener logListener ) {
		reserveThread.setLogCodeListener( logListener );
	}
	
	/**
	 * Register to be notified just before reserving
	 * the catalogue (only for successful operations)
	 * @param listener
	 */
	public void setStartReserveListener ( Listener listener ) {
		reserveThread.setStartReserveListener( listener );
	}
	
	/**
	 * Register a listener in order to be notified when
	 * the reserve operation finishes.
	 * @param finishListener
	 */
	public void setFinishlistener ( ReserveFinishedListener finishListener ) {
		reserveThread.setFinishlistener( finishListener );
	}
	
	/**
	 * Register a listener in order to be notified when
	 * the editing is forced by the user without
	 * having reserved the catalogue
	 * @param listener
	 */
	public void setForcedEditListener ( ForcedEditingListener listener ) {
		reserveThread.setForcedEditlistener( listener );
	}
	
	/**
	 * Set a progress bar which will be called if an
	 * import process is performed
	 * @param progressBar
	 */
	public void setProgressBar ( FormProgressBar progressBar ) {
		reserveThread.setProgressBar( progressBar );
	}
}
