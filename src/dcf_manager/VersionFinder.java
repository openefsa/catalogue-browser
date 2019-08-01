package dcf_manager;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import xml_to_excel.XsltCompiler;

/**
 * Class used to find the version fields of a catalogue
 * inspecting its xml file.
 * @author avonva
 *
 */
public class VersionFinder {

	private String version;
	private String status;
	
	/**
	 * Search in the catalogue xml file the version and the status of
	 * the catalogue.
	 * @param filename
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public VersionFinder( String filename ) throws TransformerException, 
	ParserConfigurationException, SAXException, IOException {
		
		String output = filename + "_last_version.xml";
		
		// filter the catalogue xml contained in the file
		// to get only its version in order to reduce the
		// size of the document
		XsltCompiler xsltConverter = new XsltCompiler( filename, 
				"catalogueVersion.xslt", output );

		// filter it
		xsltConverter.compile();

		// here we have created the file
		// so we read it

		File file = new File( output );

		// read the xml file with java DOM
		DocumentBuilderFactory factory =
				DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = factory.newDocumentBuilder();

		// get the xml
		Document doc = builder.parse( file );

		// get the nodes we are interested in
		// <catalogueVersion> we get all its children
		NodeList children = doc.getChildNodes().item(0).getChildNodes();
		
		// get the fields values
		for ( int i = 0; i < children.getLength(); i++ ) {
			
			switch ( children.item( i ).getNodeName() ) {
			case "version":
				this.version = children.item(i).getTextContent();
				break;
			case "status":
				this.status = children.item(i).getTextContent();
				break;
			}
		}
		
		file.delete();
	}
	
	/**
	 * Get the version of the catalogue in the xml
	 * @return
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * Get the status of the catalogue in the xml
	 * @return
	 */
	public String getStatus() {
		return status;
	}
	
	/**
	 * Get if the catalogue is in draft or not
	 * @return
	 */
	public boolean isStatusDraft() {
		return status.contains( "DRAFT" );
	}
	
	/**
	 * Get if the catalogue is in minor draft/release or not
	 * @return
	 */
	public boolean isStatusMinor() {
		return status.contains( "MINOR" );
	}
	
	/**
	 * Get if the catalogue is in major draft/release or not
	 * @return
	 */
	public boolean isStatusMajor() {
		return status.contains( "MAJOR" );
	}
}
