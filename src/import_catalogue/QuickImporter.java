package import_catalogue;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import open_xml_reader.ResultDataSet;
import open_xml_reader.WorkbookReader;

/**
 * Import a work sheet in a quicker way
 * separating the import and reading work using threads.
 * To use this class you need to implement {@link #importData(ResultDataSet)}.
 * @author avonva
 *
 */
public abstract class QuickImporter {

	protected WorkbookReader workbookReader;
	protected String sheetName;
	protected int batchSize;
	
	public QuickImporter( WorkbookReader workbookReader,
			String sheetName, int batchSize ) {
		this.workbookReader = workbookReader;
		this.sheetName = sheetName;
		this.batchSize = batchSize;
	}
	
	/**
	 * Import the current sheet
	 * @throws CloneNotSupportedException
	 * @throws XMLStreamException
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 */
	public void importSheet () 
			throws XMLStreamException, InvalidFormatException, IOException {
		
		// get the sheet
		workbookReader.processSheetName( sheetName );
		workbookReader.setBatchSize( batchSize );
		
		if ( !workbookReader.hasNext() )
			return;
		
		ResultDataSet fetched = workbookReader.next();

		// read the first batch
		while ( fetched != null ) {
			
			// copy the data set to use it in the import
			ResultDataSet current;
			try {
				current = (ResultDataSet) fetched.clone();
			} catch (CloneNotSupportedException e1) {
				e1.printStackTrace();
				return;
			}
			
			// meanwhile read the second batch
			// note that this will override the fetched
			// result data set since we are pointing to
			// that result data set
			SheetReaderThread t = new SheetReaderThread( workbookReader );
			t.start();

			// import the first batch of data
			// while reading the second
			importData( current );

			// wait that the read thread is finished
			try {
				
				t.join();
				
				// close used result set
				current.close();
				
				// if no next data stop!
				if ( t.getData() == null ) {
					if ( fetched != null ) {
						fetched.close();
						fetched = null;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Import the read result data set in a separate
	 * thread. The import of the data will processed
	 * in parallel with the data reading.
	 * @param rs
	 */
	public abstract void importData ( ResultDataSet rs );
}
