package catalogue_generator;

import java.io.IOException;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import dcf_user.User;
import progress_bar.FormProgressBar;

/**
 * Thread used to remove in background a catalogue database
 * while showing a progress bar in the ui thread.
 * @author avonva
 *
 */
public class CatalogueDestroyer extends Thread {

	private static final Logger LOGGER = LogManager.getLogger(CatalogueDestroyer.class);
	
	private ThreadFinishedListener doneListener;
	private FormProgressBar progressBar;
	private Collection<Catalogue> catalogues;
	
	/**
	 * Initialize the destroyer
	 * @param catalogues the catalogues we want to delete from the db
	 */
	public CatalogueDestroyer( Collection<Catalogue> catalogues ) {
		this.catalogues = catalogues;
	}
	
	/**
	 * Start the catalogues removal
	 */
	public void run() {

		int code = ThreadFinishedListener.OK;

		// the gained progress for each deleted catalogue
		double step = 100 / catalogues.size();

		// remove the catalogues from the database
		for ( Catalogue catalogue : catalogues ) {

			if ( progressBar != null )
				progressBar.addProgress( step );

			// cannot remove reserved or pending catalogues
			if ( catalogue.isReserved() || User.getInstance().hasPendingRequestsFor(catalogue) ) {
				code = ThreadFinishedListener.ERROR;
				continue; 
			}

			LOGGER.info ( "Deleting catalogue " + catalogue.getCode() );

			// delete the catalogue database
			try {
				
				// delete on disk
				DatabaseManager.deleteDb( catalogue );
				
				// delete record on main db
				final CatalogueDAO catDao = new CatalogueDAO();
				catDao.delete( catalogue );
				
			} catch (IOException e) {
				e.printStackTrace();
				LOGGER.error("Cannot delete catalogue=" + catalogue, e);
			}
		}

		if ( progressBar != null )
			progressBar.close();
		
		// finished
		doneListener.finished( this, code, null );
	};
	
	/**
	 * Set the progress bar for the process
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Set the listener which will be called when the
	 * thread finishes its work
	 * @param doneListener
	 */
	public void setDoneListener(ThreadFinishedListener doneListener) {
		this.doneListener = doneListener;
	}
}
