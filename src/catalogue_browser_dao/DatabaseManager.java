package catalogue_browser_dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import catalogue_object.Catalogue;
import global_manager.GlobalManager;
import sql.SQLScriptExec;
import user_preferences.UIPreference;
import user_preferences.UIPreferenceDAO;
import utilities.GlobalUtil;

/**
 * Manager which manages physical actions on the db, as
 * backups, db compression...
 * @author avonva
 *
 */
public class DatabaseManager {
	
	// name of the main database (i.e. the one which contains all the catalogues metadata)
	private static final String MAIN_CAT_DB_FOLDER_NAME = "CataloguesMetadata";

	/**
	 *  directory which contains all the official databases of the catalogue browser (i.e.
	 *  the catalogues downloaded from the dcf or the catalogues imported through .ecf files)
	 */
	public static final String OFFICIAL_CAT_DB_FOLDER = "Database" + System.getProperty("file.separator");

	/**
	 *  folder of the main database which contains all the catalogues
	 *  metadata
	 */
	public static final String MAIN_CAT_DB_FOLDER = OFFICIAL_CAT_DB_FOLDER + 
			MAIN_CAT_DB_FOLDER_NAME + System.getProperty("file.separator");

	/**
	 *  folder where the local catalogues are stored
	 */
	public static final String LOCAL_CAT_DB_FOLDER = OFFICIAL_CAT_DB_FOLDER + 
			"LocalCatalogues" + System.getProperty("file.separator");

	/**
	 * Get a derby connection url to open the main db connection
	 * @return
	 */
	private static String getMainDBURL ( ) {
		return "jdbc:derby:" + OFFICIAL_CAT_DB_FOLDER + MAIN_CAT_DB_FOLDER_NAME + ";user=dbuser;password=dbuserpwd";
	}

	/**
	 * Get a derby connection url to stop the main db connection
	 * @return
	 */
	private static String stopMainDBURL ( ) {
		return "jdbc:derby:" + OFFICIAL_CAT_DB_FOLDER + MAIN_CAT_DB_FOLDER_NAME + ";shutdown=true";
	}

	/**
	 * Get a derby connection url to create the main db
	 * @return
	 */
	private static String createMainDBURL ( ) {
		return "jdbc:derby:" + OFFICIAL_CAT_DB_FOLDER + MAIN_CAT_DB_FOLDER_NAME + ";create=true";
	}

	/**
	 * Open the db connection with the currently open catalogue
	 * @return
	 * @throws SQLException
	 */
	public static Connection getMainDBConnection () throws SQLException {

		return DriverManager.getConnection( getMainDBURL() );
	}

	/**
	 * Create the main database (the one which contains the catalogues metadata)
	 * @throws SQLException 
	 */
	private static void createMainDB () {

		// create the database
		try {

			// set a "create" connection
			DriverManager.getConnection( createMainDBURL() );

			// sql script to create the database
			SQLScriptExec script = new SQLScriptExec( getMainDBURL(), 
					ClassLoader.getSystemResourceAsStream( "createMainDB" ) );

			script.exec();
			
			// insert the default preferences into the main
			// catalogues database
			UIPreferenceDAO prefDao = new UIPreferenceDAO();
			prefDao.insertDefaultPreferences();

		} catch ( IOException | SQLException e ) {
			e.printStackTrace();
			return;
		}
	}


	/**
	 * Connect to the main catalogues database if present, otherwise create it and then connect
	 * @param DBString
	 * @throws Exception
	 */
	public static void startMainDB () {

		try {

			// load the jdbc driver
			System.out.println( "Starting embedded database..." );
			Class.forName( "org.apache.derby.jdbc.EmbeddedDriver" );

			// check if the database is present or not
			System.out.println( "Testing database connection..." );

			Connection con = getMainDBConnection();
			con.close();

		} catch ( ClassNotFoundException e ) {
			e.printStackTrace();
			System.err.println ( "Cannot start embedded database: embedded driver missing" );

		} catch ( SQLException e1 ) {

			System.err.println( "Main database not present, trying to create a new one" );

			// Create a new database if possible
			createMainDB ();
		}
	}
	


