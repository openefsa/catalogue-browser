package open_xml_reader;

import java.io.InputStream;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class BufferedSheetReader {
	
	private XMLReader parser;
	private XSSFReader reader = null;
	private WorkbookHandler workbookHandler = null;
	private OPCPackage pkg = null;
	
	public BufferedSheetReader( String filename ) throws SAXException {
		parser = XMLReaderFactory.createXMLReader( 
				"org.apache.xerces.parsers.SAXParser" );
		
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
}
