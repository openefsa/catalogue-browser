package open_xml_reader;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.DateUtil;

/**
 * This class provides an interface for data set from Parsing raw excel data.
 * The interface is like resultset of sql data.
 * 
 * @author thomm
 * 
 */
public class ResultDataSet implements ResultSet, Cloneable {

	HashMap< String, String > headers;
	ArrayList< HashMap< String, String >> dataRecords;
	HashMap< String, String > currentDataRow;
	Integer cursorPosition = -1;

	public ResultDataSet() {
		headers = new HashMap<>();
		dataRecords = new ArrayList< HashMap< String, String >>();
		currentDataRow = new HashMap< String, String >();
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {

		ResultDataSet cloned = new ResultDataSet();
		
		// copy headers
		cloned.setHeaders ( headers );
		
		// copy data
		for ( HashMap<String,String> row : dataRecords ) {
			
			HashMap<String,String> newRow = new HashMap<>();
			
			for ( String key : row.keySet() ) {
				newRow.put( key, row.get(key) );
			}
			cloned.addRow ( newRow );
		}
		
		cloned.initScan();

		return cloned;
	}
	
	private void setHeaders ( HashMap< String, String > headers ) {
		this.headers = headers;
	}
	
	private void addRow ( HashMap< String, String > row ) {
		dataRecords.add( row );
	}
	
	/**
	 * Get the currently processed row
	 * @return
	 */
	public HashMap<String, String> getCurrentRow() {
		return currentDataRow;
	}
	
	/**
	 * Get the current position of the cursor (read-only)
	 * @return
	 */
	public Integer getCurrentPosition() {
		return cursorPosition;
	}

	/**
	 * Move the cursor to the -1 position. 
	 * Calling {@link #next()} after this method
	 * will return the first data record.
	 */
	private void initScan ( ) {
		cursorPosition = -1;
	}
	
	/**
	 * Clear all the data (not headers)
	 */
	public void clear() {
		
		// clear all the hashmap data
		for ( HashMap<String,String> map : dataRecords ) {
			map.clear();
		}
		
		// clear the array list
		dataRecords.clear();
		
		currentDataRow.clear();
		
		initScan();
	}
	
	/**
	 * Print the first {@code numRows} of rows
	 * of the dataset
	 * @param numRows
	 */
	@Deprecated
	public void printRows ( int numRows ) {
		
		int counter = 0;
		
		String printHeaders = "";
		for ( String value : headers.values() )
			printHeaders = printHeaders + "h" + value + ";";
		
		System.out.println( printHeaders );
		
		for ( HashMap<String,String> map : dataRecords ) {
			System.out.println( map );
			counter++;

			if ( counter >= numRows )
				break;

		}
	}
	
	/**
	 * Is the result set empty?
	 * @return
	 */
	public boolean isEmpty() {
		return dataRecords.isEmpty();
	}
	
	/**
	 * Get the number of rows contained in the
	 * sheet
	 * @return
	 */
	public int getRowsCount() {
		return dataRecords.size();
	}
	
	/**
	 * Get the progress between 0 and 100
	 * of the rows we are processing
	 * @return
	 */
	public double getProgress() {
		return 100 * (double) cursorPosition / dataRecords.size();
	}


	/**
	 * This method confirm that collection of data has next.
	 * 
	 * @return
	 */
	public boolean hasNext () {
		return cursorPosition < dataRecords.size() - 1;
	}
	
	/**
	 * Move the cursor to the next row. Return true if
	 * it exists also another row after the 
	 * returned one.
	 * @return
	 */
	public boolean next () {
		
		if ( !hasNext() )
			return false;
		
		cursorPosition++;
		currentDataRow = dataRecords.get( cursorPosition );
		
		return true;
	}


	/**
	 * Get the cell value in the header key for the current row
	 * @param key
	 * @return
	 */
	public String getString ( String header ) {
		
		// get the excel column letter from the column header
		String key = getColumnLetter ( header );

		// if the header is not found return default value
		if ( key == null ) {
			return "";
		}
		
		// return the value inside the cell identified by the column letter
		// and by the current row
		String value = currentDataRow.get( key.toUpperCase() );

		// if no value found, return the default
		if ( value == null )
			return "";
		
		return value;
	}
	
	/**
	 * Get a boolean value
	 * @param header
	 * @param defaultVal
	 * @return
	 */
	public boolean getBoolean ( String header, boolean defaultVal ) {
		
		// get the excel column letter from the column header
		String key = getColumnLetter ( header );
		
		if ( key == null )
			return defaultVal;
				
		// return the value inside the cell identified by the column letter
		// and by the current row
		String value = currentDataRow.get( key.toUpperCase() );
		
		// if no value found, return the default
		if ( value == null || value.isEmpty() )
			return defaultVal;
		
		// if integer values for booleans
		if ( value.equals("1") )
			value = "true";
		else if ( value.equals ("0") )
			value = "false";
		
		return Boolean.parseBoolean( value );
	}
	
	
	/**
	 * Get a integer value
	 * @param header
	 * @param defaultVal
	 * @return
	 */
	public Integer getInt ( String header, Integer defaultVal ) {
		
		// get the excel column letter from the column header
		String key = getColumnLetter ( header );
				
		if ( key == null )
			return defaultVal;

		// return the value inside the cell identified by the column letter
		// and by the current row
		String value = currentDataRow.get( key.toUpperCase() );
		
		// if no value found, return the default
		if ( value == null || value.isEmpty() )
			return defaultVal;
		
		return Integer.parseInt( value );
	}
	
	/**
	 * Get a long value
	 * @param header
	 * @param defaultVal
	 * @return
	 */
	public long getLong ( String header, long defaultVal ) {
		
		// get the excel column letter from the column header
		String key = getColumnLetter ( header );
				
		if ( key == null )
			return defaultVal;
		
		// return the value inside the cell identified by the column letter
		// and by the current row
		String value = currentDataRow.get( key.toUpperCase() );
		
		// if no value found, return the default
		if ( value == null || value.isEmpty() )
			return defaultVal;
		
		return Long.parseLong( value );
	}
	
	
	/**
	 * Get a timestamp from the result set (we try to get it from either a long or a string)
	 * @param header
	 * @return
	 */
	
	public Timestamp getTimestamp ( String columnLabel ) {
		return getTimestamp ( columnLabel, false );
	}
	
	/**
	 * Get timestamp from long or string field, set excelDate to true to parse a long
	 * field which is formatted as excel date (i.e. the long represents a date starting
	 * from the 1900).
	 * @param header
	 * @param excelDate
	 * @return
	 */
	public Timestamp getTimestamp ( String header, boolean excelDate ) {
		
		// get the excel column letter from the column header
		String key = getColumnLetter ( header );
				
		if ( key == null )
			return null;
		
		// return the value inside the cell identified by the column letter
		// and by the current row
		String value = currentDataRow.get( key.toUpperCase() );
		
		// if no value found, return the default
		if ( value == null || value.isEmpty() )
			return null;
		
		
		Timestamp ts = null;
		try {
			
			// get the date in long format
			long date = Long.parseLong( value );
			
			// if we need to convert from an excel date, we convert it
			if ( excelDate )
				ts = toSQLTimestamp( DateUtil.getJavaDate( date ) );
			else // otherwise directly create a timestamp from the long date
				ts = new Timestamp( date );
		}
		catch ( NumberFormatException e ) {
			
			// it was a string! => format as simple string
			try {
				
				ts = getTimestampFromString( value, "yyyy/MM/dd" );
				
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
		}
		return ts;
	}
	
	/**
	 * Convert a java.util.date in a java.sql.timestamp, in order to store the information
	 * in a jdbc database
	 * @param date
	 * @return
	 */
	public static java.sql.Timestamp toSQLTimestamp ( Date date ) {
		
		// get the timestamp from the date
		java.sql.Timestamp timestamp = new java.sql.Timestamp( date.getTime() );
		
		// return the timestamp
		return timestamp; 
	}
	
	/**
	 * Trasform a date string into a timestamp
	 * @param dateString
	 * @param dateFormat
	 * @return
	 * @throws ParseException
	 */
	public static java.sql.Timestamp getTimestampFromString ( String dateString, 
			String dateFormat ) throws ParseException {
		
		SimpleDateFormat format = new SimpleDateFormat( dateFormat );
	    Date parsedDate = format.parse( dateString );
	    return new java.sql.Timestamp( parsedDate.getTime() );
	}
	
	
	/**
	 * Get the excel column letter from the column header
	 * @param colHeader
	 * @return
	 */
	private String getColumnLetter ( String colHeader ) {
		
		String excelColLetter = headers.get( colHeader.toUpperCase() );
		
		return excelColLetter;
	}

	/**
	 * Close all the dataset
	 */
	public void close ( ) {
		clear();
		currentDataRow.clear();
		cursorPosition = -1;
	}

	/**
	 * Set an header of the data table
	 * @param key
	 * @param value
	 */
	public void setHeader ( String key , String value ) {
		headers.put( key.trim(), value.trim() );
	}

	/**
	 * Set an element of the current data row
	 * @param key
	 * @param value
	 */
	public void setElem ( String key , String value ) {
		currentDataRow.put( key, value );
	}

	public void setRow ( ) {
		dataRecords.add( currentDataRow );
		currentDataRow = new HashMap< String, String >();
	}

	public String size ( ) {
		return new String( Integer.toString( dataRecords.size() ) );
	}

	public HashMap< String, String > getData ( Integer i ) {
		return dataRecords.get( i );
	}

	public HashMap< String, String > getHeaders ( ) {
		return headers;
	}

	@SuppressWarnings("unused")
	private String get ( String string ) {
		return dataRecords.get( 1 ).get( string );
	}

	public boolean contains ( String string ) {
		return currentDataRow.containsKey( headers.get( string.toUpperCase() ));
	}

	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void afterLast() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeFirst() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteRow() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean first() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Array getArray(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		return getBoolean( columnLabel, false );
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCursorName() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public java.sql.Date getDate(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public java.sql.Date getDate(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public java.sql.Date getDate(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public java.sql.Date getDate(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getFetchDirection() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getFetchSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRow() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Statement getStatement() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getType() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void insertRow() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFirst() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLast() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean last() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveToInsertRow() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean previous() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void refreshRow() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rowInserted() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateDate(int columnIndex, java.sql.Date x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateDate(String columnLabel, java.sql.Date x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNull(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNull(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateObject(int columnIndex, Object x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateRow() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean wasNull() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
}
