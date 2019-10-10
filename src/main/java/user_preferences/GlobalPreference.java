package user_preferences;

/**
 * Preference which is used to store preferences related to general settings 
 * (logging, hide deprecated, hide not reportable... )
 * @author avonva
 *
 */
public class GlobalPreference extends Preference {

	public static final String HIDE_DEPR_MAIN = "hideDeprMain";
	public static final String HIDE_NOT_REP_MAIN = "hideNotReprMain";
	public static final String HIDE_TERM_CODE_MAIN = "hideTermCodeMain";
	public static final String HIDE_DEPR_DESCRIBE = "hideDeprDescribe";
	public static final String HIDE_NOT_REP_DESCRIBE = "hideNotReprDescribe";
	public static final String HIDE_TERM_CODE_DESCRIBE = "hideTermCodeDescribe";
	public static final String LAST_OPENED_CAT_PROD = "lastOpenedCatalogueProduction";
	public static final String LAST_OPENED_CAT_TEST = "lastOpenedCatalogueTest";
	
	public GlobalPreference( String key, PreferenceType type, 
			Object value, boolean editable ) {
		super(key, type, value, editable);
	}
}
