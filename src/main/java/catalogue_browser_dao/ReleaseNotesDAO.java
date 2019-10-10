package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue.ReleaseNotes;
import catalogue.ReleaseNotesOperation;

/**
 * Dao to get the release note information from the database
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class ReleaseNotesDAO {

	private static final Logger LOGGER = LogManager.getLogger(ReleaseNotesDAO.class);

	private Catalogue catalogue;

	/**
	 * Initialize the dao with the catalogue we want to work with
	 * 
	 * @param catalogue
	 */
	public ReleaseNotesDAO(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	/**
	 * Insert the catalogue release notes into the db
	 * 
	 * @return
	 */
	public synchronized boolean insert(ReleaseNotes notes) {

		String query = "update APP.CATALOGUE set CAT_RN_DESCRIPTION=?, " + "CAT_RN_VERSION_DATE=?,"
				+ " CAT_RN_INTERNAL_VERSION=?," + "CAT_RN_INTERNAL_VERSION_NOTE=? " + "where CAT_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, notes.getDescription());
			stmt.setTimestamp(2, notes.getDate());
			stmt.setString(3, notes.getInternalVersion());
			stmt.setString(4, notes.getInternalVersionNote());
			stmt.setInt(5, catalogue.getId());

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
	 * Get the catalogue release notes
	 * 
	 * @return
	 */
	public ReleaseNotes getReleaseNotes() {

		ReleaseNotes rn = null;

		String query = "select CAT_RN_DESCRIPTION, CAT_RN_VERSION_DATE, CAT_RN_INTERNAL_VERSION, "
				+ "CAT_RN_INTERNAL_VERSION_NOTE from APP.CATALOGUE where CAT_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setFetchSize(200);
			
			stmt.setInt(1, catalogue.getId());

			try (ResultSet rs = stmt.executeQuery();) {

				if (rs.next()) 
					rn = getByResultSet(rs);
				
				rs.close();
			}
			
			stmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return rn;
	}

	/**
	 * Get the release notes from the result set
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public ReleaseNotes getByResultSet(ResultSet rs) throws SQLException {

		String desc = rs.getString("CAT_RN_DESCRIPTION");
		Timestamp date = rs.getTimestamp("CAT_RN_VERSION_DATE");
		String intVersion = rs.getString("CAT_RN_INTERNAL_VERSION");
		String note = rs.getString("CAT_RN_INTERNAL_VERSION_NOTE");

		ReleaseNotesOperationDAO opDao = new ReleaseNotesOperationDAO(catalogue);
		Collection<ReleaseNotesOperation> ops = opDao.getAll();

		// create the release note
		ReleaseNotes rn = new ReleaseNotes(desc, date, intVersion, note, ops);

		return rn;
	}
}
