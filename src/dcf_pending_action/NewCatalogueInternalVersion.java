package dcf_pending_action;

import java.io.IOException;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import dcf_manager.Dcf.DcfType;
import import_catalogue.ImportCatalogueThread;
import import_catalogue.ImportCatalogueThread.ImportFileFormat;
import ui_progress_bar.FormProgressBar;
import utilities.GlobalUtil;

/**
 * Class to manage new version of a catalogue using only
 * code and version information. We have also the information
 * related to the xml filename in which all the catalogue
 * information are contained.
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
	
	private DcfType dcfType;
	
	private FormProgressBar progressBar;

	
	public NewCatalogueInternalVersion( String newCode, 
			String newVersion, String filename, 
			DcfType dcfType ) {
		this.newCode = newCode;
		this.newVersion = newVersion;
		this.filename = filename;
		this.dcfType = dcfType;
	}
	
	/**
	 * Set the progress bar for the import process
	 * @param progressBar
	 */
	public void setProgressBar( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Import the new catalogue version into the database
	 * @param doneListener
	 */
	public void importNewCatalogueVersion ( final Listener doneListener ) {
		
		ImportCatalogueThread importCat = new 
				ImportCatalogueThread( filename, ImportFileFormat.XML );
		
		// download the last internal version
		// and when the process is finished
		// reserve the NEW catalogue
		if ( progressBar != null )
			importCat.setProgressBar( progressBar );
		
		importCat.addDoneListener( new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				// get the new catalogue version
				CatalogueDAO catDao = new CatalogueDAO();
				newCatalogue = catDao.getCatalogue( 
						newCode, newVersion, dcfType );
				
				// delete the input file since it is a temporary file
				try {
					GlobalUtil.deleteFileCascade( filename );
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				doneListener.handleEvent( arg0 );
			}
		});
		
		importCat.start();
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
	
	@Override
	public String toString() {
		return "NewCatVersion: code=" + newCode + ",version=" + newVersion + ",XmlFilename=" + filename;
	}
}
