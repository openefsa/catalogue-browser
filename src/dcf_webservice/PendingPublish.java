package dcf_webservice;

import org.w3c.dom.Document;

import catalogue_object.Catalogue;
import catalogue_object.Status;
import dcf_log_util.LogParser;
import dcf_webservice.Publish.PublishLevel;

/**
 * Pending actions for publish operations.
 * @author avonva
 *
 */
public class PendingPublish extends PendingAction {

	public static final String TYPE = "PUBLISH";
	private PublishLevel publishLevel;
	
	public PendingPublish( Catalogue catalogue, String logCode, String username, Priority priority, PublishLevel publishLevel ) {
		
		super(catalogue, logCode, username, priority);
		
		this.publishLevel = publishLevel;
		setData( publishLevel.toString() );
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
}
