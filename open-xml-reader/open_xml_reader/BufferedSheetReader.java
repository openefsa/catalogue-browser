package open_xml_reader;

import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

/**
 * Parser which can be used to parse a huge file in
 * a buffered way, without maintaining all the information
 * in ram memory.
 * @author avonva
 *
 */
public class BufferedSheetReader {

	// shared string table related to the sheet
	// we are reading
	private SharedStringsTable sharedStrings;
	
	// the size of a single batch operation,
	// i.e., the number of rows processed
	// before waiting next()
	private int batchSize;        // size of each batch
	private int processedBatches; // number of processed batches
	private int processedRows;    // number of processed rows
	
	// variables used to keep track of what is
	// happening
	private int	currentRow;       // the row number in the worksheet
	private String cellType;      // given a row and a column, this is the celltype of the cell
	private String currentCol;    // current excel column name (as A1, G5, AA4...)
	private ResultDataSet resultSet;  // object which contains the read data
	
	// xml parser
	XMLEventReader eventReader;
	
	public BufferedSheetReader( InputStream input, SharedStringsTable sharedStrings ) 
			throws XMLStreamException {

		this.sharedStrings = sharedStrings;
		this.resultSet = new ResultDataSet();
		this.batchSize = -1;
		this.currentRow = -1;
		this.processedBatches = 0;
		
		// create the reader if not created yet
		if ( eventReader == null ) {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			
			// say to the parser to avoid to separate strings
			// into several pieces
			factory.setProperty( XMLInputFactory.IS_COALESCING, true );
			eventReader = factory.createXMLEventReader( input );
		}
	}
	
	/**
	 * Set the number of rows which will be processed
	 * each time the {@link #next()} method is called.
	 * @param batchSize
	 */
	public void setBatchSize (  int batchSize ) {
		this.batchSize = batchSize;
	}

	public int getBatchSize() {
		return batchSize;
	}
	
	/**
	 * Clear the data of the parser
	 */
	public void clear() {
		
		if ( resultSet != null )
			resultSet.clear();
		
		sharedStrings = null;
		resultSet = null;
		
		// run garbage collector
		System.gc();
	}
	
	/**
	 * Close the reader
	 * @throws XMLStreamException 
	 */
	public void close () throws XMLStreamException {
		
		if ( eventReader != null )
			eventReader.close();

		eventReader = null;
		
		clear();
	}
	
	/**
	 * Get the number of processed batches
	 * up to now
	 * @return
	 */
	public int getProcessedBatches() {
		return processedBatches;
	}
	
	/**
	 * Some nodes are still not processed?
	 * @return
	 */
	public boolean hasNext() {
		return eventReader.hasNext();
	}
	
	/**
	 * Check if we can parse or we have to wait
	 * @return
	 */
	private boolean canParse() {
		
		// we can process a node in two cases:
		// if we have not set a batch size we go on until end
		// if we have set a batch size we go on until the processed
		// rows counter is less than the specified batchSize
		boolean goOn = batchSize == -1 || processedRows < batchSize;
		
		return goOn;
	}
	
