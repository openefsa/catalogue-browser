package dcf_webservice;

import java.util.Iterator;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Node;

import dcf_manager.Dcf.DcfType;

/**
 * Class to manage a ping request to the DCF web service
 * @author avonva
 *
 */
public class Ping extends SOAPAction {

	// The correct value that the ping should return to be correct
	private static final String PING_CORRECT_VALUE = "TRXOK";
	
	private static final String PING_NODE_NAME = "PingResponse";

	// namespace used in the ping xml message
	private static final String PING_NAMESPACE = "http://dcf-elect.efsa.europa.eu/";
	
	// web service link of the ping service
	private static final String URL = "https://dcf-elect.efsa.europa.eu/elect2";
	private static final String TEST_URL = "https://dcf-01.efsa.test/dcf-dp-ws/elect2/?wsdl";
	
	/**
	 * Initialize the ping message
	 */
	public Ping( DcfType type ) {
		super ( type, PING_NAMESPACE );
	}
	
	/**
	 * Make a ping
	 * @return
	 */
	public boolean ping () {
		boolean check;
		
		try {
			String url = getType() == DcfType.PRODUCTION ? URL : TEST_URL;
			check = (boolean) makeRequest( url );
		} catch (SOAPException e) {
			check = false;
		}

		return check;
	}
	
	/**
	 * Create a ping request message
	 * 
	 * @param serverURI
	 * @throws SOAPException
	 */
	public SOAPMessage createRequest( SOAPConnection soapConnection ) throws SOAPException {

		/*
		 * This it the xml message which will be sent
		<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:dcf="http://dcf-elect.efsa.europa.eu/">
		   <soapenv:Header/>
		   <soapenv:Body>
		      <dcf:Ping/>
		   </soapenv:Body>
		</soapenv:Envelope>
		 */
		
		// create the standard structure and get the message
		SOAPMessage soapMsg = createTemplateSOAPMessage ( "dcf" );

		// get the body of the message
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();

		// create the xml message structure to make a ping with SOAP
		soapBody.addChildElement( "Ping", "dcf" );

		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}
	
	/**
	 * Get the ping response message and check if the results is ok (i.e. if the content is TRXOK)
	 * @param soapResponse, the ping response
	 * @param correctPingValue, the correct value that the ping should return if the server is up
	 * @return
	 * @throws SOAPException
	 */
	public Object processResponse ( SOAPMessage soapResponse ) throws SOAPException {
		
		String response = "";
		
		// get the children of the body
		Iterator<?> children = soapResponse.getSOAPPart().getEnvelope().getBody().getChildElements();

		// find the ping response node
		while ( children.hasNext() ) {
			
			// get the current child of the body
			Node currentChild = (Node) children.next();
			
			// if the node is the ping response, get the PingResponse node
			if ( currentChild.getLocalName().equals( PING_NODE_NAME ) ) {

				// get then the 'return' node from the pingResponse node
				Node child = currentChild.getFirstChild();

				// and get the trxState node from the 'return' node
				child = child.getFirstChild();
				
				// get the trxState content from the node
				response = child.getFirstChild().getNodeValue();
				
				break;
			}
		}
		
		// is it correct or not ?
		boolean check = response.equals( PING_CORRECT_VALUE );
		
		// If the ping was not successful
		if ( !check )
			System.err.println( "NO CONNECTION - DCF does not respond" );
		
		return check;
	}
}
