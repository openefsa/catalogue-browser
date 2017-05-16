package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import catalogue_object.Catalogue;
import dcf_webservice.ReserveLevel;

/**
 * DAO to communicate with the FORCED_CATALOGUE table of the main database.
 * We use this dao to save the force editing state of the catalogue.
 * Different threads can access the force edit table, therefore we need to make
 * it synchronized.
 * @author avonva
 *
 */
public class ForceCatEditDAO {
	
	/**
	 * Save into the database that the User with name {@code username} 
	 * has forced the editing of the current catalogue.
	 * @param username the name of the user which forced the editing of the catlaogue
	 * @return true if everything went welln during the insertion
	 */
	public synchronized boolean forceEditing ( Catalogue catalogue, String username, ReserveLevel editLevel ) {
		
		String query = "insert into APP.FORCED_CATALOGUE (CAT_CODE, CAT_VERSION, "
				+ "FORCED_USERNAME, FORCED_EDIT, FORCED_LEVEL ) values (?,?,?,?,?)";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.setString( 1, catalogue.getCode() );
			stmt.setString( 2, catalogue.getVersion() );
			stmt.setString( 3, username );
			stmt.setBoolean( 4, true );
			stmt.setString( 5, editLevel.toString() );
			
			stmt.executeUpdate();
			
			stmt.close();
			con.close();
			
			return true;
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return false;
	}

	/**
	 * Check if the user is forcing the edit of the current catalogue
	 * @param catalogue the catalogue we want to check
	 * @param username the name of the user we want to check
	 * @return the editing level the user has forced (if there is one). If no
	 * record is found ReserveLevel.NONE is returned by default
	 */
	public synchronized ReserveLevel getEditingLevel ( Catalogue catalogue, String username ) {
		
		ReserveLevel reserveLevel = ReserveLevel.NONE;
		
		String query = "select * from APP.FORCED_CATALOGUE where CAT_CODE = ? and CAT_VERSION = ? and FORCED_USERNAME = ?";

		try {

			Connection con = DatabaseManager.getMainDBConnection();

			PreparedStatement stmt = con.prepareStatement( query );

			stmt.setString( 1, catalogue.getCode() );
			stmt.setString( 2, catalogue.getVersion() );
			stmt.setString( 3, username );

			ResultSet rs = stmt.executeQuery();

			// if the record is present, the user is
			// forcing the editing
			if ( rs.next() )
				reserveLevel = ReserveLevel.valueOf( 
						rs.getString( "FORCED_LEVEL" ) );
			
			rs.close();
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}

		return reserveLevel;
	}

	/**
	 * Remove the forced editing from the db for the current catalogue
	 * @return
	 */
	public synchronized boolean removeForceEditing ( Catalogue catalogue, String username ) {

		String query = "delete from APP.FORCED_CATALOGUE where CAT_CODE = ? and CAT_VERSION = ? and FORCED_USERNAME = ?";

		try {

			Connection con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.clearParameters();

			stmt.setString( 1, catalogue.getCode() );
			stmt.setString( 2, catalogue.getVersion() );
			stmt.setString( 3, username );
			
			stmt.executeUpdate();
			
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
}
