package catalogue_browser_dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import global_manager.GlobalManager;
import sql.SQLExecutor;
import user_preferences.GlobalPreferenceDAO;
import utilities.GlobalUtil;

/**
 * Manager which manages physical actions on the db, as backups, db
 * compression...
 * 
 * @author shahaal
 * @author avonva
 *
 */
public class DatabaseManager {

	private static final Logger LOGGER = LogManager.getLogger(DatabaseManager.class);

	// name of the main database (i.e. the one which contains all the catalogues
	// metadata)
	private static final String MAIN_CAT_DB_FOLDER_NAME = "MAIN_CATS_DB";

	/**
	 * directory which contains all the official databases of the catalogue browser
	 * (i.e. the catalogues downloaded from the dcf or the catalogues imported
	 * through .ecf files)
	 */
	public static final String OFFICIAL_CAT_DB_FOLDER = "database" + System.getProperty("file.separator");

	/**
	 * Where the production catalogues are stored
	 */
	public static final String PRODUCTION_CAT_DB_FOLDER = OFFICIAL_CAT_DB_FOLDER + "PRODUCTION_CATS";

	/**
	 * Where the mtx catalogue is stored (in production)
	 */
	public static final String MTX_CAT_DB_FOLDER = PRODUCTION_CAT_DB_FOLDER + System.getProperty("file.separator")
			+ "CAT_MTX_DB";

	/**
	 * Where the test catalogues are stored
	 */
	public static final String TEST_CAT_DB_FOLDER = OFFICIAL_CAT_DB_FOLDER + "TEST_CATS";

	/**
	 * folder of the main database which contains all the catalogues metadata
	 */
	public static final String MAIN_CAT_DB_FOLDER = OFFICIAL_CAT_DB_FOLDER + MAIN_CAT_DB_FOLDER_NAME
			+ System.getProperty("file.separator");

	/**
	 * folder where the local catalogues are stored
	 */
	public static final String LOCAL_CAT_DB_FOLDER = OFFICIAL_CAT_DB_FOLDER + "LOCAL_CATS";

	/**
	 * Get a derby connection url to open the main db connection
	 * 
	 * @return
	 */
	public static String getMainDBURL() {
		return "jdbc:derby:" + GlobalUtil.getWorkingDir() + OFFICIAL_CAT_DB_FOLDER + MAIN_CAT_DB_FOLDER_NAME
				+ ";user=dbuser;password=dbuserpwd";

	}

	/**
	 * Get a derby connection url to stop the main db connection
	 * 
	 * @return
	 */
	private static String stopMainDBURL() {
		return "jdbc:derby:" + GlobalUtil.getWorkingDir() + OFFICIAL_CAT_DB_FOLDER + MAIN_CAT_DB_FOLDER_NAME
				+ ";shutdown=true";
	}

	/**
	 * Get a derby connection url to create the main db
	 * 
	 * @return
	 */
	public static String createMainDBURL() {
		return "jdbc:derby:" + GlobalUtil.getWorkingDir() + OFFICIAL_CAT_DB_FOLDER + MAIN_CAT_DB_FOLDER_NAME
				+ ";create=true";
	}

	/**
	 * Open the db connection with the currently open catalogue
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static Connection getMainDBConnection() throws SQLException {
		return DriverManager.getConnection(getMainDBURL());
	}

	/**
	 * Create the main database (the one which contains the catalogues metadata)
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void createMainDB() throws SQLException, IOException {

		// set a "create" connection
		try (Connection con = DriverManager.getConnection(createMainDBURL());
				SQLExecutor executor = new SQLExecutor(con);) {
			executor.exec(ClassLoader.getSystemResourceAsStream("createMainDB"));
		}

		// insert the default preferences into the main
		// catalogues database
		GlobalPreferenceDAO prefDao = new GlobalPreferenceDAO();
		prefDao.insertDefaultPreferences();
	}

	/**
	 * Connect to the main catalogues database if present, otherwise create it and
	 * then connect
	 * 
	 * @param DBString
	 * @throws SQLException
	 * @throws IOException
	 * @throws Exception
	 */
	public static void startMainDB() throws SQLException, IOException {

		try (Connection con = getMainDBConnection()) {

			// load the jdbc driver
			LOGGER.info("Starting embedded database...");
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");

			// check if the database is present or not
			LOGGER.info("Testing database connection...");

			con.close();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			LOGGER.error("Cannot start embedded database: embedded driver missing", e);

		} catch (SQLException e1) {

			LOGGER.info("Main database not present, creating it...");

			// Create a new database if possible
			createMainDB();

			// create official cat directory
			if (!GlobalUtil.fileExists(DatabaseManager.OFFICIAL_CAT_DB_FOLDER)) {
				new File(DatabaseManager.OFFICIAL_CAT_DB_FOLDER).mkdir();
			}

			// create production cat directory
			if (!GlobalUtil.fileExists(DatabaseManager.PRODUCTION_CAT_DB_FOLDER)) {
				new File(DatabaseManager.PRODUCTION_CAT_DB_FOLDER).mkdir();
			}

			// create test cat directory
			if (!GlobalUtil.fileExists(DatabaseManager.TEST_CAT_DB_FOLDER)) {
				new File(DatabaseManager.TEST_CAT_DB_FOLDER).mkdir();
			}

			LOGGER.info("Main database created");
		}
	}

