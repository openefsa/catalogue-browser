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
	private boolean							nextIsString;
	private ArrayList< ArrayList< String >>	_result		= null;
//	private ArrayList< String >				_currRowCfg	= null;
	private String							_currColumn	= null;
	private Integer							_currRowPos	= 0;
	private ResultDataSet					_resDataSet;

	public SheetOOXMLHandler( SharedStringsTable sst ) {
		_sst = sst;
		_result = new ArrayList< ArrayList< String >>();
		_resDataSet = new ResultDataSet();
	}

	@Override
	public void startElement ( String uri , String localName , String qName , Attributes attributes )
			throws SAXException {

		if ( qName.equals( "row" ) ) {
//			_currRowCfg = new ArrayList< String >();
			_currRowPos = Integer.valueOf( attributes.getValue( "r" ) );
		}
		if ( qName.equals( "c" ) ) {
			
			String cellType = attributes.getValue( "t" );
			
			_currColumn = attributes.getValue( "r" );
			if ( cellType != null && cellType.equals( "s" ) ) {
				nextIsString = true;
			} else {
				nextIsString = false;
			}
		}
		// Clear contents cache
		lastContents = "";
	}

	@Override
	public void endElement ( String uri , String localName , String qName ) throws SAXException {
		
		
		
		if ( nextIsString ) {
			
			int idx = Integer.parseInt( lastContents );
			
			lastContents = new XSSFRichTextString( _sst.getEntryAt( idx ) ).toString();

			nextIsString = false;
			
			String cellLetter = getStringPart( _currColumn, _currRowPos ).trim().toUpperCase();
			
			if ( _currRowPos == 1 ) {
				_resDataSet.setElem( lastContents.trim().toUpperCase(), cellLetter );
			} else {
				_resDataSet.setElem( cellLetter.trim(), lastContents );
			}
			
			
//			lastContents += "|" + cellLetter;
//			_currRowCfg.add( lastContents );
		}
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

					if ( _currRowPos == 1 ) {
						_resDataSet.setElem( lastContents.trim().toUpperCase(), cellLetter );
					} else {
						_resDataSet.setElem( cellLetter.trim(), lastContents );
					}
				}}
		}

		if ( qName.equals( "row" ) ) {
//			_result.add( _currRowCfg );
			_resDataSet.setRow();
		}
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
