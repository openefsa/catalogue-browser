package utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.impl.soap.SOAPConnection;
import org.apache.xmlbeans.impl.soap.SOAPConnectionFactory;
import org.apache.xmlbeans.impl.soap.SOAPException;
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
import config.AppConfig;
import dcf_user.User;
import dcf_user.UserAccessLevel;
import dcf_user.UserListener;
import i18n_messages.CBMessages;
import soap.DetailedSOAPException;
import soap.SOAPError;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * This class contains static functions and static variables that can be used
 * everywhere in the application
 * 
 * @author shahaal
 * @author avonva
 */

public final class GlobalUtil {
	
	private static final Logger LOGGER = LogManager.getLogger(GlobalUtil.class);

	public static final String TEMP_DIR_NAME = "temp";

	static private String workDir = "";

	public static final String MAIN_DIR = getMainDir();

	// directory for the user files as the settings
	public static final String CONFIG_FILES_DIR_NAME = "config";
	public static final String CONFIG_FILES_DIR_PATH = getConfigDir();
	public static final String CONFIG_FILE = getConfigDir() + "appConfig.xml";

	public final String RESERVE_SCHEMA = getConfigDir() + "reserveSchema.xml";
	public final String PUBLISH_SCHEMA = getConfigDir() + "publishSchema.xml";

	// directory which contains all the pick lists
	public static final String PICKLISTS_DIR_NAME = "picklists";
	public final String PICKLISTS_DIR_PATH = getPicklistDir();

	public static final String PREF_DIR_NAME = "preferences";
	public static final String PREF_DIR_PATH = getPrefDir();

	// directory which contains all the pick lists
	public static final String BUSINESS_RULES_DIR_NAME = "business-rules";
	public static final String BUSINESS_RULES_DIR_PATH = getBusinessRulesDir();

	// the filename of the warning messages and colours
	public static final String WARNING_MESSAGES_FILE = "warningMessages.txt";
	public static final String WARNING_COLORS_FILE = "warningColors.txt";

	// the filename of the file which contains the business rule for the describe
	public static final String BUSINESS_RULES_FILE = "BR_Data.csv";

	// change log file
	public final static String CHANGELOG_PATH = getConfigDir() + "changelog.txt";
	// flag for change log
	public static final String VERSION_FLAG_PATH = MAIN_DIR + "flag.txt";
	// path for ICT add-on and related folders/files
	public static final String UTILS_FILE_PATH = MAIN_DIR + "utils.zip";
	public static final String UTILS_FOLDER_PATH = MAIN_DIR + "utils";
	public static final String CHECK_DIR_NAME = "Check";
	public static final String CHECK_DIR_PATH = getCheckDir();
	public static final String ICT_DIR_NAME = "Interpreting_Tool";
	public static final String ICT_DIR_PATH = getIctDir();
	public static final String ICT_DATABASE_DIR_NAME = "database";
	public static final String ICT_DATABASE_DIR_PATH = getIctDatabaseDir();
	public static final String ICT_MAIN_CAT_DB_NAME = "MAIN_CATS_DB";
	public static final String ICT_MAIN_CAT_DB_PATH = getIctMainCatDbDir();
	public static final String ICT_FOODEX2_FILE_PATH = ICT_DIR_PATH + "FoodEx2.xlsx";
	public static final String ICT_MTX_CAT_DB_FOLDER = ICT_DATABASE_DIR_PATH + "/PRODUCTION_CATS/CAT_MTX_DB/";
	public static final String ICT_CONFIG_FILE = getConfigDir() + "ictConfig.xml";
	public static final String ICT_FILE_NAME = "ICT.xlsm";
	public static final String ICT_FILE_PATH = ICT_DIR_PATH + ICT_FILE_NAME;
	public static final String ICT_UPDATE_FILE_PATH = ICT_DIR_PATH + "update.bat";

	public static final String APP_NAME = AppConfig.getAppName();
	public static final String APP_VERSION = AppConfig.getAppVersion();
	public static final String APP_TITLE = APP_NAME + " " + APP_VERSION;

