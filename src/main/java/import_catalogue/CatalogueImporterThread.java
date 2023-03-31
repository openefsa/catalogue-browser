package import_catalogue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.eclipse.swt.widgets.Event;
import org.xml.sax.SAXException;

import catalogue.Catalogue;
import catalogue_generator.ThreadFinishedListener;
import ict_add_on.ICTInstaller;
import import_catalogue.CatalogueImporter.ImportFileFormat;
import progress_bar.IProgressBar;
import utilities.GlobalUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread used to import a catalogue from three different formats: .ecf, .xml
 * and .xlsx. Note that the real import process involves only the .xlsx file. If
 * an .ecf or an .xml file are used, they are first converted to an .xlsx file
 * to import it. Import pipeline: .ecf => .xml => .xlsx => import .xlsx
 * 
 * @author avonva
 *
 */
public class CatalogueImporterThread extends Thread {
	
	private static final Logger LOGGER = LogManager.getLogger(CatalogueImporterThread.class);

	private Catalogue openedCat;
	private String filename; // path of the file
	private ImportFileFormat format; // the format of the file

	// called when import is finished
	private ThreadFinishedListener doneListener;

	// progress bar used to notify the user
	private IProgressBar progressBar;
	private double maxProgress = 100;

	/**
	 * Initialize the import thread
	 * 
	 * @param filename path of the file we want to import
	 * @param format   in which format is the file that we want to import
	 */
	public CatalogueImporterThread(String filename, ImportFileFormat format) {
		this.filename = filename;
		this.format = format;
	}

	public CatalogueImporterThread(File file, ImportFileFormat format) {
		this(file.getAbsolutePath(), format);
	}

	/**
	 * Run the import thread
	 */
	public void run() {

		if (progressBar != null)
			progressBar.open();

		CatalogueImporter importer = new CatalogueImporter(filename, format, progressBar, maxProgress);

		importer.setOpenedCat(openedCat);
		try {
			importer.makeImport();

			// if the ICT installed and importing mtx foodex2
			if (GlobalUtil.isIctInstalled()) {
				// update ICT db
				ICTInstaller ict = new ICTInstaller();
				ict.createDatabase();
			}

		} catch (TransformerException | IOException | XMLStreamException | OpenXML4JException | SAXException
				| SQLException | ImportException e) {
			LOGGER.error("Error ", e);
			e.printStackTrace();
			
			doneListener.finished(this, ThreadFinishedListener.EXCEPTION, e);

			if (progressBar != null) {
				progressBar.stop(e);
				progressBar.close();
			}

			return;
		}

		handleDone();
	}

	/**
	 * Call the done listener if it was set Pass as data the xlsx filename
	 */
	private void handleDone() {

		// end process
		if (progressBar != null) {
			progressBar.fillToMax();
			progressBar.close();
		}

		if (doneListener != null) {
			Event event = new Event();
			event.data = filename;
			doneListener.finished(this, ThreadFinishedListener.OK, null);
		}
	}

	/**
	 * Called when all the operations are finished
	 * 
	 * @param doneListener
	 */
	public void addDoneListener(ThreadFinishedListener doneListener) {
		this.doneListener = doneListener;
	}

	/**
	 * Set the progress bar for the thread
	 * 
	 * @param progressForm
	 */
	public void setProgressBar(IProgressBar progressBar, double maxProgress) {
		this.progressBar = progressBar;
		this.maxProgress = maxProgress;
	}

	/**
	 * Set the progress bar for the thread
	 * 
	 * @param progressForm
	 */
	public void setProgressBar(IProgressBar progressBar) {
		this.progressBar = progressBar;
		this.maxProgress = 100;
	}

	/**
	 * If we are importing a workbook into an opened catalogue we need to specify
	 * which is the catalogue, otherwise we will get errors in the import process
	 * due to the wrong db path of the catalogue (which is determined by the
	 * catalogue code + version)
	 * 
	 * @param localCat
	 */
	public void setOpenedCatalogue(Catalogue openedCat) {
		this.openedCat = openedCat;
	}
}
