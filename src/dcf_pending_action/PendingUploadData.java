package dcf_pending_action;

import org.w3c.dom.Document;

import catalogue.Catalogue;
import dcf_log_util.LogParser;
import dcf_webservice.DcfResponse;

public class PendingUploadData extends PendingAction {

	public static final String UPLOAD_DATA_TYPE = "uploadData";
	
	public PendingUploadData(Catalogue catalogue, String logCode, 
			String username, Priority priority) {
		super(catalogue, logCode, username, "", priority);
	}

	/**
	 * Create a new pending upload data object
	 * @param logCode
	 * @param catalogue
	 * @param username
	 * @return the pending upload data object
	 */
	public static PendingUploadData addPendingUploadData ( String logCode, 
			Catalogue catalogue, String username ) {
		
		// we create a new pending publish with FAST priority
		PendingUploadData pud = new PendingUploadData( catalogue, logCode, 
				username, Priority.HIGH );
		
		// create a pending publish object in order to
		// retry the log retrieval (also if the application
		// is closed!)
		PendingActionDAO prDao = new PendingActionDAO();
		int id = prDao.insert( pud );
		
		pud.setId( id );
		
		return pud;
	}
	
	@Override
	public String getType() {
		return UPLOAD_DATA_TYPE;
	}

	@Override
	public void manageBusyStatus() {}

	@Override
	public void processLog(Document log) {}

	@Override
	public DcfResponse extractLogResponse(Document log) {
		
		DcfResponse response;
		
		// analyze the log to get the result
		LogParser parser = new LogParser ( log );
		
		// was the operation correct?
		boolean correct = parser.isOperationCorrect();
		
		if ( correct )
			response = DcfResponse.OK;
		else
			response = DcfResponse.AP;
		
		if ( response == DcfResponse.OK )
			System.out.println ( "upload data: successfully completed" );
		
		return response;
	}

	@Override
	public void processResponse(DcfResponse response) {
		// terminate the upload data action
		terminate();
	}
}
