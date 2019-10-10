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

/**
 * Data collection table DAO
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class CatalogueConfigDAO implements CatalogueEntityDAO<CatalogueConfiguration> {

	private static final Logger LOGGER = LogManager.getLogger(CatalogueConfigDAO.class);

	@Override
	public void setCatalogue(Catalogue catalogue) {
	}

	@Override
	public int insert(CatalogueConfiguration config) {

		int id = -1;

		String query = "insert into APP.CATALOGUE_CONFIG ("
				+ "CONFIG_NAME, CONFIG_CAT_CODE, CONFIG_HIERARCHY_CODE) values (?,?,?)";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			stmt.setString(1, config.getDataElementName());
			stmt.setString(2, config.getCatalogueCode());
			stmt.setString(3, config.getHierarchyCode());

			stmt.executeUpdate();

			try (ResultSet rs = stmt.getGeneratedKeys();) {

				if (rs != null && rs.next()) {
					id = rs.getInt(1);
					rs.close();
				}
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
	public boolean remove(CatalogueConfiguration config) {

		String query = "delete from APP.CATALOGUE_CONFIG where CONFIG_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, config.getId());

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
	public boolean update(CatalogueConfiguration object) {
		return false;
	}

	@Override
	public CatalogueConfiguration getById(int id) {

		CatalogueConfiguration out = null;

		String query = "select * from APP.CATALOGUE_CONFIG where CONFIG_ID = ?";

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
	public CatalogueConfiguration getByResultSet(ResultSet rs) throws SQLException {

		int id = rs.getInt("CONFIG_ID");
		String name = rs.getString("CONFIG_NAME");
		String catCode = rs.getString("CONFIG_CAT_CODE");
		String hierCode = rs.getString("CONFIG_HIERARCHY_CODE");

		CatalogueConfiguration config = new CatalogueConfiguration(name, catCode, hierCode);
		config.setId(id);

		return config;
	}

	@Override
	public Collection<CatalogueConfiguration> getAll() {

		Collection<CatalogueConfiguration> out = new ArrayList<>();

		String query = "select * from APP.CATALOGUE_CONFIG";

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
	 * Check if the config is already present or not
	 * 
	 * @param table
	 * @return
	 */
	public boolean contains(CatalogueConfiguration config) {

		boolean contains = false;

		String query = "select * from APP.CATALOGUE_CONFIG where CONFIG_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, config.getId());

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
	public List<Integer> insert(Iterable<CatalogueConfiguration> attrs) {
		return null;
	}
}
