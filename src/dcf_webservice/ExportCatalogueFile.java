package dcf_webservice;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import dcf_manager.AttachmentHandler;
import dcf_manager.Dcf.DcfType;

/**
 * Export catalogue file web service. See GDE2 and DCF manuals to
 * see all the available options. Here we can download a file from
 * the dcf, as a internal version of a catalogue or a log.
 * @author avonva
 *
 */
public class ExportCatalogueFile extends SOAPAction {

	// namespace used in the ping xml message
	private static final String EXPORT_FILE_NAMESPACE = "http://ws.catalog.dc.efsa.europa.eu/";

	// web service link of the ping service
	private static final String URL = "https://dcf-cms.efsa.europa.eu/catalogues";
	private static final String TEST_URL = "https://dcf-01.efsa.test/dc-catalog-public-ws/catalogues/?wsdl";
	
	// export types used in the request
	private static final String EXPORT_TYPE_LOG = "log";
	private static final String EXPORT_TYPE_INTERNAL_VERSION = "catalogInternalVersion";
	
	private static final String XML_FILE_TYPE = "XML";
	
	private String catalogueCode;
	private String exportType;
	private String fileType;
	
	/**
	 * Initialize the export file action
	 */
	public ExportCatalogueFile( DcfType type ) {
		super( type, EXPORT_FILE_NAMESPACE );
	}
	
	/**
	 * Export a log from the DCF given its code
	 * @param code
	 */
	public Document exportLog( String code ) {
		
		Object log = exportXml ( code, EXPORT_TYPE_LOG );
		
		if ( log != null )
			return (Document) log;
		
		return null;
	}
	
	/**
	 * Export the last internal version of the catalogue.
	 * @param catalogueCode the code of the catalogue we want to consider
	 * @return the input stream containing the xml catalogue data
	 */
	public InputStream exportLastInternalVersion ( String catalogueCode ) {
		
		Object lastVersion = exportXml ( catalogueCode, 
				EXPORT_TYPE_INTERNAL_VERSION, XML_FILE_TYPE );
		
		if ( lastVersion != null )
			return (InputStream) lastVersion;
		
		return null;
	}
	
	/**
	 * Export an xml file given the code and export type fields
	 * to be inserted in the request.
	 * @param code code of the object we are considering as the
	 * catalogue code or the log code
	 * @param exportType the export type (see GDE2)
	 * @return an object containing the xml structure (document or inputstream)
	 */
	private Object exportXml ( String code, String exportType ) {
		return exportXml( code, exportType, null );
	}
	
	/**
	 * Export an xml file given the code and export type fields
	 * to be inserted in the request.
	 * @param code code of the object we are considering as the
	 * catalogue code or the log code
	 * @param exportType the export type (see GDE2)
	 * @param fileType the type of the attachment we want
	 * @return an object containing the xml structure
	 */
	private Object exportXml ( String code, String exportType, String fileType ) {
		
		this.catalogueCode = code;
		this.exportType = exportType;
		this.fileType = fileType;
		
		Object result = export();

		return result;
	}
	
	/**
	 * Make the export request.
	 * @return
	 */
	private Object export() {
		
		try {
			
			String url = getType() == DcfType.PRODUCTION ? URL : TEST_URL;
			
			return makeRequest( url );
		} catch (SOAPException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {

		// create the standard structure and get the message
		SOAPMessage request = createTemplateSOAPMessage ( "ws" );

		// get the body of the message
		SOAPBody soapBody = request.getSOAPPart().getEnvelope().getBody();

		// export catalogue file node
		SOAPElement export = soapBody.addChildElement( "ExportCatalogueFile", "ws" );

		// add the catalogue code to the xml if there is one
		if ( catalogueCode != null ) {
			SOAPElement catCodeNode = export.addChildElement( "catalogueCode" );
			catCodeNode.setValue( catalogueCode );
		}
		
		// add the catalogue code to the xml if there is one
		if ( exportType != null ) {
			SOAPElement exportTypeNode = export.addChildElement( "exportType" );
			exportTypeNode.setValue( exportType );
		}
		
		// add the file type if required
		if ( fileType != null ) {
			SOAPElement fileTypeNode = export.addChildElement( "fileType" );
			fileTypeNode.setValue( fileType );
		}

		// save the changes
		request.saveChanges();
		
		return request;
	}

	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {
		
		Object response = null;
		
		// process the response based
		// on the export type field
		switch ( exportType ) {
		case EXPORT_TYPE_LOG:
			response = processXml ( soapResponse );
			break;
		case EXPORT_TYPE_INTERNAL_VERSION:
			response = processEfficientXml( soapResponse );
			break;
		default:
			break;
		}

		return response;
	}
	
	/**
	 * Process an xml attachment without binding it into a dom Document
	 * @param soapResponse
	 * @return the input stream containing the xml
	 * @throws SOAPException
	 */
	private InputStream processEfficientXml ( SOAPMessage soapResponse ) throws SOAPException {
		
		try {
			
			AttachmentPart part = getFirstAttachment( soapResponse );

			// if no attachment => errors in processing response, return null
			if ( part == null )
				return null;

			// create an attachment handler to analyze the soap attachment
			AttachmentHandler handler = new AttachmentHandler( part, false );

			return handler.readAttachment();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Process the response getting the xml attachment and returning
	 * it under the Document format.
	 * @param soapResponse
	 * @return the dom document which contains the xml attachment
	 * @throws SOAPException
	 */
	private Document processXml( SOAPMessage soapResponse ) throws SOAPException {
		
		try {

			AttachmentPart part = getFirstAttachment( soapResponse );

			// if no attachment => errors in processing response, return null
			if ( part == null )
				return null;

			// create an attachment handler to analyze the soap attachment
			AttachmentHandler handler = new AttachmentHandler( part, true );

			// read the xml file with java DOM
			DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance();

			DocumentBuilder builder = factory.newDocumentBuilder();

			// get the input stream of the log attachment and read it
			// note that the log file is zipped so we set the
			// zipped flag to true
			Document doc = builder.parse( handler.readAttachment() );

			return doc;

		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
