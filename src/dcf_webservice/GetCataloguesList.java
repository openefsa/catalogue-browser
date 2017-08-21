package dcf_webservice;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import catalogue.Catalogue;
import catalogue.CatalogueBuilder;
import dcf_manager.Dcf.DcfType;
import utilities.GlobalUtil;

public class GetCataloguesList extends GetList<Catalogue> {

	// web service link of the getCatalogueList service
	private static final String URL = "https://dcf-cms.efsa.europa.eu/catalogues";
	private static final String TEST_URL = "https://dcf-01.efsa.test/dc-catalog-public-ws/catalogues/?wsdl";
	private static final String LIST_NAMESPACE = "http://ws.catalog.dc.efsa.europa.eu/";
	
	public GetCataloguesList(DcfType type) {
		super( type, URL, TEST_URL, LIST_NAMESPACE );
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
	public Collection<Catalogue> getList(Document cdata) {

		// output array
		ArrayList < Catalogue > catalogues = new ArrayList<>();

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

			// set the catalogue type according to the dcf used
			cb.setCatalogueType( getType() );

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
			case "validFrom":
				
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

			case "validTo":

				if ( propertyValue != null ) {

					// convert the string to timestamp
					try {
						Timestamp validToTs = GlobalUtil.getTimestampFromString( 
								propertyValue, Catalogue.ISO_8601_24H_FULL_FORMAT );
						cb.setValidTo( validToTs );
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
}
