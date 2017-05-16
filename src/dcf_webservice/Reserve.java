package dcf_webservice;

import java.util.Base64;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;

import catalogue_object.Catalogue;
import dcf_log_util.LogCodeFinder;
import dcf_log_util.LogCodeFoundListener;
import dcf_log_util.LogParser;
import dcf_manager.Dcf;
import dcf_reserve_util.PendingReserve;
import dcf_user.User;

/**
 * Class used to reserve/unreserve catalogues through the dcf web service.
 * @author avonva
 *
 */
public class Reserve extends SOAPAction {

	// namespace used in the ping xml message
	private static final String RESERVE_NAMESPACE = "http://ws.catalog.dc.efsa.europa.eu/";

	// web service link of the ping service
	//private static final String RESERVE_URL = "https://dcf-cms.efsa.europa.eu/catalogues";
	private static final String RESERVE_URL = "https://dcf-01.efsa.test/dc-catalog-public-ws/catalogues/?wsdl";

	// the catalogue we want to reserve/unreserve
	private Catalogue catalogue;
	
	// the reserve level we want to apply to the catalogue
	private ReserveLevel reserveLevel;
	
	// the description of why we are reserving/unreserving
	private String reserveDescription;
	
	// Listener called when the reserve log code is found
	private LogCodeFoundListener logCodeListener;
	
	/**
	 * Constructor to initialize the url and namespace for the
	 * reserve operation
	 */
	public Reserve() {
		super ( RESERVE_URL, RESERVE_NAMESPACE );
	}
	
	/**
	 * Set the listener which is called when the reserve
	 * log code is found.
	 * @param logCodeListener
	 */
	public void setLogCodeListener(LogCodeFoundListener logCodeListener) {
		this.logCodeListener = logCodeListener;
	}
	
	/**
	 * Reserve a catalogue with a major or minor reserve operation or unreserve it. 
	 * An additional description on why we reserve the catalogue is mandatory.
	 * Set reserve level to None to unreserve the catalogue.
	 * @param catalogue the catalogue we want to (un)reserve
	 * @param reserveLevel the reserve level we want to set (none, minor, major)
	 * @param reserveDescription a description of why we are (un)reserving
	 * @return the {@linkplain DcfResponse} enum, if all went ok then it returns OK, 
	 * if the operation failed it returns NO, if the connection with the DCF failed 
	 * or some errors occur it returns ERROR
	 */
	public DcfResponse reserve( Catalogue catalogue, ReserveLevel reserveLevel, 
			String reserveDescription ) {
		
		this.catalogue = catalogue;
		this.reserveLevel = reserveLevel;
		this.reserveDescription = reserveDescription;
		
		// log
		switch( reserveLevel ) {
		case NONE:
			System.out.println ( "Unreserving catalogue " + catalogue );
			break;
		case MINOR:
		case MAJOR:
			System.out.println ( "Reserving catalogue " + catalogue + " at level " + reserveLevel );
			break;
		default:
			break;
		}

		
		// default response
		DcfResponse response = DcfResponse.ERROR;
		
		// make the reserve operation
		// and get the dcf response
		try {
			
			// get the log response
			response = (DcfResponse) makeRequest();

		} catch (SOAPException e) {
			e.printStackTrace();
		}
		
		// return the response
		return response;
	}

	/**
	 * Create the reserve request message
	 */
	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {

		// create the standard structure and get the message
		SOAPMessage soapMsg = createTemplateSOAPMessage ( "ws" );
		
		// get the body of the message
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();

		// upload catalogue file node
		SOAPElement upload = soapBody.addChildElement( "UploadCatalogueFile", "ws" );
		
		// file data node (child of upload cf)
		SOAPElement fileData = upload.addChildElement( "fileData" );
		
		// add attachment to the request into the node <rowData>
		// using the right message for the related reserve operation
		String attachmentData = UploadMessages.getReserveMessage(
				catalogue.getCode(), reserveLevel, reserveDescription );
		
		// encoding attachment with base 64
		byte[] encodedBytes = Base64.getEncoder().encode( attachmentData.getBytes() );
		
		// row data node (child of file data)
		SOAPElement rowData = fileData.addChildElement( "rowData" );
		rowData.setValue( new String(encodedBytes) );
		
		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}

