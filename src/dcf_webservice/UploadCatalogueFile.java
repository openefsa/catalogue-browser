package dcf_webservice;

import java.util.Base64;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import dcf_log_util.LogCodeFinder;
import dcf_manager.Dcf.DcfType;
import dcf_pending_action.PendingReserve;

/**
 * Model the upload catalogue file web service. Use the method
 * {@link #getAttachment()} to define the xml attachment which needs
 * to be uploaded to the dcf. Use {@link #processResponse(String)} to
 * process the log code related to the upload catalogue file request.
 * @author avonva
 *
 */
public abstract class UploadCatalogueFile extends SOAPAction {

	// namespace used in the ping xml message
	private static final String NAMESPACE = "http://ws.catalog.dc.efsa.europa.eu/";

	// web service link of the ping service
	private static final String URL = "https://dcf-cms.efsa.europa.eu/catalogues";
	private static final String TEST_URL = "https://dcf-01.efsa.test/dc-catalog-public-ws/catalogues/?wsdl";
	
	/**
	 * Initialize the url and the namespace of the upload catalogue file
	 * request.
	 */
	public UploadCatalogueFile( DcfType type ) {
		super ( type, NAMESPACE );
	}
	
	/**
	 * Upload the file to the dcf and get the 
	 * processed response
	 * @return
	 */
	public Object upload() {
		
		Object result = null;

		try {
			
			String url = getType() == DcfType.PRODUCTION ? URL : TEST_URL;
			
			// start the reserve operation
			result = (PendingReserve) makeRequest( url );

		} catch (SOAPException e) {

			e.printStackTrace();
		}
		
		return result;
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
		String attachmentData = getAttachment();
		
		// encoding attachment with base 64
		byte[] encodedBytes = Base64.getEncoder().encode( attachmentData.getBytes() );
		
		// row data node (child of file data)
		SOAPElement rowData = fileData.addChildElement( "rowData" );
		rowData.setValue( new String(encodedBytes) );
		
		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}
	
	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {
		
		// search the log code in the soap message
		LogCodeFinder finder = new LogCodeFinder( soapResponse );

		// cache the log code (used in the retry function)
		String logCode = finder.getLogCode();
		
		// if errors the log code is not returned
		if ( logCode == null )
			System.err.println ( "Cannot find the log code in the soap response" );
		else
			System.out.println ( "UploadCatalogueFile: found log code " + logCode );

		return processResponse( logCode );
	}
	
	/**
	 * Get the content of the attachment that will be
	 * attached to the upload catalogue file request
	 * @return
	 */
	public abstract String getAttachment();
	
	/**
	 * Process the log starting from its code
	 * @param logCode
	 * @return
	 */
	public abstract Object processResponse ( String logCode );
}
