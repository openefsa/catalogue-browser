package dcf_webservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import dcf_manager.Dcf.DcfType;

public abstract class GetList<T> extends SOAPAction {
	
	private String prodUrl;
	private String testUrl;

	/**
	 * 
	 * @param type
	 * @param prodUrl
	 * @param testUrl
	 * @param namespace
	 */
	public GetList( DcfType type, String prodUrl, String testUrl, String namespace ) {
		super ( type, namespace );
		this.prodUrl = prodUrl;
		this.testUrl = testUrl;
	}
	
	/**
	 * Get the list of T elements
	 * @return
	 * @throws DOMException
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ArrayList <T> getList () throws DOMException, Exception {	
		String url = getType() == DcfType.PRODUCTION ? prodUrl : testUrl;
		return (ArrayList<T>) makeRequest ( url );
	}
	
	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {

		// get the children of the body
		Iterator<?> children = soapResponse.getSOAPPart().
				getEnvelope().getBody().getChildElements();

		// if the body has not any child => return! Anything has to be parsed
		if ( !children.hasNext() )
			return null;

		// get 'getCatalogueListResponse' node
		Node node = (Node) children.next();

		// go deeper and get 'return' node
		node = node.getFirstChild();

		// get the CDATA field of the 'return' node (data related to the XML) and parse it
		Document cdata;
		try {
			cdata = loadXML ( node.getFirstChild().getNodeValue() );
		} catch (DOMException | ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return getList( cdata );
	}
	
	/**
	 * Parse the cdata content of the received response.
	 * @return an object which contains what we want to 
	 * return in the {@link #processResponse(SOAPMessage)}
	 * method.
	 * @param cdata
	 * @return the list
	 */
	public abstract Collection<T> getList ( Document cdata );
}
