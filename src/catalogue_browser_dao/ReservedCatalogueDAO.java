package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import catalogue_object.Catalogue;
import dcf_user.User;
import dcf_webservice.ReserveLevel;

public class ReservedCatalogueDAO {

	private Catalogue catalogue;
	
	/**
	 * Initialize the reserved catalogue dao
	 * with the catalogue we want to communicate with.
	 * @param catalogue
	 */
	public ReservedCatalogueDAO( Catalogue catalogue ) {
		this.catalogue = catalogue;
	}
	
	/**
	 * Reserve a catalogue
	 * @param catalogue
	 * @param username
	 * @param level
	 * @return
	 */
	public boolean reserveCatalogue ( String username, ReserveLevel level ) {
		
		Connection con;
		
		String query = "insert into APP.RESERVED_CATALOGUE (CAT_CODE, CAT_VERSION, RESERVE_USERNAME, RESERVE_LEVEL) values (?,?,?,?)";
		
		try {
			
			con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.clearParameters();
			
			stmt.setString( 1, catalogue.getCode() );
			stmt.setString( 2, catalogue.getVersion() );
			stmt.setString( 3, username );
			stmt.setString( 4, level.name() );

			stmt.executeUpdate();
			
			stmt.close();
			con.close();
			
		} catch ( SQLException e ) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Unreserve the catalogue
	 * @param catalogue
	 * @return
	 */
	public boolean unreserveCatalogue () {
		
		Connection con;

		String query = "delete from APP.RESERVED_CATALOGUE where CAT_CODE = ?";

		try {

			con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.clearParameters();

			stmt.setString( 1, catalogue.getCode() );
			
			stmt.executeUpdate();
			
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
	/**
	 * Check if the current catalogue can be reserved by the selected user
	 * @param catalogue
	 * @param username
	 * @return
	 */
	public boolean isReserved () {
		
		boolean isReserved = false;
		
		Connection con;

		// check if the catalogue is already reserved (i.e. if it is in the table)
		String query = "select * from APP.RESERVED_CATALOGUE where CAT_CODE = ? and CAT_VERSION = ?";
		
		try {

			con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.clearParameters();

			stmt.setString( 1, catalogue.getCode() );
			stmt.setString( 2, catalogue.getVersion() );
			
			ResultSet rs = stmt.executeQuery();
			
			// if there is a record => another user has already reserved
			// the catalogue => we cannot reserve
			// if no records found => we can reserve
			isReserved = rs.next();
			
			rs.close();
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return isReserved;
	}
	
	/**
	 * Check if the current catalogue can be reserved by the selected user
	 * @param catalogue
	 * @param username
	 * @return
	 */
	public boolean isReservedBy ( User user ) {
		
		boolean isReserved = false;
		
		Connection con;

		// check if the catalogue is already reserved (i.e. if it is in the table)
		String query = "select * from APP.RESERVED_CATALOGUE where CAT_CODE = ? and CAT_VERSION = ? and RESERVE_USERNAME = ?";
		
		try {

			con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.clearParameters();

			stmt.setString( 1, catalogue.getCode() );
			stmt.setString( 2, catalogue.getVersion() );
			stmt.setString( 3, user.getUsername() );
			
			ResultSet rs = stmt.executeQuery();
			
			// if there is a record => another user has already reserved
			// the catalogue => we cannot reserve
			// if no records found => we can reserve
			isReserved = rs.next();
			
			rs.close();
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return isReserved;
	}
}