	/**
	 * Add all the tables which were not release with the first version of the
	 * browser (if they are not present)
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void addNotExistingTables() throws SQLException, IOException {

		DatabaseMetaData dbm = getMainDBConnection().getMetaData();

		try (ResultSet rs = dbm.getTables(null, null, "USERS", null);) {

			if (!rs.next()) {

				// set a "create" connection
				try (Connection con = DriverManager.getConnection(getMainDBURL());
						SQLExecutor executor = new SQLExecutor(con);) {
					executor.exec(ClassLoader.getSystemResourceAsStream("Users"));
				}
			}

			rs.close();
		}
	}

	/**
	 * Compress the database to avoid fragmentation
	 * 
	 * TODO insert missing tables
	 */
	public static void compressDatabase() {

		LOGGER.info("Compressing database");

		// This will fail, if there are dependencies
		GlobalManager manager = GlobalManager.getInstance();

		try (Connection con = manager.getCurrentCatalogue().getConnection();
				CallableStatement cs = con.prepareCall("CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)");) {

			cs.setString(1, "APP");

			cs.setShort(3, (short) 1);

			cs.setString(2, "PARENT_TERM");
			cs.execute();
			cs.setString(2, "TERM_ATTRIBUTE");
			cs.execute();
			cs.setString(2, "TERM");
			cs.execute();
			cs.setString(2, "ATTRIBUTE");
			cs.execute();
			cs.setString(2, "HIERARCHY");
			cs.execute();

			cs.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}
	}

	/**
	 * Close the main db derby connection
	 */
	public static void stopMainDB() {
		try {
			LOGGER.info("Stopping database...");
			DriverManager.getConnection(stopMainDBURL());
		} catch (SQLException e) {
			LOGGER.info("Database disconnected");
		}
	}

	/**
	 * Create a path for a database which will contain the catalogue data
	 * 
	 * @param root
	 * @return
	 * @throws Exception
	 */
	public static String generateDBDirectory(String root, String folder) {

		// create the path of the database
		String path = root + folder;

		// create the directory
		GlobalUtil.createDirectory(path);

		// return the path
		return path;
	}

	public static boolean renameDB(File oldDb, File newDb) {
		return oldDb.renameTo(newDb);
	}

	/**
	 * Used with SQLScriptExec.java to run an sql script file
	 * 
	 * @param dbURL
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public static void executeSQLStatement(String dbURL, String sql) throws SQLException {

		try (Connection con = DriverManager.getConnection(dbURL); Statement stmt = con.createStatement();) {

			LOGGER.info("Executing " + sql);

			stmt.execute(sql);

			stmt.close();
			con.close();
		}
	}

	/**
	 * Number of attempts performed to move a file of the backup into its final
	 * position. On windows a move can transiently fail if another process (as an
	 * antivirus or an indexer) still holds a handle on the file.
	 */
	private static final int MOVE_ATTEMPTS = 10;

	/**
	 * Time waited between two move attempts. Together with MOVE_ATTEMPTS it gives
	 * a tolerance of about 3.6 seconds on a locked file.
	 */
	private static final long MOVE_RETRY_WAIT_MS = 400;

	/**
	 * Backup a catalogue database into the backup dir
	 * 
	 * @param catalogue the catalogue we want to backup
	 * @param backupDir the directory in which the backup will be created
	 * @throws SQLException if the backup cannot be created or if the created copy
	 *                      is not a database which can be read. In that case the
	 *                      backup directory is deleted, in order to avoid leaving
	 *                      an unusable database on the disk.
	 */
	public static void backupCatalogue(Catalogue catalogue, String backupDir) throws SQLException {

		try (Connection con = catalogue.getConnection();
				CallableStatement cs = con.prepareCall("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)");) {
			cs.setString(1, backupDir);
			cs.execute();
			cs.close();
			con.close();
		}

		// here we have created a backup of the database in the backupDir folder
		// since the backup copies also the database folder we have to extract
		// all the files contained in that folder to the parent folder in order
		// to have the right structure of files which is required to read
		// the catalogues databases

		try {

			flattenBackup(new File(catalogue.getDbPath()).getName(), new File(backupDir));

			assertReadableDatabase(backupDir);

		} catch (IOException | SQLException e) {

			LOGGER.error("Incomplete backup of " + catalogue + " in " + backupDir + ", deleting it", e);

			deleteQuietly(new File(backupDir));

			throw new SQLException("Cannot create a readable copy of " + catalogue + " in " + backupDir, e);
		}

		LOGGER.info("Database of " + catalogue + " copied in " + backupDir);
	}