	// private constructor to avoid unnecessary instantiation of the class
	private GlobalUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Set the working directory where the directories should be searched.
	 * 
	 * @param path
	 */
	public static void setWorkingDirectory(String path) {
		workDir = path + System.getProperty("file.separator");
		System.setProperty("user.dir", workDir);
	}

	public static String getWorkingDir() {
		return workDir;
	}

	public static String getPrefDir() {
		return (workDir + PREF_DIR_NAME + System.getProperty("file.separator"));
	}

	/**
	 * Get the user file directory path
	 * 
	 * @return
	 */
	public static String getConfigDir() {
		return (workDir + CONFIG_FILES_DIR_NAME + System.getProperty("file.separator"));
	}

	/**
	 * get the business rules directory path
	 * 
	 * @return
	 */
	public static String getBusinessRulesDir() {
		return (workDir + BUSINESS_RULES_DIR_NAME + System.getProperty("file.separator"));
	}

	/**
	 * get the main directory path
	 * 
	 * @return
	 */
	public static String getMainDir() {
		return new File(System.getProperty("user.dir")).getParent() + System.getProperty("file.separator");
	}

	/**
	 * get the check directory path
	 * 
	 * @return
	 */
	public static String getCheckDir() {
		return (workDir + CHECK_DIR_NAME + System.getProperty("file.separator"));
	}

	/**
	 * get the interpreting tool directory path
	 * 
	 * @return
	 */
	public static String getIctDir() {
		return (workDir + ICT_DIR_NAME + System.getProperty("file.separator"));
	}

	/**
	 * get the interpreting tool DB directory path
	 * 
	 * @return
	 */
	public static String getIctDatabaseDir() {
		return (getIctDir() + ICT_DATABASE_DIR_NAME + System.getProperty("file.separator"));
	}

	/**
	 * get the interpreting tool DB directory path
	 * 
	 * @return
	 */
	public static String getIctMainCatDbDir() {
		return (getIctDatabaseDir() + ICT_MAIN_CAT_DB_NAME + System.getProperty("file.separator"));
	}

	/**
	 * Get the business rules filename
	 * 
	 * @return
	 */
	public static String getBRData() {
		return getBusinessRulesDir() + BUSINESS_RULES_FILE;
	}

	/**
	 * Get the warning messages filename
	 * 
	 * @return
	 */
	public static String getBRMessages() {
		return getBusinessRulesDir() + WARNING_MESSAGES_FILE;
	}

	/**
	 * Get the warning colors filename
	 * 
	 * @return
	 */
	public static String getBRColors() {
		return getBusinessRulesDir() + WARNING_COLORS_FILE;
	}

	/**
	 * get the picklists directory path
	 * 
	 * @return
	 */
	public static String getPicklistDir() {
		return (workDir + PICKLISTS_DIR_NAME + System.getProperty("file.separator"));
	}

	/**
	 * get the temporary files directory path
	 * 
	 * @return
	 */
	public static String getTempDir() {
		return (workDir + TEMP_DIR_NAME + System.getProperty("file.separator"));
	}

	/**
	 * Delete all the temporary files in the {@link #getTempDir()}
	 */
	public static void clearTempDir() {
		File directory = new File(getTempDir());
		for (File file : directory.listFiles()) {
			file.delete();
		}
	}

	/**
	 * Check if a file exists or not
	 * 
	 * @param filename
	 * @return
	 */
	public static boolean fileExists(String filename) {
		File check = new File(filename);
		return (check.exists());
	}

	/**
	 * Create the application folders if they do not exist
	 */
	public static final void createApplicationFolders() {

		// create the user files directory
		if (!fileExists(CONFIG_FILES_DIR_PATH)) {
			new File(CONFIG_FILES_DIR_PATH).mkdir();
		}

		// create the business rules directory
		if (!fileExists(BUSINESS_RULES_DIR_PATH)) {
			new File(BUSINESS_RULES_DIR_PATH).mkdir();
		}

		// create preferences directory
		if (!fileExists(PREF_DIR_PATH)) {
			new File(PREF_DIR_PATH).mkdir();
		}

		// create the temp directory
		if (!fileExists(getTempDir())) {
			new File(getTempDir()).mkdir();
		}

	}

