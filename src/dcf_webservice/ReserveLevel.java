package dcf_webservice;

/**
 * At which level we make a reserve operation? 
 * Minor has some editing limitations.
 * Values:
 * NONE
 * MINOR
 * MAJOR
 * @author avonva
 *
 */
public enum ReserveLevel {
	
	NONE ( 0 ),
	MINOR ( 1 ),
	MAJOR ( 2 );
	
	private Integer level;

	/**
	 * Initialize a reserve level
	 * @param level
	 * @param description
	 */
	ReserveLevel( int level ) {
        this.level = level;
    }
	
	/**
	 * Get the reserve operation linked to this reserve level
	 * we use this string to create the correct attachment
	 * in reserve web service.
	 * @return
	 */
	public String getReserveOperation () {
		
		String opType = null;
		
		switch ( this ) {
		case NONE:
			opType = "unreserve";
			break;
		case MINOR:
			opType = "reserveMinor";
			break;
		case MAJOR:
			opType = "reserveMajor";
			break;
		default:
			break;
		}
		
		return opType;
	}
	
	/**
	 * Check if the level is none
	 * @return
	 */
	public boolean isNone() {
		return this == NONE;
	}
	
	/**
	 * Check if the level is minor
	 * @return
	 */
	public boolean isMinor() {
		return this == MINOR;
	}
	
	/**
	 * Check if the level is major
	 * @return
	 */
	public boolean isMajor() {
		return this == MAJOR;
	}

	/**
	 * Check if the reserve level is greater than the one
	 * passed as parameter
	 * @param other
	 * @return
	 */
    public boolean greaterThan( ReserveLevel other ) {
        return this.level > other.level;
    }
}
