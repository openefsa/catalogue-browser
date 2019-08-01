package user_preferences;

/**
 * Enum used to specify the catalogue preference value type, as integer, boolean string...
 * @author avonva
 *
 */
public enum PreferenceType {
	
	BOOLEAN,
	INTEGER,
	STRING;
	
	/**
	 * Get the preference type starting from its name
	 * @param name
	 * @return
	 */
	public static PreferenceType getTypeFromName( String name ) {
		
		for ( PreferenceType type : values() ) {
			
			if ( type.name().equals( name ) )
				return type;
		}
		
		return null;
	}
}


