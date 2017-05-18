package dcf_webservice;

import java.util.Base64;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import catalogue_object.Catalogue;
import dcf_log_util.LogCodeFinder;
import dcf_reserve_util.PendingReserve;
import dcf_reserve_util.ReserveValidator;
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
	
	/**
	 * Constructor to initialize the url and namespace for the
	 * reserve operation
	 */
	public Reserve() {
		super ( RESERVE_URL, RESERVE_NAMESPACE );
	}

	/**
	 * Reserve a catalogue with a major or minor reserve operation or unreserve it. 
	 * An additional description on why we reserve the catalogue is mandatory.
	 * Set reserve level to None to unreserve the catalogue.
	 * @param catalogue the catalogue we want to (un)reserve
	 * @param reserveLevel the reserve level we want to set (none, minor, major)
	 * @param reserveDescription a description of why we are (un)reserving
	 * @return the pending reserve which containes all the information related
	 * to this reserve request
	 */
	public PendingReserve reserve( Catalogue catalogue, ReserveLevel reserveLevel, 
			String reserveDescription ) {
		
		this.catalogue = catalogue;
		this.reserveLevel = reserveLevel;
		this.reserveDescription = reserveDescription;

		System.out.println ( reserveLevel.getReserveOperation() + ": " + catalogue );

		PendingReserve pr = null;

		try {
			
			// start the reserve operation
			pr = (PendingReserve) makeRequest();
			
		} catch (SOAPException e) {
			
			e.printStackTrace();
		}
		
		return pr;
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
	 * Process the response of the dcf. We find the log code of the response
	 * and pass to the {@link ReserveValidator} thread the task of downloading
	 * the log document (using the log code) and retrieving the dcf response
	 * contained in the log.
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
		
		System.out.println ( "Reserve: found log code " + logCode );

		// add a pending reserve object to the db
		// to save the request
		PendingReserve pr = PendingReserve.addPendingReserve( 
				logCode, reserveLevel, catalogue, 
				User.getInstance().getUsername() );

		return pr;
	}
}
