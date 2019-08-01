package import_catalogue;

import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import open_xml_reader.ResultDataSet;
import open_xml_reader.WorkbookReader;

/**
 * This thread is used to read batches of excel data
 * in background.
 * @author avonva
 *
 */
public class SheetReaderThread extends Thread {
	
	private static final Logger LOGGER = LogManager.getLogger(SheetReaderThread.class);
	
	private ResultDataSet rs;
	private WorkbookReader reader;
	private boolean finished;
	
	/**
	 * Thread which reads one batch of the sheet
	 * contained in the {@code reader}. Note that
	 * you should call {@link WorkbookReader#processSheetName(String)}
	 * and set {@link WorkbookReader#setBatchSize(int)}
	 * before using this class.
	 * @param reader
	 */
	public SheetReaderThread( WorkbookReader reader ) {
		this.reader = reader;
		this.finished = false;
	}
	
	@Override
	public void run() {

		if ( !reader.hasNext() )
			return;
		
		// read the next batch of data
		try {
			rs = reader.next();
		} catch (XMLStreamException e) {
			e.printStackTrace();
			LOGGER.error("Cannot read workbook", e);
		}

		finished = true;
	}
	
	/**
	 * Get the read data (when the process
	 * is finished)
	 * @return
	 */
	public ResultDataSet getData() {
		return rs;
	}
	
	public boolean isFinished() {
		return finished;
	}
}
