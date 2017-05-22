package dcf_reserve_util;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import catalogue_object.Catalogue;
import catalogue_object.Status;
import dcf_log_util.LogDownloader;
import dcf_log_util.LogParser;
import dcf_user.User;
import dcf_webservice.DcfResponse;
import dcf_webservice.ReserveLevel;
import ui_progress_bar.FormProgressBar;

/**
 * A pending reserve operation. If a catalogue has a pending reserve
 * operation, the application will try in background to get the
 * result of the reserve, in order to understand if the reserve
 * is finished correctly or not.
 * @author avonva
 *
 */
public class PendingReserve {
	
	// id in the database
	private int id;
	
	// the reserve log code that we need to retrieve
	private String logCode;
	
	// the reserve level we are requesting
	private ReserveLevel reserveLevel;
	
	// the catalogue we want to reserve/unreserve
	private Catalogue catalogue;
	
	// the username of the user who made the reserve action
	private String username;
	
	// the priority of the pending reserve
	private PendingPriority priority;
	
	// the status of the pending reserve
	private PendingReserveStatus status;
	
	//  the dcf response to the pending reserve
	private DcfResponse response;
	
	// the new version of the catalogue if one is found
	private NewCatalogueInternalVersion newVersion;

	private ReserveListener listener;
	private FormProgressBar progressBar;
	
	/**
	 * Initialize a pending request
	 * @param logCode the reserve log code
	 * @param reserveLevel the reserve level we want for the catalogue
	 * @param catalogue the catalogue we want to reserve
	 * @param username the name of the user who made the reserve
	 */
	public PendingReserve( String logCode, ReserveLevel reserveLevel, 
			Catalogue catalogue, String username, PendingPriority priority ) {
		this.logCode = logCode;
		this.reserveLevel = reserveLevel;
		this.catalogue = catalogue;
		this.username = username;
		this.priority = priority;
	}
	
