package detail_level;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.DatabaseManager;
import global_manager.GlobalManager;

public class DetailLevelDAO implements CatalogueEntityDAO<DetailLevelGraphics> {

	/**
	 * Check if the catalogue has detail levels or not
	 * @return
	 */
	@Deprecated
	public static boolean hasDetailLevels() {
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		// TODO put hasdetaillevels
		return !currentCat.getDetailLevels().isEmpty();
	}
	
	/**
	 * Get all the detail levels from the main db
	 * @return
	 */
	public ArrayList< DetailLevelGraphics > getAll () {
		
		ArrayList< DetailLevelGraphics >  values = new ArrayList<>();
		
		Connection con = null;

		// get all the distinct values of the attribute and return them
		String query = "select * from APP.DETAIL_LEVEL";

		try {

			// open the db connection to the main db
			con = DatabaseManager.getMainDBConnection();

			// prepare the statement and its parameters
			PreparedStatement stmt = con.prepareStatement( query );
			stmt.clearParameters();

			// get the results
			ResultSet rs = stmt.executeQuery();

			// get all the detail levels
			while ( rs.next() ) {
				values.add( getByResultSet( rs ) );
			}

			rs.close();
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return values;
	}

	@Override
	public int insert(DetailLevelGraphics object) {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public boolean remove(DetailLevelGraphics object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(DetailLevelGraphics object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DetailLevelGraphics getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DetailLevelGraphics getByResultSet(ResultSet rs) throws SQLException {
		
		String code = rs.getString( "DETAIL_LEVEL_CODE" );
		String name = rs.getString( "DETAIL_LEVEL_LABEL" );
		String imageName = rs.getString( "DETAIL_LEVEL_IMAGE_NAME" );
		
		DetailLevelGraphics dlg = new DetailLevelGraphics( code, name, imageName );
		
		return dlg;
	}
}
