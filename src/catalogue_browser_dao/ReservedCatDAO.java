package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue.ReservedCatalogue;
import soap.UploadCatalogueFileImpl.ReserveLevel;

/**
 * DAO to communicate with the Reserved Catalogue table
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class ReservedCatDAO implements CatalogueEntityDAO<ReservedCatalogue> {

	private static final Logger LOGGER = LogManager.getLogger(ReservedCatDAO.class);

	@Override
	public void setCatalogue(Catalogue catalogue) {
	}

	/**
	 * Reserve a catalogue inserting a new ReservedCatalogue
	 */
	@Override
	public int insert(ReservedCatalogue rc) {

		int id = rc.getCatalogueId();

		String query = "insert into APP.RESERVED_CATALOGUE (CAT_ID, "
				+ "RESERVE_USERNAME, RESERVE_NOTE, RESERVE_LEVEL) values (?,?,?,?)";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, rc.getCatalogueId());
			stmt.setString(2, rc.getUsername());
			stmt.setString(3, rc.getNote());
			stmt.setString(4, rc.getLevel().getDatabaseKey());

			stmt.executeUpdate();

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return id;
	}

	/**
	 * Unreserve a catalogue
	 */
	@Override
	public boolean remove(ReservedCatalogue rc) {

		int removedRows = -1;

		String query = "delete from APP.RESERVED_CATALOGUE where CAT_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, rc.getCatalogueId());

			removedRows = stmt.executeUpdate();

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return removedRows > 0;
	}

	@Override
	public boolean update(ReservedCatalogue object) {

		return false;
	}

	@Override
	public ReservedCatalogue getById(int id) {

		ReservedCatalogue rc = null;

		String query = "select * from APP.RESERVED_CATALOGUE where CAT_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, id);

			try (ResultSet rs = stmt.executeQuery();) {

				if (rs.next())
					rc = getByResultSet(rs);
				
				rs.close();
			}
			
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return rc;
	}

	@Override
	public ReservedCatalogue getByResultSet(ResultSet rs) throws SQLException {

		int id = rs.getInt("CAT_ID");
		String username = rs.getString("RESERVE_USERNAME");
		String note = rs.getString("RESERVE_NOTE");
		ReserveLevel level = ReserveLevel.fromDatabaseKey(rs.getString("RESERVE_LEVEL"));

		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue catalogue = catDao.getById(id);

		ReservedCatalogue rc = new ReservedCatalogue(catalogue, username, note, level);

		return rc;
	}

	@Override
	public Collection<ReservedCatalogue> getAll() {

		return null;
	}

	@Override
	public List<Integer> insert(Iterable<ReservedCatalogue> attrs) {

		return null;
	}
}
