package export_catalogue;

import catalogue.Catalogue;
import catalogue_generator.ThreadFinishedListener;
import progress_bar.IProgressBar;

/**
 * Class used to export a catalogue to an excel file
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class ExportActions {

	private IProgressBar progressBar;

	/**
	 * Set the progress bar for the process
	 * 
	 * @param progressBar
	 */
	public void setProgressBar(IProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	/**
	 * Export the selected catalogue into the selected filename. This method is a
	 * sync method, you will need to wait until the process is finished
	 * 
	 * @param catalogue
	 * @param filename
	 * @return
	 */
	public boolean exportSync(Catalogue catalogue, String filename, boolean b) {

		// create an export
		ExportCatalogueThread exportThread = new ExportCatalogueThread(catalogue, filename, b);

		if (progressBar != null)
			exportThread.setProgressBar(progressBar);

		exportThread.start();

		// wait export is finished
		try {
			exportThread.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Export the selected catalogue into an excel file in an async way (the method
	 * will return before the export action is finished). Set the doneListener to
	 * make actions at the end of the export process
	 * 
	 * @param catalogue      catalogue to be exported
	 * @param b
	 * @param outputFilename the name of the xlsx file
	 * @param doneListener   listener to be called when the export is finished
	 */
	public void exportAsync(Catalogue catalogue, String filename, boolean b, ThreadFinishedListener doneListener) {

		// create a thread for the excel export
		ExportCatalogueThread exportThread = new ExportCatalogueThread(catalogue, filename, b);

		if (progressBar != null)
			exportThread.setProgressBar(progressBar);

		exportThread.setListener(doneListener);
		exportThread.start();
	}
}
