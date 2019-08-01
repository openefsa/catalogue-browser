package user_preferences;

/**
 * Exception which is thrown when a catalogue preference value is not found from the database using a key
 * @author avonva
 *
 */
public class PreferenceNotFoundException extends Exception {
	
	private static final long serialVersionUID = -3059548853853109163L;

	public PreferenceNotFoundException() {
		super ( "Preference not found, getting the default value" );
	}
}
