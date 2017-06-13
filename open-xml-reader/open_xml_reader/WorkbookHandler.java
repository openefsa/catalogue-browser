package open_xml_reader;
import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class is used to read and store the relationship id
 * of the sheets of the workbook from the open xml format file.
 * @author avonva
 *
 */
public class WorkbookHandler extends DefaultHandler {

	private HashMap<String, String> sheetNames;
	
	@Override
	public void startElement ( String uri , String localName , String name , Attributes attributes )
			throws SAXException {
		
		// initialize hashmap if needed
		if ( sheetNames == null )
			sheetNames = new HashMap<>();
		
		// if we found a sheet
		if ( name.equals( "sheet" ) ) {
			
			// get the sheet name
			String sheetName = attributes.getValue( "name" );
			
			// get the sheet relationship id
			String rId = attributes.getValue( "r:id" );
			
			sheetNames.put( sheetName, rId );
		}
	}
	
	/**
	 * Get the sheet 
	 * @param name
	 * @return
	 */
	public String getSheetRelationshipId ( String name ) {
		return sheetNames.get( name );
	}
}