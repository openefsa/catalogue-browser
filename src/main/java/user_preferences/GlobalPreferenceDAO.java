package user_preferences;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import dcf_manager.Dcf;
import dcf_manager.Dcf.DcfType;

public class GlobalPreferenceDAO extends PreferenceDAO {

	private static final Logger LOGGER = LogManager.getLogger(GlobalPreferenceDAO.class);
	
	@Override
	public void setCatalogue(Catalogue catalogue) {}
	
	/**
	 * Insert the default preferences for user interface
	 */
	public void insertDefaultPreferences () {

		// first we remove all the preferences (avoid errors of duplicated keys)
		removeAll();

		insert( new GlobalPreference( GlobalPreference.HIDE_DEPR_MAIN, 
				PreferenceType.BOOLEAN, false, true ) );

		insert( new GlobalPreference( GlobalPreference.HIDE_NOT_REP_MAIN, 
				PreferenceType.BOOLEAN, false, true ) );

		insert( new GlobalPreference( GlobalPreference.HIDE_TERM_CODE_MAIN, 
				PreferenceType.BOOLEAN, false, true ) );

		insert( new GlobalPreference( GlobalPreference.HIDE_DEPR_DESCRIBE, 
				PreferenceType.BOOLEAN, false, true ) );

		insert( new GlobalPreference( GlobalPreference.HIDE_NOT_REP_DESCRIBE, 
				PreferenceType.BOOLEAN, false, true ) );

		insert( new GlobalPreference( GlobalPreference.HIDE_TERM_CODE_DESCRIBE, 
				PreferenceType.BOOLEAN, false, true ) );
	}
	
	/**
	 * Save the catalogue as last opened one.
	 * @param catalogue
	 */
	public void saveOpenedCatalogue ( Catalogue catalogue ) {

		int id = catalogue == null ? -1 : catalogue.getId();
		
		String key = Dcf.dcfType == DcfType.PRODUCTION ? 
				GlobalPreference.LAST_OPENED_CAT_PROD : 
					GlobalPreference.LAST_OPENED_CAT_TEST;
		
		Preference pref = new Preference( key, 
				PreferenceType.INTEGER, id, false);

		if ( id == -1 )
			remove ( pref );
		else
			insertUpdate( pref );
		
	}
	
	
	/**
	 * Get the last opened catalogue
	 * @return
	 * @throws PreferenceNotFoundException
	 */
	public Catalogue getLastCatalogue() throws PreferenceNotFoundException {
		
		String key = Dcf.dcfType == DcfType.PRODUCTION ? 
				GlobalPreference.LAST_OPENED_CAT_PROD : 
					GlobalPreference.LAST_OPENED_CAT_TEST;
		
		Preference pref = getPreference( key );
		
		Catalogue catalogue;
		
		try {
			int id = Integer.valueOf( pref.getValue() );
			CatalogueDAO catDao = new CatalogueDAO();
			catalogue = catDao.getById( id );
		} catch ( NumberFormatException e ) {
			e.printStackTrace();
			LOGGER.info("Cannot find last catalogue", e);
			throw new PreferenceNotFoundException();
		}
		
		return catalogue;
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return DatabaseManager.getMainDBConnection();
	}
}
