package export_catalogue;

import java.util.Collection;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import catalogue_object.Mappable;
import progress_bar.IProgressBar;
import sheet_header.SheetHeader;

/**
 * This class handle the writing procedure of an excel sheet. Extend this class
 * and provide the methods getHeaders and insertData to have a complete sheet
 * writer.
 * 
 * @author avonva
 * @author shahaal
 */
public abstract class SheetWriter {

	// progress bar which has to be updated while processing terms
	// if it is set, it is updated
	// see setProgressBar method
	private IProgressBar progressBar;

	// the maximum part of the progress bar which can be filled by processing this
	// sheet
	private int maxFill = 0;

	// the considered fields of the xml which will be the sheet columns
	public HashMap<String, SheetHeader> headers;

	// the current number of rows
	private int rowNum = 0;

	// the sheet which will be created
	private Sheet sheet;

	/**
	 * Create the sheet
	 * 
	 * @param workbook
	 * @param sheetName
	 */
	public SheetWriter(Workbook workbook, String sheetName) {

		// create the sheet
		sheet = workbook.createSheet(sheetName);
	}

	/**
	 * Write into the sheet the headers and the data This is the main process
	 */
	public void write() {

		// prepare the headers
		headers = getHeaders();

		// insert headers
		insertHeaders(sheet);

		// insert the data into the sheet
		insertData(sheet);
	}

	/**
	 * Write into the sheet the headers and the data This is the main process added
	 * also the first record with length 4000
	 * 
	 * @author shahaal
	 */
	public void writeWithDump() {

		// prepare the headers
		headers = getHeaders();

		// insert headers
		insertHeaders(sheet);

		// create the dump string with length 4000
		String dump = createDumpString();
		
		// insert the dump string as first record in the external file
		insertFirstRow(dump);

		// insert the data into the sheet
		insertData(sheet);
	}

	/**
	 * Insert the headers as first line of the sheet
	 * 
	 * @param sheet
	 */
	private void insertHeaders(Sheet sheet) {

		// get the headers
		this.headers = getHeaders();

		// create a row for the headers
		Row row = createRow(sheet);

		// insert the headers
		for (SheetHeader header : headers.values())
			createCell(header.getColumnIndex(), row, header.getColumnName());
	}

	/**
	 * Insert the data into the sheet
	 * 
	 * @param sheet
	 */
	private void insertData(Sheet sheet) {

		Collection<? extends Mappable> data = getData();

		// get of how much we have to increment the progress bar
		// we can increment at maximum of maxFill, so we add
		// for each row maxFill/#rows
		double increment = (double) maxFill / data.size();

		// for each record we create a row
		// with the values into the right cells
		for (Mappable record : data) {

			// insert a single row
			insertDataRow(record);

			// refresh the progress bar if needed
			if (progressBar != null)
				progressBar.addProgress(increment);
		}
	}

	/**
	 * create a dump string of length 4000 useful for not truncating strings in SAS
	 * 
	 * @author shahaal
	 * @return
	 */
	private String createDumpString() {
		String tempValue = "";
		for (int i = 0; i <= 4000; i++) {
			tempValue += "*";
		}

		return tempValue;
	}

	/**
	 * the method insert a dump string of length 4000 in sheet term when the cat
	 * manager export the catalogue
	 * 
	 * @author shahaal
	 * @param dump
	 */
	private void insertFirstRow(String dump) {

		// add the dump string only to the sheet "term"
		if (!sheet.getSheetName().equals("term"))
			return;

		Row row = createRow(sheet);

		// for each header we set the cell value related to it
		for (String dbColumnName : headers.keySet())
			// add the cell to the sheet
			createCell(headers.get(dbColumnName).getColumnIndex(), row, dump);
	}

