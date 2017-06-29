package dcf_pending_action;

import java.io.File;
import java.io.IOException;

import javax.xml.soap.SOAPException;

import catalogue.Catalogue;
import dcf_log.DcfLog;
import dcf_manager.Dcf;
import dcf_manager.Dcf.DcfType;
import dcf_webservice.DcfResponse;
import sas_remote_procedures.XmlUpdateFile;
import sas_remote_procedures.XmlUpdateFileDAO;

public class PendingXmlDownload extends PendingAction {

	public static final String TYPE = "DOWNLOAD_XML_UPDATES";
	
	public PendingXmlDownload( Catalogue catalogue, String username, DcfType dcfType ) {
		super( catalogue, "", username, "", Priority.HIGH, dcfType );
	}
	
	/**
	 * Create a pending upload data object and insert it into the db
	 * @param catalogue
	 * @param username
	 * @param dcfType
	 * @return
	 */
	public static PendingXmlDownload addPendingDownload ( Catalogue catalogue, 
			String username, DcfType dcfType ) {
		
		// create the pending download action
		PendingXmlDownload pa = new PendingXmlDownload( catalogue, 
				username, dcfType);
		
		// insert in db
		PendingActionDAO prDao = new PendingActionDAO();
		int id = prDao.insert( pa );
		
		pa.setId( id );
		
		return pa;
	}

	@Override
	public void start(boolean notifyStart) throws SOAPException {
		
		setStatus( PendingActionStatus.STARTED );
		
		// set that the file does not need the upload
		// anymore, since it was uploaded (we are only
		// waiting for the response)
		XmlUpdateFileDAO xmlDao = new XmlUpdateFileDAO();
		
		final XmlUpdateFile file = xmlDao.getById( getCatalogue().getId() );
		
		if ( file == null ) {
			System.err.println ( "No xml filename was found for " + getCatalogue() );
			return;
		}
		
		File xmlFile = null;
		
		// download the file
		try {
			xmlFile = file.downloadXml( 5000 );
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// create upload data request and terminate
		Dcf dcf = new Dcf();
		dcf.uploadDataBG( getCatalogue(), xmlFile, getListener() );
		
		// finish this action
		terminate();
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
		return null;
	}

	@Override
	public void processResponse(DcfResponse response) throws SOAPException {}
}
