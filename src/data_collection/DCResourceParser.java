package data_collection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Parser used to extract all the {@link DCTable} from
 * a data collection configuration .xml.
 * @author avonva
 *
 */
public class DCResourceParser {

	private static final String TABLE_NODE = "dataCollectionTable";
	private static final String TABLE_NAME_NODE = "tableName";
	private static final String CONFIG_NODE = "catalogueConfiguration";
	private static final String DATA_NODE = "dataElementName";
	private static final String CAT_CODE_NODE = "catalogueCode";
	private static final String HIER_CODE_NODE = "hierarchyCode";
	
	private Collection<DCTable> tables;  	// output list
	
	private String currentNode;  // current xml node which is parsed
	private InputStream stream;  // stream parsed
	private XMLEventReader eventReader;
	private DCTableBuilder tableBuilder;
	private CatalogueConfigurationBuilder configBuilder;
	
	/**
	 * Initialize the parser for data collection resources files
	 * @param file file we want to parse
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	public DCResourceParser( File file ) throws FileNotFoundException, XMLStreamException {
		
		tables = new ArrayList<>();
		
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty( XMLInputFactory.IS_COALESCING, true );
		
		stream = new FileInputStream( file );
		eventReader = factory.createXMLEventReader( stream );
	}
	
	/**
	 * Parse the xml resource file
	 * @return list of DCTable created
	 * @throws XMLStreamException
	 */
	public Collection<DCTable> parse() throws XMLStreamException {
		
		while ( eventReader.hasNext() ) {
			// read the node
			XMLEvent event = eventReader.nextEvent();

			// actions based on the node type
			switch( event.getEventType() ) {

			// if starting xml node
			case XMLStreamConstants.START_ELEMENT:
				start ( event );
				break;

			// if looking the xml contents
			case XMLStreamConstants.CHARACTERS:
				parseCharacters ( event );
				break;

			// if ending xml node
			case  XMLStreamConstants.END_ELEMENT:
				end( event );
				break;
			}
		}
		
		return tables;
	}

	/**
	 * A start node was found
	 * @param event
	 */
	private void start(XMLEvent event) {

		StartElement startElement = event.asStartElement();
		
		currentNode = startElement.getName().getLocalPart();

		switch ( currentNode ) {
		case TABLE_NODE:
			tableBuilder = new DCTableBuilder();
			break;
		case CONFIG_NODE:
			configBuilder = new CatalogueConfigurationBuilder();
			break;
		default:
			break;
		}
	}
	
	/**
	 * Node value was found
	 * @param event
	 */
	private void parseCharacters(XMLEvent event) {
		
		if ( currentNode == null )
			return;
		
		// get the xml node value
		String contents = event.asCharacters().getData();

		if ( contents == null )
			return;

		switch ( currentNode ) {
		case TABLE_NAME_NODE:
			tableBuilder.setName( contents );
			break;
		case DATA_NODE:
			configBuilder.setDataElementName( contents );
			break;
		case CAT_CODE_NODE:
			configBuilder.setCatalogueCode( contents );
			break;
		case HIER_CODE_NODE:
			configBuilder.setHierarchyCode( contents );
			break;
		default:
			break;
		}
	}

	/**
	 * End of node found
	 * @param event
	 */
	private void end(XMLEvent event) {
		
		// get the xml node
		EndElement endElement = event.asEndElement();
		
		// get the xml node name
		String qName = endElement.getName().getLocalPart();

		switch ( qName ) {
		case TABLE_NODE:

			// add table to output
			tables.add( tableBuilder.build() );
			tableBuilder = null;
			break;
			
		case CONFIG_NODE:
			// add the configuration to the table
			tableBuilder.addConfig( configBuilder.build() );
			configBuilder = null;
			break;
			
		default:
			break;
		}
		
		currentNode = null;
	}
	
	/**
	 * Close the parser
	 * @throws IOException
	 * @throws XMLStreamException 
	 */
	public void close() throws IOException, XMLStreamException {
		stream.close();
		eventReader.close();
	}
}
