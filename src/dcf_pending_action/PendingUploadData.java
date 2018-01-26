package dcf_pending_action;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import catalogue.Catalogue;
import dcf_log.DcfLog;
import dcf_log.DcfResponse;
import dcf_manager.Dcf.DcfType;
import sas_remote_procedures.XmlUpdateFile;
import sas_remote_procedures.XmlUpdateFileDAO;

/**
 * Pending action used to call an upload data request
 * @author avonva
 *
 */
public class PendingUploadData extends PendingAction {
	
	private static final Logger LOGGER = LogManager.getLogger(PendingUploadData.class);
	
	public static final String TYPE = "UPLOAD_DATA";
	
	public PendingUploadData(Catalogue catalogue, String logCode, 
			String username, Priority priority, DcfType dcfType) {
		super(catalogue, logCode, username, "", priority, dcfType);
	}

	/**
	 * Create a new pending upload data object
	 * @param logCode
	 * @param catalogue
	 * @param username
	 * @return the pending upload data object
	 */
	public static PendingUploadData addPendingUploadData ( String logCode, 
			Catalogue catalogue, String username, DcfType dcfType ) {
		
		// we create a new pending publish with FAST priority
		PendingUploadData pud = new PendingUploadData( catalogue, logCode, 
				username, Priority.HIGH, dcfType );
		
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
		return TYPE;
	}

	@Override
	public void manageBusyStatus() {}

	@Override
	public void processLog(DcfLog log) {}

	@Override
	public DcfResponse extractLogResponse(DcfLog log) {
		
		DcfResponse response;
		
		// was the operation correct?
		boolean correct = log.isMacroOperationCorrect();
		
		if ( correct )
			response = DcfResponse.OK;
		else
			response = DcfResponse.AP;
		
		if ( response == DcfResponse.OK )
			LOGGER.info ( "upload data of " + log.getCatalogueCode() + ": successfully completed" );
		else
			LOGGER.info( "upload data of " + log.getCatalogueCode() + ": failed" );
		
		return response;
	}

	@Override
	public void processResponse(DcfResponse response) {
		
		// remove the xml from the database
		XmlUpdateFileDAO xmlDao = new XmlUpdateFileDAO();
		XmlUpdateFile xml = xmlDao.getById( getCatalogue().getId() );
		
		if ( xml != null )
			xml.delete();
		else
			System.err.println ( "Cannot delete xml file related to " + getCatalogue() );
		
		// terminate the upload data action
		terminate();
	}
}