	/**
	 * Compress the database to avoid fragmentation
	 * TODO insert missing tables
	 */
	public static void compressDatabase () {
		
		Connection con = null;

		// This will fail, if there are dependencies

		try {
			
			GlobalManager manager = GlobalManager.getInstance();
			con = manager.getCurrentCatalogue().getConnection();

			// compact the db table by table
			CallableStatement cs = con.prepareCall( "CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)" );

			cs.setString( 1, "APP" );

			cs.setShort( 3, (short) 1 );
			
			cs.setString( 2, "PARENT_TERM" );
			cs.execute();
			cs.setString( 2, "TERM_ATTRIBUTE" );
			cs.execute();
			cs.setString( 2, "TERM" );
			cs.execute();
			cs.setString( 2, "ATTRIBUTE" );
			cs.execute();
			cs.setString( 2, "HIERARCHY" );
			cs.execute();
			
			cs.close();
			con.close();
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Close the main db derby connection
	 */
	public static void stopMainDB ( ) {
		try {
			System.out.print( "Stopping database..." );
			DriverManager.getConnection( stopMainDBURL() );
		} catch ( SQLException e ) {
			System.out.println( "Database disconnected" );
		}
	}

	/**
	 * Create a path for a database which will contain the catalogue data
	 * @param root
	 * @return
	 * @throws Exception 
	 */
	private static String generateDBDirectory ( String root, String folder ) {

		// create the path of the database
		String path = root + folder;

		// create the directory
		GlobalUtil.createDirectory ( path );

		// return the path
		return path;
	}
	
	public static String generateDBDirectory ( String root, Catalogue catalogue ) {

		return generateDBDirectory( root, "CAT_" + catalogue.getCode() + "_DB" );
	}
	

	/**
	 * Used with SQLScriptExec.java to run an sql script file
	 * @param dbURL
	 * @param sql
	 * @return
	 */
	public static boolean executeSQLStatement ( String dbURL, String sql ) {
		
		System.out.println( "Statement to execute: " + sql );
		
		try {
			
			Connection con = DriverManager.getConnection( dbURL );
			Statement stmt = con.createStatement();
			
			stmt.execute( sql );
			
			stmt.close();
			con.close();
			
			return true;
			
		} catch ( SQLException sqle ) {
			sqle.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Backup a catalogue database into the backup dir
	 * @param catalogue the catalogue we want to backup
	 * @param backupDir the directory in which the backup will be created
	 * @throws SQLException
	 */
	public static void backupCatalogue( Catalogue catalogue, String backupDir ) throws SQLException {
		
		Connection con = catalogue.getConnection();
		CallableStatement cs = con.prepareCall( "CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)" );
		
		cs.setString( 1, backupDir );
		cs.execute();
		cs.close();
		con.close();
		
		System.out.println( "Database of " + catalogue + " copied in " + backupDir );
		
		// here we have created a backup of the database in the backupDir folder
		// since the backup copies also the database folder we have to extract
		// all the files contained in that folder to the parent folder in order
		// to have the right structure of files which is required to read
		// the catalogues databases
		
		File dir = new File ( backupDir );
		
		File[] list = dir.listFiles();
		
		if ( list.length == 0 ) {
			System.err.println ( "No file was copied" );
			return;
		}
		
		File subDir = list[0];
		
		// move all the files of the sub directory
		// in the parent directory
		for ( File file : subDir.listFiles() ) {
			file.renameTo( new File( dir, file.getName() ) );
		}
		
		// delete the empty folder
		subDir.delete();
	}
	
	/**
	 * Delete the catalogue database from disk.
	 * @param catalogue
	 * @throws IOException
	 */
	public static void deleteDb ( Catalogue catalogue ) throws IOException {
		
		// delete the DB with all the subfiles
		GlobalUtil.deleteFileCascade ( new File( catalogue.getDbFullPath() ) );

		// check if no catalogue is present in the parent folder, if so delete also the
		// parent directory (the dir which contains all the catalogue versions)
		File parent = new File( catalogue.getDbFullPath() ).getParentFile();
		if ( parent.listFiles().length == 0 )
			parent.delete();
	}
	
	/**
	 * Copy a catalogue database into another folder
	 * @param catalogue
	 * @param path
	 * @throws IOException
	 */
	public static void copyDb ( Catalogue catalogue, String destPath ) throws IOException {
		copyDb ( catalogue.getDbFullPath(), destPath );
	}
	
	/**
	 * Copy a folder and its files into another path
	 * @param sourcePath the source folder
	 * @param destPath the destination folder
	 * @throws IOException
	 */
	public static void copyDb ( String sourcePath, String destPath ) throws IOException {
		
		// get the source from the catalogue
		File source = new File ( sourcePath );

		// set the destination
		File dest = new File ( destPath );

		FileInputStream input = null;
		FileOutputStream out = null;
		FileChannel sourceChannel = null;
		FileChannel destChannel = null;

		input = new FileInputStream( source );
		sourceChannel = input.getChannel();

		out = new FileOutputStream( dest );
		destChannel = out.getChannel();

		destChannel.transferFrom( sourceChannel, 0, sourceChannel.size() );

		out.close();
		input.close();
		sourceChannel.close();
		destChannel.close();
	}
	
	/**
	 * Copy an entire folder (source) into the destination folder
	 * @param source the path of the source folder
	 * @param destination the path of the destination folder
	 * @throws IOException 
	 */
	public static void copyFolder( String source, String destination ) throws IOException {
		copyFolder ( new File(source), new File (destination) );
	}
	
	/**
	 * Copy an entire folder (source) into the destination folder
	 * @param source file identifing the source folder
	 * @param destination file identifing the destination folder
	 * @throws IOException 
	 */
	public static void copyFolder( File source, File destination ) throws IOException {
		
		// if the source is a directory, we recursively
		// copy all its files
		if ( source.isDirectory() ) {
			
			// create the destination folder if
			// it does not exist
			if ( !destination.exists() ) {
				destination.mkdirs();
			}

			// get the source file and copy them
			// into the destination folder
			String files[] = source.list();

			for (String file : files) {
				
				File srcFile = new File( source, file );
				File destFile = new File( destination, file );

				copyFolder( srcFile, destFile );
			}
		}
		else {  // if instead we have a file

			InputStream in = null;
			OutputStream out = null;
			// copy the contents using file streams
			in = new FileInputStream( source );
			out = new FileOutputStream( destination );

			byte[] buffer = new byte[1024];

			int length;
			while ( (length = in.read(buffer) ) > 0 ) {
				out.write(buffer, 0, length);
			}

			// close streams
			in.close();
			out.close();
		}
	}

	/**
	 * Restore the backup for a catalogue, we reset all the
	 * changes to the database identified by the path {@link Catalogue#getBackupDbPath()}
	 * @param catalogue
	 * @throws IOException 
	 */
	public static void restoreBackup ( Catalogue catalogue ) throws IOException {
		
		File file = new File ( catalogue.getBackupDbPath() );
		
		// if the file does not exists => exception
		if ( !file.exists() )
			throw new IOException ( "The backup folder was not found. Cannot proceed." );
		
		System.out.println ( "Restoring " + catalogue + " from " + catalogue.getBackupDbPath() );
		
		// close the catalogue (we can restore backups only for
		// opened catalogues)
		catalogue.close();
		
		// delete the current database
		deleteDb ( catalogue );
		
		// copy the backup into the catalogue database
		copyFolder ( catalogue.getBackupDbPath(), catalogue.getDbFullPath() );
		
		// open the catalogue
		catalogue.open();
	}
}
