package ui_main_panel;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import catalogue_browser_dao.DatabaseManager;
import catalogue_generator.ThreadFinishedListener;
import converter.ExceptionConverter;
import dcf_user.ReauthThread;
import dcf_user.User;
import instance_checker.InstanceChecker;
import messages.Messages;
import soap.DetailedSOAPException;
import ui_main_menu.LoginActions;
import utilities.GlobalUtil;

/**
 * Entry point for the Catalogue Browser application. The user interface and the
 * database are started here.
 * 
 * @author avonva
 * @author shahaal
 */
public class CatalogueBrowserMain {

	private static final Logger LOGGER = LogManager.getLogger(CatalogueBrowserMain.class);

	/**
	 * Main, catalogue browser entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			CatalogueBrowserMain main = new CatalogueBrowserMain();
			main.launch();
		} catch (Exception e) {

			e.printStackTrace();

			LOGGER.error("Generic error occurred", e);

			String trace = ExceptionConverter.getStackTrace(e);

			GlobalUtil.showErrorDialog(new Shell(), Messages.getString("Generic.ErrorTitle"),
					Messages.getString("Generic.ErrorMessage") + trace);
		}
	}

	private void launch() throws IOException {

		InstanceChecker.closeIfAlreadyRunning();

		// application start-up message. Usage of System.err used for red chars
		LOGGER.info("Application Started " + System.currentTimeMillis());

		// system separator
		LOGGER.info("Reading OS file separator: " + System.getProperty("file.separator"));

		// database path
		LOGGER.info("Locating main database path in: " + DatabaseManager.MAIN_CAT_DB_FOLDER);

		// create the directories of the application
		GlobalUtil.createApplicationFolders();

		// clear the temporary directory
		GlobalUtil.clearTempDir();

		// connect to the main database and start it
		boolean started = false;
		try {
			DatabaseManager.startMainDB();
			started = true;
		} catch (SQLException e1) {
			e1.printStackTrace();
			GlobalUtil.showErrorDialog(new Shell(), Messages.getString("DBOpened.ErrorTitle"),
					Messages.getString("DBOpened.ErrorMessage"));
			return;
		}

		if (started) {
			try {
				DatabaseManager.addNotExistingTables();
			} catch (SQLException | IOException e) {
				e.printStackTrace();
				LOGGER.error("Cannot add not existing tables", e);
			}
		}

		// create the display and shell
		Display display = new Display();
		final Shell shell = new Shell(display);

		// set the application image into the shell
		shell.setImage(new Image(display, ClassLoader.getSystemResourceAsStream("Foodex2.ico")));
		shell.setFullScreen(true);

		// update the title of the shell
		GlobalUtil.startShellTextUpdate(shell);

		// initialise the browser user interface
		final MainPanel browser = new MainPanel(shell);

		// creates the main panel user interface
		browser.initGraphics();

		// show ui
		shell.open();

		// disable shell till all processes are completed
		shell.setEnabled(false);

		// logout
		// User.getInstance().deleteCredentials();

		if (User.getInstance().areCredentialsStored()) {

			// re-authenticate the user in background if needed
			ReauthThread reauth = new ReauthThread();
			reauth.setDoneListener(new ThreadFinishedListener() {

				@Override
				public void finished(Thread thread, final int code, Exception e) {

					if (shell.isDisposed())
						return;

					shell.getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							
							switch (code) {
							case OK:
								// start login process if everything is ok
								LoginActions.startLoginThreads(shell, new Listener() {

									@Override
									public void handleEvent(Event arg0) {
										browser.refresh();
									}
								});
								break;
							case ERROR:
								// stored credentials are not valid refresh ui
								browser.refresh();

								GlobalUtil.showErrorDialog(shell, Messages.getString("Reauth.title.error"),
										Messages.getString("Reauth.message.error"));
								break;
							default:
								// other exception
								browser.refresh();
								if (e instanceof DetailedSOAPException) {
									String[] warning = GlobalUtil.getSOAPWarning((DetailedSOAPException) e);
									GlobalUtil.showErrorDialog(shell, warning[0], warning[1]);
								}

								break;
							}
							
							// enable shell processes are completed
							shell.setEnabled(true);
						}
					});
				}
			});

			reauth.start();
		}else
			shell.setEnabled(true);

		// open last catalogue
		browser.openLastCatalogue();

		// refresh the menu
		browser.getMenu().refresh();

		// Event loop
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		// stop the database
		DatabaseManager.stopMainDB();

		// close socket lock
		InstanceChecker.close();

		// dispose shell
		shell.dispose();

		// exit app
		System.exit(0);
	}
}
