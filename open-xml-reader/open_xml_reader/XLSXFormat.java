package open_xml_reader;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This class is used to read the sheets data of
 * a workbook (open xml format) and then to return its parsed data
 * into a {@link ResultDataSet}.
 * @author avonva
 *
 */
public class XLSXFormat {
	
	private XMLReader parser = XMLReaderFactory.createXMLReader( 
			"org.apache.xerces.parsers.SAXParser" );
	
	private XSSFReader reader = null;
	private WorkbookHandler workbookHandler = null;
	private OPCPackage pkg = null;

	public XLSXFormat( String filename ) throws IOException, OpenXML4JException, SAXException {

		pkg = OPCPackage.open( filename, PackageAccess.READ );

		reader = new XSSFReader( pkg );

		if ( workbookHandler == null ) {
			InputStream wbStream = reader.getWorkbookData();
			workbookHandler = new WorkbookHandler();
			parser.setContentHandler( workbookHandler );
			parser.parse( new InputSource( wbStream ) );
			wbStream.close();
		}
	}

	/**
	 * This method returns the resultDataSet of worksheet in a file xlsx.
	 * 
	 * @param name
	 *            name of the sheet to retreieving data
	 * @return a ResultDataSet containing the data of the xlsx file
	 * @throws Exception
	 */
	public ResultDataSet processSheetName ( String name ) throws Exception {
		
		if ( reader == null )
			throw new Exception( "XSSFReader is null" );
		if ( workbookHandler == null )
			throw new Exception( "WorkbookHandler is null" );
		
		// get the sheet relationship id using the sheet name
		String sheetRId = workbookHandler.getSheetRelationshipId( name );
		
		// if not sheet id is retrieved => exception
		if ( sheetRId.equals( "" ) || sheetRId == null )
			throw new Exception( "FoodexXLSXFormat.fetchSheetName res null" );
		
		// get the sheet from the reader
		InputStream sheetReader = reader.getSheet( sheetRId );
		
		// parse the sheet and create result data set
		SheetOOXMLHandler sheetRowHandl = new SheetOOXMLHandler( 
				reader.getSharedStringsTable() );
		
		parser.setContentHandler( sheetRowHandl );
		parser.parse( new InputSource( sheetReader ) );
		
		sheetReader.close();
		
		// get the result data set
		return sheetRowHandl.getResultDataSet();
	}
	
	/**
	 * Close the dataset
	 */
	public void close() {
		try {
			pkg.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
