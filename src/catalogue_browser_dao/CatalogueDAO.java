package catalogue_browser_dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import catalogue_object.Catalogue;
import catalogue_object.CatalogueBuilder;
import dcf_user.User;
import sql.SQLScriptExec;
import utilities.GlobalUtil;


/**
 * DAO used to manage catalogues (Catalogue table of the database is considered)
 * @author avonva
 *
 */
public class CatalogueDAO implements CatalogueEntityDAO<Catalogue> {
	
	/**
	 * Insert a new catalogue into the main catalogues database. Moreover,
	 * the catalogue database is also created.
	 * @param catalogue
	 */
	public int insert ( Catalogue catalogue ) {
		
		int id = -1;

		try {

			// open the connection
			Connection con = DatabaseManager.getMainDBConnection();

			// insert the catalogue object with its parameters in the CATALOGUE table
			PreparedStatement stmt = con.prepareStatement( "insert into app.CATALOGUE ("
					+ "CAT_VERSION,"
					+ "CAT_CODE,"
					+ "CAT_NAME,"
					+ "CAT_LABEL,"
					+ "CAT_SCOPENOTE,"
					+ "CAT_TERM_CODE_MASK,"
					+ "CAT_TERM_CODE_LENGTH,"
					+ "CAT_TERM_MIN_CODE,"
					+ "CAT_ACCEPT_NON_STANDARD_CODES,"
					+ "CAT_GENERATE_MISSING_CODES,"
					+ "CAT_STATUS,"
					+ "CAT_GROUPS,"
					+ "CAT_LAST_UPDATE,"
					+ "CAT_VALID_FROM,"
					+ "CAT_VALID_TO,"
					+ "CAT_DEPRECATED,"
					+ "CAT_IS_LOCAL,"
					+ "CAT_DB_PATH,"
					+ "CAT_DB_BACKUP_PATH,"
					+ "CAT_FORCED_COUNT ) values (? ,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", 
					Statement.RETURN_GENERATED_KEYS );

			// set the query parameters
			stmt.setString ( 1,  catalogue.getVersion() );
			stmt.setString ( 2,  catalogue.getCode() );
			stmt.setString ( 3,  catalogue.getName() );
			stmt.setString ( 4,  catalogue.getLabel() );
			stmt.setString ( 5,  catalogue.getScopenotes() );
			stmt.setString ( 6,  catalogue.getTermCodeMask() );
			stmt.setInt    ( 7,  catalogue.getTermCodeLength() );
			stmt.setString ( 8,  catalogue.getTermMinCode() );
			stmt.setBoolean( 9,  catalogue.isAcceptNonStandardCodes() );
			stmt.setBoolean( 10, catalogue.isGenerateMissingCodes() );
			stmt.setString ( 11, catalogue.getStatus() );
			stmt.setString ( 12, catalogue.getCatalogueGroups() );

			stmt.setNull ( 13, java.sql.Types.TIMESTAMP );  // last update, we do not know this value

			if ( catalogue.getValidFrom() != null )
				stmt.setTimestamp ( 14, GlobalUtil.toSQLTimestamp( catalogue.getValidFrom() ) );
			else
				stmt.setNull ( 14, java.sql.Types.TIMESTAMP );

			stmt.setNull ( 15, java.sql.Types.TIMESTAMP );  // validTo, we do not know this value

			stmt.setBoolean( 16, catalogue.isDeprecated() );

			// set if the catalogue is local or not
			stmt.setBoolean( 17, catalogue.isLocal() );  
			
			// create the db path using the catalogue code and version
			stmt.setString( 18, catalogue.createDbDir() );
			
			stmt.setString( 19, catalogue.getBackupDbPath() );
			
			stmt.setInt( 20, catalogue.getForcedCount() );

			// execute the query
			stmt.execute();

			// get the generated id and return it
			ResultSet rs = stmt.getGeneratedKeys();
			if ( rs.next() ) {

				// get the new id from the db
				id = rs.getInt( 1 );

				// set the id to the catalogue object
				catalogue.setId( id );
			}

			// close the result set
			rs.close();

			// close the statement
			stmt.close();

			// close the connection
			con.close();
		}
		catch ( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}
	

	/**
	 * Add the meta data of the catalogue to the meta data table
	 * The meta data table contains the meta data of the catalogues and the
	 * external db path where the real data are actually stored for each catalogue
	 * @param catalogue
	 * @param folder the folder where the catalogue database should be created
	 * @return the db path where the catalogue data are stored
	 */
	public boolean update ( Catalogue catalogue ) {
		
		try {

			// open the connection
			Connection con = DatabaseManager.getMainDBConnection();

			// insert the catalogue object with its parameters in the CATALOGUE table
			PreparedStatement stmt = con.prepareStatement( "update APP.CATALOGUE set "
					+ "CAT_VERSION = ?,"
					+ "CAT_CODE = ?,"
					+ "CAT_NAME = ?,"
					+ "CAT_LABEL = ?,"
					+ "CAT_SCOPENOTE = ?,"
					+ "CAT_TERM_CODE_MASK = ?,"
					+ "CAT_TERM_CODE_LENGTH = ?,"
					+ "CAT_TERM_MIN_CODE = ?,"
					+ "CAT_ACCEPT_NON_STANDARD_CODES = ?,"
					+ "CAT_GENERATE_MISSING_CODES = ?,"
					+ "CAT_STATUS = ?,"
					+ "CAT_GROUPS = ?,"
					+ "CAT_LAST_UPDATE = ?,"
					+ "CAT_VALID_FROM = ?,"
					+ "CAT_VALID_TO = ?,"
					+ "CAT_DEPRECATED = ?,"
					+ "CAT_IS_LOCAL = ?,"
					+ "CAT_DB_PATH = ?,"
					+ "CAT_DB_BACKUP_PATH = ?,"
					+ "CAT_FORCED_COUNT = ? "
					+ " where CAT_ID = ?" );

			// set the query parameters
			stmt.setString ( 1,  catalogue.getVersion() );
			stmt.setString ( 2,  catalogue.getCode() );
			stmt.setString ( 3,  catalogue.getName() );
			stmt.setString ( 4,  catalogue.getLabel() );
			stmt.setString ( 5,  catalogue.getScopenotes() );
			stmt.setString ( 6,  catalogue.getTermCodeMask() );
			stmt.setInt    ( 7,  catalogue.getTermCodeLength() );
			stmt.setString ( 8,  catalogue.getTermMinCode() );
			stmt.setBoolean( 9,  catalogue.isAcceptNonStandardCodes() );
			stmt.setBoolean( 10,  catalogue.isGenerateMissingCodes() );
			stmt.setString ( 11, catalogue.getStatus() );
			stmt.setString ( 12, catalogue.getCatalogueGroups() );
			
			stmt.setNull ( 13, java.sql.Types.TIMESTAMP );  // last update, we do not know this value
			
			if ( catalogue.getValidFrom() != null )
				stmt.setTimestamp ( 14, GlobalUtil.toSQLTimestamp( catalogue.getValidFrom() ) );
			else
				stmt.setNull ( 14, java.sql.Types.TIMESTAMP );
			
			stmt.setNull ( 15, java.sql.Types.TIMESTAMP );  // validTo, we do not know this value

			stmt.setBoolean( 16, catalogue.isDeprecated() );
			
			// set if the catalogue is local or not
			stmt.setBoolean( 17, catalogue.isLocal() );  
			
			stmt.setString( 18, catalogue.getDbFullPath() );
			stmt.setString( 19, catalogue.getBackupDbPath() );  

			stmt.setInt( 20, catalogue.getForcedCount() );
			
			stmt.setInt ( 21, catalogue.getId() );
			
			// execute the query
			stmt.executeUpdate();

			// close the statement
			stmt.close();

			// close the connection
			con.close();
			
			return true;
		}
		catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Delete a catalogue from the local database (both metadata and data)
	 * @param catalogue
	 */
	public boolean delete ( Catalogue catalogue ) {

		try {

			// delete the catalogue database
			DatabaseManager.deleteDb( catalogue );

			// open the connection of the general DB to remove the catalogue entry
			// from the local master database
			Connection con = DatabaseManager.getMainDBConnection();
			
			// select the catalogue by code
			PreparedStatement stmt = con.prepareStatement( "delete from APP.CATALOGUE where CAT_ID = ?" );

			stmt.setInt( 1, catalogue.getId() );

			// execute the query and close the connections
			stmt.executeUpdate();

			stmt.close();

			con.close();
			
			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

	/**
	 * Given a result set with catalogues meta data inside it, this function will
	 * create a catalogue object taking the meta data from the result set
	 * The function takes the current item of the results set and tries to get the
	 * catalogue data. Use a while(rs.next) loop for a result set with more than one catalogue
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public Catalogue getByResultSet ( ResultSet rs ) throws SQLException {

		// create a catalogue using a builder
		CatalogueBuilder builder = new CatalogueBuilder();

		// set the catalogue meta data and create the catalogue object
		builder.setId( rs.getInt( "CAT_ID" ) );
		builder.setVersion( rs.getString( "CAT_VERSION" ) );
		builder.setCode( rs.getString( "CAT_CODE" ) );
		builder.setName( rs.getString( "CAT_NAME" ) );
		builder.setLabel( rs.getString( "CAT_LABEL" ) );
		builder.setScopenotes( rs.getString( "CAT_SCOPENOTE" ) );
		builder.setTermCodeMask( rs.getString( "CAT_TERM_CODE_MASK" ) );
		builder.setTermCodeLength( rs.getString( "CAT_TERM_CODE_LENGTH" ) );
		builder.setTermMinCode( rs.getString( "CAT_TERM_MIN_CODE" ) );
		builder.setAcceptNonStandardCodes( rs.getBoolean( "CAT_ACCEPT_NON_STANDARD_CODES" ) );
		builder.setGenerateMissingCodes( rs.getBoolean( "CAT_GENERATE_MISSING_CODES" ) );
		builder.setStatus( rs.getString( "CAT_STATUS" ) );
		builder.setCatalogueGroups( rs.getString( "CAT_GROUPS" ) );

		// set the dates with the adequate checks
		java.sql.Timestamp ts = rs.getTimestamp( "CAT_LAST_UPDATE" );
		
		if ( ts != null )
			builder.setLastUpdate( ts );

		ts = rs.getTimestamp( "CAT_VALID_FROM" );
		if ( ts != null )
			builder.setValidFrom( ts );

		ts = rs.getTimestamp( "CAT_VALID_TO" );
		if ( ts != null )
			builder.setValidTo( ts );

		builder.setDeprecated( rs.getBoolean( "CAT_DEPRECATED" ) );
		
		builder.setDbFullPath( rs.getString( "CAT_DB_PATH" ) );
		
		builder.setBackupDbPath( rs.getString( "CAT_DB_BACKUP_PATH" ) );

		builder.setLocal( rs.getBoolean( "CAT_IS_LOCAL" ) );
		
		builder.setForcedCount( rs.getInt( "CAT_FORCED_COUNT" ) );
		
		builder.setReserveUsername( rs.getString( "RESERVE_USERNAME" ) );
		
		builder.setReserveLevel( rs.getString( "RESERVE_LEVEL" ) );
		
		// return the catalogue
		return builder.build();
	}
	
	/**
	 * Create a generic catalogue db in the db path directory
	 * @param dbPath
	 */
	public void createDBTables ( String dbPath ) {

		try {

			// create the db url path, the create = true variable indicates that if
			// the db is not present it will be created
			String dbURL = "jdbc:derby:" + dbPath;

			// Set the driver for the connection
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();

			// open the connection to create the database
			// important! do not remove this line of code since
			// otherwise the database will not be created
			Connection con = DriverManager.getConnection( dbURL + ";create=true" );

			// create the catalogue db structure
			SQLScriptExec script = new SQLScriptExec( dbURL, 
					ClassLoader.getSystemResourceAsStream( "createCatalogueDB" ) );

			// execute the script
			script.exec();

			// close the connection
			con.close();


			// shutdown the connection, by default this operation throws an exception
			// but the command is correct! We close the connection since if we try
			// to delete a database which is just downloaded an error is shown since
			// the database is in use
			try {
				System.out.println ( "Closing connection with " + dbURL );
				DriverManager.getConnection( dbURL + ";shutdown=true");
			}
			catch ( Exception e ) {
			}

		} catch ( InstantiationException | IllegalAccessException | 
				ClassNotFoundException | IOException | SQLException e ) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Remove all the records of the catalogue database
	 * 
	 * @param con
	 * @throws SQLException
	 * @throws NoCatalogueOpenException 
	 */
	public void deleteDBRecords ( Catalogue catalogue ) throws SQLException {
		
		Connection con = catalogue.getConnection();
		
		// we first remove the relationships then the entities
		Statement stmt = con.createStatement();
		stmt.execute( "DELETE FROM APP.PICKLIST_TERM" );
		stmt.execute( "DELETE FROM APP.RECENT_TERM" );
		stmt.execute( "DELETE FROM APP.PICKLIST" );
		stmt.execute( "DELETE FROM APP.PREFERENCE" );
		stmt.execute( "DELETE FROM APP.PARENT_TERM" );
		stmt.execute( "DELETE FROM APP.TERM_ATTRIBUTE" );
		stmt.execute( "DELETE FROM APP.TERM_TYPE" );
		stmt.execute( "DELETE FROM APP.ATTRIBUTE" );
		stmt.execute( "DELETE FROM APP.HIERARCHY" );
		stmt.execute( "DELETE FROM APP.TERM" );
				
		stmt.close();
		con.close();
	}
	
	/**
	 * Check if a catalogue is already inserted into the db or not
	 * @param catalogue
	 * @return the db path of the already present catalogue database if found, null otherwise
	 */
	public boolean hasCatalogue ( Catalogue catalogue ) {
		
		boolean found = false;
		
		Connection con;
		
		// we check the catalogue code and then we check the version if the catalogue is not local
		// note that if the catalogue is local the version check is automatically eliminated
		String query = "select * from APP.CATALOGUE where CAT_ID = ?";
		
		try {
			
			con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.clearParameters();
			
			stmt.setInt( 1, catalogue.getId() );
			
			ResultSet rs = stmt.executeQuery();
			
			// get if something was found
			found = rs.next();
			
			rs.close();
			stmt.close();
			con.close();
			
		} catch (SQLException exception ) {
			exception.printStackTrace();
		}
		
		return found;
	}


	/**
	 * Given a result set with catalogues meta data inside it, this function will
	 * create a catalogue object taking the meta data from the result set
	 * The function takes the current item of the results set and tries to get the
	 * catalogue data. Use a while(rs.next) loop for a result set with more than one catalogue
	 * USED FOR EXCEL CATALOGUES (they have a different naming convention to the ones used in the DB)
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public Catalogue getCatalogueFromExcel ( ResultSet rs ) throws SQLException {

		// create a catalogue using a builder
		CatalogueBuilder builder = new CatalogueBuilder();

		// set the catalogue meta data and create the catalogue object
		builder.setVersion( rs.getString( "version" ) );
		builder.setCode( rs.getString( "code" ) );
		builder.setName( rs.getString( "name" ) );
		builder.setLabel( rs.getString( "label" ) );
		builder.setScopenotes( rs.getString( "scopeNote" ) );
		builder.setTermCodeMask( rs.getString( "termCodeMask" ) );
		builder.setTermCodeLength( rs.getString( "termCodeLength" ) );
		builder.setTermMinCode( rs.getString( "termMinCode" ) );
		builder.setAcceptNonStandardCodes( rs.getBoolean( "acceptNonStandardCodes" ) );
		builder.setGenerateMissingCodes( rs.getBoolean( "generateMissingCodes" ) );
		builder.setStatus( rs.getString( "status" ) );
		builder.setCatalogueGroups( rs.getString( "catalogueGroups" ) );

		// set the dates with the adequate checks
		java.sql.Timestamp ts = rs.getTimestamp( "lastUpdate" );
		
		if ( ts != null )
			builder.setLastUpdate( ts );

		ts = rs.getTimestamp( "validFrom" );
		if ( ts != null )
			builder.setValidFrom( ts );

		ts = rs.getTimestamp( "validTo" );
		if ( ts != null )
			builder.setValidTo( ts );

		builder.setDeprecated( rs.getBoolean( "deprecated" ) );
		
		// return the catalogue
		return builder.build();
	}
	
	/**
	 * Get the last release of the selected catalogue
	 * @param catalogue
	 * @return
	 */
	public Catalogue getLastVersionByCode ( String code ) {

		// output
		Catalogue lastVersion = null;
		
		try {
			
			// open the connection
			Connection con = DatabaseManager.getMainDBConnection();

			// get all the catalogues codes related to the last version of each catalogue.
			// QUERY:
			// Get first for each catalogue the max version of it. Then get all the information related to the
			// last release of the catalogue using a self join on cat code and max_version
			// we get also the information related to the reserved catalogues using a LEFT join
			PreparedStatement stmt = con.prepareStatement( 
					"select * from APP.CATALOGUE C "
					+ "left join APP.RESERVED_CATALOGUE RC "
					+ "on C.CAT_ID = RC.CAT_ID "
					+ "where C.CAT_CODE = ?");

			stmt.setString( 1, code );

			// execute the query
			ResultSet rs = stmt.executeQuery();

			ArrayList<Catalogue> catalogues = new ArrayList<>();
			
			// get the catalogues data and add them to the output array list
			while ( rs.next() )
				catalogues.add( getByResultSet ( rs ) );
			
			// if no catalogues return
			if ( catalogues.isEmpty() )
				return null;
			
			// sort the catalogues according to their version
			catalogues.sort( new Comparator<Catalogue>() {
				public int compare( Catalogue o1, Catalogue o2 ) {
					
					boolean inv1 = o1.getCatalogueVersion().isInvalid();
					boolean inv2 = o2.getCatalogueVersion().isInvalid();
					
					boolean older = o1.isOlder( o2 );
					
					// if first invalid => second before
					if ( inv1 && !inv2 )
						return 1;
					
					// if second invalid => first before
					if ( !inv1 && inv2 )
						return -1;
					
					if ( older )
						return 1;
					else
						return -1;
				};
			});
			
			// get the most recent catalogue
			lastVersion = catalogues.get(0);
			
			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return lastVersion;
	}

	/**
	 * Get the catalogues which are the last version of
	 * the ones which were downloaded locally. We use this feature to check
	 * if there is an update of a catalogue
	 * @return
	 */
	public ArrayList <Catalogue> getLastReleaseCatalogues () {

		// output array
		ArrayList < Catalogue > catalogues = new ArrayList<>();

		String query = "select * "
				+ "from APP.CATALOGUE C "
				+ "left join APP.RESERVED_CATALOGUE RC "
				+ "on C.CAT_ID = RC.CAT_ID inner join ( "
				+ "select MAX(CAT_VALID_FROM) as MAX_VF, CAT_CODE "
				+ "from APP.CATALOGUE "
				+ "group by CAT_CODE) TEMP "
				+ "on C.CAT_CODE = TEMP.CAT_CODE and C.CAT_VALID_FROM = TEMP.MAX_VF";
		
		try {
			// open the connection
			Connection con = DatabaseManager.getMainDBConnection();

			// get all the catalogues codes related to the last version of each catalogue.
			// QUERY:
			// Get first for each catalogue the max version of it. Then get all the information related to the
			// last release of the catalogue using a self join on cat code and max_version
			PreparedStatement stmt = con.prepareStatement( query );

			
			// here we have a table which contains only the rows related to the
			// last version of the catalogue!

			// execute the query
			ResultSet rs = stmt.executeQuery();

			// get the catalogues data and add them to the output array list
			while ( rs.next() )
				catalogues.add( getByResultSet ( rs ) );
			
			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return catalogues;
	}

	/**
	 * Retrieve all the meta data of all the catalogues
	 * @return
	 */
	public ArrayList < Catalogue > getLocalCatalogues () {

		try {

			// output array
			ArrayList < Catalogue > catalogues = new ArrayList<>();

			// open the connection
			Connection con = DatabaseManager.getMainDBConnection();

			// select the catalogue by code
			PreparedStatement stmt = con.prepareStatement( "select * from APP.CATALOGUE c "
					+ "left join APP.RESERVED_CATALOGUE rc on c.CAT_ID = rc.CAT_ID" );

			// get the query results
			ResultSet rs = stmt.executeQuery();

			User user = User.getInstance();
			
			// get the catalogues data and add them to the output array list
			while ( rs.next() ) {
				
				Catalogue catalogue = getByResultSet( rs );
				
				// do not consider the cat user catalogue if we are not a cm
				if ( catalogue.isCatUsersCatalogue() && !user.isCatManager() )
					continue;
				
				catalogues.add( getByResultSet ( rs ) );
			}
			
			rs.close();
			stmt.close();
			con.close();

			return catalogues;

		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	
	/**
	 * Get the catalogue metadata starting from the catalogue code and version
	 * NOTE never used!!!
	 * @param catalogueCode
	 * @return
	 */
	public Catalogue getCatalogue ( String catalogueCode, String catalogueVersion ) {

		String query = "select * from APP.CATALOGUE t1 "
				+ "left join APP.RESERVED_CATALOGUE RC on t1.CAT_ID = "
				+ "RC.CAT_ID "
				+ "where t1.CAT_CODE = ? and t1.CAT_VERSION = ?";
		
		try {

			// open the connection
			Connection con = DatabaseManager.getMainDBConnection();

			// select the catalogue by code
			PreparedStatement stmt = con.prepareStatement( query );

			// set the catalogue code and version parameters for the statement
			stmt.setString( 1, catalogueCode );
			stmt.setString( 2, catalogueVersion );

			// get the results
			ResultSet rs = stmt.executeQuery();

			// return null if no results found
			if ( !rs.next() )
				return null;

			// get the catalogue data and add them to the output array list
			Catalogue cat = getByResultSet ( rs );
			
			rs.close();
			stmt.close();
			con.close();
			
			return cat;
		}
		catch ( SQLException e ) {
			e.printStackTrace();
			return null;
		}
	}


	@Override
	public boolean remove(Catalogue object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Catalogue getById(int id) {

		String query = "select * from APP.CATALOGUE t1 "
				+ "left join APP.RESERVED_CATALOGUE RC on t1.CAT_ID = "
				+ "RC.CAT_ID "
				+ "where t1.CAT_ID = ?";
		
		try {

			// open the connection
			Connection con = DatabaseManager.getMainDBConnection();

			// select the catalogue by code
			PreparedStatement stmt = con.prepareStatement( query );

			// set the catalogue code and version parameters for the statement
			stmt.setInt( 1, id );

			// get the results
			ResultSet rs = stmt.executeQuery();

			// return null if no results found
			if ( !rs.next() )
				return null;

			// get the catalogue data and add them to the output array list
			Catalogue cat = getByResultSet ( rs );
			
			rs.close();
			stmt.close();
			con.close();
			
			return cat;
		}
		catch ( SQLException e ) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Collection<Catalogue> getAll() {
		// TODO Auto-generated method stub
		return null;
	}
}
