package dcf_reserve_util;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.xml.sax.SAXException;

import catalogue_browser_dao.CatalogueDAO;
import catalogue_object.Catalogue;
import catalogue_object.Version;
import dcf_manager.Dcf;
import dcf_manager.VersionFinder;
import dcf_webservice.ReserveLevel;
import import_catalogue.ImportActions;
import ui_progress_bar.FormProgressBar;

/**
 * This is a {@link Thread}!
 * Base class for two different implementations of the reserve
 * process. In fact, we need one process which sends the reserve
 * request to the dcf and then check the reserve log. Then, we need
 * another process which gets all the pending reserve operations and
 * makes a polling to get their logs. The implementations are very
 * similar, therefore we use this class to unify the common code.
 * Note that the class is a thread since the reserve itself was
 * thought as a background process.
 * @author avonva
 *
 */
public abstract class ReserveSkeleton extends Thread {

	private Catalogue catalogue;
	private ReserveLevel reserveLevel;
	
	/**
	 * Initialize a reserve skeleton.
	 * @param catalogue
	 * @param reserveLevel
	 */
	public ReserveSkeleton( Catalogue catalogue, ReserveLevel reserveLevel ) {
		this.catalogue = catalogue;
		this.reserveLevel = reserveLevel;
	}

	@Override
	public void run() {
		startReserve();
	}
	
	/**
	 * Start the reserve operation for the current catalogue
	 * with the current reserve level
	 */
	public void startReserve () {

		// set the catalogue status as "reserving"
		catalogue.setReserving( true );
		
		// check what we have to do before making the reserve request
		final ReserveResult reserveLog = reserveCheck( catalogue, reserveLevel );
		
		switch ( reserveLog ) {
		case MINOR_FORBIDDEN:
		case ERROR:
			
			// notify that the reserve is started
			reserveStarted( catalogue, reserveLevel, reserveLog );
			
			// reset the catalogue status
			catalogue.setReserving( false );
			
			// notify that the reserve is finished
			reserveFinished ( catalogue, reserveLevel );
			
			break;

			// if correct or unreserve
		case NOT_RESERVING:
		case CORRECT_VERSION:

			// notify that the reserve is started
			reserveStarted( catalogue, reserveLevel, reserveLog );
			
			// reserve the catalogue if possible
			if ( canReserve ( catalogue, reserveLevel ) )
				reserveCatalogue();
			
			// reset the catalogue status
			catalogue.setReserving( false );
			
			// notify that the reserve is finished
			reserveFinished ( catalogue, reserveLevel );

			break;

		case OLD_VERSION:
			
			// download the last internal version
			// and when the process is finished
			// reserve the NEW catalogue
			ImportActions imprt = new ImportActions();

			// set the progress bar if possible
			if ( getProgressBar() != null )
				imprt.setProgressBar( getProgressBar() );

			// import the catalogue xml and remove the file at
			// the end of the process
			imprt.importXml( null, reserveLog.getFilename(), true, new Listener() {

				@Override
				public void handleEvent(Event arg0) {

					// get the new catalogue version
					CatalogueDAO catDao = new CatalogueDAO();
					Catalogue newCatalogue = catDao.getCatalogue( 
							catalogue.getCode(), reserveLog.getVersion() );
					
					// set that we are reserving the new catalogue
					newCatalogue.setReserving( true );
					
					// update the catalogue of the class with
					// the new version
					catalogue = newCatalogue;
					
					// notify that we have downloaded a new version of the catalogue
					newVersionDownloaded( newCatalogue );

					// notify that the reserve is started (after having
					// downloaded the new version)
					reserveStarted( newCatalogue, reserveLevel, reserveLog );

					// reserve the new version of the catalogue if possible
					if ( canReserve ( newCatalogue, reserveLevel ) )
						reserveCatalogue();
						
					// reset the catalogue status
					catalogue.setReserving( false );
					newCatalogue.setReserving( false );
					
					// notify that the reserve is finished
					reserveFinished ( newCatalogue, reserveLevel );
				}
			} );
			
			// callback after having started the download process
			// of the last internal version
			lastVersionDownloadStarted();

			break;
		default:
			break;
		}
	}

