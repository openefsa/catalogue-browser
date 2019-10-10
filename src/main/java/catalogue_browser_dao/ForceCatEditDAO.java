package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import soap.UploadCatalogueFileImpl.ReserveLevel;

/**
 * DAO to communicate with the FORCED_CATALOGUE table of the main database. We
 * use this dao to save the force editing state of the catalogue. Different
 * threads can access the force edit table, therefore we need to make it
 * synchronized.
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class ForceCatEditDAO implements IForcedCatalogueDAO {

	private static final Logger LOGGER = LogManager.getLogger(ForceCatEditDAO.class);

	/**
	 * Save into the database that the User with name {@code username} has forced
	 * the editing of the current catalogue.
	 * 
	 * @param username the name of the user which forced the editing of the
	 *                 catlaogue
	 * @return true if everything went welln during the insertion
	 */
	public synchronized boolean forceEditing(Catalogue catalogue, String username, ReserveLevel editLevel) {

		String query = "insert into APP.FORCED_CATALOGUE (CAT_ID, "
				+ "FORCED_USERNAME, FORCED_EDIT, FORCED_LEVEL ) values (?,?,?,?)";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, catalogue.getId());
			stmt.setString(2, username);
			stmt.setBoolean(3, true);
			stmt.setString(4, editLevel.getDatabaseKey());

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

	/**
	 * Check if the user is forcing the edit of the current catalogue
	 * 
	 * @param catalogue the catalogue we want to check
	 * @param username  the name of the user we want to check
	 * @return the editing level the user has forced (if there is one). If no record
	 *         is found ReserveLevel.NONE is returned by default
	 */
	public synchronized ReserveLevel getEditingLevel(Catalogue catalogue, String username) {

		ReserveLevel reserveLevel = null;

		String query = "select * from APP.FORCED_CATALOGUE where CAT_ID = ? and FORCED_USERNAME = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, catalogue.getId());
			stmt.setString(2, username);

			try (ResultSet rs = stmt.executeQuery();) {

				// if the record is present, the user is
				// forcing the editing
				if (rs.next())
					reserveLevel = ReserveLevel.fromDatabaseKey(rs.getString("FORCED_LEVEL"));
				
				rs.close();
			}

			stmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return reserveLevel;
	}

	/**
	 * Remove the forced editing related to a single catalogue
	 * 
	 * @return
	 */
	public synchronized boolean remove(Catalogue catalogue) {

		String query = "delete from APP.FORCED_CATALOGUE where CAT_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, catalogue.getId());

			stmt.executeUpdate();

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
			return false;
		}

		return true;
	}

	/**
	 * Remove the forced editing from the db for the current catalogue
	 * 
	 * @return
	 */
	public synchronized boolean removeForceEditing(Catalogue catalogue) {

		String query = "delete from APP.FORCED_CATALOGUE where CAT_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, catalogue.getId());

			stmt.executeUpdate();

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
			return false;
		}

		return true;
	}
}
