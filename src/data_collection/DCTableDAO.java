package data_collection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.DatabaseManager;

/**
 * Data collection table DAO
 * @author avonva
 *
 */
public class DCTableDAO implements CatalogueEntityDAO<DCTable> {

	@Override
	public int insert(DCTable dc) {

		int id = -1;

		Connection con;
		String query = "insert into APP.DATA_COLLECTION_TABLE (DC_TABLE_NAME) values (?)";

		try {
			con = DatabaseManager.getMainDBConnection();

			PreparedStatement stmt = con.prepareStatement( query,
					Statement.RETURN_GENERATED_KEYS );

			stmt.setString( 1, dc.getName() );

			stmt.executeUpdate();

			ResultSet rs = stmt.getGeneratedKeys();

			if ( rs != null && rs.next() ) {
				id = rs.getInt(1);
				rs.close();
			}

			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}

	@Override
	public boolean remove(DCTable dc) {

		Connection con;
		String query = "delete from APP.DATA_COLLECTION_TABLE where DC_TABLE_ID = ?";

		try {
			con = DatabaseManager.getMainDBConnection();

			PreparedStatement stmt = con.prepareStatement( query );

			stmt.setInt( 1, dc.getId() );

			stmt.executeUpdate();

			stmt.close();
			con.close();
			
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean update(DCTable object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DCTable getById(int id) {

		DCTable out = null;

		Connection con;
		String query = "select * from APP.DATA_COLLECTION_TABLE where DC_TABLE_ID = ?";

		try {
			con = DatabaseManager.getMainDBConnection();

			PreparedStatement stmt = con.prepareStatement( query );

			stmt.setInt( 1, id );

			ResultSet rs = stmt.executeQuery();

			if ( rs.next() )
				out = getByResultSet( rs );

			rs.close();
			stmt.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return out;
	}
	
	/**
	 * Get the table by its name
	 * @param name
	 * @return
	 */
	public DCTable getByName ( String name ) {

		DCTable out = null;

		Connection con;
		String query = "select * from APP.DATA_COLLECTION_TABLE where DC_TABLE_NAME = ?";

		try {
			con = DatabaseManager.getMainDBConnection();

			PreparedStatement stmt = con.prepareStatement( query );

			stmt.setString( 1, name );

			ResultSet rs = stmt.executeQuery();

			if ( rs.next() )
				out = getByResultSet( rs );

			rs.close();
			stmt.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return out;
	}

	@Override
	public DCTable getByResultSet(ResultSet rs) throws SQLException {

		int id = rs.getInt( "DC_TABLE_ID" );
		String name = rs.getString( "DC_TABLE_NAME" );

		DCTable table = new DCTable( name );
		table.setId( id );

		return table;
	}

	/**
	 * Check if the table is already present or not in the db
	 * @param table
	 * @return
	 */
	public boolean contains ( DCTable table ) {
		
		boolean contains = false;
		
		Connection con;
		String query = "select * from APP.DATA_COLLECTION_TABLE where DC_TABLE_NAME = ?";

		try {

			con = DatabaseManager.getMainDBConnection();

			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.setString( 1, table.getName() );

			ResultSet rs = stmt.executeQuery();
			
			contains = rs.next();
			
			rs.close();
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return contains;
	}
	
	@Override
	public Collection<DCTable> getAll() {

		Collection<DCTable> out = new ArrayList<>();

		Connection con;
		String query = "select * from APP.DATA_COLLECTION_TABLE";

		try {
			con = DatabaseManager.getMainDBConnection();

			PreparedStatement stmt = con.prepareStatement( query );

			ResultSet rs = stmt.executeQuery();

			while ( rs.next() )
				out.add( getByResultSet( rs ) );

			rs.close();
			stmt.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return out;
	}
}
