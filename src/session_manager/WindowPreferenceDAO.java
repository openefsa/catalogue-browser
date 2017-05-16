package session_manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.DatabaseManager;

/**
 * This dao manages the relationships with the WINDOW_PREF table in the main database.
 * @author avonva
 *
 */
public class WindowPreferenceDAO implements CatalogueEntityDAO<WindowPreference> {

	/**
	 * Insert a new window preference into the main database
	 */
	@Override
	public int insert( WindowPreference pref ) {
		
		int id = -1;
		
		String query = "insert into APP.WINDOW_PREF (WINDOW_CODE, WINDOW_X, WINDOW_Y, "
				+ "WINDOW_W, WINDOW_H, WINDOW_MAX) values ( ?,?,?,?,?,? )";
		
		try {
			
			// connect with the main database
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query, Statement.RETURN_GENERATED_KEYS );
			
			// set the code as parameter
			stmt.setString( 1, pref.getCode() );
			stmt.setInt( 2, pref.getX() );
			stmt.setInt( 3, pref.getY() );
			stmt.setInt( 4, pref.getWidth() );
			stmt.setInt( 5, pref.getHeight() );
			stmt.setBoolean( 6, pref.isMaximized() );
			
			// insert the new pref
			stmt.executeUpdate();
			
			// get the id in the db
			ResultSet rs = stmt.getGeneratedKeys();
			
			if ( rs.next() )
				id = rs.getInt( 1 );
			
			rs.close();
			stmt.close();
			con.close();
		}
		catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return id;
	}

	@Override
	public boolean remove( WindowPreference pref ) {
		
		String query = "delete from APP.WINDOW_PREF where WINDOW_CODE = ?";
		
		try {
			
			// connect with the main database
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			// set the code as parameter
			stmt.setString( 1, pref.getCode() );
			
			stmt.executeUpdate();
			
			stmt.close();
			con.close();
			
			return true;
			
		}
		catch ( SQLException e ) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean update(WindowPreference object) {
		return false;
	}

	@Override
	public WindowPreference getById(int id) {
		return null;
	}

	/**
	 * Get a window preference using its code.
	 * @param code the code of the preference
	 * @return the preference if it is found, otherwise null
	 */
	public WindowPreference getByCode( String code ) {

		WindowPreference pref = null;
		
		String query = "select * from APP.WINDOW_PREF where WINDOW_CODE = ?";

		try {

			// connect with the main database
			Connection con = DatabaseManager.getMainDBConnection();

			PreparedStatement stmt = con.prepareStatement( query );

			// set the code as parameter
			stmt.setString( 1, code );

			ResultSet rs = stmt.executeQuery();

			if ( rs.next() )
				pref = getByResultSet( rs );
			
			rs.close();
			stmt.close();
			con.close();
		}
		catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return pref;
	}

	@Override
	public WindowPreference getByResultSet(ResultSet rs) throws SQLException {

		String code = rs.getString( "WINDOW_CODE" );
		int x = rs.getInt( "WINDOW_X" );
		int y = rs.getInt( "WINDOW_Y" );
		int w = rs.getInt( "WINDOW_W" );
		int h = rs.getInt( "WINDOW_H" );
		boolean max = rs.getBoolean( "WINDOW_MAX" );
		
		return new WindowPreference( code, x, y, w, h, max );
	}

	@Override
	public Collection<WindowPreference> getAll() {
		return null;
	}

}
