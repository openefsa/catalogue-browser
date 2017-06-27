package dcf_webservice;

import java.io.File;

import javax.xml.soap.SOAPException;

import catalogue.Catalogue;
import dcf_manager.Dcf;
import dcf_pending_action.PendingAction;
import dcf_pending_action.PendingActionListener;
import dcf_webservice.Publish.PublishLevel;
import sas_remote_procedures.XmlUpdatesCreator;
import ui_progress_bar.FormProgressBar;

/**
 * Perform an {@link UploadCatalogueFile} webservice call
 * in background.
 * @author avonva
 *
 */
public class BackgroundAction extends Thread {

	public enum Type {
		RESERVE,
		PUBLISH,
		UPLOAD_DATA
	}

	private Type type;
	private Catalogue catalogue;

	// publish stuff
	private PublishLevel pLevel;

	// reserve stuff
	private ReserveLevel rLevel;
	private String rDescription;

	private FormProgressBar progressBar;

	private PendingActionListener listener; // listen to events

	/**
	 * Initialize a {@link Publish} action
	 * @param catalogue the catalogue we want to publish
	 * @param level the publish level required
	 */
	public BackgroundAction( Catalogue catalogue, PublishLevel level ) {
		this.catalogue = catalogue;
		this.pLevel = level;
		this.type = Type.PUBLISH;
	}

	/**
	 * Initialize an {@link UploadData} action
	 * @param catalogue the catalogue we want to upload
	 */
	public BackgroundAction( Catalogue catalogue ) {
		this.catalogue = catalogue;
		this.type = Type.UPLOAD_DATA;
	}

	/**
	 * Initialize a {@link Reserve} action
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

	/**
	 * Start the pending action defined in the
	 * constructor and send it to the dcf.
	 */
	@Override
	public void run() {

		UploadCatalogueFile req = getService( type );

		// notify that we are ready to perform the action
		if ( listener != null )
			listener.requestPrepared( catalogue );

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

				// download the xml from the server if possible
				XmlUpdatesCreator xmlCreator = new XmlUpdatesCreator();
				File xmlFile = xmlCreator.downloadXml( catalogue );

				if ( xmlFile == null ) {
					System.err.println( "No .xml file found in " + xmlFile + " for " + catalogue );
					return;
				}

				// upload the xml file to the dcf using upload catalogue file
				pa = ( (UploadData) req ).uploadData( catalogue, xmlFile.getAbsolutePath() );
				break;

			default:
				System.err.println( "Type " + type + " not defined in BackgroundAction#run()" );
				return;
			}
		}
		catch ( SOAPException e ) {
			
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
	}


	/**
	 * Initialize an upload catalogue file action and return it
	 * @param actionType
	 * @return
	 */
	public UploadCatalogueFile getService ( Type actionType ) {

		UploadCatalogueFile req = null;

		// initialize the required service
		switch ( actionType ) {
		case RESERVE:
			req = new Reserve( Dcf.dcfType );
			break;
		case PUBLISH:
			req = new Publish( Dcf.dcfType );
			break;
		case UPLOAD_DATA:
			req = new UploadData( Dcf.dcfType );
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
	 * Set the progress bar which will be used if a new
	 * version of the catalogue is downloaded
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}
}
