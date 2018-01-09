package dcf_pending_action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.DatabaseManager;
import dcf_manager.Dcf.DcfType;
import dcf_pending_action.PendingAction.Priority;
import dcf_webservice.PublishLevel;
import dcf_webservice.ReserveLevel;

/**
 * Dao which is used to communicate with the PendingReserve table in the main database.
 * @author avonva
 *
 */
public class PendingActionDAO implements CatalogueEntityDAO<PendingAction> {

	@Override
	public int insert(PendingAction object) {
		
		int id = -1;
		String query = "insert into APP.PENDING_ACTION (ACTION_LOG_CODE, "
				+ "ACTION_DATA, CAT_ID, ACTION_USERNAME, ACTION_NOTE, "
				+ "ACTION_PRIORITY, ACTION_TYPE, ACTION_DCF_TYPE ) values (?,?,?,?,?,?,?,?)";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query, 
					Statement.RETURN_GENERATED_KEYS );
			
			// set the parameters
			stmt.setString( 1, object.getLogCode() );
			stmt.setString( 2, object.getData() );
			stmt.setInt( 3, object.getCatalogue().getId() );
			stmt.setString( 4, object.getUsername() );
			stmt.setString( 5, object.getNote() );
			stmt.setString( 6, object.getPriority().toString() );
			stmt.setString( 7, object.getType() );
			stmt.setString( 8, object.getDcfType().toString() );
			
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
	public boolean remove( PendingAction object ) {

		String query = "delete from APP.PENDING_ACTION where ACTION_ID = ?";
		
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
	public boolean update( PendingAction pr ) {
		
		String query = "update APP.PENDING_ACTION set ACTION_LOG_CODE = ?, "
				+ "ACTION_DATA = ?, CAT_ID = ?, ACTION_USERNAME = ?, ACTION_PRIORITY = ?, ACTION_TYPE = ? "
				+ "where ACTION_ID = ?";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			PreparedStatement stmt = con.prepareStatement( query );
			
			// set the parameters
			stmt.setString( 1, pr.getLogCode() );
			stmt.setString( 2, pr.getData() );
			stmt.setInt( 3, pr.getCatalogue().getId() );
			stmt.setString( 4, pr.getUsername() );
			stmt.setString( 5, pr.getPriority().toString() );
			stmt.setString( 6, pr.getType() );
			stmt.setInt( 7, pr.getId() );
			
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
	public PendingAction getById(int id) {
		return null;
	}
	
	@Override
	public PendingAction getByResultSet(ResultSet rs) throws SQLException {
		
		int id = rs.getInt( "ACTION_ID" );
		String logCode = rs.getString( "ACTION_LOG_CODE" );

		int catId = rs.getInt( "CAT_ID" );
		String username = rs.getString( "ACTION_USERNAME" );
		String note = rs.getString( "ACTION_NOTE" );
		Priority priority = Priority.valueOf( rs.getString( "ACTION_PRIORITY" ) );
		
		String type = rs.getString( "ACTION_TYPE" );
		
		DcfType dcfType = DcfType.valueOf( rs.getString( "ACTION_DCF_TYPE" ) );
		
		// get the catalogue related to the code and version
		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue catalogue = catDao.getById( catId );
		
		PendingAction pa = null;
		
		switch ( type ) {
		case PendingReserve.TYPE:
			
			ReserveLevel rLevel = ReserveLevel.valueOf( rs.getString( "ACTION_DATA" ) );
			
			pa = new PendingReserve( catalogue, logCode, 
					username, note, rLevel, priority, dcfType );
			break;
			
		case PendingPublish.TYPE:
			
			PublishLevel pLevel = PublishLevel.valueOf( rs.getString( "ACTION_DATA" ) );
			
			pa = new PendingPublish( catalogue, logCode, 
					username, priority, pLevel, dcfType );
			break;
		
		case PendingUploadData.TYPE:
			
			pa = new PendingUploadData( catalogue, logCode, 
					username, priority, dcfType );
			
			break;
			
		case PendingXmlDownload.TYPE:
			
			pa = new PendingXmlDownload( catalogue, username, dcfType );
			
			break;
		default:
			break;
		}

		// set the id
		if ( pa != null )
			pa.setId( id );
		
		return pa;
	}

	@Override
	public Collection<PendingAction> getAll() {
		
		Collection<PendingAction> out = new ArrayList<>();
		
		String query = "select * from APP.PENDING_ACTION";
		
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
	 * can use this method as a bijective function. Note that we have
	 * to specify the dcf type since some actions can be performed
	 * in test, while others in production.
	 * @param catalogue
	 * @param type Test or Production
	 * @return
	 */
	public Collection<PendingAction> getByCatalogue ( Catalogue catalogue, DcfType type ) {
		
		Collection<PendingAction> prs = new ArrayList<>();
		
		String query = "select * from APP.PENDING_ACTION where CAT_ID = ? and ACTION_DCF_TYPE = ?";
		
		try {
			
			Connection con = DatabaseManager.getMainDBConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.setInt( 1, catalogue.getId() );
			stmt.setString( 2, type.toString() );
			
			ResultSet rs = stmt.executeQuery();
			
			// get all the pending reserve obj
			while ( rs.next() )
				prs.add( getByResultSet(rs) );
			
			rs.close();
			stmt.close();
			con.close();
			
		} catch ( SQLException e) {
			e.printStackTrace();
		}
		
		return prs;
	}
}
