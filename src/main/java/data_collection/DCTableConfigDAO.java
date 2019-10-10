package data_collection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.DatabaseManager;

public class DCTableConfigDAO implements CatalogueEntityDAO<DCTableConfig> {

	private static final Logger LOGGER = LogManager.getLogger(DCTableConfigDAO.class);

	@Override
	public void setCatalogue(Catalogue catalogue) {
	}

	@Override
	public int insert(DCTableConfig rel) {

		int id = -1;

		String query = "insert into APP.DC_TABLE_CONFIG (DC_ID, DC_TABLE_ID, CONFIG_ID) values (?,?,?)";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			stmt.setInt(1, rel.getDc().getId());
			stmt.setInt(2, rel.getTable().getId());
			stmt.setInt(3, rel.getConfig().getId());

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
	public boolean remove(DCTableConfig rel) {

		String query = "delete from APP.DC_TABLE_CONFIG where DC_ID = ? " + "and DC_TABLE_ID = ? and CONFIG_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, rel.getDc().getId());
			stmt.setInt(2, rel.getTable().getId());
			stmt.setInt(3, rel.getConfig().getId());

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
	public boolean update(DCTableConfig object) {
		return false;
	}

	@Override
	public DCTableConfig getById(int id) {
		return null;
	}

	/**
	 * Get all the table configs related to the data collection
	 * 
	 * @param dc
	 * @return
	 */
	public Collection<DCTableConfig> getByDataCollection(DataCollection dc) {

		Collection<DCTableConfig> out = new ArrayList<>();

		String query = "select * from APP.DC_TABLE_CONFIG where DC_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, dc.getId());

			try (ResultSet rs = stmt.executeQuery();) {

				while (rs.next())
					out.add(getByResultSet(rs));
				
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
	public DCTableConfig getByResultSet(ResultSet rs) throws SQLException {

		int dcId = rs.getInt("DC_ID");
		int tableId = rs.getInt("DC_TABLE_ID");
		int configId = rs.getInt("CONFIG_ID");

		// get complete objects
		DCDAO dcDao = new DCDAO();
		DataCollection dc = dcDao.getById(dcId);

		DCTableDAO tableDao = new DCTableDAO();
		DCTable table = tableDao.getById(tableId);

		CatalogueConfigDAO configDao = new CatalogueConfigDAO();
		CatalogueConfiguration config = configDao.getById(configId);

		return new DCTableConfig(dc, table, config);
	}

	@Override
	public Collection<DCTableConfig> getAll() {

		return null;
	}

	@Override
	public List<Integer> insert(Iterable<DCTableConfig> attrs) {
		return null;
	}
}
