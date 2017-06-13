package ui_main_panel;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import catalogue_browser_dao.DatabaseManager;
import messages.Messages;
import utilities.GlobalUtil;
import xml_reader.PropertiesReader;

public class CatalogueBrowserMain {

	public static final String ApplicationName = PropertiesReader.getAppName();
	public static final String ProgramVersion = PropertiesReader.getAppVersion();

	private static ServerSocket socket;
	private static final int PORT = 9999;  

	/**
	 * Close the application if it is already running 
	 * in another instance
	 */
	public static void closeIfAlreadyRunning() {
		
		try {
			//Bind to localhost adapter with a zero connection queue 
			socket = new ServerSocket( PORT, 0, 
					InetAddress.getByAddress( new byte[] {127,0,0,1} ) );
		}
		catch (BindException e) {
			System.err.println( "Another instance of the catalogue browser is already running!" );
			
			GlobalUtil.showErrorDialog( new Shell(), 
					Messages.getString( "AlreadyRunning.ErrorTitle" ),
					Messages.getString( "AlreadyRunning.ErrorMessage" ) );

			System.exit(1);
		}
		catch (IOException e) {
			System.err.println( "Unexpected error." );
			e.printStackTrace();
			System.exit(2);
		}
	}
	
	/**
	 * Main, catalogue browser entry point
	 * 
	 * @param args
	 */
	public static void main ( String[] args ) {
		
		closeIfAlreadyRunning();
		
		// application start-up message. Usage of System.err used for red chars
		System.out.println( "Application Started " + System.currentTimeMillis() );

		// system separator
		System.out.println( "Reading OS file separator: " + System.getProperty( "file.separator" ) );

		// database path
		System.out.println( "Locating main database path in: " + DatabaseManager.MAIN_CAT_DB_FOLDER );
		
		// Files checks, create directories if they don't exist

		// if the user files directory does not exist create it
		if ( !GlobalUtil.fileExists( GlobalUtil.userFileDir ) ) {
			new File( GlobalUtil.userFileDir ).mkdir();
		}

		// if the business rules directory does not exist create it
		if ( !GlobalUtil.fileExists( GlobalUtil.businessRulesDir ) ) {
			new File( GlobalUtil.businessRulesDir ).mkdir();
		}

		// connect to the main database and start it
		DatabaseManager.startMainDB();
		
		// create the display and shell
		Display display = new Display();
		Shell shell = new Shell ( display );
		
		// set the application name in the shell
		shell.setText( ApplicationName + " " + ProgramVersion );
		
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
			if ( socket != null )
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// exit the application
		System.exit(0);
	}
}
