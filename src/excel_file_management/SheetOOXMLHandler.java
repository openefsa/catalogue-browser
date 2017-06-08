package excel_file_management;
import java.util.ArrayList;

import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SheetOOXMLHandler extends DefaultHandler {

	private SharedStringsTable				_sst;
	private String							lastContents;
	private ArrayList< ArrayList< String >>	_result		= null;
//	private ArrayList< String >				_currRowCfg	= null;
	private String							_currColumn	= null;
	private Integer							_currRowPos	= 0;
	private ResultDataSet					_resDataSet;
	private String cellType;

	public SheetOOXMLHandler( SharedStringsTable sst ) {
		_sst = sst;
		_result = new ArrayList< ArrayList< String >>();
		_resDataSet = new ResultDataSet();
	}

	@Override
	public void startElement ( String uri , String localName , String qName , Attributes attributes )
			throws SAXException {

		// if row
		if ( qName.equals( "row" ) ) {
			_currRowPos = Integer.valueOf( attributes.getValue( "r" ) );
		} // if cell
		if ( qName.equals( "c" ) ) {
			
			// get the cell type
			cellType = attributes.getValue( "t" );
			
			// get the current column (as A1, B4...)
			_currColumn = attributes.getValue( "r" );
		}
		// Clear contents cache
		lastContents = "";
	}

	@Override
	public void endElement ( String uri , String localName , String qName ) throws SAXException {

		String value = "";
		
		// if we have a cell type
		if ( cellType != null ) {
			
			switch ( cellType ) {
			// if excel string (i.e., the excel cell contains a pointer to the 
			// shared strings table which contains the real string) we need to
			// get the string from the shared table and then convert it to string
			case "s":
				
				// try to convert, if error => we have a string and we use it directly
				try {
					int idx = Integer.parseInt( lastContents );
					value = new XSSFRichTextString( _sst.getEntryAt( idx ) ).toString();
				} catch ( NumberFormatException e ) {}
				
				// note that here there is not a break since also in the "s" case we
				// want to perform the actions contained in the "inlineStr" case
				break;
			// if inlineStr (i.e., the excel cell contains the plain string), we simply
			// go on.
			case "inlineStr":
				
				value = lastContents;
				
				break;
			}
			
			String cellLetter = getStringPart( _currColumn, _currRowPos ).trim().toUpperCase();

			// header
			if ( _currRowPos == 1 ) {
				_resDataSet.setElem( value.trim().toUpperCase(), cellLetter );
			}  // data
			else {
				_resDataSet.setElem( cellLetter.trim(), value );
			}
		}
		
		// if no cell type
		else {

			if ( _currColumn != null && _currRowPos != null && lastContents != null ) {

				boolean isInt;
				try {
					Integer.parseInt( lastContents );
					isInt = true;
				} catch ( NumberFormatException e ) {
					isInt = false;
				}
				
				if ( isInt ) {

					String cellLetter = getStringPart( _currColumn, _currRowPos ).trim().toUpperCase();

					// if header add the header name as key and the cell letter as value
					if ( _currRowPos == 1 ) {
						_resDataSet.setElem( lastContents.trim().toUpperCase(), cellLetter );
					} else {
						// if data add the cell letter as key and the contents as value
						_resDataSet.setElem( cellLetter.trim(), lastContents );
					}
				}
			}
		}

		// if end of a row
		if ( qName.equals( "row" ) ) {
			_resDataSet.setRow();
		}
		
		// if end of sheet
		if ( qName.equals( "sheetData" ) ) {
			_resDataSet.initScan();
		}
	}

	@Override
	public void characters ( char[] ch , int start , int length ) throws SAXException {
		lastContents += new String( ch , start , length );
	}

	public ArrayList< ArrayList< String >> getResults ( ) {
		return _result;
	}

	public ResultDataSet getResultDataSet ( ) {
		return _resDataSet;
	}

	private String getStringPart ( String string , Integer i ) {
		return string.replace( String.valueOf( i ), "" );
	}
}
