package dcf_reserve_util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import catalogue_object.Catalogue;
import dcf_reserve_util.PendingReserve.PendingPriority;
import dcf_webservice.DcfResponse;
import dcf_webservice.ReserveLevel;
import global_manager.GlobalManager;
import messages.Messages;
import ui_main_panel.ShellLocker;
import ui_main_panel.UpdateableUI;
import utilities.GlobalUtil;

public class DefaultListeners {

	/**
	 * Get the default listener for reserve operations
	 * @param ui the UpdateableUi which will be used to show the
	 * message boxes and which will be updated if needed
	 * @return
	 */
	public static ReserveListener getReserveListener( final UpdateableUI ui ) {

		ReserveListener listener = new ReserveListener() {

			@Override
			public void requestPrepared() {
				
				ui.getShell().getDisplay().syncExec( new Runnable() {
					
					@Override
					public void run() {
						
						// lock the closure of the window since
						// we are making important things (i.e.
						// sending reserve requests or importing
						// new catalogue versions)
						ShellLocker.setLock( ui.getShell(), 
								Messages.getString( "MainPanel.CannotCloseTitle" ), 
								Messages.getString( "MainPanel.CannotCloseMessage" ) );
					}
				});
			}

			@Override
			public void requestSent( final PendingReserve pendingReserve, 
					String logCode ) {

				ui.getShell().getDisplay().syncExec( new Runnable() {
					
					@Override
					public void run() {
						
						// remove the lock from the shell
						ShellLocker.removeLock( ui.getShell() );
						
						Catalogue catalogue = pendingReserve.getCatalogue();
						String preMessage = "";
						
						if ( catalogue != null )
							preMessage = catalogue.getCode() + " - " 
									+ catalogue.getVersion() + ": ";
						
						// warn that the reserve operation was sent
						GlobalUtil.showDialog( ui.getShell(), 
								Messages.getString( "Reserve.ReserveStartedTitle" ),
								preMessage + Messages.getString( "Reserve.ReserveStartedMessage" ),
								SWT.ICON_INFORMATION );
						
						// if we are unreserving the catalogue, as we start the
						// unreserve request we disable the editing mode
						// from the user interface
						if ( pendingReserve.getReserveLevel() == ReserveLevel.NONE )
							ui.updateUI( ReserveLevel.NONE );
					}
				});
			}
			
			@Override
			public void responseReceived( final PendingReserve pendingReserve, 
					final DcfResponse response) {

				ui.getShell().getDisplay().syncExec( new Runnable() {
					
					@Override
					public void run() {
						
						Catalogue catalogue = pendingReserve.getCatalogue();
						ReserveLevel level = pendingReserve.getReserveLevel();
						
						// remove the lock from the shell
						ShellLocker.removeLock( ui.getShell() );

						// update the UI since the reserve level
						// is potentially changed
						ui.updateUI( level );

						// show dialog
						warnOfResponse( ui, catalogue, response, level );
					}
				});
			}
			
			@Override
			public void statusChanged( final PendingReserve pendingReserve, 
					final PendingReserveStatus status) {

				ui.getShell().getDisplay().syncExec( new Runnable() {
					
					@Override
					public void run() {

						// Warn user of the performed actions
						warnOfStatus ( ui, status );
						
						switch ( status ) {
						
						case OLD_VERSION:
							
							// lock the closure of the window since
							// we are importing a new catalogue version
							ShellLocker.setLock( ui.getShell(), 
									Messages.getString( "Reserve.CannotCloseTitle" ), 
									Messages.getString( "Reserve.CannotCloseMessage" ) );
							break;
							
							// remove shell lock if sending, here we can
							// try how many times we want
						case SENDING:
							
							ShellLocker.removeLock( ui.getShell() );
							break;
							
						case COMPLETED:

							// open the catalogue if the operation was a reserve operation
							// and if we have completed the process
							if ( pendingReserve.getReserveLevel()
									.greaterThan( ReserveLevel.NONE ) ) {

								Catalogue current = 
										GlobalManager.getInstance().getCurrentCatalogue();

								// open the catalogue only if there is already another
								// catalogue opened, and it is the same catalogue in terms
								// of code (i.e. only the version is different)
								// in this way we open the catalogue only if we are working
								// with a previous version of it
								if ( current != null && 
										current.equals( pendingReserve.getCatalogue() ) )
									pendingReserve.getCatalogue().open();
							}

							break;
							
						default:
							break;
						}
					}
				});
			}

			@Override
			public void queued( final PendingReserve pendingReserve ) {

				ui.getShell().getDisplay().syncExec( new Runnable() {
					
					@Override
					public void run() {
						
						// no busy notification if low priority
						// it is an hidden process
						if ( pendingReserve.getPriority() == PendingPriority.LOW )
							return;
						
						Catalogue catalogue = pendingReserve.getCatalogue();
						ReserveLevel level = pendingReserve.getReserveLevel();
						
						// update the UI based on the forced reserve level
						ui.updateUI( level );
						
						// the dcf is busy => warn the user
						warnOfResponse( ui, catalogue, DcfResponse.BUSY, level );
					}
				});
			}

			@Override
			public void internalVersionChanged(PendingReserve pendingReserve, Catalogue newVersion) {}


		};
		
		return listener;
	}
	
	
	/**
	 * Warn the user based on the reserve log status
	 * the correct reserve and not reserving are not included
	 * since are covered by the start listener of the reserve log
	 * (Otherwise they would have been called two times)
	 * @param reserveLog
	 */
	private static void warnOfStatus ( UpdateableUI ui, PendingReserveStatus status ) {
		
		Shell shell = ui.getShell();
		
		// Warn user of the performed actions

		switch ( status ) {
		case ERROR:
			GlobalUtil.showErrorDialog( shell, 
					Messages.getString( "Reserve.GeneralErrorTitle" ),
					Messages.getString( "Reserve.GeneralErrorMessage" ) );
			break;
			
		case MINOR_FORBIDDEN:
			GlobalUtil.showErrorDialog( shell, 
					Messages.getString( "Reserve.MinorErrorTitle" ),
					Messages.getString( "Reserve.MinorErrorMessage" ) );
			break;
			
		case OLD_VERSION:
			
			// Warn user that a new version of the catalogue
			// is being downloaded
			GlobalUtil.showDialog(shell, 
					Messages.getString( "Reserve.OldTitle" ),
					Messages.getString( "Reserve.OldMessage" ), 
					SWT.ICON_WARNING );
			break;
		default:
			break;
		}
	}
	
