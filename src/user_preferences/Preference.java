package user_preferences;

/**
 * Model a general preference with key/value.
 * @author avonva
 *
 */
public class Preference {
	private String key, value;
	private PreferenceType type;
	private boolean editable;
	
	/**
	 * Constructor with key, value. The value variable is always converted to string
	 * in order to be saved.
	 * @param key
	 * @param value
	 */
	public Preference( String key, PreferenceType type, Object value, boolean editable ) {
		this.key = key;
		this.type = type;
		this.value = String.valueOf( value );
		this.editable = editable;
	}
	
	/**
	 * Get the preference key (i.e. the value which identifies the preference)
	 * @return
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * Get the preference value type
	 * @return
	 */
	public PreferenceType getType() {
		return type;
	}
	
	/**
	 * Check if the user can edit the preference or not
	 * @return
	 */
	public boolean isEditable() {
		return editable;
	}
	
	/**
	 * Get the preference string value
	 * @return
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Set the preference value
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	/**
	 * Set the preference value
	 * @param value
	 */
	public void setValue(Object value) {
		this.value = String.valueOf( value );
	}
	
	@Override
	public String toString() {
		return key + " => " + value;
	}
	
	@Override
	public boolean equals(Object arg0) {
		
		String other = null;
		if (arg0 instanceof Preference) {
			
			Preference pref = (Preference) arg0;
			other = pref.getKey();
		}
		else if (arg0 instanceof String) {
			other = (String) arg0;
		}
		
		if (other != null)
			return this.key.equalsIgnoreCase(other);
		
		return super.equals(arg0);
	}
}
