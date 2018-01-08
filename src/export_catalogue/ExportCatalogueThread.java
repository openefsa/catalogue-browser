package export_catalogue;

import java.io.IOException;

import catalogue.Catalogue;
import catalogue_generator.ThreadFinishedListener;
import progress_bar.IProgressBar;

/**
 * Thread used to export a calogue in .xlsx format in background
 * while the progress bar uses the UI thread.
 * @author avonva
 *
 */
public class ExportCatalogueThread extends Thread {

	private String filename;
	private Catalogue catalogue;
	private ThreadFinishedListener listener;
	private IProgressBar progressForm;
	
	/**
	 * Start an export process.
	 * @param catalogue the catalogue we want to export
	 * @param filename the .xlsx file which will host the catalogue data
	 */
	public ExportCatalogueThread( Catalogue catalogue, String filename ) {
		this.catalogue = catalogue;
		this.filename = filename;
	}
	
	@Override
	public void run() {
		
		// prepare the import procedure
		final ExportCatalogueWorkbook exportCat = new ExportCatalogueWorkbook();
		
		if ( progressForm != null )
			exportCat.setProgressBar( progressForm );
		
		// export the catalogue
		try {
			exportCat.exportCatalogue( catalogue, filename );
		} catch (IOException e) {
			e.printStackTrace();

			// exception
			if ( listener != null )
				listener.finished( ExportCatalogueThread.this, 
						ThreadFinishedListener.EXCEPTION, e );

			finish();
			
			return;
		}


		// finished
		if ( listener != null )
			listener.finished( ExportCatalogueThread.this, 
					ThreadFinishedListener.OK, null );

		finish();
	}
	
	private void finish() {
		progressForm.close();
	}
	
	public void setListener(ThreadFinishedListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Set the progress bar for the thread
	 * @param progressForm
	 */
	public void setProgressBar( IProgressBar progressForm ) {
		this.progressForm = progressForm;
	}
}
