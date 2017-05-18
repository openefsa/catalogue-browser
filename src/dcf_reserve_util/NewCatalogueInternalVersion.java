package dcf_reserve_util;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue_browser_dao.CatalogueDAO;
import catalogue_object.Catalogue;
import import_catalogue.ImportActions;
import ui_progress_bar.FormProgressBar;

/**
 * Class to manage new version of a catalogue during the reserve
 * process.
 * @author avonva
 *
 */
public class NewCatalogueInternalVersion {

	/**
	 *  the new version of the catalogue
	 *  this variable is defined only after 
	 *  having called {@link #importNewCatalogueVersion(Listener)}
	 */
	private Catalogue newCatalogue;
	
	private String newCode;
	
	// if a new version is discovered, we
	// save the new version of the catalogue
	private String newVersion;
	
	// and the location in the laptop where
	// the xml file of the new version of the 
	// catalogue is stored
	private String filename;
	
	private ImportActions imprt;
	
	public NewCatalogueInternalVersion( String newCode, String newVersion, String filename ) {
		this.newCode = newCode;
		this.newVersion = newVersion;
		this.filename = filename;
		imprt = new ImportActions();
	}
	
	/**
	 * Set the progress bar for the import process
	 * @param progressBar
	 */
	public void setProgressBar( FormProgressBar progressBar ) {
		imprt.setProgressBar( progressBar );
	}
	
	/**
	 * Import the new catalogue version into the database
	 * @param doneListener
	 */
	public void importNewCatalogueVersion ( final Listener doneListener ) {
		
		// download the last internal version
		// and when the process is finished
		// reserve the NEW catalogue
		ImportActions imprt = new ImportActions();

		// import the catalogue xml and remove the file at
		// the end of the process
		imprt.importXml( null, filename, true, new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {

				// get the new catalogue version
				CatalogueDAO catDao = new CatalogueDAO();
				newCatalogue = catDao.getCatalogue( 
						newCode, newVersion );
				
				doneListener.handleEvent( arg0 );
			}
		} );
	}
	
	/**
	 * Get the new version of the catalogue
	 * Note that you need to call {@link #importNewCatalogueVersion(Listener)}
	 * before, otherwise you will get null.
	 * @return
	 */
	public Catalogue getNewCatalogue() {
		return newCatalogue;
	}
}
