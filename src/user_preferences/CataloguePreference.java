package user_preferences;

/**
 * Model a catalogue preference ( record of the table Preference of the catalogue db )
 * @author avonva
 *
 */
public class CataloguePreference extends Preference {

	// default static keys for preferences
	public static String currentPicklistKey = "favouritePicklist";
	public static String maxRecentTerms = "maxRecentTerms";
	public static String minSearchChar = "minSearchChar";
	public static String copyImplicitFacets = "copyImplicitFacets";
	public static String enableBusinessRules = "enableBusinessRules";
	public static String logging = "logging";
	
	/**
	 * Constructor with key, value. The value variable is always converted to string
	 * in order to be saved.
	 * @param key
	 * @param value
	 */
	public CataloguePreference( String key, PreferenceType type, Object value ) {
		super(key, type, value);
	}
}
