package data_collection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.DatabaseManager;

/**
 * Data collection DAO
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class DCDAO implements CatalogueEntityDAO<DataCollection> {

	private static final Logger LOGGER = LogManager.getLogger(CatalogueConfigDAO.class);

	@Override
	public void setCatalogue(Catalogue catalogue) {
	}

	@Override
	public int insert(DataCollection dc) {

		int id = -1;

		String query = "insert into APP.DATA_COLLECTION (DC_CODE, DC_DESCRIPTION, "
				+ "DC_ACTIVE_FROM, DC_ACTIVE_TO, DC_RESOURCE_ID) values (?,?,?,?,?)";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			stmt.setString(1, dc.getCode());
			stmt.setString(2, dc.getDescription());
			stmt.setTimestamp(3, dc.getActiveFrom());
			stmt.setTimestamp(4, dc.getActiveTo());
			stmt.setString(5, dc.getResourceId());

			stmt.executeUpdate();

			try (ResultSet rs = stmt.getGeneratedKeys();) {
				if (rs != null && rs.next())
					id = rs.getInt(1);
				
				rs.close();
			}

			stmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return id;
	}

	@Override
	public boolean remove(DataCollection dc) {

		String query = "delete from APP.DATA_COLLECTION where DC_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, dc.getId());

			stmt.executeUpdate();

			stmt.close();
			con.close();

			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return false;
	}

	@Override
	public boolean update(DataCollection object) {

		return false;
	}

	@Override
	public DataCollection getById(int id) {

		DataCollection out = null;

		String query = "select * from APP.DATA_COLLECTION where DC_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, id);

			try (ResultSet rs = stmt.executeQuery();) {

				if (rs.next())
					out = getByResultSet(rs);
				
				rs.close();
			}
			
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return out;
	}

	@Override
	public DataCollection getByResultSet(ResultSet rs) throws SQLException {

		int id = rs.getInt("DC_ID");
		String code = rs.getString("DC_CODE");
		String desc = rs.getString("DC_DESCRIPTION");
		Timestamp activeFrom = rs.getTimestamp("DC_ACTIVE_FROM");
		Timestamp activeTo = rs.getTimestamp("DC_ACTIVE_TO");
		String resId = rs.getString("DC_RESOURCE_ID");

		DataCollection dc = new DataCollection(code, desc, null, activeFrom, activeTo, resId);
		dc.setId(id);

		return dc;
	}

	@Override
	public Collection<DataCollection> getAll() {

		Collection<DataCollection> out = new ArrayList<>();

		String query = "select * from APP.DATA_COLLECTION";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);
				ResultSet rs = stmt.executeQuery();) {

			while (rs.next())
				out.add(getByResultSet(rs));

			rs.close();
			stmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return out;
	}

	/**
	 * Check if the data collection was already downloaded or not
	 * 
	 * @param
	 * @return
	 */
	public boolean contains(DataCollection dc) {

		boolean contains = false;

		String query = "select * from APP.DATA_COLLECTION where DC_CODE = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, dc.getCode());

			try (ResultSet rs = stmt.executeQuery();) {

				contains = rs.next();
				
				rs.close();
			}
			
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return contains;
	}

	@Override
	public List<Integer> insert(Iterable<DataCollection> attrs) {
		return null;
	}
}
