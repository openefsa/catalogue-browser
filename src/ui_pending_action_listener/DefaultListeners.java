package ui_pending_action_listener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import dcf_log.LogNodesForm;
import dcf_pending_action.PendingAction;
import dcf_pending_action.PendingActionListener;
import dcf_pending_action.PendingPublish;
import dcf_pending_action.PendingReserve;
import dcf_pending_action.PendingActionStatus;
import dcf_pending_action.PendingUploadData;
import dcf_webservice.DcfResponse;
import dcf_webservice.ReserveLevel;
import messages.Messages;
import ui_main_panel.ShellLocker;
import ui_main_panel.UpdateableUI;
import utilities.GlobalUtil;

/**
 * This class contains all the actions and messages which are processed
 * and displayed depending on the pending actions states and response.
 * In fact, the user is notified if something important happens.
 * We put all this stuff here to make the code clearer and to reuse
 * the same listener in two different places (login button and
 * tools menu)
 * @author avonva
 *
 */
public class DefaultListeners {

	/**
	 * Get the default listeners for pending actions
	 * @param ui the ui which hosts the messages and needs possibly updates
	 * if some special conditions occur
	 * @return the default listener
	 */
	public static PendingActionListener getDefaultPendingListener ( final UpdateableUI ui ) {
		
		PendingActionListener listener = new PendingActionListener() {
			
			@Override
			public void connectionFailed( final Catalogue catalogue ) {
				ui.getShell().getDisplay().asyncExec( new Runnable() {

					@Override
					public void run() {
						
						GlobalUtil.showErrorDialog( ui.getShell(), 
								createPremessage ( catalogue ), 
								Messages.getString( "SOAP.BadConnection" ) );
						
						// remove the lock from the shell
						ShellLocker.removeLock( ui.getShell() );	
					}
				});
			}
			
			@Override
			public void statusChanged( final PendingAction pendingAction, 
					final PendingActionStatus status) {
				
				ui.getShell().getDisplay().asyncExec( new Runnable() {

					@Override
					public void run() {
						
						// make actions based on the status
						if ( pendingAction instanceof PendingReserve ) {
							
							PendingReserve pr = (PendingReserve) pendingAction;
							performStatusAction ( pr, status, ui );
						}
						
						warnStatus ( pendingAction, status, ui );
					}
				});
			}

			@Override
			public void responseReceived( final PendingAction pendingAction, 
					final DcfResponse response) {
				
				ui.getShell().getDisplay().asyncExec( new Runnable() {

					@Override
					public void run() {
						
						// show log errors
						showErrorsDialog ( ui, pendingAction );

						// warn the user of upload cat file status
						warnResponse( pendingAction, response, ui );
					}
				});
			}
			
			@Override
			public void requestSent( final PendingAction pendingAction, 
					String logCode ) {
				
				ui.getShell().getDisplay().asyncExec( new Runnable() {

					@Override
					public void run() {
						
						// remove the lock from the shell
						ShellLocker.removeLock( ui.getShell() );	
						
						// if we are unreserving the catalogue, as we start the
						// unreserve request we disable the editing mode
						// from the user interface
						if ( pendingAction instanceof PendingReserve && 
								((PendingReserve) pendingAction)
								.getReserveLevel()== ReserveLevel.NONE )
							ui.updateUI( ReserveLevel.NONE );
					}
				});
			}
			
			@Override
			public void requestPrepared( Catalogue catalogue ) {
				
				ui.getShell().getDisplay().asyncExec( new Runnable() {

					@Override
					public void run() {

						// lock the closure of the window since
						// we are making important things (i.e.
						// sending upload cat file requests or importing
						// new catalogue versions)
						ShellLocker.setLock( ui.getShell(), 
								Messages.getString( "MainPanel.CannotCloseTitle" ), 
								Messages.getString( "MainPanel.CannotCloseMessage" ) );
					}
				});
			}
		};
		
		return listener;
	}
	
	
	/**
	 * Warn the user of the dcf response related to reserve operations.
	 * @param pr
	 * @param response
	 * @param ui
	 */
	private static void warnResponse ( PendingAction pa, DcfResponse response, UpdateableUI ui ) {
		
		Catalogue catalogue = pa.getCatalogue();
		
		String title = createPremessage( catalogue );
		String msg = getResponseMessage( pa, response );
		int icon = SWT.ICON_INFORMATION;
		
		Shell shell = ui.getShell();
		
		switch ( response ) {
			case ERROR:
			case AP:
			case FORBIDDEN:
				icon = SWT.ICON_ERROR;
				break;
			case OK:
				icon = SWT.ICON_INFORMATION;
				break;
			default:
				break;
		}

		if ( msg != null )
			GlobalUtil.showDialog( shell, title, msg, icon );
	}
	