	/**
	 * Get the next result set. If {@link #setBatchSize(int)}
	 * was called, only the next {@link #batchSize} rows will be processed
	 * and returned into the {@link ResultDataSet}, otherwise
	 * all the rows will be processed.
	 * @return
	 * @throws XMLStreamException
	 */
	public ResultDataSet next() throws XMLStreamException {

		// next => start from 0 processed rows
		processedRows = 0;
		
		// clear the result set content but maintain
		// the headers
		if ( !resultSet.isEmpty() ) {
			resultSet.clear();
		}

		// for each node of the xml
		while ( eventReader.hasNext() && canParse() ) {

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
		
		// count the processed batches
		processedBatches++;
		
		// return the result set parsed in this step
		return resultSet;
	}
	
	/**
	 * Parse the a node when it starts
	 * @param event
	 */
	private void start( XMLEvent event ) {
		
		StartElement startElement = event.asStartElement();

		String qName = startElement.getName().getLocalPart();
		
		// if excel row
		if ( qName.equals( "row" ) ) {
			Attribute rowNumAttr = startElement.getAttributeByName( new QName( "r" ) );
			currentRow = Integer.valueOf( rowNumAttr.getValue() );
		} 
		
		// if excel cell
		if ( qName.equals( "c" ) ) {

			Attribute cellTypeAttr = startElement.getAttributeByName( new QName( "t" ) );
			Attribute colNumAttr = startElement.getAttributeByName( new QName( "r" ) );

			// get the cell type
			if ( cellTypeAttr != null )
				cellType = cellTypeAttr.getValue();
			else
				cellType = null;

			// get the current column (as A1, B4...)
			if ( colNumAttr != null )
				currentCol = colNumAttr.getValue();
			else
				currentCol = null;
		}
	}
	
	/**
	 * Parse the characters of the xml
	 * @param event
	 */
	private void parseCharacters ( XMLEvent event ) {
		
		// get the xml node value
		String contents = event.asCharacters().getData();

		if ( contents == null || currentRow == -1 || currentCol == null )
			return;
		
		// get the cell letter from the currentCol and row
		String colLetter = getColLetter( currentCol, currentRow );
		
		// if we have a cell type (we have a string)
		if ( cellType != null ) {
			
			contents = processString( cellType, contents );

			if ( contents != null )
				addToResultSet ( colLetter, contents );
		}
		else {
			if ( isIntegerContent ( contents ) )
				addToResultSet( colLetter, contents );
		}
	}
	
	/**
	 * Parse a node when it ends
	 * @param event
	 */
	private void end ( XMLEvent event ) {
		
		if ( currentCol == null || currentRow == -1 )
			return;
		
		// get the xml node
		EndElement endElement = event.asEndElement();
		
		// get the xml node name
		String qName = endElement.getName().getLocalPart();

		// if end of a row
		if ( qName.equals( "row" ) ) {
			
			// if not header create the row
			if ( currentRow > 1 )
				resultSet.setRow();

			processedRows++;
		}
	}
	
	/**
	 * Get the string value from a cell. If shared string
	 * we need to get the string from the shared table,
	 * if inline string we can take it as it is.
	 * @param cellType
	 * @param contents
	 * @return
	 */
	private String processString ( String cellType, String contents ) {
		
		String value = null;
		
		switch ( cellType ) {
		
		// if excel string (i.e., the excel cell contains a pointer to the 
		// shared strings table which contains the real string) we need to
		// get the string from the shared table and then convert it to string
		case "s":
			
			// try to convert, if error => we have a string and we use it directly
			try {
				int idx = Integer.parseInt( contents );
				value = new XSSFRichTextString( sharedStrings.getEntryAt( idx ) ).toString();
			} catch ( NumberFormatException e ) {}
			
			// note that here there is not a break since also in the "s" case we
			// want to perform the actions contained in the "inlineStr" case
			break;
			
		// if inlineStr (i.e., the excel cell contains the plain string), we simply
		// go on.
		case "inlineStr":
			value = contents;
			break;
		}
		
		return value;
	}
	
	/**
	 * Add the value to the current row in the chosen column
	 * @param colLetter the column where we want to add the value
	 * @param value the value we want to add
	 */
	private void addToResultSet( String colLetter, String value ) {

		// header
		if ( currentRow == 1 ) {
			resultSet.setHeader( value.trim().toUpperCase(), colLetter );
		}
		else  // data 
			resultSet.setElem( colLetter.trim(), value );
	}
	
	/**
	 * Check if the content is an integer or not
	 * @param value
	 * @return
	 */
	private boolean isIntegerContent ( String value ) {
		
		// if we have an integer add it
		boolean isInt;
		try {
			Integer.parseInt( value );
			isInt = true;
		} catch ( NumberFormatException e ) {
			isInt = false;
		}
		
		return isInt;
	}
	
	/**
	 * Get the column letter starting from the 
	 * column letter plus the row number.
	 * @param string
	 * @param i
	 * @return
	 */
	private String getColLetter ( String column , int i ) {
		return column.replace( String.valueOf( i ), "" ).trim().toUpperCase();
	}
}
