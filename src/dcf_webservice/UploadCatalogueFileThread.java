package dcf_webservice;

import java.io.File;
import java.io.IOException;

import javax.xml.soap.SOAPException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Listener;

import catalogue.Catalogue;
import dcf_manager.Dcf;
import dcf_pending_action.PendingAction;
import dcf_pending_action.PendingActionListener;
import dcf_pending_action.PendingXmlDownload;
import dcf_user.User;
import progress_bar.FormProgressBar;
import soap.UploadCatalogueFile;
import user.IDcfUser;

/**
 * Perform an {@link UploadCatalogueFile} webservice call
 * in background.
 * @author avonva
 *
 */
public class UploadCatalogueFileThread extends Thread {

	private static final Logger LOGGER = LogManager.getLogger(UploadCatalogueFileThread.class);
	
	public enum Type {
		RESERVE,
		PUBLISH,
		DOWNLOAD_XML_UPDATES,
		UPLOAD_DATA
	}

	private Type type;
	private Catalogue catalogue;

	// publish stuff
	private PublishLevel pLevel;

	// reserve stuff
	private ReserveLevel rLevel;
	private String rDescription;

	// upload data stuff
	private File xmlFile;
	
	private FormProgressBar progressBar;

	private PendingActionListener listener; // listen to events
	private Listener doneListener;

	/**
	 * Initialize a {@link Publish} action
	 * @param catalogue the catalogue we want to publish
	 * @param level the publish level required
	 */
	public UploadCatalogueFileThread( Catalogue catalogue, PublishLevel level ) {
		this.catalogue = catalogue;
		this.pLevel = level;
		this.type = Type.PUBLISH;
	}

	/**
	 * Initialize an {@link UploadData} action
	 * @param catalogue the catalogue we want to upload
	 * @param xmlFile the xml updates file we will upload as attachment
	 */
	public UploadCatalogueFileThread( Catalogue catalogue, File xmlFile ) {
		this.catalogue = catalogue;
		this.type = Type.UPLOAD_DATA;
		this.xmlFile = xmlFile;
	}

	/**
	 * Initialize a {@link Reserve} action
	 * @param catalogue the catalogue we want to reserve
	 * @param reserveLevel the reserve level we want
	 * @param reserveDescription the reserve description
	 */
	public UploadCatalogueFileThread( Catalogue catalogue, ReserveLevel level, 
			String rDescription ) {

		this.catalogue = catalogue;
		this.rLevel = level;
		this.rDescription = rDescription;
		this.type = Type.RESERVE;
	}
	
	/**
	 * Create a download XML updates pending action
	 * @param catalogue
	 */
	public UploadCatalogueFileThread ( Catalogue catalogue ) {
		this.catalogue = catalogue;
		this.type = Type.DOWNLOAD_XML_UPDATES;
	}

	/**
	 * Start the pending action defined in the
	 * constructor and send it to the dcf.
	 */
	@Override
	public void run() {

		// get the correct service for the correct upload type
		UploadCatalogueFile req = getService( type );
		
		// notify that we are ready to perform the action
		if ( listener != null )
			listener.requestPrepared( catalogue, type );

		PendingAction pa = null;

		try {

			switch ( type ) {
			case RESERVE:
				// start the reserve process
				pa = ((Reserve) req).reserve( catalogue, rLevel, rDescription );
				break;

			case PUBLISH:
				// start the publish process
				pa = ((Publish) req).publish( catalogue, pLevel );
				break;

			case UPLOAD_DATA:

				if ( xmlFile == null ) {
					LOGGER.error( "Null upload data file, blocking action" );
					break;
				}	
				
				// upload the downloaded xml file to the dcf using upload catalogue file
				pa = ( (UploadData) req ).uploadData( catalogue, xmlFile );
				break;
				
			case DOWNLOAD_XML_UPDATES:
				
				// create xml pending data
				pa = PendingXmlDownload.addPendingDownload(
						catalogue, User.getInstance().getUsername(), Dcf.dcfType );

				break;

			default:
				LOGGER.error( "Type " + type + " not defined in BackgroundAction#run()" );
				return;
			}
		}
		catch ( SOAPException | IOException e ) {
			
			catalogue.setRequestingAction( false );
			
			// notify that the connection is bad
			// note that here the pending action was
			// not created, so we are consistent
			if ( listener != null )
				listener.connectionFailed( catalogue );

			return;
		}

		// if we have successfully sent the request,
		// we can notify the caller that we have
		// the log code of the request saved in the database
		if ( listener != null && pa != null )
			listener.requestSent( pa, pa.getLogCode() );

		// start the pending reserve we have just created
		Dcf dcf = new Dcf();
		dcf.setProgressBar( progressBar );
		dcf.startPendingAction( pa, listener );
		
		// call the listener, the process is finished
		if ( doneListener != null ) {
			doneListener.handleEvent( null );
		}
	}

	/**
	 * Initialize an upload catalogue file action and return it
	 * @param actionType
	 * @return
	 */
	public UploadCatalogueFile getService ( Type actionType ) {

		UploadCatalogueFile req = null;

		IDcfUser user = User.getInstance();
		
		// initialize the required service
		switch ( actionType ) {
		case RESERVE:
			req = new Reserve(user, Dcf.dcfType);
			break;
		case PUBLISH:
			req = new Publish(user, Dcf.dcfType);
			break;
		case UPLOAD_DATA:
			req = new UploadData(user, Dcf.dcfType);
			break;	
		default:
			break;
		}

		return req;
	}

	/**
	 * Set the listener for reserve events.
	 * @param listener
	 */
	public void setListener(PendingActionListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Listener called when the actions are finished
	 * @param doneListener
	 */
	public void setDoneListener(Listener doneListener) {
		this.doneListener = doneListener;
	}

	/**
	 * Set the progress bar which will be used if a new
	 * version of the catalogue is downloaded
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}
}
