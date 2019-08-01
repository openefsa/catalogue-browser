package user_preferences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue_browser_dao.CatalogueEntityDAO;

public abstract class PreferenceDAO implements CatalogueEntityDAO<Preference> {

	private static final Logger LOGGER = LogManager.getLogger(PreferenceDAO.class);

	/**
	 * Get the preference value as string
	 * 
	 * @param key
	 * @return
	 */
	public String getPreferenceValue(String key) {

		String result = "";

		try {
			result = getPreference(key).getValue();
		} catch (PreferenceNotFoundException e) {

		}

		return result;
	}

	/**
	 * Get the preference value as integer, if not found => defaultvalue
	 * 
	 * @param key
	 * @return
	 */
	public int getPreferenceIntValue(String key, int defaultValue) {

		int result = defaultValue;

		try {
			result = Integer.valueOf(getPreference(key).getValue());
		} catch (NumberFormatException | PreferenceNotFoundException e) {

		}

		return result;
	}

	/**
	 * Get the preference value as boolean, if not found => return default value
	 * 
	 * @param key
	 * @return
	 */
	public boolean getPreferenceBoolValue(String key, boolean defaultValue) {

		boolean result = defaultValue;

		try {
			result = Boolean.valueOf(getPreference(key).getValue());
		} catch (PreferenceNotFoundException e) {
		}

		return result;
	}

	/**
	 * Get the preference identified by the id "key"
	 * 
	 * @param key
	 * @return
	 * @throws PreferenceNotFoundException
	 */
	public Preference getPreference(String key) throws PreferenceNotFoundException {

		// output
		Preference pref = null;

		String query = "select * from APP.PREFERENCE where PREFERENCE_KEY = ?";

		try (Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setFetchSize(2000);
			stmt.clearParameters();

			stmt.setString(1, key);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) 
					pref = getByResultSet(rs);
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (pref == null)
			throw new PreferenceNotFoundException();

		return pref;
	}

	/**
	 * Get all the preferences
	 * 
	 * @param key
	 * @return
	 */
	public ArrayList<Preference> getAll() {

		// output
		ArrayList<Preference> preferences = new ArrayList<>();

		String query = "select * from APP.PREFERENCE";

		try (Connection con = getConnection();
				PreparedStatement stmt = con.prepareStatement(query);
				ResultSet rs = stmt.executeQuery();) {

			while (rs.next()) {

				// create the preference
				Preference pref = getByResultSet(rs);

				// add the preference to the list
				preferences.add(pref);
			}

			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return preferences;
	}

	/**
	 * Insert a new preference into the database
	 * 
	 * @param pref
	 * @return
	 */
	public int insert(Preference pref) {

		int id = -1;

		String query = "insert into APP.PREFERENCE (PREFERENCE_KEY, "
				+ "PREFERENCE_TYPE, PREFERENCE_VALUE, PREFERENCE_EDITABLE) values (?,?,?,?)";

		try (Connection con = getConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			stmt.clearParameters();

			stmt.setString(1, pref.getKey());
			stmt.setString(2, pref.getType().name());
			stmt.setString(3, pref.getValue());
			stmt.setBoolean(4, pref.isEditable());

			stmt.executeUpdate();

			try (ResultSet rs = stmt.getGeneratedKeys();) {

				if (rs.next())
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

	/**
	 * Update a preference of the database
	 * 
	 * @param pref
	 * @return
	 */
	public boolean update(Preference pref) {

		String query = "update APP.PREFERENCE set PREFERENCE_TYPE = ?, "
				+ "PREFERENCE_VALUE = ?, PREFERENCE_EDITABLE = ? where PREFERENCE_KEY = ?";

		try (Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setString(1, pref.getType().name());
			stmt.setString(2, pref.getValue());
			stmt.setBoolean(3, pref.isEditable());
			stmt.setString(4, pref.getKey());

			int affectedRows = stmt.executeUpdate();

			// if no preference was found, insert it!
			if (affectedRows == 0) {
				insert(pref);
			}

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
	 * Remove all the preferences from the database
	 */
	public void removeAll() {

		String query = "delete from APP.PREFERENCE";

		try (Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.executeUpdate();

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}
	}

	public boolean remove(Preference object) {

		String query = "delete from APP.PREFERENCE where PREFERENCE_KEY = ?";

		try (Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, object.getKey());
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

	public Preference getById(int id) {
		LOGGER.error("PreferenceDAO: getById not supported!");
		return null;
	}

	/**
	 * Check if the preference is already present or not
	 * 
	 * @param pref
	 * @return
	 */
	public boolean contains(String key) {
		return contains(new Preference(key, null, null, false));
	}

	/**
	 * Check if the preference is already present or not
	 * 
	 * @param pref
	 * @return
	 */
	public boolean contains(Preference pref) {

		boolean contained = false;

		String query = "select * from APP.PREFERENCE where PREFERENCE_KEY = ?";

		try (Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, pref.getKey());

			try (ResultSet rs = stmt.executeQuery();) {

				if (rs.next())
					contained = true;

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return contained;
	}

	/**
	 * Insert a preference if not present, otherwise update it
	 * 
	 * @param pref
	 */
	public void insertUpdate(Preference pref) {

		if (!contains(pref.getKey())) {

			// create the preference
			insert(pref);
		} else {
			// update the preference
			update(pref);
		}
	}

	/**
	 * Get the preference from the result set
	 */
	public Preference getByResultSet(ResultSet rs) throws SQLException {

		String key = rs.getString("PREFERENCE_KEY");
		String type = rs.getString("PREFERENCE_TYPE");
		String value = rs.getString("PREFERENCE_VALUE");
		boolean editable = rs.getBoolean("PREFERENCE_EDITABLE");

		// create the preference
		Preference pref = new Preference(key, PreferenceType.getTypeFromName(type), value, editable);

		return pref;
	}

	@Override
	public List<Integer> insert(Iterable<Preference> attrs) {

		return null;
	}

	public abstract Connection getConnection() throws SQLException;
}
