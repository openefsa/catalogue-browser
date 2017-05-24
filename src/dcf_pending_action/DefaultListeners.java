package dcf_pending_action;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import catalogue_object.Catalogue;
import dcf_user.User;
import dcf_webservice.DcfResponse;
import dcf_webservice.ReserveLevel;
import global_manager.GlobalManager;
import messages.Messages;
import ui_main_panel.ShellLocker;
import ui_main_panel.UpdateableUI;
import utilities.GlobalUtil;

public class DefaultListeners {

	/**
	 * Get the default publish listener
	 * @param ui
	 * @return
	 */
	public static PendingActionListener getPublishListener ( final UpdateableUI ui ) {
		
		PendingActionListener listener = new PendingActionListener() {
			
			@Override
			public void statusChanged(PendingAction pendingAction, 
					PendingReserveStatus status) {}
			
			@Override
			public void responseReceived( final PendingAction pendingAction, 
					final DcfResponse response) {
				
				ui.getShell().getDisplay().asyncExec( new Runnable() {

					@Override
					public void run() {
						// if pending publish
						if ( pendingAction instanceof PendingPublish ) {

							PendingPublish pp = (PendingPublish) pendingAction;

							// open the new version if ok response
							if ( response == DcfResponse.OK ) {
								openNewVersion( pp.getCatalogue() );
							}

							// warn the user of publish status
							warnPublishResponse( pp, response, ui );
						}
					}
				});
			}

			@Override
			public void requestSent( final PendingAction pendingAction, String logCode) {
				
				ui.getShell().getDisplay().asyncExec( new Runnable() {
					@Override
					public void run() {
						
						// remove shell lock if we start sending
						// (we have the log code at this point)
						ShellLocker.removeLock( ui.getShell() );

						Catalogue catalogue = pendingAction.getCatalogue();
						String preMessage = createPremessage( catalogue );
						
						// warn that the reserve operation was sent
						GlobalUtil.showDialog( ui.getShell(), 
								Messages.getString( "Publish.PublishStartedTitle" ),
								preMessage + Messages.getString( "Publish.PublishStartedMessage" ),
								SWT.ICON_INFORMATION );
					}
				});
			}
			
			@Override
			public void requestPrepared() {

				ui.getShell().getDisplay().asyncExec( new Runnable() {

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
		};
		
		return listener;
	}
	
	
	/**
	 * Warn the user of the dcf response related to publish operations.
	 * @param pr
	 * @param response
	 * @param ui
	 */
	private static void warnPublishResponse ( PendingPublish pp, DcfResponse response, UpdateableUI ui ) {
		
		Catalogue catalogue = pp.getCatalogue();
		
		String title = null;
		String msg = createPremessage( catalogue );
		
		Shell shell = ui.getShell();
		
		switch ( response ) {
			case ERROR:
				title = Messages.getString( "Publish.ErrorTitle" );
				msg += Messages.getString( "Publish.ErrorMessage" );
				break;
			case AP:
				title = Messages.getString( "Publish.ApTitle" );
				msg += Messages.getString( "Publish.ApMessage" );
				break;
			case FORBIDDEN:
				title = Messages.getString( "Publish.MinorErrorTitle" );
				msg += Messages.getString( "Publish.MinorErrorMessage" );
				break;
			case OK:
				title = Messages.getString( "Publish.OkTitle" );
				msg += Messages.getString( "Publish.OkMessage" );
				break;
			default:
				break;
		}

		// show the dialog
		GlobalUtil.showDialog( shell, title, msg,
				SWT.ICON_INFORMATION );
	}
	
	/**
	 * Get the default listener for reserve operations
	 * @param ui the UpdateableUi which will be used to show the
	 * message boxes and which will be updated if needed
	 * @return
	 */
	public static PendingActionListener getReserveListener( final UpdateableUI ui ) {

		PendingActionListener listener = new PendingActionListener() {

			@Override
			public void requestPrepared() {
				
				ui.getShell().getDisplay().asyncExec( new Runnable() {
					
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
			public void requestSent( final PendingAction pendingAction, 
					String logCode ) {

				ui.getShell().getDisplay().asyncExec( new Runnable() {
					
					@Override
					public void run() {
						
						// remove the lock from the shell
						ShellLocker.removeLock( ui.getShell() );
						
						PendingReserve pr = (PendingReserve) pendingAction;
						Catalogue catalogue = pr.getCatalogue();
						String preMessage = createPremessage( catalogue );
						
						// warn that the reserve operation was sent
						GlobalUtil.showDialog( ui.getShell(), 
								Messages.getString( "Reserve.ReserveStartedTitle" ),
								preMessage + Messages.getString( "Reserve.ReserveStartedMessage" ),
								SWT.ICON_INFORMATION );
						
						// if we are unreserving the catalogue, as we start the
						// unreserve request we disable the editing mode
						// from the user interface
						if ( pr.getReserveLevel() == ReserveLevel.NONE )
							ui.updateUI( ReserveLevel.NONE );
					}
				});
			}
			
			@Override
			public void responseReceived( final PendingAction pendingAction, 
					final DcfResponse response ) {

				ui.getShell().getDisplay().asyncExec( new Runnable() {
					
					@Override
					public void run() {
						
						// remove the lock from the shell
						ShellLocker.removeLock( ui.getShell() );

						// if pending reserve
						if ( pendingAction instanceof PendingReserve ) {
							
							PendingReserve pr = (PendingReserve) pendingAction;
							
							warnReserveResponse( pr, response, ui );
						}
					}
				});
			}
			
			@Override
			public void statusChanged( final PendingAction pendingAction, 
					final PendingReserveStatus status) {

				ui.getShell().getDisplay().asyncExec( new Runnable() {
					
					@Override
					public void run() {
						
						PendingReserve pr = (PendingReserve) pendingAction;
						Catalogue catalogue = pr.getCatalogue();
						ReserveLevel level = pr.getReserveLevel();

						String preMessage = createPremessage ( catalogue );
						
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
							
							if ( pr.getResponse() == DcfResponse.OK ) {

								String title = Messages.getString( "Reserve.OkTitle" );
								String msg = preMessage;
								if ( level.greaterThan( ReserveLevel.NONE ) )
									msg += Messages.getString( "Reserve.OkMessage" );
								else
									msg += Messages.getString( "Unreserve.OkMessage" );

								// show the dialog
								GlobalUtil.showDialog( ui.getShell(), title, msg,
										SWT.ICON_INFORMATION );
							}
							
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
						
						// Warn user of the performed actions
						warnReserveStatus ( ui, status, preMessage, level );
					}
				});
			}
		};
		
		return listener;
	}

	/**
	 * Check if the catalogue has the same code of the current catalogue
	 * or not
	 * @param catalogue
	 * @return
	 */
	private static boolean sameCodeOfCurrent( Catalogue catalogue ) {
		
		Catalogue current = 
				GlobalManager.getInstance().getCurrentCatalogue();
		
		// open the new version of the catalogue if it was
		// created by the pending reserve (only if a previous
		// version of the catalogue is already opened in the
		// browser)
		return ( current != null && 
				current.equals( catalogue ) );
	}
	
	/**
	 * Open the new version of the catalogue only if
	 * there is an older version already opened in the
	 * browser
	 * @param newVersion
	 */
	private static void openNewVersion( Catalogue newVersion ) {
		
		// open the new version of the catalogue if it was
		// created by the pending reserve (only if a previous
		// version of the catalogue is already opened in the
		// browser)
		// do not open the new version if we have a forced version
		// since if we are editing
		if ( sameCodeOfCurrent ( newVersion ) 
				&& !newVersion.isForceEdit( User.getInstance().getUsername() ) ) {
			newVersion.open();
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
	 */
	private static void warnReserveStatus ( UpdateableUI ui, 
			PendingReserveStatus status, String preMessage, ReserveLevel level ) {
		
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
		
			// warn user that we cannot use this version
		case INVALID_VERSION:
			
			// update catalogue label
			ui.updateUI( null );
			
			GlobalUtil.showDialog( shell, 
					Messages.getString( "Reserve.InvalidVersionTitle" ),
					preMessage + Messages.getString( "Reserve.InvalidVersionMessage" ),
					SWT.ICON_ERROR );
			break;
			
		case INVALID_RESPONSE:
			
			// update catalogue label
			ui.updateUI( null );
			
			GlobalUtil.showDialog( shell, 
					Messages.getString( "Reserve.InvalidResponseTitle" ),
					preMessage + Messages.getString( "Reserve.InvalidResponseMessage" ),
					SWT.ICON_ERROR );
			break;
			
		case QUEUED:
			
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
			
		case COMPLETED:
			break;
			
		default:
			break;
		}
	}
	
	/**
	 * Warn the user of the dcf response related to reserve operations.
	 * @param pr
	 * @param response
	 * @param ui
	 */
	private static void warnReserveResponse ( PendingReserve pr, DcfResponse response, UpdateableUI ui ) {
		
		Catalogue catalogue = pr.getCatalogue();
		
		String title = null;
		String msg = createPremessage( catalogue );
		
		boolean show = true;
		
		Shell shell = ui.getShell();
		
		switch ( response ) {
			case ERROR:
				title = Messages.getString( "Reserve.ErrorTitle" );
				msg += Messages.getString( "Reserve.ErrorMessage" );
				break;
			case AP:
				title = Messages.getString( "Reserve.ApTitle" );
				msg += Messages.getString( "Reserve.ApMessage" );
				break;
			case FORBIDDEN:
				title = Messages.getString( "Reserve.MinorErrorTitle" );
				msg += Messages.getString( "Reserve.MinorErrorMessage" );
				break;
			default:
				show = false;
				break;
		}

		// show the dialog
		if ( show )
			GlobalUtil.showDialog( shell, title, msg,
					SWT.ICON_INFORMATION );
	}
}
