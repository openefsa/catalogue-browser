package dcf_reserve_util;

import catalogue_object.Catalogue;
import dcf_log_util.BusyDcfListener;
import dcf_user.User;
import dcf_webservice.DcfResponse;
import dcf_webservice.ReserveLevel;

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
	
	/**
	 * Initialize a pending request
	 * @param logCode the reserve log code
	 * @param reserveLevel the reserve level we want for the catalogue
	 * @param catalogue the catalogue we want to reserve
	 * @param username the name of the user who made the reserve
	 */
	public PendingReserve( String logCode, ReserveLevel reserveLevel, Catalogue catalogue, String username ) {
		this.logCode = logCode;
		this.reserveLevel = reserveLevel;
		this.catalogue = catalogue;
		this.username = username;
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
		
		PendingReserve pr = new PendingReserve( logCode, level, 
				catalogue, username );
		
		// create a pending reserve object in order to
		// retry the log retrieval (also if the application
		// is closed!)
		PendingReserveDAO prDao = new PendingReserveDAO();
		prDao.insert( pr );
		
		return pr;
	}
	
	/**
	 * Delete the pending request from the database
	 * Multiple threads can access the database with this
	 * method, for this reason we use the synchronized keyword.
	 */
	public synchronized void delete() {
		PendingReserveDAO prDao = new PendingReserveDAO();
		prDao.remove( this );
	}
	
	/**
	 * Retry retriving the log with the default settings
	 * @return
	 */
	public DcfResponse retry() {
		return retry ( null, null );
	}
	
	/**
	 * Retry retriving the log with default settings
	 * @param dcfBusyListener called each time the dcf is found busy
	 * @return
	 */
	public DcfResponse retry( BusyDcfListener dcfBusyListener ) {
		return retry ( dcfBusyListener, null );
	}
	
	/**
	 * Retry retriving the log with default settings
	 * @param forcedListener called when the catalogue editing is forced
	 * since we have found a busy dcf
	 * @return
	 */
	public DcfResponse retry( ForcedEditingListener forcedListener ) {
		return retry ( null, forcedListener );
	}
	
	/**
	 * Try to get the log related to the pending reserve in background
	 * with default settings.
	 * Note that this call is a blocking call. We are stuck here until
	 * we find the log.
	 * @param listener called each time that the dcf is found busy while
	 * asking for the log
	 * @return the log response
	 */
	public DcfResponse retry( BusyDcfListener listener, ForcedEditingListener forcedListener ) {
		
		long retryTime = 300000; // 5 minutes
		
		return retry ( retryTime, listener, forcedListener );
	}
	
	/**
	 * Try to get the log related to the pending reserve in background.
	 * Note that this call is a blocking call. We are stuck here until
	 * we find the log.
	 * @param retryTime time to wait between each attempt
	 * @param listener called each time that the dcf is found busy while
	 * asking for the log
	 * @param forcedListener called when the catalogue editing mode is forced
	 * @return the log response
	 */
	public DcfResponse retry( long retryTime, final BusyDcfListener listener, 
			final ForcedEditingListener forcedListener ) {
		
		ReserveLogDownloader downloader = new ReserveLogDownloader( logCode, retryTime );
		
		// each time that the dcf is found busy while asking the log
		downloader.setListener( new BusyDcfListener() {
			
			@Override
			public void dcfIsBusy(String logCode) {
				
				// force the editing of the catalogue (only if we are reserving)
				// if the dcf is busy and the catalogue editing
				// is not already forced
				if ( !catalogue.isForceEdit( username ) 
						&& reserveLevel.greaterThan( ReserveLevel.NONE ) ) {
					
					catalogue.forceEdit( username );
					
					// forced catalogue editing => call listener
					if ( forcedListener != null )
						forcedListener.editingForced( catalogue, 
								logCode, reserveLevel );
				}

				// dcf is busy => call listener
				if ( listener != null )
					listener.dcfIsBusy( logCode );
			}
		});
		
		// get the response contained in the log
		// note that this call is a blocking call
		// therefore until we do not find the log
		// we are stuck here
		DcfResponse response = downloader.download();
		
		// reserve finished => we remove the force editing, since
		// we know if the reserve went ok or not
		catalogue.removeForceEdit( username );
		
		// if the reserve did not go ok => stultify the current catalogue
		if ( response != DcfResponse.OK && 
				catalogue.isForceEdit( username ) ) {
			
			// mark the catalogue as invalid
			catalogue.stultify();
		}
		
		// here we have the log downloaded
		// therefore we do not need anymore
		// the pending reserve and we can
		// delete it
		delete();
		
		// return the log response
		return response;
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
		return "PendingReserve: id=" + id + ",logCode=" + logCode + ",cat=" + catalogue;
	}
}
