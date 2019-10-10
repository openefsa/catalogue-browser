package ui_main_panel;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

import catalogue.Catalogue;
import dcf_log.DcfResponse;
import dcf_pending_request.PendingRequestActions;
import dcf_pending_request.PendingRequestActionsListener;
import import_catalogue.ImportException;
import pending_request.IPendingRequest;
import pending_request.PendingRequestStatus;
import pending_request.PendingRequestStatusChangedEvent;
import pending_request.PendingRequestWorker;
import sas_remote_procedures.XmlChangesService;
import soap.UploadCatalogueFileImpl;
import soap.UploadCatalogueFileImpl.PublishLevel;
import soap.UploadCatalogueFileImpl.ReserveLevel;
import ui_main_panel.IBrowserPendingRequestWorker.PendingRequestWorkerListener.WorkerStatus;

/**
 * Worker which starts several {@link IPendingRequest} and receives
 * back a feedback to update the main user interface of the application
 * and database contents.
 * @author avonva
 *
 */
public class BrowserPendingRequestWorker extends PendingRequestWorker implements IBrowserPendingRequestWorker {

	private static final Logger LOGGER = LogManager.getLogger(BrowserPendingRequestWorker.class);
	private static BrowserPendingRequestWorker updater;
	
	private WorkerStatus status;
	private PendingRequestActions actions;
	private Collection<PendingRequestWorkerListener> listeners;
	
	private BrowserPendingRequestWorker() {
		super();  // IMPORTANT, otherwise no notification will be received from requests
		this.status = WorkerStatus.WAITING;
		this.actions = new PendingRequestActions();
		this.listeners = new ArrayList<>();
	}
	
	public static BrowserPendingRequestWorker getInstance() {
		
		if (updater == null) {
			updater = new BrowserPendingRequestWorker();
			updater.execute();  // launch the worker in background
		}
		
		return updater;
	}
	
	/**
	 * Add a listener to the status changes of pending requests
	 */
	public void addListener(PendingRequestWorkerListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Add a listener to the actions which are performed in background
	 * due to the pending requests
	 * @param listener
	 */
	public void addActionListener(PendingRequestActionsListener listener) {
		actions.addListener(listener);
	}
	
	private void setStatus(WorkerStatus status) {
		this.status = status;
		for (PendingRequestWorkerListener l : listeners)
			l.workerStatusChanged(status);
	}
	
	public WorkerStatus getStatus() {
		return status;
	}
	
	@Override
	public void statusChanged(PendingRequestStatusChangedEvent event) {
		
		LOGGER.debug("Triggered event=" + event);
		
		setStatus(WorkerStatus.ONGOING);
		
		// here manages database stuff
		
		IPendingRequest request = event.getPendingRequest();
		PendingRequestStatus status = event.getNewStatus();
		
		String catalogueCode = request.getData()
				.get(UploadCatalogueFileImpl.CATALOGUE_CODE_DATA_KEY);
		
		String username = request.getRequestor().getUsername();
		
		// update the graphics
		switch(status) {
		case QUEUED:
			switch(request.getType()) {
			case IPendingRequest.TYPE_RESERVE_MINOR:
			case IPendingRequest.TYPE_RESERVE_MAJOR:
				
				// force the catalogue to the reserve level required
				ReserveLevel level = ReserveLevel.fromRequestType(request.getType());
				actions.forceReserve(catalogueCode, level, username);
				break;
			}
			break;
			
		case COMPLETED:
			
			// now response is available
			DcfResponse response = request.getResponse();
			
			switch(request.getType()) {
			case IPendingRequest.TYPE_RESERVE_MINOR:
			case IPendingRequest.TYPE_RESERVE_MAJOR:
				
				if (response == DcfResponse.OK) {
					
					// reserve the catalogue in the database
					String reservationNote = request.getData()
							.get(UploadCatalogueFileImpl.RESERVE_NOTE_DATA_KEY);
	
					ReserveLevel level = ReserveLevel.fromRequestType(request.getType());
					
					try {
						Catalogue lastVersion = actions.getLastVersion(catalogueCode);
						
						String dcfInternalVersion = event.getPendingRequest().getLog().getCatalogueVersion();
						
						// confirm version for temporary versions or simply create
						// a new internal version for standard versions
						if (lastVersion.getCatalogueVersion().isForced()) {
							actions.reserveCompletedAfterForcedReserve(catalogueCode, dcfInternalVersion, 
									level, reservationNote, username);
						}
						else
							actions.reserveCompletedBeforeForcing(catalogueCode, dcfInternalVersion, 
									level, reservationNote, username);
						
					} catch (SOAPException | TransformerException | IOException | XMLStreamException
							| OpenXML4JException | SAXException | SQLException | ImportException e) {
						e.printStackTrace();
					}
				}
				else {
					Catalogue lastVersion = actions.getLastVersion(catalogueCode);
					
					// forced reserve failed
					if (lastVersion.getCatalogueVersion().isForced())
						actions.reserveFailedAfterForcedReserve(catalogueCode);
					
					// else reserve simply failed
				}

				break;
				
			case IPendingRequest.TYPE_PUBLISH_MINOR:
			case IPendingRequest.TYPE_PUBLISH_MAJOR:
				
				if (response == DcfResponse.OK) {
					PublishLevel level = PublishLevel.fromRequestType(request.getType());
					actions.publishCompleted(catalogueCode, level);
				}
				
				break;
				
			case IPendingRequest.TYPE_UNRESERVE:
				
				if (response == DcfResponse.OK) {
					actions.unreserveCompleted(catalogueCode, username);
				}
				
				break;
			case XmlChangesService.TYPE_UPLOAD_XML_DATA:
				if (response == DcfResponse.OK) {
					
					String catId = event.getPendingRequest().getData()
							.get(XmlChangesService.CATALOGUE_ID_DATA_KEY);
					
					int catalogueId = Integer.valueOf(catId);

					try {
						actions.uploadXmlChangesCompleted(catalogueId);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				break;
			}
			break;
			
		case DOWNLOADING:
		case ERROR:
		case WAITING:
			// anything to do
			break;
		default:
			break;
		}
		
		// listeners for ui components (after updating database!)
		for (PendingRequestWorkerListener listener : listeners)
			listener.statusChanged(event);
		
		setStatus(WorkerStatus.WAITING);
	}
}
