package dcf_manager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.soap.SOAPException;

import org.eclipse.swt.widgets.Listener;
import org.w3c.dom.Document;

import catalogue_browser_dao.CatalogueDAO;
import catalogue_object.Catalogue;
import dcf_reserve_util.BackgroundReserve;
import dcf_reserve_util.PendingReserve;
import dcf_reserve_util.PendingReserveDAO;
import dcf_reserve_util.ReserveListener;
import dcf_reserve_util.ReserveValidator;
import dcf_user.User;
import dcf_user.UserAccessLevel;
import dcf_webservice.ExportCatalogue;
import dcf_webservice.ExportCatalogueFile;
import dcf_webservice.GetCataloguesList;
import dcf_webservice.Ping;
import dcf_webservice.ReserveLevel;
import ui_progress_bar.FormProgressBar;

/**
 * Class to model the DCF. Here we can download
 * catalogues and perform web service operations.
 * @author avonva
 *
 */
public class Dcf {
	
	// progress bar
	private FormProgressBar progressBar;
	
	/**
	 * A list which contains all the published 
	 * dcf catalogues
	 */
	private static ArrayList < Catalogue > catalogues = null;

	/**
	 *  True if we are currently getting catalogue updates
	 *  false otherwise
	 */
	private static boolean gettingUpdates = false;

	/**
	 * Get all the catalogues which can 
	 * be downloaded from the dcf
	 * @return
	 */
	public static ArrayList<Catalogue> getCatalogues() {
		return catalogues;
	}

	/**
	 * Get the codes of the catalogues which need to be updated or are new
	 * compared to the ones that I already have downloaded in my pc.
	 * 
	 * A list which contains all the catalogues 
	 * which can be downloaded compared to the ones we
	 * have already downloaded in our local machine. This
	 * means that this list contains all the catalogues we
	 * don't have and the catalogues which need an update
	 * compared to the version we have in our machine.
	 * @return
	 */
	public static ArrayList <Catalogue> getDownloadableCat () {
		
		// list of catalogues from which the user can select
		// we want them to be only the catalogues which have not been downloaded yet
		// or catalogues updates
		ArrayList <Catalogue> catalogueToShow = new ArrayList<>();

		if ( catalogues == null || catalogues.isEmpty() )
			return catalogueToShow;
		
		CatalogueDAO catDao = new CatalogueDAO();
		
		// get the catalogues which are currently 
		// present into the user database
		// at their last release status!
		ArrayList < Catalogue > myCatalogues = 
				catDao.getLastReleaseCatalogues ();

		// for each DCF catalogue
		for ( Catalogue cat : catalogues ) {
			
			// if the catalogue is not contained in the my catalogues
			// we have found a catalogue which has not been downloaded yet
			// note we add it only if it is not deprecated
			// do not show cat users catalogue to users
			if ( myCatalogues.contains( cat ) || cat.isDeprecated() || 
					cat.isCatUsersCatalogue() )
				continue;
			
			catalogueToShow.add( cat );
		}
		
		
		// here we searches for catalogues updates
		for ( Catalogue myCat : myCatalogues ) {

			// if we already have the last release go on
			if ( myCat.isLastRelease() )
				continue;
			
			// get the catalogue which is the updated version of the
			// my cat catalogue (or null if no update is available)
			Catalogue updateCat = getLastPublishedRelease ( myCat );
			
			// if there is an update => add the catalogue to the list
			// we exclude the cat users catalogue since it is downloaded
			// automatically with the login process
			if ( updateCat != null && !updateCat.isCatUsersCatalogue() ) {
				catalogueToShow.add( updateCat );
			}
		}

		// sort catalogues by label and version
		Collections.sort( catalogueToShow );
		
		return catalogueToShow;
	}
	
	/**
	 * Is the application getting the catalogues updates
	 * from the dcf?
	 * @return
	 */
	public static boolean isGettingUpdates() {
		return gettingUpdates;
	}

	/**
	 * Are the catalogue meta data being downloaded now?
	 * @param gettingUpdates
	 */
	public static void setGettingUpdates(boolean gettingUpdates) {
		Dcf.gettingUpdates = gettingUpdates;
	}

	/**
	 * Get the last release of a catalogue
	 * @param catalogue
	 * @return
	 */
	public static Catalogue getLastPublishedRelease ( Catalogue catalogue ) {

		if ( catalogues == null ) {
			System.err.println( "No dcf catalogues found in the cache" );
			return null; 
		}

		// get the catalogue in the dcf list
		// using only its code
		int index = catalogues.indexOf( catalogue );

		// if not found
		if ( index == -1 )
			return null;

		return catalogues.get( index );
	}

