package dcf_webservice;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import catalogue_object.Catalogue;
import catalogue_object.CatalogueBuilder;
import utilities.GlobalUtil;

public class GetCataloguesList extends SOAPAction {


	// web service link of the getCatalogueList service
	//private static final String CATALOGUE_LIST_URL = "https://dcf-cms.efsa.europa.eu/catalogues";
	private static final String CATALOGUE_LIST_URL = "https://dcf-01.efsa.test/dc-catalog-public-ws/catalogues/?wsdl";
	
	// namespace used in getting the catalogue list xml message
	private static final String CATALOGUE_LIST_NAMESPACE = "http://ws.catalog.dc.efsa.europa.eu/";

	/**
	 * Initialize the get catalogue list request
	 */
	public GetCataloguesList() {
		super ( CATALOGUE_LIST_URL, CATALOGUE_LIST_NAMESPACE );
	}
	
	/**
	 * Get all the available catalogues meta data from the DCF
	 * @param soapConnection
	 * @param username
	 * @param password
	 * @return
	 * @throws DOMException
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ArrayList < Catalogue > getCataloguesList () throws DOMException, Exception {
		return (ArrayList<Catalogue>) makeRequest ();
	}

	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {

		/*
		 * This it the xml message which will be created
		 * 
		<soapenv:Envelope xmlns:soapenv=http://schemas.xmlsoap.org/soap/envelope/ xmlns:dcf="http://dcf-elect.efsa.europa.eu/">
			   <soapenv:Header/>
			   <soapenv:Body>
			      <dcf:getDatasetList>
			         <dataCollectionCode>MOPER_WF2</dataCollectionCode>
			         <!--Optional:-->
			         <status></status>
			      </dcf:getDatasetList>
			   </soapenv:Body>
			</soapenv:Envelope>
		 */

		// create the standard structure and get the message
		SOAPMessage soapMsg = createTemplateSOAPMessage ( "ws" );

		// get the body of the message
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();

		// create the xml message structure to get the dataset list
		SOAPElement soapElem = soapBody.addChildElement( "getCatalogueList", "ws" );