	/**
	 * remove preferences folder
	 * 
	 */
	public static final void removePreferencesFolder() {

		try {
			FileUtils.deleteDirectory(new File(getPrefDir()));
		} catch (IOException e) {
			LOGGER.error("Error during removal preferences folder ", e);
			e.printStackTrace();
		}
	}

	/**
	 * remove ICT folder
	 * 
	 */
	public static final void removeICTFolder() {

		try {
			FileUtils.deleteDirectory(new File(ICT_DIR_PATH));
		} catch (IOException e) {
			LOGGER.error("Error during removal ICT folder ", e);
			e.printStackTrace();
		}
	}

	/**
	 * prepare the ICT folders and check if old versions installed
	 */
	public final static void createIctFolders() {

		// create check folder
		if (!fileExists(CHECK_DIR_PATH)) {
			new File(CHECK_DIR_PATH).mkdir();
		}

		// create ICT main folder
		if (!fileExists(ICT_DIR_PATH)) {
			new File(ICT_DIR_PATH).mkdir();
		}

		// create ICT db folder
		if (!fileExists(ICT_DATABASE_DIR_PATH)) {
			new File(ICT_DATABASE_DIR_PATH).mkdir();
		}

	}

	/**
	 * Add a column to the parentTable
	 * 
	 * @param parentTable
	 * @param labelProvider, the column label provider which has to be used for the
	 *                       column
	 * @param name,          the name of the column
	 * @param width,         the width of the column
	 * @param resizable,     if the column is resizable
	 * @param moveable,      if the column is moveable
	 * @return
	 */
	public static TableViewerColumn addStandardColumn(TableViewer parentTable, ColumnLabelProvider labelProvider,
			String name, int width, boolean resizable, boolean moveable, int alignment) {

		// Add the column to the parent table
		TableViewerColumn column = new TableViewerColumn(parentTable, SWT.NONE);

		// set the label provider for column
		column.setLabelProvider(labelProvider);
		column.getColumn().setText(name);
		column.getColumn().setWidth(width);
		column.getColumn().setResizable(resizable);
		column.getColumn().setMoveable(moveable);
		column.getColumn().setAlignment(alignment);

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
	public static TableViewerColumn addStandardColumn(TableViewer parentTable, ColumnLabelProvider labelProvider,
			String name, int width) {

		return addStandardColumn(parentTable, labelProvider, name, width, true, true, SWT.LEFT);
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
	public static TableViewerColumn addStandardColumn(TableViewer parentTable, ColumnLabelProvider labelProvider,
			String name, int width, int alignment) {

		return addStandardColumn(parentTable, labelProvider, name, width, true, true, alignment);
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
	public static TableViewerColumn addStandardColumn(TableViewer parentTable, ColumnLabelProvider labelProvider,
			String name, int width, boolean resizable, boolean moveable) {

		return addStandardColumn(parentTable, labelProvider, name, width, resizable, moveable, SWT.LEFT);
	}

	/**
	 * Standard construction for our purposes
	 * 
	 * @param parentTable
	 * @param labelProvider
	 * @param name
	 * @return
	 */
	public static TableViewerColumn addStandardColumn(TableViewer parentTable, ColumnLabelProvider labelProvider,
			String name) {

		return addStandardColumn(parentTable, labelProvider, name, 150, true, false);
	}

	/**
	 * Dispose all the menu items of a menu
	 * 
	 * @param menu
	 */
	public static void disposeMenuItems(Menu menu) {

		// get the number of menu items
		int count = menu.getItemCount();

		// for each menu item => dispose (we recreate them each time)
		for (int i = 0; i < count; i++) {
			menu.getItem(0).dispose();
		}
	}

	/**
	 * Create a directory in the absolute path return true is everything went well
	 * 
	 * @param path
	 */
	public static boolean createDirectory(String path) {

		File file = new File(path);

		// create the directory if it does not exist
		if (!file.exists())
			file.mkdir();

		return !file.exists();
	}

	/**
	 * Convert a java.util.date in a java.sql.timestamp, in order to store the
	 * information in a jdbc database
	 * 
	 * @param date
	 * @return
	 */
	public static java.sql.Timestamp toSQLTimestamp(Date date) {

		// get the timestamp from the date
		java.sql.Timestamp timestamp = new java.sql.Timestamp(date.getTime());

		// return the timestamp
		return timestamp;
	}

	/**
	 * Convert a java.sql.timestamp into a java.util.date
	 * 
	 * @param ts
	 * @return
	 */
	public static Date SQLTimestampToDate(java.sql.Timestamp ts) {
		Date date = new Date(ts.getTime());
		return date;
	}

	/**
	 * Convert the timestamp to a string (as the DCF date string)
	 * 
	 * @param ts
	 * @return
	 */
	public static String DCFDateFormat(java.sql.Timestamp ts) {

		Date date = SQLTimestampToDate(ts);

		// convert the time stamp to string
		DateFormat sdf = new SimpleDateFormat(Catalogue.ISO_8601_24H_FULL_FORMAT);

		return sdf.format(date);
	}

	/**
	 * Trasform a date string into a timestamp
	 * 
	 * @param dateString
	 * @param dateFormat
	 * @return
	 * @throws ParseException
	 */
	public static Timestamp getTimestampFromString(String dateString, String dateFormat) throws ParseException {

		SimpleDateFormat format = new SimpleDateFormat(dateFormat);
		Date parsedDate = format.parse(dateString);
		Timestamp ts = new Timestamp(parsedDate.getTime());
		return ts;
	}

	/**
	 * Set the shell cursor to the cursorType one
	 * 
	 * @param shell
	 * @param cursorType
	 */
	public static void setShellCursor(Shell shell, int cursorType) {

		// change the cursor to the new cursor
		shell.setCursor(shell.getDisplay().getSystemCursor(cursorType));
	}

	/**
	 * Open a soap connection
	 * 
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws SOAPException
	 */
	public static SOAPConnection openSOAPConnection() throws UnsupportedOperationException, SOAPException {

		// Connect to the DCF, given username and password
		SOAPConnectionFactory connectionFactory = SOAPConnectionFactory.newInstance();

		// create the soap connection
		return connectionFactory.createConnection();
	}

	/**
	 * Copy the source file into the target file
	 * 
	 * @param source
	 * @param target
	 * @return
	 * @throws IOException
	 */
	public static void copyFile(File source, File target) throws IOException {
		// copy the .start file into the sas folder
		DatabaseManager.copyFolder(source, target);
	}

	/**
	 * Delete a folder with all the sub files.
	 * 
	 * @param directory
	 * @throws IOException
	 */
	public static void deleteFileCascade(String directory) throws IOException {
		deleteFileCascade(new File(directory));
	}

	/**
	 * Delete a folder with all the sub files.
	 * 
	 * @param directory
	 * @throws IOException
	 */
	public static void deleteFileCascade(File directory) throws IOException {

		// delete all the sub files recursively
		if (directory.isDirectory()) {

			File[] files = directory.listFiles();

			// some JVM return null for listfiles
			if (files == null)
				return;

			for (File file : files) {
				file.setWritable(true);
				deleteFileCascade(file);
			}
		}

		// remove the directory if it exists
		Path path = Paths.get(directory.getAbsolutePath());
		if (Files.exists(path))
			Files.delete(path);
	}

	/**
	 * Open a standard error dialog
	 * 
	 * @param shell
	 * @param title
	 * @param message
	 */
	public static void showErrorDialog(Shell shell, String title, String message) {
		showDialog(shell, title, message, SWT.ICON_ERROR);
	}

	/**
	 * Show a generic dialog
	 * 
	 * @param shell
	 * @param title
	 * @param message
	 * @param style
	 * @return
	 */
	public static int showDialog(Shell shell, String title, String message, int style) {
		MessageBox mb = new MessageBox(shell, style);
		mb.setText(title);
		mb.setMessage(message);
		return mb.open();
	}

	/**
	 * Show a dialog to select an excel file from the file browser. Return the
	 * filename.
	 * 
	 * @param shell
	 * @param text
	 * @param defaultFilename default filename which will appear in the dialog
	 * @return
	 */
	public static String showExcelFileDialog(Shell shell, String text, String defaultFilename, int buttonType) {
		return showFileDialog(shell, text, new String[] { " *.xlsx" }, defaultFilename, buttonType);
	}

	/**
	 * Show a dialog to select an excel file from the file browser. Return the
	 * filename.
	 * 
	 * @param shell
	 * @param shell
	 * @param text
	 * @return
	 */
	public static String showExcelFileDialog(Shell shell, String text, int buttonType) {
		return showFileDialog(shell, text, new String[] { " *.xlsx" }, buttonType);
	}

	/**
	 * Show a generic file dialog to select a file from the pc
	 * 
	 * @param shell
	 * @param text
	 * @param extensions
	 * @param defaultFilename the default file name for the dialog
	 * @return
	 */
	public static String showFileDialog(Shell shell, String text, String[] extensions, String defaultFilename,
			int buttonType) {

		FileDialog dialog = new FileDialog(shell, buttonType);

		dialog.setOverwrite(true);

		// set dialog text
		dialog.setText(text);

		// get the working directory from the user preferences
		String workingDir = DatabaseManager.MAIN_CAT_DB_FOLDER;
		dialog.setFilterPath(workingDir + System.getProperty("file.separator"));
		dialog.setFilterExtensions(extensions);
		dialog.setFileName(defaultFilename);

		String filename = dialog.open();

		return filename;
	}

	public static String[] getSOAPWarning(DetailedSOAPException e) {

		String title = null;
		String message = null;
		SOAPError error = e.getError();

		switch (error) {
		case QUOTA_EXCEEDED:
			title = CBMessages.getString("error.title");
			message = CBMessages.getString("starter.week.message");
			break;
		case TOO_MANY_REQUESTS:
			title = CBMessages.getString("warning.title");
			message = CBMessages.getString("starter.minute.message");
			break;
		case MESSAGE_SEND_FAILED:
			title = CBMessages.getString("error.title");
			message = CBMessages.getString("send.message.failed");
			break;
		case UNAUTHORIZED:
		case FORBIDDEN:
			title = CBMessages.getString("error.title");
			message = CBMessages.getString("wrong.credentials");
			break;
		case NO_CONNECTION:
			title = CBMessages.getString("error.title");
			message = CBMessages.getString("no.connection");
			break;
		default:
			break;
		}

		return new String[] { title, message };
	}

	/**
	 * Show a generic file dialog to select a file from the pc
	 * 
	 * @param shell
	 * @param text
	 * @param extensions
	 * @return
	 */
	public static String showFileDialog(Shell shell, String text, String[] extensions, int buttonType) {
		return showFileDialog(shell, text, extensions, "", buttonType);
	}

	/**
	 * the function move a file into another folder
	 * 
	 * @author shahaal
	 * @param sourcePath
	 * @param targetPath
	 * @return
	 */
	public static boolean moveFile(String sourcePath, String targetPath) {

		boolean fileMoved = true;

		try {

			Files.move(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);

		} catch (Exception e) {
			LOGGER.error("Error during move of a file into another folder ", e);
			fileMoved = false;
			e.printStackTrace();
		}

		return fileMoved;
	}

	/**
	 * the method check if ict files are present
	 * 
	 * @return
	 */
	public static boolean isIctInstalled() {

		// if Ict file + foodEx2 + db exists then Ict is installed
		if (new File(GlobalUtil.ICT_FILE_PATH).exists() && new File(GlobalUtil.ICT_FOODEX2_FILE_PATH).exists()
				&& new File(GlobalUtil.ICT_DATABASE_DIR_PATH).exists()
				&& new File(GlobalUtil.ICT_UPDATE_FILE_PATH).exists())
			return true;

		return false;
	}

	/**
	 * Method used for updating the name of the shell
	 * 
	 * @param shell
	 */
	public static void startShellTextUpdate(final Shell shell) {

		// default
		shell.setText(APP_TITLE + " " + CBMessages.getString("App.Disconnected"));

		User.getInstance().addUserListener(new UserListener() {

			@Override
			public void userLevelChanged(UserAccessLevel newLevel) {
				final String connectedAs = (newLevel == UserAccessLevel.CATALOGUE_MANAGER)
						? CBMessages.getString("App.ConnectedCM")
						: CBMessages.getString("App.ConnectedDP");

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
}