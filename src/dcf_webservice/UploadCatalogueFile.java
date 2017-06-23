package dcf_webservice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import dcf_log.LogCodeFinder;
import dcf_manager.Dcf.DcfType;
import dcf_pending_action.PendingAction;

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
	 * Upload the attached file to the dcf and get the 
	 * processed response
	 * @return
	 * @throws SOAPException 
	 */
	public Object upload() throws SOAPException {

		Object result = null;
		String url = getType() == DcfType.PRODUCTION ? URL : TEST_URL;

		// start the reserve operation
		result = (PendingAction) makeRequest( url );

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
		
		// get the attachment in base64 format
		byte[] encodedBytes = getEncodedAttachment();
		
		// row data node (child of file data)
		SOAPElement rowData = fileData.addChildElement( "rowData" );
		rowData.setValue( new String(encodedBytes) );
		
		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}
	
	/**
	 * Get the catalogue attachment and encode it based on
	 * its attachment type.
	 * @return
	 */
	private byte[] getEncodedAttachment () {

		// add attachment to the request into the node <rowData>
		// using the right message for the related reserve operation
		CatalogueAttachment att = getAttachment();

		byte[] data = null;
		if ( att.getType() == AttachmentType.ATTACHMENT ) {
			data = att.getContent().getBytes();
		}
		else {
			
			String attachmentPath = att.getContent();
			Path path = Paths.get( attachmentPath );
			
			// read the file as byte array
			try {
				data = Files.readAllBytes( path );
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// encode the data with base64
		return Base64.getEncoder().encode( data );
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
	 * Define the type of the attachment
	 * @author avonva
	 *
	 */
	public enum AttachmentType {
		FILE_PATH,
		ATTACHMENT;
	}
	
	/**
	 * Catalogue attachment. It contains either a file path
	 * (with {@link #type} = {@link AttachmentType#FILE_PATH}
	 * or an attachment its self with {@link #type } = 
	 * {@link AttachmentType#ATTACHMENT}.
	 * @author avonva
	 *
	 */
	public class CatalogueAttachment {
		
		private AttachmentType type;
		private String content;
		
		/**
		 * Initialize a catalogue attachment. If you pass in {@code content} a file path
		 * set as {@code type} the value {@link AttachmentType#FILE_PATH}.
		 * Otherwise, if you pass in {@code content} the attachment itself in string format,
		 * set {@code type } = {@link AttachmentType#ATTACHMENT}.
		 * 
		 * @param type {@link AttachmentType#FILE_PATH} for file paths,
		 * {@link AttachmentType#ATTACHMENT} for attachments
		 * @param content the contents of the attachment, a path or the attachment itself
		 */
		public CatalogueAttachment( AttachmentType type, String content ) {
			this.type = type;
			this.content = content;
		}
		
		/**
		 * Get the type of the attachment. 
		 * @see {@link #CatalogueAttachment}
		 * @return
		 */
		public AttachmentType getType() {
			return type;
		}
		
		/**
		 * Get the content of the attachment.
		 * @return
		 */
		public String getContent() {
			return content;
		}
	}
	
	/**
	 * Get the content of the attachment that will be
	 * attached to the upload catalogue file request
	 * @return
	 */
	public abstract CatalogueAttachment getAttachment();
	
	/**
	 * Process the log starting from its code
	 * @param logCode
	 * @return
	 */
	public abstract Object processResponse ( String logCode );
}