	/**
	 * Warn the user of the dcf response related to reserve operations.
	 * @param pr
	 * @param response
	 * @param ui
	 */
	private static void warnStatus ( PendingAction pa, PendingActionStatus status, UpdateableUI ui ) {
		
		Catalogue catalogue = pa.getCatalogue();
		
		String title = createPremessage( catalogue );
		String msg = getStatusMessage( pa, status );
		int icon = SWT.ICON_INFORMATION;
		
		Shell shell = ui.getShell();
		
		switch ( status ) {
			case ERROR:
			case INVALID_RESPONSE:
			case INVALID_VERSION:
				icon = SWT.ICON_ERROR;
				break;
				
			case IMPORTING_LAST_VERSION:
			case QUEUED:
				icon = SWT.ICON_WARNING;
				break;
			default:
				break;
		}

		if ( msg != null )
			GlobalUtil.showDialog( shell, title, msg, icon );
	}
	
	/**
	 * Get the current box of messages related to the current pending action
	 * @param pa
	 */
	private static PendingActionMessages getCurrentMessageBox ( PendingAction pa ) {
		
		PendingActionMessages pam = null;

		if ( pa instanceof PendingReserve ) {

			ReserveLevel level = ((PendingReserve) pa).getReserveLevel();

			// reserve or unreserve?
			if ( level.isNone() )
				pam = new PendingUnreserveMessages();
			else
				pam = new PendingReserveMessages();
		}

		else if ( pa instanceof PendingPublish )
			pam = new PendingPublishMessages();

		else if ( pa instanceof PendingUploadData )
			pam = new PendingUploadDataMessages();

		return pam;
	}
	
	/**
	 * Get the message which will be shown to the user according
	 * to the pending action and the dcf response.
	 * @param pa
	 * @param response
	 * @return
	 */
	private static String getResponseMessage ( PendingAction pa, DcfResponse response ) {
		
		String msg = null;
		PendingActionMessages pam = getCurrentMessageBox( pa );
		
		if ( pam != null )
			msg = pam.getResponseMessage( response );
		
		return msg;
	}
	
	/**
	 * Get the message which will be shown to the user according
	 * to the pending action status.
	 * @param pa
	 * @param response
	 * @return
	 */
	private static String getStatusMessage ( PendingAction pa, PendingActionStatus status ) {
		
		String msg = null;
		PendingActionMessages pam = getCurrentMessageBox( pa );
		
		if ( pam != null )
			msg = pam.getStatusMessage( status );
		
		return msg;
	}
	
	/**
	 * Perform some actions related to pending reserves
	 * and to its status
	 * @param pr
	 * @param status
	 * @param ui
	 */
	private static void performStatusAction ( final PendingReserve pr, 
			final PendingActionStatus status, final UpdateableUI ui ) {

		ui.getShell().getDisplay().asyncExec( new Runnable() {
			
			@Override
			public void run() {

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
				
				case FORCING_EDITING:
					// we are creating a new database => set the shell lock
					ShellLocker.setLock( ui.getShell(), 
							Messages.getString( "MainPanel.CannotCloseTitle" ), 
							Messages.getString( "MainPanel.CannotCloseMessage" ) );
					break;
					
				case QUEUED:
					
					// we have queued the pending reserve, so if
					// the process of forcing the catalogue editing
					// is finished => we remove the shell lock
					ShellLocker.removeLock( ui.getShell() );
					break;

				case COMPLETED:
					// process completed, remove lock
					ShellLocker.removeLock( ui.getShell() );
					
					break;
				
				case INVALID_RESPONSE:
				case INVALID_VERSION:
					// update the label of the catalogue since
					// we have invalidated the catalogue
					ui.updateUI( pr.getCatalogue() );
					break;
				default:
					break;
				}
			}
		});
	}

	/**
	 * Show the log errors table
	 * @param ui
	 * @param pa
	 */
	private static void showErrorsDialog ( UpdateableUI ui, PendingAction pa ) {
		
		// do not show anything if no log nodes are found
		if ( pa.getParsedLog().getLogNodesWithErrors().isEmpty() )
			return;
		
		LogNodesForm errors = new LogNodesForm ( ui.getShell(), pa.getParsedLog() );
		errors.display();
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
}
