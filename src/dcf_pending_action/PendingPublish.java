package dcf_pending_action;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.w3c.dom.Document;

import catalogue.Catalogue;
import catalogue_object.Status;
import dcf_log_util.LogParser;
import dcf_webservice.DcfResponse;
import dcf_webservice.Publish.PublishLevel;

/**
 * Pending actions for publish operations.
 * @author avonva
 *
 */
public class PendingPublish extends PendingAction {

	public static final String TYPE = "PUBLISH";
	private PublishLevel publishLevel;
	
	public PendingPublish( Catalogue catalogue, String logCode, String username, 
			Priority priority, PublishLevel publishLevel ) {
		
		super(catalogue, logCode, username, "", priority);
		
		this.publishLevel = publishLevel;
		setData( publishLevel.toString() );
	}
	

	/**
	 * Create a new pending publish object
	 * @param logCode
	 * @param level
	 * @param catalogue
	 * @param username
	 * @return
	 */
	public static PendingPublish addPendingPublish ( String logCode, 
			PublishLevel level, Catalogue catalogue, String username ) {
		
		// we create a new pending publish with FAST priority
		PendingPublish pr = new PendingPublish( catalogue, logCode, 
				username, Priority.HIGH, level );
		
		// create a pending publish object in order to
		// retry the log retrieval (also if the application
		// is closed!)
		PendingActionDAO prDao = new PendingActionDAO();
		int id = prDao.insert( pr );
		
		pr.setId( id );
		
		return pr;
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
	/**
	 * Get the publish level
	 * @return
	 */
	public PublishLevel getPublishLevel() {
		return publishLevel;
	}

	@Override
	public void manageBusyStatus() {}

	@Override
	public DcfResponse extractLogResponse(Document log) {
		
		DcfResponse response;
		
		// analyze the log to get the result
		LogParser parser = new LogParser ( log );
		
		Status catStatus = parser.getCatalogueStatus();
		boolean correct = parser.isOperationCorrect();
		boolean isDraft = catStatus.isDraft();
		
		// if we have sent a minor reserve but the
		// catalogue status is major draft, then the
		// action is forbidden
		if ( !correct && !isDraft ) {
			response = DcfResponse.FORBIDDEN;
		}
		// return ok if correct operation
		else if ( correct )
			response = DcfResponse.OK;
		else
			response = DcfResponse.AP;
		
		if ( response == DcfResponse.OK )
			System.out.println ( publishLevel.getOp() 
					+ ": successfully completed" );
		else
			System.out.println ( publishLevel.getOp() 
					+ ": failed - the dcf rejected the operation" );

		return response;
	}

	@Override
	public void processResponse(DcfResponse response) {
		
		// only correct operations
		if ( response != DcfResponse.OK ) {
			terminate();
			return;
		}
		
		// import the last version if there is one
		// and publish the catalogue
		importLastVersion( new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				publish();
				terminate();
			}
		});
	}
	
	/**
	 * Publish the catalogue
	 */
	private void publish() {
		
		Catalogue newVersion = null;
		
		Catalogue catalogue = getCatalogue();
		
		switch ( publishLevel ) {
		case MAJOR:
			newVersion = catalogue.publishMajor();
			break;
		case MINOR:
			newVersion = catalogue.publishMinor();
			break;
		default:
			break;
		}
		
		// update the catalogue of the pending action
		setCatalogue( newVersion );
	}


	@Override
	public void processLog(Document log) {}
}
