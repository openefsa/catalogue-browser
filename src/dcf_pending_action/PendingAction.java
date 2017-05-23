package dcf_pending_action;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import catalogue_object.Catalogue;
import dcf_log_util.LogDownloader;
import dcf_user.User;
import dcf_webservice.DcfResponse;
import dcf_webservice.Publish;
import dcf_webservice.ReserveLevel;
import dcf_webservice.Publish.PublishLevel;
import ui_progress_bar.FormProgressBar;

/**
 * Class which models a generic pending action, that is, a
 * web request which needs to be completed. In fact,
 * a {@link #PendingAction(Catalogue, String, String, Priority)}
 * contains only the code of the log document related to the
 * web request. This log needs to be retrieved and only at this
 * point we can get the dcf response and close the pending action.
 * @author avonva
 *
 */
public abstract class PendingAction {
	
	// id in the database
	private int id;
	
	// the catalogue related to this action
	private Catalogue catalogue;
	
	// the reserve log code that we need to retrieve
	private String logCode;

	// the username of the user who made the reserve action
	private String username;
	
	/**
	 * Additional data used for the pending action
	 * we can have {@link ReserveLevel} or {@link PublishLevel}
	 * for example. We need a generic field to generalize
	 * the concept of pending action and to use a single table
	 * for all the pending action types.
	 */
	private String data;

	// the priority of the pending reserve
	private Priority priority;

	// the status of the pending reserve
	private PendingReserveStatus status;

	//  the dcf response to the pending reserve
	private DcfResponse response;

	// listener called for events
	private PendingActionListener listener;

	private FormProgressBar progressBar;

	/**
	 * Initialize a pending request
	 * @param catalogue the catalogue related to this pending action
	 * @param logCode the reserve log code
	 * @param username the name of the user who made the reserve
	 * @param priority the action priority
	 */
	public PendingAction( Catalogue catalogue, String logCode, 
			String username, Priority priority ) {
		this.catalogue = catalogue;
		this.logCode = logCode;
		this.username = username;
		this.priority = priority;
		this.data = "";
	}

	/**
	 * Start the reserve operation for the current catalogue
	 * with the current reserve level
	 */
	public void start () {
		
		System.out.println( "Starting " + this );
		
		// we are starting the process
		setStatus( PendingReserveStatus.STARTED );
		
		// send the pending reserve request
		// to the dcf
		send();
	}
	
	/**
	 * Send the pending reserve to the dcf in order to retrieve its log.
	 * Note that this call is a blocking call. We are stuck here until
	 * we find the log.
	 * @param retryTime time to wait between each attempt
	 * @param listener called if the dcf is found busy while
	 * asking for the log with HIGH priority
	 * @return the log response
	 */
	private DcfResponse send() {
		
		// update the status
		setStatus( PendingReserveStatus.SENDING );
		
		Document log = getLog();
		
		// if no log in high priority => the available time is finished
		if ( log == null && priority == Priority.HIGH ) {
			
			// manage this case
			manageBusyStatus();
			
			// notify the user that the dcf was found busy
			// and that the pending reserve was queued
			// we call it after having forced the editing
			// in order to refresh correctly the UI
			setStatus( PendingReserveStatus.QUEUED );

			System.out.println( "Downgrading to LOW priority " + this );
			
			// downgrade the pending reserve priority
			downgradePriority();
			
			// restart the process with low priority
			log = getLog();
		}
		
		// here the log was retrieved for sure
		// since if it was LOW priority we have
		// found the log, if it was HIGH priority
		// or it was found within the allowed time
		// or it was downgraded to LOW priority and
		// then it was found since we found it for
		// sure if we are in LOW priority
		
		// get the response of the dcf looking
		// into the log document
		this.response = extractLogResponse( log );
		
		// process the dcf response
		processResponse ( response );
		
		// we have completed the pending reserve
		// process, therefore we can delete it
		terminate();
		
		// notify that the reserve operation is finished
		if ( listener != null )
			listener.responseReceived ( this, response );
		
		// return the log response
		return response;
	}
	
	/**
	 * Delete the pending request from the database
	 * Multiple threads can access the database with this
	 * method, for this reason we use the synchronized keyword.
	 */
	private synchronized void terminate() {
		
		System.out.println( "Terminating " + this );
		
		PendingActionDAO prDao = new PendingActionDAO();
		prDao.remove( this );

		// set the status as completed
		setStatus( PendingReserveStatus.COMPLETED );
	}
	
	/**
	 * Downgrade the priority of the pending reserve
	 * and save this change into the database
	 */
	private synchronized void downgradePriority() {
		
		// downgrade the pending reserve to LOW priority
		priority = Priority.LOW;
		
		PendingActionDAO prDao = new PendingActionDAO();
		prDao.update( this );
	}
	
