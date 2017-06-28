package ui_main_panel;

import java.io.IOException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import catalogue_browser_dao.DatabaseManager;
import instance_checker.InstanceChecker;
import utilities.GlobalUtil;
import xml_reader.PropertiesReader;

/**
 * Entry point for the Catalogue Browser application.
 * The user interfaces and the database are started here.
 * @author avonva
 *
 */
public class CatalogueBrowserMain {

	public static final String APP_NAME = PropertiesReader.getAppName();
	public static final String APP_VERSION = PropertiesReader.getAppVersion();
	
	/**
	 * Main, catalogue browser entry point
	 * 
	 * @param args
	 */
	public static void main ( String[] args ) {
		
		InstanceChecker.closeIfAlreadyRunning();
		
		// application start-up message. Usage of System.err used for red chars
		System.out.println( "Application Started " + System.currentTimeMillis() );

		// system separator
		System.out.println( "Reading OS file separator: " + System.getProperty( "file.separator" ) );

		// database path
		System.out.println( "Locating main database path in: " + DatabaseManager.MAIN_CAT_DB_FOLDER );

		// create the directories of the application
		GlobalUtil.createApplicationFolders();

		// connect to the main database and start it
		DatabaseManager.startMainDB();
		
		// create the display and shell
		Display display = new Display();
		Shell shell = new Shell ( display );
		
		// set the application name in the shell
		shell.setText( APP_NAME + " " + APP_VERSION );
		
		// set the application image into the shell
		shell.setImage( new Image( Display.getCurrent(), 
				ClassLoader.getSystemResourceAsStream( "Foodex2.ico" ) ) );
		
		// initialize the browser user interface
		MainPanel browser = new MainPanel( shell );

		// creates the main panel user interface
		browser.initGraphics();

		// show ui
		browser.shell.open();

		// Event loop
		while ( !shell.isDisposed() ) {
			if ( !display.readAndDispatch() )
				display.sleep();
		}

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
