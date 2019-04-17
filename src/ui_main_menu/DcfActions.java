package ui_main_menu;

import java.io.IOException;
import java.sql.SQLException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import catalogue_generator.ThreadFinishedListener;
import dcf_manager.Dcf;
import dcf_user.User;
import messages.Messages;
import progress_bar.FormProgressBar;
import ui_main_panel.ShellLocker;
import utilities.GlobalUtil;

/**
 * the class refresh all the methods while the user log in/out from the DCF/Openapi
 * @author shahaal
 *
 */
public class DcfActions {

	/**
	 * Start all the processes after the user is logged in
	 * @param shell
	 * @param listener
	 */
	public static void startLoginThreads(final Shell shell, final Listener userLevelListener) {
		
		//if not logged at all (dcf and openapi) then return
		if(!User.getInstance().isLoggedIn()&&!User.getInstance().isLoggedInOpenAPI())
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
							
							try {
								User.getInstance().startPendingRequests();
							} catch (SQLException | IOException e) {
								e.printStackTrace();
							}

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
							GlobalUtil.showDialog(shell, 
									Messages.getString("ExportCatalogue.WarningTitle"), 
									Messages.getString("ExportCatUsers.WarningMessage"),
									SWT.ICON_INFORMATION);
						}
					}
				});
			}
		});
	}
}
