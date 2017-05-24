package dcf_pending_action;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.w3c.dom.Document;

import catalogue_object.Catalogue;
import catalogue_object.Status;
import dcf_log_util.LogParser;
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
public class PendingReserve extends PendingAction {
	
	public static final String TYPE = "RESERVE";
	
	// the reserve level we are requesting
	private ReserveLevel reserveLevel;
	
	// the new version of the catalogue if one is found
	private NewCatalogueInternalVersion newVersion;
	
	// boolean which is true if a new version
	// of the catalogue should be downloaded
	private boolean needNewVersion = false;

	/**
	 * Initialize a pending request
	 * @param logCode the reserve log code
	 * @param reserveLevel the reserve level we want for the catalogue
	 * @param catalogue the catalogue we want to reserve
	 * @param username the name of the user who made the reserve
	 */
	public PendingReserve( String logCode, ReserveLevel reserveLevel, 
			Catalogue catalogue, String username, Priority priority ) {
		
		super( catalogue, logCode, username, priority );
		
		this.reserveLevel = reserveLevel;
		
		// set the data of the pending action in order to
		// save the reserve level into the database under
		// the data column
		setData( reserveLevel.toString() );
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
				catalogue, username, Priority.HIGH );
		
		// create a pending reserve object in order to
		// retry the log retrieval (also if the application
		// is closed!)
		PendingActionDAO prDao = new PendingActionDAO();
		int id = prDao.insert( pr );
		
		pr.setId( id );
		
		return pr;
	}
	
	@Override
	public void processResponse( DcfResponse response ) {
		
		Catalogue catalogue = getCatalogue();
		String username = getUsername();
		
		// if we have forced the catalogue
		boolean forced = catalogue.isForceEdit( username );
		
		// if the reserve did not succeed and we
		// had edited the catalogue
		// invalidate the current catalogue
		boolean invalid = response != DcfResponse.OK && forced;
		boolean invalid2 = needNewVersion && forced;

		if ( invalid || invalid2 ) {

			// if not last version and the catalogue was
			// forced to editing => stultify the catalogue
			System.out.println( "Invalid catalogue " + catalogue );
			invalidate();
		}
		
		// Remove the forced editing from the catalogue
		// if it was enabled
		// since here we know the real dcf response
		// remove before reserve! We need to restore the
		// correct version of the catalogue
		//if ( forced )
		//	catalogue.removeForceEdit( username );
		
		// if the reserve succeeded download the
		// last internal version if present and
		// then reserve the catalogue
		if ( response == DcfResponse.OK ) {
			
			if ( needNewVersion ) {
				
				// download the last version of the catalogue
				importLastVersion ( new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {
						reserve();
					}
				});
			}  // otherwise simply reserve
			else
				reserve();
		}
	}

	/**
	 * Reserve or unreserve the catalogue related
	 * to the pending reserve
	 */
	private void reserve() {
		
		Catalogue catalogue = getCatalogue();
		
		if ( reserveLevel.greaterThan( ReserveLevel.NONE ) ) {
			
			// get the new internal version created by the reserve
			// and set it to the pending operation
			Catalogue newVersion = catalogue.reserve ( reserveLevel );
			setCatalogue( newVersion );
		}
		else {
			catalogue.unreserve ();
		}
		
		// update the catalogue status
		catalogue.setRequestingAction( false );
	}

	/**
	 * Stultify the current catalogue. This happens
	 * if we force the editing mode and then
	 * we discover that we could not edit it.
	 */
	private void invalidate() {
		
		Catalogue catalogue = getCatalogue();
		
		catalogue.invalidate();
		setStatus( PendingReserveStatus.INVALIDATED );
	}
	
	@Override
	public void processLog(Document log) {
		
		// analyze the log to get the result
		LogParser parser = new LogParser ( log );
		
		// get if we need a new version
		this.needNewVersion = parser.getCatalogueVersion().compareTo( 
				getCatalogue().getCatalogueVersion() ) > 0;
	}
	
	/**
	 * Get the log response analyzing the log xml
	 * @param log the log we want to analyze
	 * @return the dcf response contained in the log
	 */
	@Override
	public DcfResponse extractLogResponse ( Document log ) {
		
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
			response = DcfResponse.FORBIDDEN;
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
	 * Get the reserve level of the pending reserve request
	 * @return
	 */
	public ReserveLevel getReserveLevel() {
		return reserveLevel;
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

	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public void manageBusyStatus() {
		
		Catalogue catalogue = getCatalogue();
		
		// force editing of the catalogue since we have waited
		// but no response was received from the dcf
		// force the edit only if the editing was not already
		// forced and if we are reserving (not unreserving)
		if ( !catalogue.isForceEdit( getUsername() ) 
				&& reserveLevel.greaterThan( ReserveLevel.NONE ) ) {
			
			setStatus( PendingReserveStatus.FORCING_EDITING );
			
			Catalogue forcedCat = catalogue.forceEdit( 
					getUsername(), reserveLevel );
			
			setCatalogue( forcedCat );
		}
	}
}
