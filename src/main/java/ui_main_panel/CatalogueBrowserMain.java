package ui_main_panel;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import catalogue_browser_dao.DatabaseManager;
import catalogue_generator.ThreadFinishedListener;
import config.AppConfig;
import converter.ExceptionConverter;
import dcf_user.ReauthThread;
import dcf_user.User;
import i18n_messages.CBMessages;
import instance_checker.InstanceChecker;
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
			LOGGER.error("Generic error occurred", e);
			e.printStackTrace();

			String trace = ExceptionConverter.getStackTrace(e);

			GlobalUtil.showErrorDialog(new Shell(), CBMessages.getString("Generic.ErrorTitle"),
					CBMessages.getString("Generic.ErrorMessage") + trace);
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
            LOGGER.error("Error during launch ", e1);
			e1.printStackTrace();
			GlobalUtil.showErrorDialog(new Shell(), CBMessages.getString("DBOpened.ErrorTitle"),
					CBMessages.getString("DBOpened.ErrorMessage"));
			return;
		}

		if (started) {
			try {
				DatabaseManager.addNotExistingTables();
			} catch (SQLException | IOException e) {
				LOGGER.error("Cannot add not existing tables", e);
				e.printStackTrace();
			}
		}

		// create the display and shell
		Display display = new Display();
		final Shell shell = new Shell(display);

		// set the application image into the shell
		shell.setImage(
				new Image(display, CatalogueBrowserMain.class.getClassLoader().getResourceAsStream("Foodex2.ico")));
		shell.setFullScreen(true);

		// update the title of the shell
		GlobalUtil.startShellTextUpdate(shell);

		// initialise the browser user interface
		final MainPanel browser = new MainPanel(shell);

		// creates the main panel user interface
		browser.initGraphics();
		
		// update the title of the shell
		Program.launch(AppConfig.getHelpRepositoryURL());
				
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

								GlobalUtil.showErrorDialog(shell, CBMessages.getString("Reauth.title.error"),
										CBMessages.getString("Reauth.message.error"));
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
		} else
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