		// set that we want an XML file as output
		SOAPElement arg = soapElem.addChildElement( "arg2" );
		arg.setTextContent( "XML" );  // set the proper request value

		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}

	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {

		// output array
		ArrayList < Catalogue > catalogues = new ArrayList<>();

		// get the children of the body
		Iterator<?> children = soapResponse.getSOAPPart().getEnvelope().getBody().getChildElements();

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
			cdata = loadXMLFromString ( node.getFirstChild().getNodeValue() );
		} catch (DOMException | ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			return null;
		}

		// get all the catalogues nodes from the CDATA field (which is text, but XML formatted)
		NodeList cataloguesNodes = cdata.getElementsByTagName( "catalogue" );


		// for each catalogue node get its properties and store it in Catalogue objects
		for ( int i = 0; i < cataloguesNodes.getLength(); i++ ) {

			// create a catalogue builder to build the catalogue step by step
			CatalogueBuilder cb = new CatalogueBuilder();

			// get the current catalogue node
			Node catalogueNode = cataloguesNodes.item( i );

			// add the catalogueDesc properties (i.e. code, name, label, scopenote, termCodeMask, 
			// termCodeLength, acceptNonStandardCodes, generateMissingCodes)
			cb = addProperties ( cb, catalogueNode.getFirstChild() );

			// add the catalogueVersion properties (i.e. version, validTo, status)
			// Note that since we have only 2 nodes, we can get the last child as the second one
			cb = addProperties ( cb, catalogueNode.getLastChild() );

			// create the catalogue with the builder
			Catalogue currentCatalogue = cb.build();

			// add the catalogue into the output array
			catalogues.add( currentCatalogue );

		}

		// return all the retrieved catalogues
		return catalogues;
	}

	/**
	 * Given a node searches in its children for the catalogue properties. If a catalogue property node
	 * is found, then it is added to the catalogue builder, which is then returned as output.
	 * 
	 * You can use this method multiple times on different nodes, passing as input for the i-th method the catalogue builder
	 * which is returned by the i-th - 1 method, in order to add all the properties to a unique catalogue builder.
	 * 
	 * You can then use the build() function of the catalogue builder to create a Catalogue object with all the properties
	 * which were added.
	 * 
	 * @param cb, catalogue builder which will get the properties values
	 * @param node, the parent node which has catalogue property children (i.e. catalogueDesc, catalogueVersion )
	 * @return the modified catalogue builder
	 * @throws ParseException, if a boolean or a double value cannot be parsed from the string
	 */
	private static CatalogueBuilder addProperties ( CatalogueBuilder cb, 
			Node node ) {
		
		// get the children of the parent node
		NodeList catalogueProperties = node.getChildNodes();
		
		// For each child (i.e. each property which is son of the parent node)
		for ( int j = 0; j < catalogueProperties.getLength(); j++ ) {
			
			// get the current property (e.g. code, name, validTo... )
			Node property = catalogueProperties.item( j );
			
			// get the property name (e.g. "code", "name", ... )
			String propertyName = property.getNodeName();
			
			// Note: to get the value it is necessary to get 
			// the child first, and then we can get the node value
			// from the child (I don't know why but this is it)
			Node propertyValueNode = property.getFirstChild();
			
			// skip if no name or value is found (happen if there are catalogue errors)
			if ( propertyName == null || propertyValueNode == null )
				continue;

			// here we have the property name and the node which contains
			// the property value => we have to extract the property value
			// and then add it to the catalogue builder
			
			// Get the value of the property node
			String propertyValue = propertyValueNode.getNodeValue();
			
			// Add the property value to the catalogue builder
			// according to the property name
			switch ( propertyName ) {
			
			case "code":
				cb.setCode( propertyValue );
				break;
			case "name":
				cb.setName( propertyValue );
				break;
			case "label":
				cb.setLabel( propertyValue );
				break;
			case "scopeNote":
				cb.setScopenotes( propertyValue );
				break;
			case "termCodeMask":
				cb.setTermCodeMask( propertyValue );
				break;
			case "termCodeLength":
				cb.setTermCodeLength( propertyValue );
				break;
			case "acceptNonStandardCodes":
				cb.setAcceptNonStandardCodes( Boolean.parseBoolean( propertyValue ) );
				break;
			case "generateMissingCodes":
				cb.setGenerateMissingCodes( Boolean.parseBoolean( propertyValue ) );
				break;
			case "version":
				cb.setVersion( propertyValue );
				break;
			// TODO: c'è un errore nei cataloghi scaricati e il valid to in realtà è il valid from!
			// sistemare appena il bug viene fixato
			case "validTo":
				
				if ( propertyValue != null ) {
					
					// convert the string to timestamp
					try {
						Timestamp validFromTs = GlobalUtil.getTimestampFromString( 
								propertyValue, Catalogue.ISO_8601_24H_FULL_FORMAT );
						cb.setValidFrom( validFromTs );
					}
					catch ( ParseException e ) {
						e.printStackTrace();
					}
				}
				
				break;
			case "status":
				cb.setStatus( propertyValue );
				break;
			}  // end switch
		}  // end for

		return cb;  // return the catalogue builder
	}
	
	/**
	 * Get an xml document starting from a string text formatted as xml
	 * @param xml
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws Exception
	 */
	public static Document loadXMLFromString( String xml ) throws ParserConfigurationException, SAXException, IOException
	{
		// create the factory object to create the document object
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    
	    // get the builder from the factory
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    
	    // Set the input source (the text string)
	    InputSource is = new InputSource( new StringReader( xml ) );
	    
	    // get the xml document and return it
	    return builder.parse(is);
	}
}