	/**
	 * Insert a single record into the sheet
	 * 
	 * @param record, the record which has to be inserted
	 */
	private void insertDataRow(Mappable record) {

		Row row = createRow(sheet);

		// for each header we set the cell value related to it
		for (String dbColumnName : headers.keySet()) {

			// get the value of the catalogue field
			String value = record.getValueByKey(dbColumnName);

			// add the cell to the sheet
			createCell(headers.get(dbColumnName).getColumnIndex(), row, value);
		}
	}

	/**
	 * Set a progress bar which needs to be updated. Max fill is used to limit the
	 * maximum relative amount of progress that this sheet can achieve. The label is
	 * the displayed title.
	 * 
	 * @param progressBar, the progress bar which is displayed in the main UI
	 * @param maxFill,     the maximum relative amount that this sheet can be add to
	 *                     the progress bar
	 * @param label,       the label which will be displayed
	 */
	public void setProgressBar(IProgressBar progressBar, int maxFill, String label) {
		this.progressBar = progressBar;
		this.maxFill = maxFill;
		this.progressBar.setLabel(label);
	}

	/**
	 * Set the progress bar which needs to be updated. Max fill is used to limit the
	 * maximum relative amount of progress that this sheet can achieve.
	 * 
	 * @param progressBar, the progress bar which is displayed in the main UI
	 * @param maxFill,     the maximum relative amount that this sheet can be add to
	 *                     the progress bar
	 */
	public void setProgressBar(IProgressBar progressBar, int maxFill) {
		this.setProgressBar(progressBar, maxFill, "");
	}

	/**
	 * Create a new cell in the sheet.
	 * 
	 * @param columIndex, index of the column in which the cell is created
	 * @param row,        row where the cell is created
	 * @param value,      cell value
	 * @return
	 */
	public Cell createCell(int columnIndex, Row row, String value) {

		if (columnIndex < 0)
			return null;

		Cell cell = row.createCell(columnIndex);
		cell.setCellValue(value);
		return cell;
	}

	/**
	 * Create a new cell in the sheet.
	 * 
	 * @param headerName, name of the column in which the cell is created
	 * @param row,        row where the cell is created
	 * @param value,      cell value
	 * @return
	 */
	public Cell createCell(String columnLabel, Row row, String value) {

		// get the current header
		SheetHeader header = headers.get(columnLabel);

		// if the header is not in the headers hashmap return
		if (header == null) {
			return null;
		}

		// create the cell
		Cell cell = createCell(header.getColumnIndex(), row, value);
		return cell;
	}

	/**
	 * Create a new row in the selected sheet
	 * 
	 * @param sheet
	 * @return
	 */
	public Row createRow(Sheet sheet) {
		return sheet.createRow(rowNum++);
	}

	/**
	 * Get the sheet headers, we need to implement this method to choose which
	 * columns we want to insert into the sheet. Note that for terms this is a bit
	 * complicated since their columns names depend on hierarchies, attributes and
	 * on the catalogue code (to discover which is the master hierarchy). @return,
	 * hashmap of headers, the string is an identifier of the header (usually can be
	 * used the xml node if it is unique, otherwise a naming convention should be
	 * declared! You can use also the column names if they are unique, or a
	 * combination of xml node names and excel column names. It is sufficient that
	 * you are aware of the keys)
	 */
	public abstract HashMap<String, SheetHeader> getHeaders();

	/**
	 * Get the list of elements which needs to be inserted into the sheet. Note that
	 * it is sufficient that the objects implements the method getValueByKey in
	 * order to be inserted into the sheet. In fact, we use the headers key as key
	 * to retrieve the fields of the object. For example, if we have as header
	 * headers.put( "CAT_CODE", new SheetHeader(0, "code" ) ); we will use CAT_CODE
	 * to retrieve the code of the catalogue, in order to put it as value in the
	 * current row under the header "code" which is specified in the SheetHeader
	 * together with the column index.
	 * 
	 * @return
	 */
	public abstract Collection<? extends Mappable> getData();

}
