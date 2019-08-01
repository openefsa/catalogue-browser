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
public class DCTableDAO implements CatalogueEntityDAO<DCTable> {

	private static final Logger LOGGER = LogManager.getLogger(DCTableDAO.class);

	@Override
	public void setCatalogue(Catalogue catalogue) {
	}

	@Override
	public int insert(DCTable dc) {

		int id = -1;

		String query = "insert into APP.DATA_COLLECTION_TABLE (DC_TABLE_NAME) values (?)";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			stmt.setString(1, dc.getName());

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
	public boolean remove(DCTable dc) {

		String query = "delete from APP.DATA_COLLECTION_TABLE where DC_TABLE_ID = ?";

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
	public boolean update(DCTable object) {

		return false;
	}

	@Override
	public DCTable getById(int id) {

		DCTable out = null;

		String query = "select * from APP.DATA_COLLECTION_TABLE where DC_TABLE_ID = ?";

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

	/**
	 * Get the table by its name
	 * 
	 * @param name
	 * @return
	 */
	public DCTable getByName(String name) {

		DCTable out = null;

		String query = "select * from APP.DATA_COLLECTION_TABLE where DC_TABLE_NAME = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, name);

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
	public DCTable getByResultSet(ResultSet rs) throws SQLException {

		int id = rs.getInt("DC_TABLE_ID");
		String name = rs.getString("DC_TABLE_NAME");

		DCTable table = new DCTable(name);
		table.setId(id);

		return table;
	}

	/**
	 * Check if the table is already present or not in the db
	 * 
	 * @param table
	 * @return
	 */
	public boolean contains(DCTable table) {

		boolean contains = false;

		String query = "select * from APP.DATA_COLLECTION_TABLE where DC_TABLE_NAME = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, table.getName());

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
	public Collection<DCTable> getAll() {

		Collection<DCTable> out = new ArrayList<>();

		String query = "select * from APP.DATA_COLLECTION_TABLE";

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

	@Override
	public List<Integer> insert(Iterable<DCTable> attrs) {
		return null;
	}
}
