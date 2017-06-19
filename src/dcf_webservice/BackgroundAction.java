package dcf_webservice;

import catalogue.Catalogue;
import dcf_manager.Dcf;
import dcf_pending_action.PendingAction;
import dcf_pending_action.PendingActionListener;
import dcf_pending_action.PendingPublish;
import dcf_webservice.Publish.PublishLevel;
import ui_progress_bar.FormProgressBar;

/**
 * Perform an {@link UploadCatalogueFile} webservice call
 * in background.
 * @author avonva
 *
 */
public class BackgroundAction extends Thread {

	private Type type;
	private Catalogue catalogue;
	private PublishLevel pLevel;
	
	private ReserveLevel rLevel;
	private String rDescription;
	private FormProgressBar progressBar;
	
	private PendingActionListener listener; // listen to events
	
	public enum Type {
		RESERVE,
		PUBLISH,
		UPLOAD_DATA
	}
	
	/**
	 * Set the listener for reserve events.
	 * @param listener
	 */
	public void setListener(PendingActionListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Set the progress bar which will be used if a new
	 * version of the catalogue is downloaded
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Initialize a publish action
	 * @param catalogue the catalogue we want to publish
	 * @param level the publish level required
	 */
	public BackgroundAction( Catalogue catalogue, PublishLevel level ) {
		this.catalogue = catalogue;
		this.pLevel = level;
		this.type = Type.PUBLISH;
	}
	
	/**
	 * Initialize an upload data action
	 * @param catalogue the catalogue we want to upload
	 */
	public BackgroundAction( Catalogue catalogue ) {
		this.catalogue = catalogue;
		this.type = Type.UPLOAD_DATA;
	}
	
	/**
	 * Initialize a reserve action
	 * @param catalogue the catalogue we want to reserve
	 * @param reserveLevel the reserve level we want
	 * @param reserveDescription the reserve description
	 */
	public BackgroundAction( Catalogue catalogue, ReserveLevel level, 
			String rDescription ) {

		this.catalogue = catalogue;
		this.rLevel = level;
		this.rDescription = rDescription;
		this.type = Type.RESERVE;
	}
	
	@Override
	public void run() {

		Dcf dcf = new Dcf();
		UploadCatalogueFile req = dcf.uploadCatFile( type );

		// notify that we are ready to perform the action
		if ( listener != null )
			listener.requestPrepared();
		
		PendingAction pa = null;
		
		switch ( type ) {
		case RESERVE:
			// start the reserve process
			pa = ((Reserve) req).reserve( catalogue, rLevel, rDescription );
			break;
			
		case PUBLISH:
			// start the publish process
			pa = (PendingPublish) ((Publish) req).publish( catalogue, pLevel );
			break;
		default:
			break;
		}
		
		// if we have successfully sent the request,
		// we can notify the caller that we have
		// the log code of the request saved in the database
		if ( listener != null && pa != null )
			listener.requestSent( pa, pa.getLogCode() );
		
		// start the pending reserve we have just created
		dcf.setProgressBar(progressBar);
		dcf.startPendingAction( pa, listener );
	}
}