	/**
	 * Move the content of the derby backup sub directory into the backup directory
	 * itself, since the catalogues databases are read from that level.
	 * 
	 * @param dbFolderName the name of the source database folder, which is also
	 *                     the name used by derby for the backup sub directory
	 * @param dir          the directory which contains the backup
	 * @throws IOException if the sub directory cannot be found, or if its content
	 *                     cannot be moved
	 */
	private static void flattenBackup(String dbFolderName, File dir) throws IOException {

		File subDir = new File(dir, dbFolderName);

		if (!subDir.isDirectory())
			subDir = getOnlySubDirectory(dir);

		File[] content = subDir.listFiles();

		if (content == null || content.length == 0)
			throw new IOException("The backup folder " + subDir + " is empty");

		for (File file : content)
			move(file, new File(dir, file.getName()));

		if (!subDir.delete())
			throw new IOException("Cannot delete the backup folder " + subDir);
	}

	/**
	 * Get the only sub directory contained in a directory
	 * 
	 * @param dir
	 * @return
	 * @throws IOException if there is not exactly one sub directory
	 */
	private static File getOnlySubDirectory(File dir) throws IOException {

		File[] content = dir.listFiles();

		if (content == null)
			throw new IOException("Cannot read the backup directory " + dir);

		File subDir = null;

		for (File file : content) {

			if (!file.isDirectory())
				continue;

			if (subDir != null)
				throw new IOException("Several backup folders found in " + dir + ", cannot choose one");

			subDir = file;
		}

		if (subDir == null)
			throw new IOException("No backup folder found in " + dir);

		return subDir;
	}

	/**
	 * Move a file of the backup into its final position, retrying if the move
	 * transiently fails.
	 * 
	 * @param source
	 * @param target
	 * @throws IOException if the file cannot be moved
	 */
	private static void move(File source, File target) throws IOException {

		IOException lastError = null;

		for (int attempt = 1; attempt <= MOVE_ATTEMPTS; attempt++) {

			try {

				Files.move(source.toPath(), target.toPath());
				return;

			} catch (IOException e) {

				lastError = e;

				LOGGER.warn("Cannot move " + source + " into " + target + ", attempt " + attempt + "/" + MOVE_ATTEMPTS);
			}

			try {
				Thread.sleep(MOVE_RETRY_WAIT_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while moving " + source + " into " + target, e);
			}
		}

		throw new IOException("Cannot move " + source + " into " + target, lastError);
	}

	/**
	 * Check that a folder contains a database which can be booted and read. An
	 * incomplete copy fails the boot with errors as "The conglomerate (x)
	 * requested does not exist", which would otherwise be discovered only later,
	 * when the catalogue is opened and it looks empty.
	 * 
	 * @param dbPath the path of the database
	 * @throws SQLException if the database cannot be read
	 */
	private static void assertReadableDatabase(String dbPath) throws SQLException {

		if (!new File(dbPath, "service.properties").isFile())
			throw new SQLException("Missing service.properties in " + dbPath);

		if (!new File(dbPath, "seg0").isDirectory())
			throw new SQLException("Missing seg0 folder in " + dbPath);

		String dbUrl = "jdbc:derby:" + dbPath + ";user=dbuser;password=dbuserpwd";

		try (Connection con = DriverManager.getConnection(dbUrl);
				Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM SYS.SYSTABLES");) {

			if (!rs.next() || rs.getInt(1) == 0)
				throw new SQLException("No table found in the database " + dbPath);

		} finally {

			try (Connection con = DriverManager.getConnection(dbUrl + ";shutdown=true");) {
			} catch (SQLException e) {
				LOGGER.info("Checked database " + dbPath + " shutted down with code : " + e.getErrorCode()
						+ " and state " + e.getSQLState());
			}
		}
	}

	/**
	 * Delete a directory without failing, used to clean up an invalid database
	 * 
	 * @param dir
	 */
	private static void deleteQuietly(File dir) {

		try {
			GlobalUtil.deleteFileCascade(dir);
		} catch (IOException e) {
			LOGGER.error("Cannot delete " + dir, e);
		}
	}

	/**
	 * Delete the catalogue database from disk.
	 * 
	 * @param catalogue
	 * @throws IOException
	 */
	public static void deleteDb(Catalogue catalogue) throws IOException {

		System.gc();

		// close all the catalogue connections
		catalogue.closeConnection();

		// delete the DB with all the subfiles
		GlobalUtil.deleteFileCascade(new File(catalogue.getDbPath()));

		// check if no catalogue is present in the parent folder, if so delete also the
		// parent directory (the dir which contains all the catalogue versions)
		File parent = new File(catalogue.getDbPath()).getParentFile();
		if (parent.listFiles().length == 0)
			parent.delete();
	}

	/**
	 * Copy a catalogue database into another folder
	 * 
	 * @param catalogue
	 * @param path
	 * @throws IOException
	 */
	public static void copyDb(Catalogue catalogue, String destPath) throws IOException {
		copyFileInto(catalogue.getDbPath(), destPath);
	}

	public static void copyFileInto(File source, File dest) throws IOException {

		FileInputStream input = null;
		FileOutputStream out = null;
		FileChannel sourceChannel = null;
		FileChannel destChannel = null;

		input = new FileInputStream(source);
		sourceChannel = input.getChannel();

		out = new FileOutputStream(dest);
		destChannel = out.getChannel();

		destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());

		out.close();
		input.close();
		sourceChannel.close();
		destChannel.close();
	}

