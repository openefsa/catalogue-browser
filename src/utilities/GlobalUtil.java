package utilities;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_browser_dao.DatabaseManager;
import global_manager.GlobalManager;
import user_preferences.CataloguePreference;
import user_preferences.CataloguePreferenceDAO;


/**
 * This class contains static functions and static variables that can be used everywhere in the application
 * @author Valentino
 *
 */

public class GlobalUtil {
	
	private static String workDir = "";
	
	// directory for the user files as the settings 
	final static public String     userFileDirName                = "User Files";
	
	// directory for the user files as the settings 
	final static public String     userFileDir                    = getUserFileDir();
	
	// file where there is contained the selected picklist for favourite described terms
	final static public String     selectedPicklistFilename       = getUserFileDir() + "selectedPicklist.txt"; 
	
	// directory which contains all the pick lists
	final static public String     picklistsDirName               = "Picklists";
	
	// directory which contains all the pick lists
	final static public String     picklistsDir                   = getPicklistDir();

	// directory which contains all the pick lists
	final static public String     businessRulesDirName           = "Business Rules";
	
	// directory which contains all the pick lists
	final static public String     businessRulesDir               = getBRFileDir();

	// the header of the recentlyDescribeTermsFilename file
	final static public String     recentlyDescribeTermsHeader    = "level;Pick list elements;pick list code\r\n";
	
	// the filename of the file which contains the warning messages for the describe window
	final static public String     warningMessagesFilename        = getBRFileDir() + "warningMessages.txt";
	
	// the filename of the file which contains the warning colors for the warning describe window
	final static public String     warningColorsFilename          = getBRFileDir() + "warningColors.txt";
	
	// file where the recently described terms are saved
	final static public String     recentlyDescribeTermsFilename  = getUserFileDir() + "recentlyDescribedTerms.txt";
	
	// the filename of the file which contains the business rule for the describe 
	final static public String     businessRuleFilename           = getBRFileDir() + "BR_Data.csv";
	
	// the filename of the file which contains the business rule EXCEPTIONS for the describe 
	final static public String     businessRulexceptionsFilename  = getBRFileDir() + "BR_Exceptions.csv";
	
	// the filename of the file which contains the global search options for the state flags
	final static public String    _listStateFlagFile              = getUserFileDir() + "listStateFlag.txt";
	
	// the filename of the file which contains the global search options for the state flags
	final static public String    _listSearchNamesFile            = getUserFileDir() + "listSearchNames.txt";
	
	// the default file for the user preferences xml
	final static public String     userPreferencesFile            = getUserFileDir() + "userPreferences.xml";
	
	// the filename of the default property file (xml)
	final static public String     appPropertiesFile          = getUserFileDir() + "DefaultProperties.xml";
	
	// the filename of the file which stores the information related to the windows dimensions etc
	final static public String     restoreSessionWindowFile       = getUserFileDir() + "restoreSession.properties";
	
	// name of user preferences fields
	final static public String     minSearchCharPref              = "FoodexBrowser.MinSearchChar";
	final static public String     loggingPref                    = "FoodexBrowser.Logging";
	final static public String     currentDirPref                 = "FoodexBrowser.CurrentDirectory";
	final static public String     copyImplicitPref               = "FoodexBrowser.CopyImplicitFacets";
	final static public String     maxRecentTermsPref             = "FoodexBrowser.MaxRecentTerms";
	final static public String     BusinessRulesPref              = "FoodexBrowser.EnableBusinessRules";
	
	
	/**
	 * Set the working directory where the directories should
	 * be searched.
	 * @param path
	 */
	public static void setWorkingDirectory( String path ) {
		workDir = path + System.getProperty( "file.separator" );
		System.setProperty( "user.dir", workDir );
	}
	
	public static String getWorkingDir() {
		return workDir;
	}

	/**
	 * Get the user file directory path
	 * @return
	 */
	public static String getUserFileDir() {
		return ( workDir + userFileDirName + System.getProperty( "file.separator" ) );
	}
	
	/**
	 * get the business rules directory path
	 * @return
	 */
	public static String getBRFileDir() {
		return ( businessRulesDirName + System.getProperty( "file.separator" ) );
	}
	
	public static String getBusinessrulefilename() {
		return workDir + businessRuleFilename;
	}
	public static String getWarningcolorsfilename() {
		return workDir + warningColorsFilename;
	}
	public static String getWarningmessagesfilename() {
		return workDir + warningMessagesFilename;
	}
	
	/**
	 * get the picklists directory path
	 * @return
	 */
	public static String getPicklistDir() {
		return ( workDir + picklistsDirName + System.getProperty( "file.separator" ) );
	}

	
	/**
	 * Check if a file exists or not
	 * @param filename
	 * @return
	 */
	public static boolean fileExists ( String filename ) {
		File check = new File ( filename );
		return ( check.exists() );
	}
	
