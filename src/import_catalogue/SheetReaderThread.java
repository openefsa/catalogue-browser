package import_catalogue;

import javax.xml.stream.XMLStreamException;

import open_xml_reader.ResultDataSet;
import open_xml_reader.WorkbookReader;

public class SheetReaderThread extends Thread {
	
	private ResultDataSet rs;
	private WorkbookReader reader;
	private boolean finished;
	
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
