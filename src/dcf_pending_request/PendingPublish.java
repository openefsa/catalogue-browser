package dcf_pending_action;

import javax.xml.soap.SOAPException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue.Catalogue;
import catalogue_generator.CatalogueDownloader;
import catalogue_generator.ThreadFinishedListener;
import catalogue_object.Status;
import dcf_log.DcfLog;
import dcf_log.DcfResponse;
import dcf_manager.Dcf;
import dcf_manager.Dcf.DcfType;
import dcf_manager.UpdatesChecker;
import dcf_webservice.PublishLevel;

/**
 * Pending actions for publish operations.
 * @author avonva
 *
 */
public class PendingPublish extends PendingAction {

	private static final Logger LOGGER = LogManager.getLogger(PendingPublish.class);
	
	public static final String TYPE = "PUBLISH";
	private PublishLevel publishLevel;
	
	public PendingPublish( Catalogue catalogue, String logCode, String username, 
			Priority priority, PublishLevel publishLevel, DcfType dcfType ) {
		
		super(catalogue, logCode, username, "", priority, dcfType);
		
		this.publishLevel = publishLevel;
		setData( publishLevel.toString() );
	}
	

	/**
	 * Create a new pending publish object
	 * @param logCode
	 * @param level
	 * @param catalogue
	 * @param username
	 * @return
	 */
	public static PendingPublish addPendingPublish ( String logCode, 
			PublishLevel level, Catalogue catalogue, String username, 
			DcfType dcfType ) {
		
		// we create a new pending publish with FAST priority
		PendingPublish pr = new PendingPublish( catalogue, logCode, 
				username, Priority.HIGH, level, dcfType );
		
		// create a pending publish object in order to
		// retry the log retrieval (also if the application
		// is closed!)
		PendingActionDAO prDao = new PendingActionDAO();
		int id = prDao.insert( pr );
		
		pr.setId( id );
		
		return pr;
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
	/**
	 * Get the publish level
	 * @return
	 */
	public PublishLevel getPublishLevel() {
		return publishLevel;
	}

	@Override
	public void manageBusyStatus() {}

	@Override
	public DcfResponse extractLogResponse(DcfLog log) {
		
		DcfResponse response;
		
		Status catStatus = new Status(log.getCatalogueStatus());
		boolean correct = log.isMacroOperationCorrect();
		boolean isDraft = catStatus.isDraft();
		
		// if we have sent a minor reserve but the
		// catalogue status is major draft, then the
		// action is forbidden
		if ( !correct && !isDraft ) {
			response = DcfResponse.FORBIDDEN;
		}
		// return ok if correct operation
		else if ( correct )
			response = DcfResponse.OK;
		else
			response = DcfResponse.AP;
		
		if ( response == DcfResponse.OK )
			LOGGER.info ( publishLevel.getOp() + " of " + log.getCatalogueCode()
					+ ": successfully completed" );
		else
			LOGGER.info ( publishLevel.getOp() + " of " + log.getCatalogueCode()
					+ ": failed - the dcf rejected the operation" );

		return response;
	}

	@Override
	public void processResponse(DcfResponse response) throws SOAPException {
		
		// only correct operations
		if ( response != DcfResponse.OK ) {
			terminate();
			return;
		}
		
		// Refresh the dcf catalogue list, 
		// a new published version is available
		UpdatesChecker checker = new UpdatesChecker();
		checker.setUpdatesListener( new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				
				// download the published version
				download();
			}
		});
		
		checker.start();
	}
	
	/**
	 * Download the published catalogue
	 */
	private void download() {
		
		// get published catalogue from the list
		// of official catalogues
		Catalogue publishedVersion = Dcf.getCatalogueByCode( 
				getCatalogue().getCode() );
		
		// prepare downloader for the catalogue
		CatalogueDownloader downloader = 
				new CatalogueDownloader( publishedVersion );
		
		// reset and set the progress bar
		
		if ( getProgressBar() != null ) {
			getProgressBar().reset();
			downloader.setProgressBar( getProgressBar() );
		}
		
		// start downloading the catalogue
		downloader.setDoneListener( new ThreadFinishedListener() {
			
			@Override
			public void finished(Thread thread, int code, Exception e) {
				
				if ( code != ThreadFinishedListener.OK )
					setStatus( PendingActionStatus.ERROR );
				
				// terminate pending action if correct
				terminate();
			}
		});
		
		downloader.start();
	}


	@Override
	public void processLog(DcfLog log) {}
}
