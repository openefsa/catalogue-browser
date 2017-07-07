package dcf_webservice;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;

import data_collection.DCResourceParser;
import data_collection.DCTable;
import dcf_manager.Dcf.DcfType;
import utilities.GlobalUtil;

public class GetFile extends SOAPAction {

	// namespace used in the ping xml message
	private static final String NAMESPACE = "http://dcf-elect.efsa.europa.eu/";

	// web service link of the ping service
	private static final String URL = "https://dcf-elect.efsa.europa.eu/elect2";
	private static final String TEST_URL = "https://dcf-01.efsa.test/dcf-dp-ws/elect2/?wsdl";

	private String resourceId;

	public GetFile(DcfType type) {
		super(type, NAMESPACE);
	}

	@SuppressWarnings("unchecked")
	public Collection<DCTable> getFile( String resourceId ) throws SOAPException {
		this.resourceId = resourceId;
		String url = getType() == DcfType.PRODUCTION ? URL : TEST_URL;
		return (Collection<DCTable>) makeRequest( url );
	}

	/**
	 * Create a GetFile request
	 */
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {

		// create the standard structure and get the message
		SOAPMessage soapMsg = createTemplateSOAPMessage ( "dcf" );
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();
		SOAPElement soapElem = soapBody.addChildElement( "GetFile", "dcf" );

		// add resource id
		SOAPElement arg = soapElem.addChildElement( "trxResourceId" );
		arg.setTextContent( resourceId );

		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}

	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {

		String filename = GlobalUtil.getTempDir() + "_" + resourceId +
				"_config_" + System.nanoTime() + ".xml";

		// write the attachment
		File file;
		try {

			// write the xml attachment
			file = writeAttachment( soapResponse, filename, false );

			if ( file == null )
				return null;

			// parse the xml and get the tables
			DCResourceParser parser = new DCResourceParser ( file );
			
			Collection<DCTable> tables = parser.parse();
			
			// close the parser
			parser.close();
			
			// delete temporary file
			GlobalUtil.deleteFileCascade( file );
			
			return tables;
			
		} catch (IOException | XMLStreamException e) {
			e.printStackTrace();
		}

		return null;
	}
}
