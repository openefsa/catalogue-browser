package ui_main_panel;

import java.io.File;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import business_rules.WarningUtil;
import catalogue_browser_dao.DatabaseManager;
import utilities.GlobalUtil;
import xml_reader.PropertiesReader;

public class CatalogueBrowserMain {

	public static final String ApplicationName = PropertiesReader.getAppName();
	public static final String ProgramVersion = PropertiesReader.getAppVersion();

	/**
	 * Main, catalogue browser entry point
	 * 
	 * @param args
	 */
	public static void main ( String[] args ) {

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

		// Here the program stars showing the graphical interface, 
		// so here we call a specific function
		// to use only warnings if needed
		if ( args.length > 0 ) {
			
			// argument checks
			if ( args.length != 2 ) {
				System.err.println( "Wrong number of arguments, please check! Remember that if you want \n"
						+ "to use the Foodex Browser as Warning Checker you have to provide two parameters,\n"
						+ "that is, the input file path (collection of codes to be analysed) and the output file path.\n"
						+ "Otherwise, if you want to open the Foodex Browser Interface, no argument has to be set.");
				return;
			}
			
			WarningUtil.performWarningChecksOnly( args );
			
			// exit from the program, we do not need anything else
			return;
		}
		
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
		
		// exit the application
		System.exit(0);
	}
}
