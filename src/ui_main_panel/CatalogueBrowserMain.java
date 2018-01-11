package ui_main_panel;

import java.io.IOException;
import java.sql.SQLException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import catalogue_browser_dao.DatabaseManager;
import catalogue_generator.ThreadFinishedListener;
import config.AppConfig;
import dcf_user.ReauthThread;
import dcf_user.User;
import dcf_user.UserAccessLevel;
import dcf_user.UserListener;
import exception_manager.ExceptionConverter;
import instance_checker.InstanceChecker;
import messages.Messages;
import ui_main_menu.LoginActions;
import utilities.GlobalUtil;

/**
 * Entry point for the Catalogue Browser application.
 * The user interface and the database are started here.
 * @author avonva
 *
 */
public class CatalogueBrowserMain {

	public static final String APP_NAME = AppConfig.getAppName();
	public static final String APP_VERSION = AppConfig.getAppVersion();
	public static final String APP_TITLE = APP_NAME + " " + APP_VERSION;
	/**
	 * Main, catalogue browser entry point
	 * 
	 * @param args
	 */
	public static void main ( String[] args ) {

		try {
			launch();
		}
		catch(Exception e) {
			
			e.printStackTrace();
			
			String trace = ExceptionConverter.getStackTrace(e);
			
			GlobalUtil.showErrorDialog(new Shell(), 
					Messages.getString("Generic.ErrorTitle"), 
					Messages.getString("Generic.ErrorMessage") + trace);
		}
	}
	
	private static void startShellTextUpdate(final Shell shell) {
		
		// default
		shell.setText( APP_TITLE + " " + Messages.getString("App.Disconnected") );
		
		User.getInstance().addUserListener(new UserListener() {
			
			@Override
			public void userLevelChanged(UserAccessLevel newLevel) {
				
				final String connectedAs = newLevel == UserAccessLevel.CATALOGUE_MANAGER ? 
						Messages.getString("App.ConnectedCM") :
							Messages.getString("App.ConnectedDP");

				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						shell.setText(APP_TITLE + " " + connectedAs);
					}
				});
			}
			
			@Override
			public void connectionChanged(boolean connected) {
				
			}
		});
	}
	
	private static void launch() {
		InstanceChecker.closeIfAlreadyRunning();
		
		// application start-up message. Usage of System.err used for red chars
		System.out.println( "Application Started " + System.currentTimeMillis() );

		// system separator
		System.out.println( "Reading OS file separator: " + System.getProperty( "file.separator" ) );

		// database path
		System.out.println( "Locating main database path in: " + DatabaseManager.MAIN_CAT_DB_FOLDER );

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
			GlobalUtil.showErrorDialog(new Shell(), 
					Messages.getString("DBOpened.ErrorTitle"), 
					Messages.getString("DBOpened.ErrorMessage"));
			return;
		}
		
		if (started) {
			try {
				DatabaseManager.addNotExistingTables();
			} catch (SQLException | IOException e) {
				e.printStackTrace();
			}
		}
		
		// create the display and shell
		Display display = new Display();
		final Shell shell = new Shell ( display );
		
		// set the application name in the shell
		shell.setText( APP_TITLE + " " + Messages.getString("App.Disconnected") );
		
		// set the application image into the shell
		shell.setImage( new Image( Display.getCurrent(), 
				ClassLoader.getSystemResourceAsStream( "Foodex2.ico" ) ) );
		
		startShellTextUpdate(shell);
		
		// initialize the browser user interface
		final MainPanel browser = new MainPanel( shell );

		// creates the main panel user interface
		browser.initGraphics();
		
		// show ui
		browser.shell.open();
		
		if (User.getInstance().areCredentialsStored()) {
			
			// reauthenticate the user in background if needed
			ReauthThread reauth = new ReauthThread();
			reauth.setDoneListener(new ThreadFinishedListener() {
				
				@Override
				public void finished(Thread thread, final int code, Exception e) {
					
					if (shell.isDisposed())
						return;
					
					shell.getDisplay().asyncExec(new Runnable() {
						
						@Override
						public void run() {
							
							switch(code) {
							case OK:

								LoginActions.startLoggedThreads(shell, 
										browser.getMenu().getListener(),
										new Listener() {

									@Override
									public void handleEvent(Event arg0) {
										browser.refresh();
										//shell.setText(APP_TITLE + " " + Messages.getString("App.Connected"));
									}
								});
								break;
							case ERROR:
								// stored credentials are not valid
								
								// enable manual login
								browser.refresh();
								//shell.setText( APP_TITLE + " " + Messages.getString("App.Disconnected") );
								GlobalUtil.showErrorDialog(shell, 
										Messages.getString("Reauth.title.error"), 
										Messages.getString("Reauth.message.error"));
								break;
							case EXCEPTION:
								browser.refresh();
								//shell.setText( APP_TITLE + " " + Messages.getString("App.Disconnected") );
								GlobalUtil.showErrorDialog(shell, 
										Messages.getString("Reauth.title.error"), 
										Messages.getString("Reauth.BadConnection"));
								break;
							default:
								break;
							}
						}
					});
				}
			});
			
			reauth.start();
		}
		
		browser.openLastCatalogue();
		
		browser.getMenu().refresh();

		// Event loop
		while ( !shell.isDisposed() ) {
			if ( !display.readAndDispatch() )
				display.sleep();
		}

		if (!display.isDisposed())
			display.dispose();

		// stop the database
		DatabaseManager.stopMainDB();
		
		// close socket lock
		try {
			InstanceChecker.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// exit the application
		System.exit(0);
	}
}