	/**
	 * Add a column to the parentTable
	 * @param parentTable
	 * @param labelProvider, the column label provider which has to be used for the column
	 * @param name, the name of the column
	 * @param width, the width of the column
	 * @param resizable, if the column is resizable
	 * @param moveable, if the column is moveable
	 * @return
	 */
	public static TableViewerColumn addStandardColumn ( TableViewer parentTable, ColumnLabelProvider labelProvider, 
			String name, int width, boolean resizable, boolean moveable, int alignment ) {

		// Add the column to the parent table
		TableViewerColumn column = new TableViewerColumn( parentTable, SWT.NONE );

		// set the label provider for column
		column.setLabelProvider( labelProvider );
		column.getColumn().setText( name );
		column.getColumn().setWidth( width );
		column.getColumn().setResizable( resizable );
		column.getColumn().setMoveable( moveable );
		column.getColumn().setAlignment( alignment );

		return column;
	}
	
	
	/**
	 * 
	 * @param parentTable
	 * @param labelProvider
	 * @param name
	 * @param width
	 * @return
	 */
	public static TableViewerColumn addStandardColumn ( TableViewer parentTable, ColumnLabelProvider labelProvider, 
			String name, int width ) {

		return addStandardColumn( parentTable, labelProvider, name, width, true, true, SWT.LEFT );
	}
	

	/**
	 * 
	 * @param parentTable
	 * @param labelProvider
	 * @param name
	 * @param width
	 * @param alignment
	 * @return
	 */
	public static TableViewerColumn addStandardColumn ( TableViewer parentTable, ColumnLabelProvider labelProvider, 
			String name, int width, int alignment ) {

		return addStandardColumn( parentTable, labelProvider, name, width, true, true, alignment );
	}
	
	/**
	 * 
	 * @param parentTable
	 * @param labelProvider
	 * @param name
	 * @param width
	 * @param resizable
	 * @param moveable
	 * @return
	 */
	public static TableViewerColumn addStandardColumn ( TableViewer parentTable, ColumnLabelProvider labelProvider, 
			String name, int width, boolean resizable, boolean moveable ) {

		return addStandardColumn( parentTable, labelProvider, name, width, resizable, moveable, SWT.LEFT );
	}
	
	/**
	 * Standard construction for our purposes
	 * @param parentTable
	 * @param labelProvider
	 * @param name
	 * @return
	 */
	public static TableViewerColumn addStandardColumn ( TableViewer parentTable, 
			ColumnLabelProvider labelProvider, String name ) {
		
		return addStandardColumn(parentTable, labelProvider, name, 150, true, false);
	}
	
	/**
	 * Dispose all the menu items of a menu
	 * @param menu
	 */
	public static void disposeMenuItems ( Menu menu ) {
		
		// get the number of menu items
		int count = menu.getItemCount();

		// for each menu item => dispose (we recreate them each time)
		for ( int i = 0 ; i < count ; i++ ) {
			menu.getItem( 0 ).dispose();
		}
	}
	
	/**
	 * Create a directory in the absolute path
	 * return true is everything went well
	 * @param path
	 */
	public static boolean createDirectory ( String path ) {
		
		File file = new File( path );
		
		// create the directory if it does not exist
		if ( !file.exists() )
			file.mkdir();
		
		return !file.exists();
	}
	
	
	/**
	 * Convert a java.util.date in a java.sql.timestamp, in order to store the information
	 * in a jdbc database
	 * @param date
	 * @return
	 */
	public static java.sql.Timestamp toSQLTimestamp ( Date date ) {
		
		// get the timestamp from the date
		java.sql.Timestamp timestamp = new java.sql.Timestamp( date.getTime() );
		
		// return the timestamp
		return timestamp; 
	}
	
	/**
	 * Convert a java.sql.timestamp into a java.util.date
	 * @param ts
	 * @return
	 */
	public static Date SQLTimestampToDate ( java.sql.Timestamp ts ) {
		Date date = new Date( ts.getTime() );
		return date;
	}
	
	/**
	 * Convert the timestamp to a string (as the DCF date string)
	 * @param ts
	 * @return
	 */
	public static String DCFDateFormat ( java.sql.Timestamp ts ) {
		
		Date date = SQLTimestampToDate ( ts );
		
		// convert the time stamp to string
		DateFormat sdf = new SimpleDateFormat ( Catalogue.ISO_8601_24H_FULL_FORMAT );
		
		return sdf.format( date );
	}
	
	/**
	 * Trasform a date string into a timestamp
	 * @param dateString
	 * @param dateFormat
	 * @return
	 * @throws ParseException
	 */
	public static Timestamp getTimestampFromString ( String dateString, 
			String dateFormat ) throws ParseException {
		
		SimpleDateFormat format = new SimpleDateFormat( dateFormat );
	    Date parsedDate = format.parse( dateString );
	    Timestamp ts = new Timestamp( parsedDate.getTime() );
	    return ts;
	}
	