	/**
	 * Warn the user based on the dcf response
	 * @param catalogue additional information to show, set to null otherwise
	 * @param response
	 * @param level the reserve level we wanted, set to null if not needed
	 */
	private static void warnOfResponse ( UpdateableUI ui, Catalogue catalogue, 
			DcfResponse response, ReserveLevel level ) {
		
		Shell shell = ui.getShell();
		
		String preMessage = "";
		
		if ( catalogue != null )
			preMessage = catalogue.getCode() + " - " 
					+ catalogue.getVersion() + ": ";
		
		switch ( response ) {
		
		// if wrong reserve operation notify the user
		case ERROR:
			
			GlobalUtil.showErrorDialog( shell, 
					Messages.getString( "Reserve.ErrorTitle" ),
					preMessage + Messages.getString( "Reserve.ErrorMessage" ) );
			break;
		
		case BUSY:
			
			// warn user according to the reserve level
			
			if ( level != null && level.greaterThan( ReserveLevel.NONE ) ) {
				
				GlobalUtil.showDialog( shell, 
						Messages.getString( "Reserve.BusyTitle" ),
						preMessage + Messages.getString( "Reserve.BusyMessage" ),
						SWT.ICON_WARNING );
			}
			else {

				if ( level != null ) {
					
					GlobalUtil.showDialog( shell, 
							Messages.getString( "Unreserve.BusyTitle" ),
							preMessage + Messages.getString( "Unreserve.BusyMessage" ),
							SWT.ICON_WARNING );
				}
			}
			break;
			
		// if the catalogue is already reserved
		case NO:
			
			GlobalUtil.showErrorDialog( shell, 
					Messages.getString( "Reserve.NoTitle" ),
					preMessage + Messages.getString( "Reserve.NoMessage" ) );
			break;

		// if everything went ok
		case OK:
			
			String message;
			if ( level.greaterThan( ReserveLevel.NONE ) )
				message = Messages.getString( "Reserve.OkMessage" );
			else
				message = Messages.getString( "Unreserve.OkMessage" );
			
			GlobalUtil.showDialog( shell, 
					Messages.getString( "Reserve.OkTitle" ),
					preMessage + message,
					SWT.ICON_INFORMATION );
			
			break;
		default:
			break;
		}
	}
}
