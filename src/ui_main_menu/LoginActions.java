package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import catalogue_generator.ThreadFinishedListener;
import dcf_manager.Dcf;
import dcf_pending_action.PendingActionListener;
import dcf_pending_action.PendingPublish;
import dcf_pending_action.PendingReserve;
import dcf_pending_action.PendingUploadData;
import dcf_pending_action.PendingXmlDownload;
import dcf_user.User;
import messages.Messages;
import progress_bar.FormProgressBar;
import ui_main_panel.ShellLocker;
import utilities.GlobalUtil;

public class LoginActions {

	/**
	 * Start all the processes after the user is logged in
	 * @param shell
	 * @param listener
	 */
	public static void startLoggedThreads(final Shell shell, 
			final PendingActionListener listener, final Listener userLevelListener) {
		
		if(!User.getInstance().isLoggedIn())
			return;
		
		// Check catalogues updates and 
		// user access level
		Dcf dcf = new Dcf();

		// start checking updates for the catalogues
		dcf.checkUpdates( new Listener() {

			@Override
			public void handleEvent(Event arg0) {}
		});

		dcf.refreshDataCollections();

		// progress bar for the user level
		// Note that the progress bar does not block the user interaction
		final FormProgressBar progressBar = new FormProgressBar(shell, 
				Messages.getString( "Login.UserLevelProgressBarTitle" ),
				false, SWT.TITLE );

		dcf.setProgressBar( progressBar );

		ShellLocker.setLock(shell, Messages.getString("MainPanel.CannotCloseTitle"), 
				Messages.getString("MainPanel.CannotCloseMessage"));
		
		dcf.setUserLevel( new ThreadFinishedListener() {

			@Override
			public void finished(Thread thread, final int code, Exception e) {
				
				if (shell.isDisposed())
					return;
				
				shell.getDisplay().asyncExec( new Runnable() {

					@Override
					public void run() {

						ShellLocker.removeLock(shell);
						
						progressBar.close();

						// if correct
						if ( code == ThreadFinishedListener.OK || 
								code == ThreadFinishedListener.ERROR ) {

							if (userLevelListener != null)
								userLevelListener.handleEvent(null);
							
							// once we have finished checking the user
							// level we start with the pending reserves
							// we do this here to avoid concurrence
							// editing of the database
							startPendingActions(shell, listener);

							String title = Messages.getString( "Login.PermissionTitle" );
							String msg;

							if ( User.getInstance().isCatManager() )
								msg = Messages.getString("Login.CatalogueManagerMessage");
							else
								msg = Messages.getString("Login.DataProviderMessage");

							GlobalUtil.showDialog(shell, title, msg, SWT.ICON_INFORMATION );

						}
						else { // errors
							if (userLevelListener != null)
								userLevelListener.handleEvent(null);
							GlobalUtil.showErrorDialog(shell, 
									Messages.getString("ExportCatalogue.ErrorTitle"), 
									Messages.getString("ExportCatUsers.ErrorMessage"));
						}
					}
				});
			}
		});
	}
	
	/**
	 * Start all the pending actions in the database
	 * @return
	 */
	public static void startPendingActions(Shell shell, PendingActionListener listener) {

		Dcf dcf = new Dcf();

		// progress bar for pending reserve
		FormProgressBar bar = new FormProgressBar( shell, 
				Messages.getString("Reserve.NewInternalTitle") );
		
		bar.setLocation( bar.getLocation().x, bar.getLocation().y + 170 );
		
		dcf.setProgressBar( bar );
		
		// start reserve actions
		dcf.startPendingActions( PendingReserve.TYPE, listener );

		
		// progress bar for pending publish
		FormProgressBar bar2 = new FormProgressBar( shell, 
				Messages.getString("Publish.DownloadPublished") );
		
		bar.setLocation( bar2.getLocation().x, bar2.getLocation().y + 170 );
		
		dcf.setProgressBar( bar2 );
		
		// start publish actions
		dcf.startPendingActions( PendingPublish.TYPE, listener );
		
		// start upload data actions
		dcf.startPendingActions( PendingUploadData.TYPE, listener );

		// start upload data actions
		dcf.startPendingActions( PendingXmlDownload.TYPE, listener );
	}
}
