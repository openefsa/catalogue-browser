package export_catalogue;

import org.eclipse.swt.widgets.Listener;

import catalogue.Catalogue;
import ui_progress_bar.FormProgressBar;

/**
 * Class used to export a catalogue to an excel file
 * @author avonva
 *
 */
public class ExportActions {

	private FormProgressBar progressBar;
	
	/**
	 * Set the progress bar for the process
	 * @param progressBar
	 */
	public void setProgressBar ( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Export the selected catalogue into the selected filename.
	 * This method is a sync method, you will need to wait until
	 * the process is finished
	 * @param catalogue
	 * @param filename
	 * @return
	 */
	public boolean exportSync ( Catalogue catalogue, String filename ) {
		
		// create an export
		ExportCatalogueThread exportThread = new ExportCatalogueThread( catalogue, filename );
		
		if ( progressBar != null )
			exportThread.setProgressBar( progressBar );

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
	 * Export the selected catalogue into an excel file
	 * in an async way (the method will return before the
	 * export action is finished). Set the doneListener to
	 * make actions at the end of the export process
	 * @param catalogue catalogue to be exported
	 * @param outputFilename the name of the xlsx file
	 * @param doneListener listener to be called when the export is finished
	 */
	public void exportAsync ( Catalogue catalogue, String filename,
			Listener doneListener ) {
		// create a thread for the excel export
		ExportCatalogueThread exportThread = new ExportCatalogueThread( catalogue, filename );
		if ( progressBar != null )
			exportThread.setProgressBar( progressBar );
		exportThread.addDoneListener( doneListener );
		exportThread.start();
	}
}
