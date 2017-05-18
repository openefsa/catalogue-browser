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
import dcf_reserve_util.PendingReserve.PendingPriority;
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
				+ "RESERVE_LEVEL, CAT_ID, RESERVE_USERNAME, RESERVE_PRIORITY ) values (?,?,?,?,?)";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query, 
					Statement.RETURN_GENERATED_KEYS );
			
			// set the parameters
			stmt.setString( 1, object.getLogCode() );
			stmt.setString( 2, object.getReserveLevel().toString() );
			stmt.setInt( 3, object.getCatalogue().getId() );
			stmt.setString( 4, object.getUsername() );
			stmt.setString( 5, object.getPriority().toString() );
			
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

		String query = "delete from APP.PENDING_RESERVE where RESERVE_ID = ?";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.setInt( 1, object.getId() );
			
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
	public boolean update( PendingReserve pr ) {
		
		String query = "update APP.PENDING_RESERVE set RESERVE_LOG_CODE = ?, "
				+ "RESERVE_LEVEL = ?, CAT_ID = ?, RESERVE_USERNAME = ?, RESERVE_PRIORITY = ? "
				+ "where RESERVE_ID = ?";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );
			
			// set the parameters
			stmt.setString( 1, pr.getLogCode() );
			stmt.setString( 2, pr.getReserveLevel().toString() );
			stmt.setInt( 3, pr.getCatalogue().getId() );
			stmt.setString( 4, pr.getUsername() );
			stmt.setString( 5, pr.getPriority().toString() );
			stmt.setInt( 6, pr.getId() );
			
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
	public PendingReserve getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PendingReserve getByResultSet(ResultSet rs) throws SQLException {
		
		int id = rs.getInt( "RESERVE_ID" );
		String logCode = rs.getString( "RESERVE_LOG_CODE" );
		ReserveLevel level = ReserveLevel.valueOf( rs.getString( "RESERVE_LEVEL" ) );
		int catId = rs.getInt( "CAT_ID" );
		String username = rs.getString( "RESERVE_USERNAME" );
		PendingPriority priority = PendingPriority.valueOf( rs.getString( "RESERVE_PRIORITY" ) );
		
		// get the catalogue related to the code and version
		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue catalogue = catDao.getById( catId );
		
		PendingReserve pr = new PendingReserve( logCode, level, catalogue, username, priority );
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
	
	/**
	 * Get a pending reserve by the related catalogue. We can have
	 * only one catalogue related to the pending reserves, thus we
	 * can use this method as a bijective function
	 * @param catalogue
	 * @return
	 */
	public PendingReserve getByCatalogue ( Catalogue catalogue ) {
		
		PendingReserve pr = null;
		
		String query = "select * from APP.PENDING_RESERVE where CAT_ID = ?";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.setInt( 1, catalogue.getId() );
			
			ResultSet rs = stmt.executeQuery();
			
			// get all the pending reserve obj
			if ( rs.next() )
				pr = getByResultSet(rs);
			
			rs.close();
			stmt.close();
			con.close();
			
		} catch ( SQLException e) {
			e.printStackTrace();
		}
		
		return pr;
		
	}
}