	/**
	 * Create a new pending reserve which will be
	 * used for retrieving the log related to the
	 * reserve action.
	 * @param logCode the code of the log we want to retrieve
	 * @param level the reserve level we want
	 * @param catalogue the catalogue we want to reserve
	 * @param username the username of the user who made the reserve
	 * @return the new pending reserve
	 */
	public static PendingReserve addPendingReserve ( String logCode, 
			ReserveLevel level, Catalogue catalogue, String username ) {
		
		// we create a new pending reserve with FAST priority
		PendingReserve pr = new PendingReserve( logCode, level, 
				catalogue, username, PendingPriority.HIGH );
		
		// create a pending reserve object in order to
		// retry the log retrieval (also if the application
		// is closed!)
		PendingReserveDAO prDao = new PendingReserveDAO();
		int id = prDao.insert( pr );
		
		pr.setId( id );
		
		return pr;
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
		
		//Document log = getLog();

		// busy dcf simulated TODO da RRIMUOVERE!!!!!!###################################################
		Document log = null;
		
		// if no log in high priority => the available time is finished
		if ( log == null ) { //&& priority == PendingPriority.HIGH ) {
			
			// force editing of the catalogue since we have waited
			// but no response was received from the dcf
			// force the edit only if the editing was not already
			// forced and if we are reserving (not unreserving)
			if ( !catalogue.isForceEdit( username ) 
					&& reserveLevel.greaterThan( ReserveLevel.NONE ) ) {
				
				setStatus( PendingReserveStatus.FORCING_EDITING );
				
				Catalogue forcedCat = catalogue.forceEdit( 
						username, reserveLevel );
				
				setNewVersion( forcedCat );
			}
			
			// notify the user that the dcf was found busy
			// and that the pending reserve was queued
			// we call it after having forced the editing
			// in order to refresh correctly the UI
			setStatus( PendingReserveStatus.QUEUED );

			System.out.println( "Downgrading to LOW priority " + this );
			
			// downgrade the pending reserve priority
			downgradePriority();
			
			try {
				Thread.sleep( 30000 );
			} catch (InterruptedException e) {
				// TODO RIMUOVI ###############################################################
				e.printStackTrace();
			}
			
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
		
		// get the response contained in the log
		this.response = extractLogResponse( log );
		
		boolean invalid = response != DcfResponse.OK && 
				catalogue.isForceEdit( username );
		
		// if the reserve did not succeed  
		// invalidate the current catalogue
		if ( invalid ) {
			invalidate();
		}
		
		// Remove the forced editing from the catalogue
		// if it was enabled
		// since here we know the real dcf response
		// remove before reserve! We need to restore the
		// correct version of the catalogue
		if ( catalogue.isForceEdit( username ) )
			catalogue.removeForceEdit( username );
		
		// reserve the catalogue if the reserve succeeded
		if ( response == DcfResponse.OK ) {
			reserve();
		}
		
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
	 * Reserve the catalogue contained in the pending
	 * reserve with the pending reserve level
	 * Note that if we are not working with the last
	 * internal version, first the last internal version
	 * will be imported.
	 */
	private void reserve() {
		
		// set the catalogue as (un)reserved at the selected level
		if ( reserveLevel.greaterThan( ReserveLevel.NONE ) ) {
			
			// before reserving, check the version of the catalogue
			// if it is the last one
			boolean isLast = importLastVersion ( new Listener() {
				
				@Override
				public void handleEvent(Event arg0) {
					
					setStatus( PendingReserveStatus.RESERVING );
					
					// get the new internal version created by the reserve
					// and set it to the pending operation
					Catalogue newVersion = catalogue.reserve ( reserveLevel );
					setNewVersion( newVersion );
				}
			});
			
			// if not last version and the catalogue was
			// forced to editing => stultify the catalogue
			if ( !isLast && catalogue.isForceEdit( username ) ) {
				invalidate();
			}
		}
		else {
			
			setStatus( PendingReserveStatus.UNRESERVING );
			catalogue.unreserve ();
		}
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
	private boolean importLastVersion ( final Listener doneListener ) {
		
		try {
			
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
			lastVersion.setProgressBar( progressBar );
			
			// import the new version
			lastVersion.importNewCatalogueVersion( new Listener() {

				@Override
				public void handleEvent(Event arg0) {

					// update the pending reserve catalogue
					setNewVersion( lastVersion.getNewCatalogue() );

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
	 * Stultify the current catalogue. This happens
	 * if we force the editing mode and then
	 * we discover that we could not edit it.
	 */
	private void invalidate() {
		
		catalogue.invalidate();
		setStatus( PendingReserveStatus.INVALIDATED );
	}
	
	/**
	 * Downgrade the priority of the pending reserve
	 * and save this change into the database
	 */
	private synchronized void downgradePriority() {
		
		// downgrade the pending reserve to LOW priority
		priority = PendingPriority.LOW;
		
		PendingReserveDAO prDao = new PendingReserveDAO();
		prDao.update( this );
	}
	
	/**
	 * Delete the pending request from the database
	 * Multiple threads can access the database with this
	 * method, for this reason we use the synchronized keyword.
	 */
	private synchronized void terminate() {
		
		System.out.println( "Terminating " + this );
		
		PendingReserveDAO prDao = new PendingReserveDAO();
		prDao.remove( this );
		
		// update the catalogue status
		catalogue.setReserving( false );

		// set the status as completed
		setStatus( PendingReserveStatus.COMPLETED );
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
	 * Get the log response analyzing the log xml
	 * @param log the log we want to analyze
	 * @return the dcf response contained in the log
	 */
	private DcfResponse extractLogResponse ( Document log ) {
		
		DcfResponse response;
		
		// analyze the log to get the result
		LogParser parser = new LogParser ( log );
		
		Status catStatus = parser.getCatalogueStatus();
		boolean correct = parser.isOperationCorrect();
		boolean minorForbidden = catStatus.isDraft() && catStatus.isMajor() 
				&& reserveLevel.isMinor();
		
		// if we have sent a minor reserve but the
		// catalogue status is major draft, then the
		// action is forbidden
		if ( !correct && minorForbidden ) {
			response = DcfResponse.MINOR_FORBIDDEN;
		}
		// return ok if correct operation
		else if ( correct )
			response = DcfResponse.OK;
		else
			response = DcfResponse.AP;
		
		
		if ( response == DcfResponse.OK )
			System.out.println ( reserveLevel.getOp() 
					+ ": successfully completed" );
		else
			System.out.println ( reserveLevel.getOp() 
					+ ": failed - the dcf rejected the operation" );
		
		return response;
	}
	
	/**
	 * Set the status of the pending reserve
	 * @param status
	 */
	private void setStatus( PendingReserveStatus status ) {
		
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
		return "PendingReserve: id=" + id + ",priority=" + priority 
				+ ",logCode=" + logCode + ",cat=" + catalogue;
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
	 * Get the log code of the pending reserve request
	 * @return
	 */
	public String getLogCode() {
		return logCode;
	}
	
	/**
	 * Get the reserve level of the pending reserve request
	 * @return
	 */
	public ReserveLevel getReserveLevel() {
		return reserveLevel;
	}
	
	/**
	 * Update the catalogue with the new version of it
	 * @param newVersion
	 */
	private synchronized void setNewVersion( Catalogue newVersion ) {
		
		this.catalogue = newVersion;
		
		// update the pending reserve also in the database
		PendingReserveDAO prDao = new PendingReserveDAO();
		prDao.update( this );
	}
	
	/**
	 * Get the catalogue which will be (un)reserved if
	 * the (un)reserve operation will succeed
	 * @return
	 */
	public Catalogue getCatalogue() {
		return catalogue;
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
	public PendingPriority getPriority() {
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
	 * Get the new version of the catalogue if it was
	 * found during {@link #checkStatus()}. Note that
	 * here we have only the information regarding
	 * the code, the version and the xml filename
	 * related to the new version of the catalogue.
	 * @return
	 */
	public NewCatalogueInternalVersion getNewVersion() {
		return newVersion;
	}
	
	/**
	 * Set the listener which is used to listen
	 * several reserve events
	 * @param listener
	 */
	public void setListener(ReserveListener listener) {
		this.listener = listener;
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
	 * Enumerator to identify the pending
	 * reserve as new or as retry.
	 * If new we try to get the log each 10 seconds for 2 minutes
	 * if retry we try to get the log each 5 minutes until
	 * we find the log
	 * @author avonva
	 *
	 */
	public enum PendingPriority {
		HIGH,
		LOW
	}
}
