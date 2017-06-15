package dcf_log;

import java.util.Iterator;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Node;

/**
 * Class to analyze a dcf response which contains the LOG code
 * for the previouly performed operation (through web service)
 * @author avonva
 *
 */
public class LogCodeFinder {
	
	private SOAPMessage message;
	
	/**
	 * Initialize the finder.
	 * @param message the message to be analyzed. Note that the message
	 * should be a log message! Otherwise errors are thrown.
	 */
	public LogCodeFinder( SOAPMessage message ) {
		
		this.message = message;
	}

	/**
	 * Search the log code in the soap message
	 * @return
	 * @throws SOAPException
	 */
	public String getLogCode() throws SOAPException {

		// get the children of the body
		Iterator<?> children = message.getSOAPPart().getEnvelope().getBody().getChildElements();
		
		if ( !children.hasNext() )
			return null;
		
		// get the UploadCatalogueFileResponse node
		Node node = (Node) children.next();
		
		// get the return node
		node = node.getFirstChild();

		// continue only if we have indeed the return node
		if ( node == null || !node.getLocalName().equals( "return" ) )
			return null;
		
		// return the text inside the return node
		return node.getTextContent();
	}
}
