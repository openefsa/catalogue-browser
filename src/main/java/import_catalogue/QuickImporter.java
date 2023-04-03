package import_catalogue;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import open_xml_reader.ResultDataSet;
import open_xml_reader.WorkbookReader;

/**
 * Import a work sheet in a quicker way separating the import and reading work
 * using threads. To use this class you need to implement
 * {@link #importData(ResultDataSet)}.
 * 
 * @author avonva
 *
 */
public abstract class QuickImporter {

	private static final Logger LOGGER = LogManager.getLogger(QuickImporter.class);

	protected WorkbookReader workbookReader;
	protected String sheetName;
	protected int batchSize;

	public QuickImporter(WorkbookReader workbookReader, int batchSize) {
		this.workbookReader = workbookReader;
		this.batchSize = batchSize;
	}

	/**
	 * Import the current sheet
	 * 
	 * @author shahaal
	 * @author avonva
	 * 
	 * @throws CloneNotSupportedException
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws InvalidFormatException
	 * @throws ImportException
	 */
	public void importSheet() throws XMLStreamException, InvalidFormatException, IOException, ImportException {

		workbookReader.setBatchSize(batchSize);

		if (!workbookReader.hasNext())
			return;

		ResultDataSet fetched = null;

		// resolve memory leak
		try {

			fetched = workbookReader.next();

			// read the first batch
			while (fetched != null) {

				// copy the data set to use it in the import
				ResultDataSet current = null;
				try {
					current = (ResultDataSet) fetched.clone();
				} catch (CloneNotSupportedException e1) {
					LOGGER.error("Error during clone ", e1);
					e1.printStackTrace();
					return;
				}

				// meanwhile read the second batch
				// note that this will override the fetched
				// result data set since we are pointing to
				// that result data set
				SheetReaderThread t = new SheetReaderThread(workbookReader);
				t.start();

				// import the first batch of data
				// while reading the second
				importData(current);

				// wait that the read thread is finished
				try {

					t.join();

					// close used result set
					current.close();

					// if no next data stop!
					if (t.getData() == null) {
						if (fetched != null) {
							fetched.close();
							fetched = null;
						}
					}
				} catch (InterruptedException e) {
					LOGGER.error("Cannot import sheet", e);
					e.printStackTrace();
				}
				// solve memory leak
				current.close();
			}
		} catch (Exception e) {
			LOGGER.error("Cannot import sheet", e);
			e.printStackTrace();
		} finally {
			try {
				fetched.close();
			} catch (Exception e) {
				LOGGER.error("Error during close", e);
				e.printStackTrace();
				// TODO: handle exception
			}
		}

	}

	/**
	 * Import the read result data set in a separate thread. The import of the data
	 * will processed in parallel with the data reading.
	 * 
	 * @param rs
	 */
	public abstract void importData(ResultDataSet rs) throws ImportException;
}