	/**
	 * Process the response of the dcf, we check if everything went
	 * using the log which is returned in the response message. In particular,
	 * the log code is given. We can export the log using another web service
	 * and setting the log code as parameter.
	 * @return {@linkplain DcfResponse} enum, which shows if the request was
	 * successful or if there were errors.
	 */
	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {
		
		// search the log code in the soap message
		LogCodeFinder finder = new LogCodeFinder( soapResponse );

		// cache the log code (used in the retry function)
		String logCode = finder.getLogCode();
		
		// if errors the log code is not returned
		if ( logCode == null ) {
			System.err.println ( "Cannot find the log code in the soap response" );
			return DcfResponse.ERROR;
		}
		
		System.out.println ( "Found reserve log code " + logCode );

		// get the response and return it
		return getLogResponse ( logCode );
	}
	
	
	/**
	 * Get the response contained in the Log
	 * identified by the logCode
	 * @param logCode the code of the log to download and analyze
	 * @return the {@linkplain DcfResponse} response of the log
	 */
	private DcfResponse getLogResponse ( String logCode ) {
		
		int attempts = 12;              // 12 times 10 seconds => 2 minutes
		long interAttemptsTime = 10000; // 10 seconds
		
		// add a pending reserve in order to
		// retry getting the reserve log in bg 
		// ( also if the application
		// is closed! ) if the dcf will be busy
		PendingReserve pr = PendingReserve.addPendingReserve( 
				logCode, reserveLevel, catalogue, 
				User.getInstance().getUsername() );
		
		// call the log code listener if it was set
		// we call it here because we have saved the
		// log code into the database
		if ( logCodeListener != null )
			logCodeListener.logCodeFound( logCode );
		
		// get the log from the dcf through the pending
		// reserve action
		Document log = getLog( logCode, attempts, 
				interAttemptsTime );

		// get the response
		DcfResponse response = getLogResponse( log );

		// delete the pending reserve if the
		// dcf is not busy (the retry is meaningless)
		if ( response != DcfResponse.BUSY ) {
			pr.delete();
		}
		else {
			
			// set the pending reserve to the enum
			// if dcf is busy
			response.setPendingReserve( pr );
		}

		return response;
	}
	
	/**
	 * Get the log response analyzing the log xml
	 * @param log the log we want to analyze
	 * @return the dcf response contained in the log
	 */
	public DcfResponse getLogResponse ( Document log ) {
		
		// no log is found => dcf is busy
		if ( log == null )
			return DcfResponse.BUSY;
		
		// analyze the log to get the result
		LogParser parser = new LogParser ( log );

		// return ok if correct operation
		if ( parser.isOperationCorrect() ) {
			System.out.println ( "(Un)reserve successfully completed" );
			return DcfResponse.OK;
		}
		else {
			System.out.println ( "(Un)reserve failed - the dcf rejected the operation" );
			return DcfResponse.NO;
		}
	}
	
	/**
	 * Get a log from the Dcf. Since it is possible that 
	 * the log is not immediately created by the Dcf, we
	 * try to get it several times (i.e. attempts parameter)
	 * @param logCode the code of the log we want to retrieve
	 * @param attempts the maximum number of attempts
	 * @param interAttemptsTime the waiting time in milliseconds 
	 * between one attempt and the other.
	 * @param retryTime the waiting time in milliseconds which
	 * is waited to retry the reserve after all attempts failed
	 * @return the log in a {@linkplain Document} format
	 * since the log is an xml file.
	 */
	public Document getLog( String logCode, int attempts, 
			long interAttemptsTime ) {

		// try different times to get the dcf log
		// since it is not created immediately
		Document log = null;

		// try several times to get the log from the dcf
		for ( int i = 0; i < attempts; i++ ) {

			// ask for the log to the dcf
			Dcf dcf = new Dcf();
			log = dcf.exportLog( logCode );

			// if log found break, otherwise retry
			if ( log != null ) {
				break;
			}

			System.err.println ( "Log not found, retrying attempt n° " 
					+ (i+1) + "/" + attempts );

			// wait if a next iteration is planned
			if ( i < attempts - 1 ) {
				try {
					Thread.sleep( interAttemptsTime );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		return log;
	}
}
