package dcf_webservice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import data_collection.CatalogueConfiguration;
import data_collection.CatalogueConfigurationBuilder;
import data_collection.DCTable;
import data_collection.DCTableBuilder;
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

		String filename = GlobalUtil.getTempDir() + "_DCcatalogueConfigs.xml";

		// write the attachment
		File file;
		try {

			file = writeAttachment( soapResponse, filename, false );
			
			if ( file == null )
				return null;

			Document doc = loadXml( file );

			// delete the file (not required anymore)
			GlobalUtil.deleteFileCascade( file );

			return getTables( doc );
			
		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Get all the catalogue configurations
	 * @param doc
	 * @return
	 */
	private Collection <DCTable> getTables ( Document doc ) {

		// output array
		Collection <DCTable> tables = new ArrayList<>();

		// get all tables
		NodeList nodes = doc.getElementsByTagName( "dataCollectionTable" );

		for ( int i = 0; i < nodes.getLength(); ++i ) {
			tables.add( parseTable( nodes.item(i) ) );
		}

		return tables;
	}

	/**
	 * Get a dc table from the current node
	 * @param node
	 * @return
	 */
	private DCTable parseTable ( Node node ) {
		
		DCTableBuilder builder = new DCTableBuilder();
		
		// get table data
		NodeList nodes = node.getChildNodes();
		
		for ( int i = 0; i < nodes.getLength(); ++i ) {
			
			Node property = nodes.item(i);
			
			String nodeName = property.getNodeName();
			
			// Note: to get the value it is necessary to get 
			// the child first, and then we can get the node value
			// from the child (I don't know why but this is it)
			Node propertyValueNode = property.getFirstChild();

			// skip if no name or value is found (happen if there are catalogue errors)
			if ( nodeName == null || propertyValueNode == null )
				continue;

			// Get the value of the property node
			String value = propertyValueNode.getNodeValue();
			
			switch ( nodeName ) {
			case "tableName":
				builder.setName( value );
				break;
			case "catalogueConfiguration":
				builder.addConfig( parseConfig(property) );
				break;
			}
		}

		return builder.build();
	}
	
	/**
	 * Get a single catalogue configuration from
	 * a catalogue configuration node
	 * @param node
	 * @return
	 */
	private CatalogueConfiguration parseConfig ( Node node ) {
		CatalogueConfigurationBuilder builder = 
				new CatalogueConfigurationBuilder();
		
		NodeList nodes = node.getChildNodes();
		
		// for each node
		for ( int i = 0; i < nodes.getLength(); ++i ) {

			Node property = nodes.item( i );

			// get the property name (e.g. "code", "name", ... )
			String propertyName = property.getNodeName();

			// Note: to get the value it is necessary to get 
			// the child first, and then we can get the node value
			// from the child (I don't know why but this is it)
			Node propertyValueNode = property.getFirstChild();

			// skip if no name or value is found (happen if there are catalogue errors)
			if ( propertyName == null || propertyValueNode == null )
				continue;

			// Get the value of the property node
			String value = propertyValueNode.getNodeValue();

			switch ( propertyName ) {

			case "dataElementName":
				builder.setDataElementName( value );
				break;
			case "catalogueCode":
				builder.setCatalogueCode( value );
				break;
			case "hierarchyCode":
				builder.setHierarchyCode( value );
				break;
			}
		}
		
		return builder.build();
	}
}
