package user_preferences;

import java.sql.Connection;
import java.sql.SQLException;

import catalogue_browser_dao.DatabaseManager;

public class UIPreferenceDAO extends PreferenceDAO {

	/**
	 * Insert the default preferences for user interface
	 */
	public void insertDefaultPreferences () {

		// first we remove all the preferences (avoid errors of duplicated keys)
		removeAll();

		insert( new UIPreference( UIPreference.hideDeprMain, 
				PreferenceType.BOOLEAN, false ) );

		insert( new UIPreference( UIPreference.hideNotReprMain, 
				PreferenceType.BOOLEAN, false ) );

		insert( new UIPreference( UIPreference.hideTermCodeMain, 
				PreferenceType.BOOLEAN, false ) );

		insert( new UIPreference( UIPreference.hideDeprDescribe, 
				PreferenceType.BOOLEAN, false ) );

		insert( new UIPreference( UIPreference.hideNotReprDescribe, 
				PreferenceType.BOOLEAN, false ) );

		insert( new UIPreference( UIPreference.hideTermCodeDescribe, 
				PreferenceType.BOOLEAN, false ) );
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return DatabaseManager.getMainDBConnection();
	}
}