	/**
	 * Copy a folder and its files into another path
	 * 
	 * @param sourcePath the source folder
	 * @param destPath   the destination folder
	 * @throws IOException
	 */
	public static void copyFileInto(String sourcePath, String destPath) throws IOException {

		// get the source from the catalogue
		File source = new File(sourcePath);

		// set the destination
		File dest = new File(destPath);

		copyFileInto(source, dest);
	}

	/**
	 * Copy an entire folder (source) into the destination folder
	 * 
	 * @param source      the path of the source folder
	 * @param destination the path of the destination folder
	 * @throws IOException
	 */
	public static void copyFolder(String source, String destination) throws IOException {
		copyFolder(new File(source), new File(destination));
	}

	/**
	 * Copy an entire folder (source) into the destination folder
	 * 
	 * @param source      file identifing the source folder
	 * @param destination file identifing the destination folder
	 * @throws IOException
	 */
	public static void copyFolder(File source, File destination) throws IOException {

		LOGGER.info("Copying " + source + " into " + destination);

		// if the source is a directory, we recursively
		// copy all its files
		if (source.isDirectory()) {

			// create the destination folder if
			// it does not exist
			if (!destination.exists()) {
				destination.mkdirs();
			}

			// get the source file and copy them
			// into the destination folder
			String files[] = source.list();

			for (String file : files) {

				File srcFile = new File(source, file);
				File destFile = new File(destination, file);

				copyFolder(srcFile, destFile);
			}
		} else { // if instead we have a file

			InputStream in = null;
			OutputStream out = null;
			// copy the contents using file streams
			in = new FileInputStream(source);
			out = new FileOutputStream(destination);

			byte[] buffer = new byte[1024];

			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}

			// close streams
			in.close();
			out.close();
		}
	}

	/**
	 * Restore the backup for a catalogue, we reset all the changes to the database
	 * identified by the path {@link Catalogue#getBackupDbPath()}
	 * 
	 * @param catalogue
	 * @throws IOException
	 */
	public static void restoreBackup(Catalogue catalogue) throws IOException {

		File file = new File(catalogue.getBackupDbPath());

		// if the file does not exists => exception
		if (!file.exists())
			throw new IOException("The backup folder was not found. Cannot proceed.");

		LOGGER.info("Restoring " + catalogue + " from " + catalogue.getBackupDbPath());

		// close the catalogue (we can restore backups only for
		// opened catalogues)
		catalogue.close();

		// delete the current database
		deleteDb(catalogue);

		// copy the backup into the catalogue database
		copyFolder(catalogue.getBackupDbPath(), catalogue.getDbPath());

		// open the catalogue
		catalogue.open();
	}

	/**
	 * Create a generic catalogue db in the db path directory
	 * 
	 * @param dbPath
	 * @throws IOException
	 */
	public static void createCatalogueDatabase(String dbPath) throws IOException {

		// create the db url path, the create = true variable indicates that if
		// the db is not present it will be created
		String dbURL = "jdbc:derby:" + dbPath;

		try (Connection con = DriverManager.getConnection(dbURL + ";create=true");
				SQLExecutor executor = new SQLExecutor(con);) {

			// open the connection to create the database
			// important! do not remove this line of code since
			// otherwise the database will not be created

			// create the catalogue db structure
			executor.exec(ClassLoader.getSystemResourceAsStream("createCatalogueDB"));

			// close the connection
			con.close();

			// shutdown the connection, by default this operation throws an exception
			// but the command is correct! We close the connection since if we try
			// to delete a database which is just downloaded an error is shown since
			// the database is in use
			try {
				LOGGER.info("Closing connection with " + dbURL);
				try (Connection con2 = DriverManager.getConnection(dbURL + ";shutdown=true");) {
				}
			} catch (Exception e) {
			}

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}
	}
}
