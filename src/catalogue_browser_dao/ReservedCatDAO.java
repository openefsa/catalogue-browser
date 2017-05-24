package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import catalogue_object.ReservedCatalogue;

/**
 * DAO to communicate with the Reserved Catalogue table
 * @author avonva
 *
 */
public class ReservedCatDAO implements CatalogueEntityDAO<ReservedCatalogue> {

	/**
	 * Reserve a catalogue inserting a new ReservedCatalogue
	 */
	@Override
	public int insert( ReservedCatalogue rc ) {
		
		int id = -1;
		
		Connection con;
		
		String query = "insert into APP.RESERVED_CATALOGUE (CAT_ID, "
				+ "RESERVE_USERNAME, RESERVE_NOTE, RESERVE_LEVEL) values (?,?,?,?)";
		
		try {
			
			con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query, 
					Statement.RETURN_GENERATED_KEYS );
			
			stmt.clearParameters();
			
			stmt.setInt( 1, rc.getCatalogueId() );
			stmt.setString( 2, rc.getUsername() );
			stmt.setString( 3, rc.getNote() );
			stmt.setString( 4, rc.getLevel().toString() );

			stmt.executeUpdate();
			
			// get the id
			ResultSet rs = stmt.getGeneratedKeys();
			if ( rs.next() )
				id = rs.getInt( 1 );
			
			stmt.close();
			con.close();
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return id;
	}
	
	/**
	 * Unreserve a catalogue
	 */
	@Override
	public boolean remove( ReservedCatalogue rc ) {
		
		Connection con;

		String query = "delete from APP.RESERVED_CATALOGUE where CAT_ID = ?";

		try {

			con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.clearParameters();

			stmt.setInt( 1, rc.getCatalogueId() );
			
			stmt.executeUpdate();
			
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public boolean update(ReservedCatalogue object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ReservedCatalogue getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReservedCatalogue getByResultSet(ResultSet rs) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<ReservedCatalogue> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

}