	/**
	 * Set the shell cursor to the cursorType one
	 * @param shell
	 * @param cursorType
	 */
	public static void setShellCursor ( Shell shell, int cursorType ) {
		
		// change the cursor to the new cursor
		shell.setCursor( shell.getDisplay().getSystemCursor( cursorType ) );
	}
	
	
	/**
	 * Open a soap connection
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws SOAPException
	 */
	public static SOAPConnection openSOAPConnection () throws UnsupportedOperationException, SOAPException {
		
		// Connect to the DCF, given username and password
		SOAPConnectionFactory connectionFactory = SOAPConnectionFactory.newInstance();

		// create the soap connection
		return connectionFactory.createConnection();
	}
	
	
	/**
	 * Delete a folder with all the sub files.
	 * @param directory
	 * @throws IOException
	 */
	public static void deleteFileCascade( String directory ) throws IOException {
		deleteFileCascade( new File( directory ) );
	}
	
	/**
	 * Delete a folder with all the sub files.
	 * @param directory
	 * @throws IOException
	 */
	public static void deleteFileCascade( File directory ) throws IOException {
		
		// delete all the sub files recursively
		if ( directory.isDirectory() ) {
			
			File[] files = directory.listFiles();
			
			// some JVM return null for listfiles
			if ( files == null )
				return;
			
			for ( File file : files ) {
				file.setWritable( true );
				deleteFileCascade( file );
			}
		}

		// delete the directory
		if ( !directory.delete() )
			throw new FileNotFoundException( "Failed to delete file: " + directory );
	}

	/**
	 * Open a standard error dialog
	 * @param shell
	 * @param title
	 * @param message
	 */
	public static void showErrorDialog ( Shell shell, String title, String message ) {
		showDialog ( shell, title, message, SWT.ICON_ERROR );
	}
	
	/**
	 * Show a generic dialog
	 * @param shell
	 * @param title
	 * @param message
	 * @param style
	 * @return
	 */
	public static int showDialog ( Shell shell, String title, String message, int style ) {
		MessageBox mb = new MessageBox( shell, style );
		mb.setText( title ); 
		mb.setMessage( message );
		return mb.open();
	}
	
	
	/**
	 * Show a dialog to select an excel file from the file browser. Return the filename.
	 * @param shell
	 * @param text
	 * @param defaultFilename default filename which will appear in the dialog
	 * @return
	 */
	public static String showExcelFileDialog ( Shell shell, String text, 
			String defaultFilename, int buttonType ) {
		return showFileDialog( shell, text, new String[] { " *.xlsx" }, 
				defaultFilename, buttonType );
	}
	
	/**
	 * Show a dialog to select an excel file from the file browser. Return the filename.
	 * @param shell
	 * @param shell
	 * @param text
	 * @return
	 */
	public static String showExcelFileDialog ( Shell shell, String text, int buttonType ) {
		return showFileDialog( shell, text, new String[] { " *.xlsx" }, buttonType );
	}
	
	/**
	 * Show a generic file dialog to select a file from the pc
	 * @param shell
	 * @param text
	 * @param extensions
	 * @param defaultFilename the default file name for the dialog
	 * @return
	 */
	public static String showFileDialog ( Shell shell, String text, 
			String[] extensions, String defaultFilename, int buttonType ) {

		FileDialog dialog = new FileDialog( shell, buttonType );
		
		dialog.setOverwrite( true );
		
		// set dialog text
		dialog.setText( text );
		
		// get the working directory from the user preferences
		String workingDir = DatabaseManager.MAIN_CAT_DB_FOLDER;
		dialog.setFilterPath( workingDir + System.getProperty( "file.separator" ) );
		dialog.setFilterExtensions( extensions );
		dialog.setFileName( defaultFilename );
		
		String filename = dialog.open();
		
		return filename;
	}
	
	/**
	 * Show a generic file dialog to select a file from the pc
	 * @param shell
	 * @param text
	 * @param extensions
	 * @return
	 */
	public static String showFileDialog ( Shell shell, String text, String[] extensions, int buttonType ) {
		return showFileDialog( shell, text, extensions, "", buttonType );
	}
	
	
	/**
	 * Refresh logging state if the user set the preference
	 */
	public static void refreshLogging () {
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		// return if no catalogue is open
		if ( currentCat == null )
			return;
		
		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( currentCat );
		
		boolean active = prefDao.getPreferenceBoolValue( CataloguePreference.logging, false );
		
		// start logging if it was enabled
		if ( active ) {

			// create a logging file
			Calendar dCal = Calendar.getInstance();
			Format formatter;
			formatter = new SimpleDateFormat( "yyyyMMddhhmmss" );

			String code = currentCat.getCode();
			String dbDir = currentCat.getDbPath();
			
			FileOutputStream os = null;
			try {
				os = new FileOutputStream( dbDir + code + "_LOG" + formatter.format( dCal.getTime() )
				+ ".txt" );
			} catch ( FileNotFoundException e ) {
				e.printStackTrace();
			}

			// set the logging file as output for the standard error messages
			PrintStream ps = new PrintStream( os );
			System.setErr( ps );
		}
		else {
			// reset the standard error output
			System.setErr( new PrintStream(new FileOutputStream( FileDescriptor.err ) ) );
		}
	}
}