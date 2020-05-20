package user_preferences;

/**
 * Model a catalogue preference (
 *  record of the table Preference of the catalogue db )
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class CataloguePreference extends Preference {

	// default static keys for preferences
	public final static String currentPicklistKey = "Favourite Picklist";
	public final static String maxRecentTerms = "Maximum Recent Terms";
	public final static String minSearchChar = "Minimum Search Char";
	public final static String copyImplicitFacets = "Copy Implicit Facets";
	public final static String enableBusinessRules = "Enable Business Rules";
	public final static String rememberLastSelected = "Remember Last Selected Term";

	// last graphical objects which were opened
	public final static String LAST_TERM_PREF = "lastTermId";
	public final static String LAST_HIER_PREF = "lastHierarchyId";

	/**
	 * Constructor with key, value. The value variable is always converted to string
	 * in order to be saved.
	 * 
	 * @param key
	 * @param value
	 */
	public CataloguePreference(String key, PreferenceType type, Object value, boolean editable) {
		super(key, type, value, editable);
	}

	public CataloguePreference(Preference p) {
		super(p.getKey(), p.getType(), p.getValue(), p.isEditable());
	}
}
