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

						String preMessage = createPremessage ( catalogue );
						
						// show dialog
						warnOfResponse( ui, response, level, preMessage );
					}
				});
			}
			
			@Override
			public void statusChanged( final PendingReserve pendingReserve, 
					final PendingReserveStatus status) {

				ui.getShell().getDisplay().syncExec( new Runnable() {
					
					@Override
					public void run() {
						
						String preMessage = createPremessage ( pendingReserve.getCatalogue() );
						
						// Warn user of the performed actions
						warnOfStatus ( ui, status, preMessage );
						
						switch ( status ) {
						
						case IMPORTING_LAST_VERSION:
							
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
								
								Catalogue catalogue = pendingReserve.getCatalogue();
								
								Catalogue current = 
										GlobalManager.getInstance().getCurrentCatalogue();

								// open the new version of the catalogue if it was
								// created by the pending reserve (only if a previous
								// version of the catalogue is already opened in the
								// browser)
								if ( current != null && 
										current.equals( catalogue ) &&
										current.isOlder( catalogue ) )
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
						
						String preMessage = createPremessage ( catalogue );
						
						// the dcf is busy => warn the user
						warnDcfBusy( ui, level, preMessage );
					}
				});
			}

			@Override
			public void internalVersionChanged(PendingReserve pendingReserve, Catalogue newVersion) {}


		};
		
		return listener;
	}
	
	/**
	 * Warn that the dcf is busy
	 * @param ui
	 * @param catalogue
	 * @param level
	 */
	private static void warnDcfBusy ( UpdateableUI ui, ReserveLevel level, String preMessage ) {

		Shell shell = ui.getShell();
		
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
	}
	

	/**
	 * Create a default pre message for warning using
	 * catalogue information
	 * @param catalogue
	 * @return
	 */
	private static String createPremessage ( Catalogue catalogue ) {
		
		String preMessage = null;
		
		if ( catalogue != null )
			preMessage = catalogue.getCode() + " - " 
					+ catalogue.getVersion() + ": ";
		
		return preMessage;
	}
	
	/**
	 * Warn the user based on the reserve log status
	 * the correct reserve and not reserving are not included
	 * since are covered by the start listener of the reserve log
	 * (Otherwise they would have been called two times)
	 * @param reserveLog
	 */
	private static void warnOfStatus ( UpdateableUI ui, PendingReserveStatus status, String preMessage ) {
		
		Shell shell = ui.getShell();
		
		// Warn user of the performed actions

		switch ( status ) {
		case ERROR:
			GlobalUtil.showErrorDialog( shell, 
					Messages.getString( "Reserve.GeneralErrorTitle" ),
					preMessage + Messages.getString( "Reserve.GeneralErrorMessage" ) );
			break;
			
		case IMPORTING_LAST_VERSION:
			
			// Warn user that a new version of the catalogue
			// is being downloaded
			GlobalUtil.showDialog(shell, 
					Messages.getString( "Reserve.OldTitle" ),
					preMessage + Messages.getString( "Reserve.OldMessage" ), 
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
	private static void warnOfResponse ( UpdateableUI ui, DcfResponse response, ReserveLevel level,
			String preMessage ) {
		
		Shell shell = ui.getShell();

		switch ( response ) {
		
		// if wrong reserve operation notify the user
		case ERROR:
			
			GlobalUtil.showErrorDialog( shell, 
					Messages.getString( "Reserve.ErrorTitle" ),
					preMessage + Messages.getString( "Reserve.ErrorMessage" ) );
			break;
			
		case MINOR_FORBIDDEN:
			GlobalUtil.showErrorDialog( shell, 
					Messages.getString( "Reserve.MinorErrorTitle" ),
					Messages.getString( "Reserve.MinorErrorMessage" ) );
			break;
			
		// if the catalogue is already reserved
		case AP:
			
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
