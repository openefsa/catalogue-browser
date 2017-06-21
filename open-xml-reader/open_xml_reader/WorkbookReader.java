package open_xml_reader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import dcf_manager.Dcf.DcfType;
import import_catalogue.CatalogueWorkbookImporter;

/**
 * This class is used to read the sheets data of
 * a workbook (open xml format) and then to return its parsed data
 * into a {@link ResultDataSet}.
 * @author avonva
 *
 */
public class WorkbookReader {
	
	private BufferedSheetReader sheetParser;
	private InputStream sheetReader;
	private XSSFReader reader = null;
	private WorkbookHandler workbookHandler = null;
	private OPCPackage pkg = null;

	/**
	 * Initialize a workbook reader
	 * @param filename the name of the workbook file
	 * @throws IOException
	 * @throws OpenXML4JException
	 * @throws SAXException
	 */
	public WorkbookReader( String filename ) 
			throws IOException, OpenXML4JException, SAXException {

		// open the workbook in open xml format
		pkg = OPCPackage.open( filename, PackageAccess.READ );

		// read it
		reader = new XSSFReader( pkg );

		// get its data and parse them to retrieve
		// the sheet information (relationshipId)
		InputStream wbStream = reader.getWorkbookData();
		workbookHandler = new WorkbookHandler();

		XMLReader parser = XMLReaderFactory.createXMLReader( 
				"org.apache.xerces.parsers.SAXParser" );
		parser.setContentHandler( workbookHandler );
		parser.parse( new InputSource( wbStream ) );
		
		wbStream.close();
	}


	/**
	 * Process a single sheet of the workbook. Note that this
	 * method will override {@link #sheetParser} in order to
	 * parse the new sheet. Be sure that if another {@link #sheetParser}
	 * was created before that it has finished its work.
	 * @param name
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 * @throws XMLStreamException 
	 */
	public void processSheetName ( String name ) throws IOException, 
	InvalidFormatException, XMLStreamException {

		// close the previous sheet reader if there was one
		if ( sheetReader != null )
			sheetReader.close();
		
		if ( sheetParser != null )
			sheetParser.clear();

		// get the sheet relationship id using the sheet name
		String sheetRId = workbookHandler.getSheetRelationshipId( name );
		
		// if not sheet id is retrieved => exception
		if ( sheetRId.equals( "" ) || sheetRId == null ) {
			System.err.println( "No sheet named " + name + " was found!" );
			return;
		}
		
		// get the sheet from the reader
		sheetReader = reader.getSheet( sheetRId );

		// create a parser with pull pattern
		sheetParser = new BufferedSheetReader ( sheetReader, 
				reader.getSharedStringsTable() );
	}
	
	/**
	 * The parser has other nodes to parse?
	 * @return
	 */
	public boolean hasNext() {

		if ( sheetParser == null )
			return false;

		return sheetParser.hasNext();
	}
	
	/**
	 * Set the batch size of the current
	 * {@link #sheetParser}. Using {@link #next()}
	 * will create a {@link ResultDataSet} with
	 * only {@code batchSize} rows processed
	 * at a time (you need to call {@link #next()}
	 * until the data are finished!)
	 * @param batchSize
	 */
	public void setBatchSize ( int batchSize ) {
		
		if ( sheetParser == null )
			return;
		
		sheetParser.setBatchSize(batchSize);
	}
	public BufferedSheetReader getSheetParser() {
		return sheetParser;
	}
	/**
	 * Get the next batch result set from the parser
	 * @return
	 * @throws XMLStreamException
	 */
	public ResultDataSet next() throws XMLStreamException {
		
		if ( sheetParser == null )
			return null;
		
		return sheetParser.next();
	}
	
	/**
	 * Close the dataset
	 */
	public void close() {
		try {
			
			if ( sheetParser != null )
				sheetParser.close();
			
			sheetReader.close();
			pkg.close();
		} catch (IOException | XMLStreamException e) {
			e.printStackTrace();
		}
	}
	/*public static void main ( String[] args ) throws IOException, XMLStreamException, 
	OpenXML4JException, SAXException, SQLException {
		
		DatabaseManager.startMainDB();
		
		CatalogueDAO catDao = new CatalogueDAO();
		ArrayList<Catalogue> cats = catDao.getLocalCatalogues( DcfType.LOCAL );
		Catalogue catalogue = null;
		for ( Catalogue cat : cats ) {
			if ( cat.getCode().equals( "MTX" ) )
				catalogue = cat;
		}
		
		CatalogueWorkbookImporter importer = new CatalogueWorkbookImporter();
		importer.setOpenedCatalogue( catalogue );
		importer.importWorkbook( 
				"C:\\Users\\avonva\\Desktop\\CatalogueBrowser\\CatalogueBrowser\\Database\\LocalCatalogues\\CAT_MTX_DB\\MTX", 
				"C:\\Users\\avonva\\Desktop\\MTX_8.7.xlsx");
	}*/
}