	/**
	 * Import a the last internal version of the catalogue
	 * if there is one. If no newer internal versions are
	 * found, no action is performed and the {@code doneListener}
	 * is called.
	 * If a new internal version is found the import process
	 * starts and the the status of the 
	 * pending reserve is set to 
	 * {@link PendingReserveStatus#OLD_VERSION}. In this case,
	 * only when the import process is finished the 
	 * {@code doneListener} is called.
	 * 
	 * Note that if a new internal version is found, the
	 * {@link #catalogue} of the pending reserve will be
	 * updated with the new internal version.
	 * @param doneListener listener which specify the actions
	 * needed when we finish the method.
	 * @return true if we already had the last internal version, 
	 * false otherwise
	 */
	public boolean importLastVersion ( final Listener doneListener ) {
		
		try {
			
			Catalogue catalogue = getCatalogue();
			
			final NewCatalogueInternalVersion lastVersion = 
					catalogue.getLastInternalVersion();
			
			// if no version is found => we have the last one
			if ( lastVersion == null ) {
				
				// call the listener since we have finished
				doneListener.handleEvent( new Event() );
				return true;
			}
			
			System.out.println ( this + ": This is not the last version "
					+ "of the catalogue, importing " + lastVersion );

			// and import the last internal version
			// and when the process is finished
			// reserve the new version of the catalogue
			lastVersion.setProgressBar( getProgressBar() );
			
			// import the new version
			lastVersion.importNewCatalogueVersion( new Listener() {

				@Override
				public void handleEvent(Event arg0) {

					// update the pending reserve catalogue
					setCatalogue( lastVersion.getNewCatalogue() );

					doneListener.handleEvent( arg0 );
				}
			} );
			
			// update the status of the pending reserve
			setStatus( PendingReserveStatus.IMPORTING_LAST_VERSION );
			
			return false;
			
		} catch (IOException | TransformerException | 
				ParserConfigurationException | SAXException e) {
			e.printStackTrace();
			
			setStatus( PendingReserveStatus.ERROR );
		}
		return true;
	}
	
	/**
	 * Download the log using the pending reserve. The speed
	 * behavior of the process is defined by {@link #priority}
	 * @return the log related to the reserve operation if it
	 * was found in the available time, otherwise null
	 */
	private Document getLog () {
		
		Document log = null;
		
		int attempts = 12;              // 12 times 10 seconds => 2 minutes
		long interAttemptsTime = 10000; 
		
		// set inter attempts time according to the priority
		switch ( priority ) {
		case HIGH:
			interAttemptsTime = 10000;  // 10 seconds
			break;
		case LOW:
			interAttemptsTime = 30000;  // 5 minutes TODO
			break;
		default:
			break;
		}

		// initialize a log downloader with the current priority
		LogDownloader logDown = new LogDownloader( logCode, 
				interAttemptsTime, attempts, priority );
		
		// get the log
		log = logDown.getLog();
		
		return log;
	}
	
	
	/**
	 * Set the status of the pending reserve
	 * @param status
	 */
	protected void setStatus( PendingReserveStatus status ) {
		
		this.status = status;
		
		// notify that the pending reserve status changed
		listener.statusChanged( this, status );
	}
	
	/**
	 * Get the current status
	 * @return
	 */
	public PendingReserveStatus getStatus() {
		return status;
	}
	
	/**
	 * Check if this pending reserve was made
	 * by the {@code user}
	 * @param user
	 * @return true if it was the {@code user} who made the reserve
	 * action, false otherwise
	 */
	public boolean madeBy ( User user ) {
		return user.getUsername().equals( username );
	}
	
	@Override
	public String toString() {
		return "PendingAction: id=" + id + ",priority=" + priority 
				+ ",logCode=" + logCode + ",type=" + getType();
	}
	
	/**
	 * Set the id of the pending reserve object
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Get the id in the db of the pending reserve object
	 * if it was set
	 * @return
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Update the catalogue with the new version of it
	 * @param newVersion
	 */
	protected synchronized void setCatalogue( Catalogue catalogue ) {
		
		this.catalogue = catalogue;
		
		// update the pending reserve also in the database
		PendingActionDAO prDao = new PendingActionDAO();
		prDao.update( this );
	}
	
	/**
	 * Get the current catalogue
	 * @return
	 */
	public Catalogue getCatalogue() {
		return catalogue;
	}
	
	/**
	 * Get the log code of the pending reserve request
	 * @return
	 */
	public String getLogCode() {
		return logCode;
	}
	
	/**
	 * Get the username of the user who made 
	 * the reserve action
	 * @return
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Get the state of the pending reserve
	 * @return
	 */
	public Priority getPriority() {
		return priority;
	}
	
	/**
	 * Get the dcf response of this pending reserve
	 * Note that you should call {@link #send()} before
	 * to get a real result.
	 * @return the dcf response
	 */
	public DcfResponse getResponse() {
		return response;
	}
	
	/**
	 * Set the listener which is used to listen
	 * several reserve events
	 * @param listener
	 */
	public void setListener(PendingActionListener listener) {
		this.listener = listener;
	}
	
	
	/**
	 * Enumerator to identify the pending
	 * reserve as new or as retry.
	 * If new we try to get the log each 10 seconds for 2 minutes
	 * if retry we try to get the log each 5 minutes until
	 * we find the log
	 * @author avonva
	 *
	 */
	public enum Priority {
		HIGH,
		LOW
	}
	
	
	/**
	 * Set the data which need to be included
	 * into the pending action
	 * @param data
	 */
	public void setData(String data) {
		this.data = data;
	}
	
	/**
	 * Get the pending action data
	 * @return
	 */
	public String getData() {
		return data;
	}
	
	
	/**
	 * Set the progress bar which is used for possible
	 * import actions related to new catalogue versions
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Get the progress bar
	 * @return
	 */
	public FormProgressBar getProgressBar() {
		return progressBar;
	}
	
	/**
	 * Get the soap action type. This field should
	 * be a unique code to define which web action
	 * is this pending action. For example, if we
	 * have a PendingReserve we could use as constant
	 * type the string "reserve".
	 * @return
	 */
	public abstract String getType();
	
	/**
	 * Actions performed if the dcf is busy and
	 * the pending action was put in queue.
	 */
	public abstract void manageBusyStatus ();
	
	/**
	 * Extract the dcf response from the retrieved log document
	 * @param log
	 * @return
	 */
	public abstract DcfResponse extractLogResponse ( Document log );
	
	/**
	 * Process the dcf response related to this pending action
	 * @param response the retrieved response
	 */
	public abstract void processResponse ( DcfResponse response );
}
