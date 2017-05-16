package dcf_reserve_util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.DatabaseManager;
import catalogue_object.Catalogue;
import dcf_webservice.ReserveLevel;

/**
 * Dao which is used to communicate with the PendingReserve table in the main database.
 * @author avonva
 *
 */
public class PendingReserveDAO implements CatalogueEntityDAO<PendingReserve> {

	@Override
	public int insert(PendingReserve object) {
		
		int id = -1;
		String query = "insert into APP.PENDING_RESERVE (RESERVE_LOG_CODE, "
				+ "RESERVE_LEVEL, CAT_CODE, CAT_VERSION, RESERVE_USERNAME) values (?,?,?,?,?)";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query, 
					Statement.RETURN_GENERATED_KEYS );
			
			// set the parameters
			stmt.setString( 1, object.getLogCode() );
			stmt.setString( 2, object.getReserveLevel().toString() );
			stmt.setString( 3, object.getCatalogue().getCode() );
			stmt.setString( 4, object.getCatalogue().getVersion() );
			stmt.setString( 5, object.getUsername() );
			
			// insert the pending reserve object
			stmt.executeUpdate();
			
			// get the id of the new object
			// from the database
			ResultSet rs = stmt.getGeneratedKeys();
			if ( rs.next() )
				id = rs.getInt( 1 );
			
			rs.close();
			stmt.close();
			con.close();
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}

	@Override
	public boolean remove( PendingReserve object ) {

		String query = "delete from APP.PENDING_RESERVE "
				+ "where CAT_CODE = ? and CAT_VERSION = ?";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.setString( 1, object.getCatalogue().getCode() );
			stmt.setString( 2, object.getCatalogue().getVersion() );
			
			// remove
			stmt.executeUpdate();
			
			stmt.close();
			con.close();
			
			return true;
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public boolean update(PendingReserve object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public PendingReserve getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PendingReserve getByResultSet(ResultSet rs) throws SQLException {
		
		int id = rs.getInt( "RESERVE_ID" );
		String logCode = rs.getString( "RESERVE_LOG_CODE" );
		ReserveLevel level = ReserveLevel.valueOf( rs.getString( "RESERVE_LEVEL" ) );
		String catCode = rs.getString( "CAT_CODE" );
		String catVer = rs.getString( "CAT_VERSION" );
		String username = rs.getString( "RESERVE_USERNAME" );
		
		// get the catalogue related to the code and version
		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue catalogue = catDao.getCatalogue( catCode, catVer );
		
		PendingReserve pr = new PendingReserve( logCode, level, catalogue, username );
		pr.setId( id );
		
		return pr;
	}

	@Override
	public Collection<PendingReserve> getAll() {
		
		Collection<PendingReserve> out = new ArrayList<>();
		
		String query = "select * from APP.PENDING_RESERVE";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			ResultSet rs = stmt.executeQuery();
			
			// get all the pending reserve obj
			while ( rs.next() ) {
				out.add( getByResultSet(rs) );
			}
			
			rs.close();
			stmt.close();
			con.close();
			
		} catch ( SQLException e) {
			e.printStackTrace();
		}
		
		return out;
	}
}
