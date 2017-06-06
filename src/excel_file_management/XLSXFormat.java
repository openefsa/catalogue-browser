package excel_file_management;
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

public class XLSXFormat {
	String			_fn		= null;
	XSSFReader		_reader	= null;
	XMLReader		_parser	= XMLReaderFactory.createXMLReader( "org.apache.xerces.parsers.SAXParser" );
	WorkbookHandler	_wbXml	= null;
	OPCPackage pkg = null;

	public XLSXFormat( String filename ) throws IOException, OpenXML4JException, SAXException {
		_fn = filename;
		pkg = OPCPackage.open( filename, PackageAccess.READ );
		_reader = new XSSFReader( pkg );

		if ( _wbXml == null ) {
			InputStream wbStream = _reader.getWorkbookData();
			_wbXml = new WorkbookHandler();
			_parser.setContentHandler( _wbXml );
			_parser.parse( new InputSource( wbStream ) );
			wbStream.close();
		}

		// System.out.println("CONFIG rid:"+_wbXml.getCONFIG());
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
		if ( _reader == null )
			throw new Exception( "XSSFReader is null" );
		if ( _wbXml == null )
			throw new Exception( "WorkbookHandler is null" );
		InputStream configSheet = _reader.getSheet( fetchSheetName( name ) );
		SheetOOXMLHandler sheetRowHandl = new SheetOOXMLHandler( _reader.getSharedStringsTable() );
		_parser.setContentHandler( sheetRowHandl );
		_parser.parse( new InputSource( configSheet ) );
		configSheet.close();
		return sheetRowHandl.getResultDataSet();
	}

	private String fetchSheetName ( String name ) throws Exception {
		String res = "";
		if ( name.toUpperCase().equals( "ATTRIBUTE" ) ) {
			res = _wbXml.getATTRIBUTE();
		} else if ( name.toUpperCase().equals( "HIERARCHY" ) ) {
			res = _wbXml.getHIERARCHY();
		} else if ( name.toUpperCase().equals( "TERM" ) ) {
			res = _wbXml.getTERM();
		} else if ( name.toUpperCase().equals( "RELEASENOTES" ) ) {
			res = _wbXml.getRELEASENOTES();
		} else if ( name.toUpperCase().equals( "CATALOGUE" ) ) {
			res = _wbXml.getCATALOGUE();
		}
		if ( res.equals( "" ) || res == null )
			throw new Exception( "FoodexXLSXFormat.fetchSheetName res null" );
		return res;
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
