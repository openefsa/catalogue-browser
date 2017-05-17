package dcf_log_util;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue_object.Catalogue;
import dcf_reserve_util.ForcedEditingListener;
import dcf_reserve_util.PendingReserve;
import dcf_reserve_util.ReserveFinishedListener;
import dcf_reserve_util.ReserveResult;
import dcf_reserve_util.ReserveSkeleton;
import dcf_webservice.DcfResponse;
import dcf_webservice.ReserveLevel;
import ui_progress_bar.FormProgressBar;

/**
 * This class should be used to retrieve the reserve
 * log, which is created when a reserve operations is sent
 * to the dcf and it is finished. This log says if the
 * reserve went fine or not. If the log is not found then
 * the dcf is busy and we need to make a polling.
 * @author avonva
 *
 */
public class LogRetriever extends ReserveSkeleton {
	
	// the pending reserve we want to use
	// to retrieve the log of the reserve operation
	private PendingReserve pendingReserve;
	
	// the response contained in the log
	private DcfResponse logResponse;
	
	// called when the reserve operation finishes
	private ReserveFinishedListener finishListener;
	
	// called just before reserving the catalogue in the db
	private Listener startReserveListener;
	
	// called when the dcf is busy
	private ForcedEditingListener forcedListener;
	
	// called when a new version is downloaded
	private Listener newVersionListener;
	
	/**
	 * Initialize a polling operation for the log
	 * @param pendingReserve
	 */
	public LogRetriever( PendingReserve pendingReserve ) {
		super( pendingReserve.getCatalogue(), pendingReserve.getReserveLevel() );
		this.pendingReserve = pendingReserve;
	}

	@Override
	public void run() {

		// since we are retrying to get the log document
		// related to the reserve operation we have already
		// sent, the editing of the catalogue should be
		// forced since we have already waited for 2 minutes
		
		Catalogue catalogue = pendingReserve.getCatalogue();
		ReserveLevel level = pendingReserve.getReserveLevel();
		String username = pendingReserve.getUsername();
		
		// force editing the catalogue
		if ( !catalogue.isForceEdit( username ) 
				&& level.greaterThan( ReserveLevel.NONE ) ) {
			
			catalogue.forceEdit( username, level );
		}
		
		super.run();
	}
	
	/**
	 * Set the listener which will be called when the reserve action finishes.
	 * @param finishListener
	 */
	public void setFinishListener(ReserveFinishedListener finishListener) {
		this.finishListener = finishListener;
	}
	
	
	/**
	 * Set the listener which is called when a new version of
	 * the catalogue is downloaded. If we find a different
	 * version, it means that we could not reserve the previous
	 * internal version of the catalogue, therefore we need
	 * to disable the force editing and warn the user that 
	 * a new version is available.
	 * @param newVersionListener
	 */
	public void setNewVersionListener(Listener newVersionListener) {
		this.newVersionListener = newVersionListener;
	}
	
	/**
	 * Set the listener called when the catalogue is being reserved
	 * (just before)
	 * @param startReserveListener
	 */
	public void setStartReserveListener(Listener startReserveListener) {
		this.startReserveListener = startReserveListener;
	}
	
	/**
	 * Set the listener which is called each time that the dcf is busy
	 * @param busyDcfListener
	 */
	public void setForceEditListener(ForcedEditingListener forcedListener) {
		this.forcedListener = forcedListener;
	}

	@Override
	public FormProgressBar getProgressBar() {
		return null;
	}

	@Override
	public void newVersionDownloaded(Catalogue newVersion) {}

	@Override
	public boolean canReserve( Catalogue catalogue, ReserveLevel reserveLevel ) {

		System.out.println ( "Retrying: " + pendingReserve );
		
		// retry getting the log of the pending reserve operation
		this.logResponse = pendingReserve.retry( forcedListener );
		
		System.out.println ( "Log found for:" + 
				pendingReserve + 
				" with response " + logResponse );

		// can reserve only if the log contains OK
		return logResponse == DcfResponse.OK;
	}

	@Override
	public void reserveStarted( Catalogue catalogue, 
			ReserveLevel reserveLevel, ReserveResult reserveLog ) {}

	@Override
	public void reserveFinished(Catalogue catalogue, ReserveLevel reserveLevel) {
			
		// call the finish listener if it was set
		if ( finishListener != null )
			finishListener.reserveFinished( catalogue, logResponse );
	}

	@Override
	public void lastVersionDownloadStarted() {
		
		if ( newVersionListener != null )
			newVersionListener.handleEvent( new Event() );
	}

	@Override
	public void reservingCatalogue(Catalogue catalogue, ReserveLevel reserveLevel) {
		
		// call the start reserve listener if it was set
		if ( startReserveListener != null )
			startReserveListener.handleEvent( new Event() );
	}
}
