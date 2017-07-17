package dcf_webservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

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
	private String filename;
	
	/**
	 * Initialize the export file action
	 */
	public ExportCatalogueFile( DcfType type ) {
		super( type, EXPORT_FILE_NAMESPACE );
	}

	/**
	 * Download a log file related to an upload catalogue file operation.
	 * @param code the code of the log we want to download
	 * @param filename the file where we want to store the log
	 * @return a File object which points to the log file
	 * @throws SOAPException 
	 */
	public File exportLog ( String code, String filename ) 
			throws SOAPException {
		
		Object log = exportXml ( code, EXPORT_TYPE_LOG, 
				XML_FILE_TYPE, filename );
		
		if ( log != null )
			return (File) log;
		
		return null;
	}
	
	/**
	 * Export the last internal version of the catalogue.
	 * @param catalogueCode the code of the catalogue we want to consider
	 * @param filename the file where we want to store the downloaded catalogue
	 * @return a File object which points to the downloaded catalogue .xml file
	 * @throws SOAPException 
	 */
	public File exportLastInternalVersion ( String catalogueCode, String filename  ) 
			throws SOAPException {
		
		Object lastVersion = exportXml ( catalogueCode, 
				EXPORT_TYPE_INTERNAL_VERSION, 
				XML_FILE_TYPE, filename );
		
		if ( lastVersion != null )
			return (File) lastVersion;
		
		return null;
	}
	
	/**
	 * Export an xml file given the code and export type fields
	 * to be inserted in the request.
	 * @param code code of the object we are considering as the
	 * catalogue code or the log code
	 * @param exportType the export type (see GDE2)
	 * @param fileType the type of the attachment we want
	 * @return an object containing the xml structure
	 * @throws SOAPException 
	 */
	private Object exportXml ( String code, String exportType, 
			String fileType, String filename ) throws SOAPException {
		
		this.catalogueCode = code;
		this.exportType = exportType;
		this.fileType = fileType;
		this.filename = filename;
		
		Object result = export();

		return result;
	}
	
	/**
	 * Make the export request.
	 * @return
	 * @throws SOAPException 
	 */
	private Object export() throws SOAPException {

		String url = getType() == DcfType.PRODUCTION ? URL : TEST_URL;

		return makeRequest( url );
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
			response = processXml ( soapResponse, true );
			break;
		case EXPORT_TYPE_INTERNAL_VERSION:
			response = processXml( soapResponse, false );
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
	private File processXml ( SOAPMessage soapResponse, boolean isZipped ) throws SOAPException {
		
		try {
			
			AttachmentPart part = getFirstAttachment( soapResponse );

			// if no attachment => errors in processing response, return null
			if ( part == null )
				return null;

			// create an attachment handler to analyze the soap attachment
			AttachmentHandler handler = new AttachmentHandler( part, isZipped );

			File file = new File( filename );
			
			// create the file if it does not exist
			file.createNewFile();
			
			InputStream inputStream = handler.readAttachment();
			OutputStream outputStream = new FileOutputStream( file );

			byte[] buf = new byte[512];
			int num;
			
			// write file
			while ( (num = inputStream.read(buf) ) != -1) {
				outputStream.write(buf, 0, num);
			}
			
			outputStream.close();
			inputStream.close();
			handler.close();
			
			
			return file;

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