	/**
	 * Refresh the catalogues of the dcf downloading their
	 * meta data. Refresh also the downloadable catalogues.
	 */
	public void refreshCatalogues() {
		
		// flag to say that we are getting the updates
		gettingUpdates = true;
		
		// get all the dcf catalogues and save them
		try {
			
			catalogues = new GetCataloguesList().getCataloguesList();
			
			// sort catalogues by label and version
			Collections.sort( catalogues );
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// we have finished to get updates
		gettingUpdates = false;
	}
	
	/**
	 * Set a progress bar which is called for
	 * webservices.
	 * @param progressBar
	 */
	public void setProgressBar ( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Set the progress bar title
	 * @param title
	 */
	public void setProgressBarTitle ( String title ) {
		if ( progressBar != null )
			progressBar.setLabel( title );
	}
	
	/**
	 * Make a ping to the dcf
	 * @return true if the dcf is responding correctly
	 */
	public boolean ping() {
		
		boolean check;
		
		try {
			check = (boolean) ( new Ping() ).makeRequest();
		} catch (SOAPException e) {
			e.printStackTrace();
			check = false;
		}
		
		return check;
	}
	
	/**
	 * Start the thread which checks the user access
	 * level (see {@link UserAccessLevel} ).
	 * @param doneListener {@link Listener } called when
	 * the thread has finished its work.
	 */
	public void setUserLevel( Listener doneListener ) {
		
		// set the access level of the user
		final UserProfileChecker userLevel = new UserProfileChecker();
		
		userLevel.addDoneListener( doneListener );
		
		userLevel.start();
	}
	
	/**
	 * Get the catalogues updates from the dcf. In particular,
	 * we download the all the published catalogues (only metadata)
	 * and refresh the dcf catalogues cache ( {@link Dcf#catalogues} )
	 * @param doneListener
	 */
	public void checkUpdates ( Listener doneListener ) {
		
		// start downloading the catalogues updates (meta data only)
		final UpdatesChecker catUpdates = new UpdatesChecker();
		
		// set the listener
		catUpdates.setUpdatesListener( doneListener );
		
		catUpdates.start();
	}
	
	/**
	 * Get the list of all the dcf catalogues (only published)
	 * @return array list of dcf published catalogues
	 */
	public ArrayList<Catalogue> getCataloguesList() {
		
		ArrayList<Catalogue> list = new ArrayList<>();
		
		// get the dcf catalogues
		GetCataloguesList catList = new GetCataloguesList();
		
		try {
			
			list = catList.getCataloguesList();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	/**
	 * Download a dcf catalogue into the local machine. The
	 * catalogue is downloaded in xml format.
	 * @param catalogue the catalogue we want to download
	 * @param filename the xml filename
	 * @return true if the export was successful
	 */
	public boolean exportCatalogue( Catalogue catalogue, String filename ) {
		
		// export the catalogue and save its attachment into an xml file
		ExportCatalogue export = new ExportCatalogue ( catalogue, filename );
		return export.exportCatalogue();
	}
	
	/**
	 * Export a log from the dcf given its code
	 * @param logCode the code of the log which needs to
	 * be downloaded
	 * @return a dom document which contains the log
	 */
	public Document exportLog ( String logCode ) {
		
		// ask for the log to the dcf
		ExportCatalogueFile export = new ExportCatalogueFile();

		// get the log document
		return export.exportLog( logCode );
	}

	/**
	 * Export the last internal version of a catalogue into the selected
	 * filename. If no internal version is retrieved no action is
	 * performed.
	 * @param catalogueCode the code which identifies the catalogue
	 * we want to download
	 * @param filename the filename in which we want to store the
	 * last internal version of the catalogue
	 * @return true if the file was correctly created
	 * @throws IOException
	 */
	public boolean exportCatalogueInternalVersion ( String catalogueCode, 
			String filename ) throws IOException {
		
		// ask for the log to the dcf
		ExportCatalogueFile export = new ExportCatalogueFile();

		// get the catalogue xml as input stream
		InputStream stream = export.exportLastInternalVersion( catalogueCode );
		
		// if not internal version ok, you can go on
		if ( stream == null ) {
			System.out.println ( "No internal version found for " + catalogueCode );
			return false;
		}

		// write the input stream into the .xml file
		byte[] buffer = new byte[ stream.available() ];
		stream.read( buffer );

		// save the last internal version into a file
		// in order to possibly import it by the xml
		File targetFile = new File( filename );
		OutputStream outStream = new FileOutputStream( targetFile );
		outStream.write( buffer );
		outStream.close();
		
		return true;
	}
	
	/**
	 * Start a reserve operation in background.
	 * @param catalogue the catalogue we want to reserve
	 * @param level the reserve level we want
	 * @param description
	 * @param listener listener called when reserve events occur
	 * see {@link ReserveListener} to check which are the events
	 */
	public void reserve ( Catalogue catalogue, 
			ReserveLevel level, String description, ReserveListener listener ) {
		
		BackgroundReserve reserve = new BackgroundReserve( catalogue, 
				level, description );
		
		reserve.setListener( listener );
		reserve.setProgressBar( progressBar );
		reserve.start();
	}
	
	/**
	 * Start a single pending reserve process
	 * @param pr the pending reserve we want to start (i.e. we want
	 * to check if the dcf finished the reserve operation related
	 * to the pending reserve
	 * @param listener called to listen reserve events
	 */
	public void startPendingReserve ( PendingReserve pr, ReserveListener listener ) {
		
		// start the validator for the current pending reserve
		ReserveValidator validator = new ReserveValidator( pr, listener );
		validator.setProgressBar( progressBar );
		validator.start();
	}
	
	/**
	 * Start all the pending reserves in the database of this user
	 * Be aware that this method should be called only if no pending
	 * reserve is currently running, otherwise you will get a duplicated
	 * process for the same pending reserve.
	 * @param listener listener which listens to several
	 * reserve events, used mainly to notify the user
	 */
	public void startPendingReserves ( ReserveListener listener ) {

		PendingReserveDAO prDao = new PendingReserveDAO();

		// for each pending reserve action (i.e. reserve
		// actions which did not finish until now, this
		// includes also the requests made in other
		// instances of the CatBrowser if it was closed)
		for ( PendingReserve pr : prDao.getAll() ) {
			
			// skip all the pending reserves which
			// were not made by the current user
			if ( !pr.madeBy( User.getInstance() ) )
				continue;
			
			startPendingReserve ( pr, listener );
		}
	}
}
