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
 * the class refresh all the methods while the user log in/out from the
 * DCF/Openapi
 * 
 * @author shahaal
 *
 */
public class LoginActions {

	/**
	 * Start all the processes after the user is logged in
	 * 
	 * @param shell
	 * @param listener
	 */
	public static void startLoginThreads(final Shell shell, final Listener userLevelListener) {

		User user = User.getInstance();

		// if not logged at all (dcf/openapi) return
		if (!user.isLoggedIn() && !user.isLoggedInOpenAPI())
			return;

		// Check catalogues updates and user access level
		Dcf dcf = new Dcf();

		// start checking updates for the catalogues
		dcf.checkUpdates(new Listener() {

			@Override
			public void handleEvent(Event arg0) {
			}
		});

		dcf.refreshDataCollections();

		// progress bar for the user level (block the user input)
		final FormProgressBar progressBar = new FormProgressBar(shell,
				Messages.getString("Login.UserLevelProgressBarTitle"), false, SWT.TITLE | SWT.APPLICATION_MODAL);

		dcf.setProgressBar(progressBar);

		ShellLocker.setLock(shell, Messages.getString("MainPanel.CannotCloseTitle"),
				Messages.getString("MainPanel.CannotCloseMessage"));

		dcf.setUserLevel(new ThreadFinishedListener() {

			@Override
			public void finished(Thread thread, final int code, Exception e) {

				if (shell.isDisposed())
					return;

				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						ShellLocker.removeLock(shell);

						progressBar.close();

						String title = null, msg = null;

						if (code == ThreadFinishedListener.OK || code == ThreadFinishedListener.ERROR) {
							try {
								User.getInstance().startPendingRequests();
							} catch (SQLException | IOException e) {
								title = Messages.getString("FormDCFLogin.ErrorTitle");
								msg = Messages.getString("FormDCFLogin.WrongCredentialMessage");
								GlobalUtil.showDialog(shell, title, msg, SWT.ICON_INFORMATION);
								e.printStackTrace();
							}
						} else {
							if (userLevelListener != null)
								userLevelListener.handleEvent(null);

							title = Messages.getString("ExportCatalogue.WarningTitle");
							msg = Messages.getString("ExportCatUsers.WarningMessage");
							GlobalUtil.showDialog(shell, title, msg, SWT.ICON_INFORMATION);
						}

					}
				});
			}
		});
	}
}
