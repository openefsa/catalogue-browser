package dcf_webservice;

import java.io.IOException;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import catalogue.Catalogue;
import dcf_manager.AttachmentHandler;
import dcf_manager.Dcf.DcfType;
import utilities.GlobalUtil;

/**
 * Export catalogue web service action. We write the attachment of the xml response to the
 * selected filename.
 * @author avonva
 *
 */
public class ExportCatalogue extends SOAPAction {

	// namespace used in the export catalogue xml message
	private static final String EXPORT_CATALOGUE_NAMESPACE = "http://ws.catalog.dc.efsa.europa.eu/";

	// web service link of the export catalogue service
	private static final String URL = "https://dcf-cms.efsa.europa.eu/catalogues";
	private static final String TEST_URL = "https://dcf-01.efsa.test/dc-catalog-public-ws/catalogues/?wsdl";
	
	private Catalogue catalogue;
	private String filename;
	
	/**
	 * Start an export catalogue action. We want to export a catalogue into the
	 * selected file
	 * @param filename
	 */
	public ExportCatalogue( DcfType type, Catalogue catalogue, String filename ) {
		
		super( type, EXPORT_CATALOGUE_NAMESPACE );
		
		this.catalogue = catalogue;
		this.filename = filename;
	}
	
	/**
	 * Export the catalogue, return true if the procedure was successful
	 * @return
	 */
	public boolean exportCatalogue() {
		try {
			String url = getType() == DcfType.PRODUCTION ? URL : TEST_URL;
			makeRequest( url );
		} catch (SOAPException e) {
			e.printStackTrace();
		}
		
		return GlobalUtil.fileExists( filename );
	}
	
	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {
		/*
		<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ws="http://ws.catalog.dc.efsa.europa.eu/">
			<SOAP-ENV:Header/>
			<SOAP-ENV:Body>
				<ws:ExportCatalogueFile>
					<catalogueCode>ADDFOOD</catalogueCode>
					<exportType>catalogFullDefinition</exportType>
					<fileType>XML</fileType>
				</ws:ExportCatalogueFile>
			</SOAP-ENV:Body>
		</SOAP-ENV:Envelope>
		 */

		// create the standard structure and get the message
		SOAPMessage soapMsg = createTemplateSOAPMessage ( "ws" );

		// get the body of the message
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();

		// create the xml message structure to make a ping with SOAP
		SOAPElement soapElement = soapBody.addChildElement( "ExportCatalogueFile", "ws" );

		SOAPElement catCode = soapElement.addChildElement( "catalogueCode" );
		catCode.setValue( catalogue.getCode() );

		SOAPElement exportType = soapElement.addChildElement( "exportType" );
		exportType.setValue( "catalogFullDefinition" );

		SOAPElement fileType = soapElement.addChildElement( "fileType" );
		fileType.setValue( "XML" );

		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}
	
	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {

		// get the response attachment
		AttachmentPart attachment = getFirstAttachment( soapResponse );
		
		if ( attachment == null ) {
			System.err.println( "ExportCatalogueFile: Attachment not found for " + catalogue );
			return null;
		}
		
		// write the attachment to the file
		try {
			
			// the attachment is not zipped!
			AttachmentHandler handler = new AttachmentHandler( attachment, false );
			handler.writeAttachment( filename );
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
