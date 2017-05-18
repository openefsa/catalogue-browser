package dcf_reserve_util;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import catalogue_object.Catalogue;
import catalogue_object.Version;
import dcf_log_util.LogDownloader;
import dcf_log_util.LogParser;
import dcf_manager.Dcf;
import dcf_manager.VersionFinder;
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
		
		// check the status of the pending reserve
		PendingReserveStatus status = updateStatus();
		
		switch ( status ) {
		
		// if errors, terminate the pending response
		case MINOR_FORBIDDEN:
		case ERROR:
			terminate();
			break;

			// if correct or unreserve
			// go on with the process
		case NOT_RESERVING:
		case CORRECT_VERSION:
			
			send();
			break;

			// if old version import the last one
			// note that the new version was already
			// downloaded to perform ReserveChecks
			// therefore here we just import it
		case OLD_VERSION:
			
			// download the last internal version
			// and when the process is finished
			// reserve the NEW catalogue
			
			importNewCatalogueVersion( progressBar, new Listener() {
				
				@Override
				public void handleEvent(Event arg0) {
					
					// we use another thread, otherwise the UI freezes
					new Thread( new Runnable() {
						
						@Override
						public void run() {
							
							// send the pending reserve
							send();
						}
					}).start();
				}
			});

			break;
		default:
			break;
		}
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
		if ( log == null && priority == PendingPriority.HIGH ) {

			// downgrade the pending reserve to LOW priority
			priority = PendingPriority.LOW;
			
			// force editing of the catalogue since we have waited
			// but no response was received from the dcf
			// force the edit only if the editing was not already
			// forced and if we are reserving (not unreserving)
			if ( !catalogue.isForceEdit( username ) 
					&& reserveLevel.greaterThan( ReserveLevel.NONE ) ) {
				
				catalogue.forceEdit( username, reserveLevel );
			}
			
			// notify the user that the dcf was found busy
			// and that the pending reserve was queued
			if ( listener != null )
				listener.queued( this );
			
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
		
		// if the reserve did not succeed => stultify the current catalogue
		if ( response != DcfResponse.OK && 
				catalogue.isForceEdit( username ) ) {

			// mark the catalogue as invalid, we have modified the
			// catalogue but we did not have the rights to do it
			catalogue.stultify();
		}
		
		// Remove the forced editing from the catalogue
		// if it was enabled
		// since here we know the real dcf response
		if ( catalogue.isForceEdit( username ) )
			catalogue.removeForceEdit( username );
		
		// reserve the catalogue if the reserve succeeded
		if ( response == DcfResponse.OK )
			reserve();
		
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
	 * Note that this operation closes the pending reserve
	 * and delete it from the database
	 */
	private void reserve() {
		
		// set the catalogue as (un)reserved at the selected level
		if ( reserveLevel.greaterThan( ReserveLevel.NONE ) ) {
			
			// get the new internal version created by the reserve
			// and set it to the pending operation
			Catalogue newVersion = this.catalogue.reserve ( reserveLevel );
			setNewVersion( newVersion );
		}
		else
			this.catalogue.unreserve ();
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
			interAttemptsTime = 300000;  // 5 minutes
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
		
		// no log is found => dcf is busy
		if ( log == null )
			return DcfResponse.BUSY;
		
		// analyze the log to get the result
		LogParser parser = new LogParser ( log );

		// return ok if correct operation
		if ( parser.isOperationCorrect() ) {
			System.out.println ( reserveLevel.getReserveOperation() 
					+ ": successfully completed" );
			return DcfResponse.OK;
		}
		else {
			System.out.println ( reserveLevel.getReserveOperation() 
					+ ": failed - the dcf rejected the operation" );
			return DcfResponse.NO;
		}
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
	 * Perform checks on the pending reserve request. Here we
	 * make some checks on the correctness of the reserve request.
	 * @return the {@link PendingReserveStatus} which contains
	 * the pending reserve status.
	 */
	private PendingReserveStatus updateStatus() {
		
		// compute the current status and update it
		PendingReserveStatus status = getCurrentStatus();
		
		setStatus( status );
		
		return status;
	}
	
	/**
	 * Check the state of the pending reserve and return it
	 * @return 
	 */
	private PendingReserveStatus getCurrentStatus() {
		
		// only if we are reserving (and not unreserving)
		if ( reserveLevel.isNone() ) {
			return PendingReserveStatus.NOT_RESERVING;
		}

		String format = ".xml";
		String filename = "temp_" + catalogue.getCode();
		String input = filename + format;
		String output = filename + "_version" + format;
		
		try {
			
			Dcf dcf = new Dcf();
			
			// export the internal version in the file
			boolean written = dcf.exportCatalogueInternalVersion( 
					catalogue.getCode(), input );

			// if no internal version is retrieved we have
			// the last version of the catalogue
			if ( !written )
				return PendingReserveStatus.CORRECT_VERSION;
			
			VersionFinder finder = new VersionFinder( input, output );

			// if we are minor reserving a major draft => error
			// it is a forbidden action
			if ( reserveLevel.isMinor() && finder.isStatusMajor() 
					&& finder.isStatusDraft() ) {
				
				System.err.println ( "Cannot perform a reserve minor on major draft" );
				
				return PendingReserveStatus.MINOR_FORBIDDEN;
			}

			// compare the catalogues versions
			Version intVersion = new Version ( finder.getVersion() );
			Version localVersion = catalogue.getRawVersion();
			
			// if the downloaded version is newer than the one we
			// are working with => we are using an old version
			if ( intVersion.compareTo( localVersion ) < 0 ) {

				System.err.println ( "Cannot perform reserve on old version. Downloading the new version" );
				System.err.println ( "Last internal " + finder.getVersion() + 
						" local " + catalogue.getVersion() );

				// save the new version of the catalogue
				newVersion = new NewCatalogueInternalVersion( catalogue.getCode(), 
						finder.getVersion(), input );

				return PendingReserveStatus.OLD_VERSION;
			} 
			else {
				
				System.out.println ( "The last internal version has a lower or equal version "
						+ "than the catalogue we are working with." );
				System.out.println ( "Last internal " + finder.getVersion() + 
						" local " + catalogue.getVersion() );
				
				// if we have the updated version
				return PendingReserveStatus.CORRECT_VERSION;
			}

		} catch ( IOException | 
				TransformerException | 
				ParserConfigurationException | 
				SAXException e ) {
			
			e.printStackTrace();
			
			return PendingReserveStatus.ERROR;
		}
	}
	
	/**
	 * Import the new catalogue version which was found with the
	 * {@link #checkStatus()}
	 * @param progressBar
	 * @param doneListener
	 */
	public void importNewCatalogueVersion( FormProgressBar progressBar, final Listener doneListener ) {
		
		if ( newVersion == null ) {
			System.err.println( "Cannot import a new version since it is not defined" );
			return;
		}
		
		newVersion.setProgressBar( progressBar );
		newVersion.importNewCatalogueVersion( new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {

				// update the pending reserve catalogue
				setNewVersion( newVersion.getNewCatalogue() );
				
				doneListener.handleEvent( arg0 );
			}
		} );
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
		
		// notify that we have downloaded a 
		// new version of the catalogue
		if ( listener != null )
			listener.internalVersionChanged( 
					this, newVersion );
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
