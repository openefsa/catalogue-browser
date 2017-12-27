package dcf_webservice;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import data_collection.DataCollection;
import data_collection.DataCollectionBuilder;
import dcf_manager.Dcf.DcfType;

public class GetDataCollectionsList extends GetList<DataCollection> {

	// namespace used in the ping xml message
	private static final String NAMESPACE = "http://dcf-elect.efsa.europa.eu/";

	// web service link of the ping service
	private static final String URL = "https://dcf-elect.efsa.europa.eu/elect2";
	private static final String TEST_URL = "https://dcf-01.efsa.test/dcf-dp-ws/elect2/?wsdl";

	public GetDataCollectionsList(DcfType type) {
		super( type, URL, TEST_URL, NAMESPACE );
	}

	@Override
	public Collection <DataCollection> getList(Document cdata) {

		Collection <DataCollection> dcs = new ArrayList<>();

		NodeList dcNodes = cdata.getElementsByTagName( "dataCollectionMainInfo" );

		// for each node
		for ( int i = 0; i < dcNodes.getLength(); i++ ) {

			// get the current node
			Node node = dcNodes.item( i );

			dcs.add( getSingleElement( node ) );
		}

		return dcs;
	}

	/**
	 * Get a single datacollection element from the node values
	 * @param node
	 * @return
	 */
	private DataCollection getSingleElement ( Node node ) {

		// get elements
		NodeList nodes = node.getChildNodes();

		DataCollectionBuilder builder = new DataCollectionBuilder();
		
		// for each element
		for ( int i = 0; i < nodes.getLength(); ++i ) {

			// get the current node
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
			String propertyValue = propertyValueNode.getNodeValue();

			// Add the property value to the catalogue builder
			// according to the property name
			switch ( propertyName ) {

			case "dcCode":
				builder.setCode( propertyValue );
				break;
			case "dcDescription":
				builder.setDescription( propertyValue );
				break;
			case "dcCategory":
				builder.setCategory( propertyValue );
				break;

			case "activeFrom":

				if ( propertyValue != null ) {
					builder.setActiveFrom( DataCollection.
							getTimestampFromString( propertyValue ) );
				}

				break;
				
			case "activeTo":

				if ( propertyValue != null ) {
					builder.setActiveTo( DataCollection.
							getTimestampFromString( propertyValue ) );
				}

				break;
			case "resourceId":
				builder.setResourceId( propertyValue );
				break;
			}  // end switch
		}  // end for
		
		return builder.build();
	}
	
	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {

		// create the standard structure and get the message
		SOAPMessage soapMsg = createTemplateSOAPMessage ( "dcf" );

		// get the body of the message
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();

		// create the xml message structure to get the dataset list
		soapBody.addChildElement( "GetDataCollectionList", "dcf" );

		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}
}