	/**
	 * Check if we can go on with the reserve operation or not.
	 * 
	 * @param catalogue the catalogue we want to reserve
	 * @param level the reserve level we want
	 * @return {@linkplain ReserveResult} which indicates if we can 
	 * or cannot do the reserve operation
	 * @throws IOException
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public ReserveResult reserveCheck ( final Catalogue catalogue, 
			final ReserveLevel level ) {

		// only if we are reserving (and not unreserving)
		if ( level.isNone() ) {
			return ReserveResult.NOT_RESERVING;
		}

		String format = ".xml";
		String filename = "temp_" + catalogue.getCode();
		String input = filename + format;
		String output = filename + "_version" + format;
		
		try {
			
			Dcf dcf = new Dcf();
			
			// export the internal version in the file
			boolean written = dcf.exportCatalogueInternalVersion( 
					catalogue.getCode(), input );

			// if no internal version is retrieved we have
			// the last version of the catalogue
			if ( !written )
				return ReserveResult.CORRECT_VERSION;
			
			VersionFinder finder = new VersionFinder( input, output );

			// if we are minor reserving a major draft => error
			// it is a forbidden action
			if ( level.isMinor() && finder.isStatusMajor() 
					&& finder.isStatusDraft() ) {
				
				System.err.println ( "Cannot perform a reserve minor on major draft" );
				
				return ReserveResult.MINOR_FORBIDDEN;
			}

			// compare the catalogues versions
			Version intVersion = new Version ( finder.getVersion() );
			Version localVersion = catalogue.getRawVersion();
			
			// if the downloaded version is newer than the one we
			// are working with => we are using an old version
			if ( intVersion.compareTo( localVersion ) < 0 ) {

				System.err.println ( "Cannot perform reserve on old version. Downloading the new version" );
				System.err.println ( "Last internal " + finder.getVersion() + 
						" local " + catalogue.getVersion() );
				
				// set the new version as data of the enum
				ReserveResult log = ReserveResult.OLD_VERSION;
				log.setVersion( finder.getVersion() );
				log.setFilename( input );

				return log;
			} 
			else {
				
				System.out.println ( "The last internal version has a lower or equal version "
						+ "than the catalogue we are working with." );
				System.out.println ( "Last internal " + finder.getVersion() + 
						" local " + catalogue.getVersion() );
				
				// if we have the updated version
				return ReserveResult.CORRECT_VERSION;
			}

		} catch ( IOException | 
				TransformerException | 
				ParserConfigurationException | 
				SAXException e ) {
			
			e.printStackTrace();
			
			return ReserveResult.ERROR;
		}
	}

	/**
	 * Reserve the current catalogue with the required
	 * reserve level
	 */
	private void reserveCatalogue () {
		
		reservingCatalogue(catalogue, reserveLevel);
		
		// set the catalogue as (un)reserved at the selected level
		if ( reserveLevel.greaterThan( ReserveLevel.NONE ) )
			this.catalogue.reserve ( reserveLevel );
		else
			this.catalogue.unreserve ();
	}
	
	/**
	 * Get the progress bar if we want to show it
	 * @return
	 */
	public abstract FormProgressBar getProgressBar();
	
	/**
	 * Called before downloading the last version of the catalogue
	 * if there is one.
	 */
	public abstract void lastVersionDownloadStarted ();
	
	/**
	 * Called when a new version of the catalogue is downloaded.
	 * This happens when we are not working with the last
	 * internal release of the catalogue.
	 * @param newVersion
	 */
	public abstract void newVersionDownloaded( Catalogue newVersion );
	
	/**
	 * A check to validate the reserve operation on the catalogue.
	 * If it returns true then the catalogue will be reserved.
	 * @param catalogue
	 * @param reserveLevel
	 * @return
	 */
	public abstract boolean canReserve ( Catalogue catalogue, 
			ReserveLevel reserveLevel );
	
	/**
	 * Called when the {@link ReserveSkeleton#reserveCatalogue(Catalogue, ReserveLevel) }
	 * starts.
	 * @param catalogue
	 * @param reserveLevel
	 * @param reserveLog the result of the preliminary reserve checks
	 */
	public abstract void reserveStarted ( Catalogue catalogue, 
			ReserveLevel reserveLevel, ReserveResult reserveLog );
	
	/**
	 * Called when the catalogue is being reserved (just before
	 * calling the {@link Catalogue#reserve(ReserveLevel)} procedure.
	 * @param catalogue
	 * @param reserveLevel
	 */
	public abstract void reservingCatalogue ( Catalogue catalogue, 
			ReserveLevel reserveLevel );
	
	/**
	 * Called when the {@link ReserveSkeleton#reserveCatalogue(Catalogue, ReserveLevel) }
	 * finishes. Note that it is not sure that the reserve succeeded.
	 * @param catalogue
	 * @param reserveLevel
	 */
	public abstract void reserveFinished ( Catalogue catalogue, 
			ReserveLevel reserveLevel );
}
