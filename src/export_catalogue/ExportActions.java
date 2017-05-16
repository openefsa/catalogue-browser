package export_catalogue;

import org.eclipse.swt.widgets.Listener;

import catalogue_object.Catalogue;
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
	 * Export the selected catalogue into an excel file
	 * @param catalogue catalogue to be exported
	 * @param outputFilename the name of the xlsx file
	 * @param doneListener listener to be called when the export is finished
	 */
	public void exportCatalogueToExcel ( Catalogue catalogue, String outputFilename, Listener doneListener ) {
		
		// create a thread for the excel export
		ExportThread exportThread = new ExportThread( outputFilename );
		exportThread.setProgressBar( progressBar );
		exportThread.addDoneListener( doneListener );
		exportThread.start();
	}
}
