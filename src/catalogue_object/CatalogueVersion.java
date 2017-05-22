package catalogue_object;

import java.util.StringTokenizer;

public class CatalogueVersion extends Version {

	// flag for invalid versions
	private static final String INVALID_VERSION = "INV";
	private static final String FORCED_VERSION = "FRC";
	
	private boolean invalid;      // true if the internal version is not the last one
	private boolean forced;       // true if the version is a dummy version
	private int forcedCount;      // number displayed after the FRC for forced versions
	
	public CatalogueVersion( String version ) {

		super ( version );
		
		this.invalid = false;
		this.forced = false;
		
		StringTokenizer st = new StringTokenizer( version, "\\." );
		
		while ( st.hasMoreTokens() ) {
			
			String token = st.nextToken();
			
			if ( token.equals( FORCED_VERSION ) ||
					token.equals ( INVALID_VERSION ) ) {
				
				if ( token.equals( FORCED_VERSION ) )
					forced = true;
				else if ( token.equals( INVALID_VERSION ) )
					invalid = true;
				
				forcedCount = Integer.valueOf( st.nextToken() );
			}
		}
		
		// if there is the invalid flag
		if ( version.contains ( INVALID_VERSION ) )
			invalid = true;
		
		// if there is the dummy flag
		if ( version.contains ( FORCED_VERSION ) )
			forced = true;
	}
	
	/**
	 * Set the forced count
	 * @param forcedCount
	 */
	public void setForcedCount(int forcedCount) {
		this.forcedCount = forcedCount;
	}
	
	/**
	 * Get the forced count of the version
	 * @return
	 */
	public int getForcedCount() {
		return forcedCount;
	}
	
	/**
	 * Get the new version crafted by
	 * incrementing the major, minor and
	 * internal versions.
	 * @return the new version
	 */
	public String getVersion() {

		if ( isWrongFormat() )
			return Catalogue.NOT_APPLICABLE_VERSION;
		
		String newVersion = super.getVersion();
		
		// add invalid flag
		if ( invalid )
			newVersion = newVersion + "." + INVALID_VERSION + "." + forcedCount;
		// add forced flag
		else if ( forced )
			newVersion = newVersion + "." + FORCED_VERSION + "." + forcedCount;
		
		return newVersion;
	}
	
	/**
	 * Set this internal version of the
	 * catalogue as not up to date version.
	 * The version will get the flag {@value #INVALID_VERSION}.
	 */
	public void invalidate() {
		invalid = true;
	}
	
	/**
	 * True if invalid version
	 * @return
	 */
	public boolean isInvalid() {
		return invalid;
	}
	
	/**
	 * Mark the current version as forced.
	 * This means that this version is not an
	 * official version. The version will
	 * get the flag {@value #FORCED_VERSION}.
	 * @param forcedCount the number which is displayed
	 * after the forced flag
	 */
	public void force( int forcedCount ) {
		forced = true;
		this.forcedCount = forcedCount;
	}
	
	/**
	 * True if forced version (i.e. not official)
	 * @return
	 */
	public boolean isForced() {
		return forced;
	}
	
	/**
	 * Remove the forced status from the version
	 * @return
	 */
	public void removeForced() {
		forced = false;
	}
	
	/**
	 * Compare the version of two catalogues
	 * @param the other version object
	 * @return an integer saying if the versions are equal or if
	 * the first version is older or newer.
	 */
	public int compareTo( CatalogueVersion version ) { 
		return compareTo ( version.getVersion() );
	}
	
	@Override
	public int compareTo( String version ) {
		
		CatalogueVersion other = new CatalogueVersion( version );
		
		// get the base class differences
		int comp = super.compareTo( version );
		
		// if differences already detected return them
		if ( comp != 0 )
			return comp;
		
		// otherwise
		// if invalid other has the priority
		if ( this.isInvalid() && !other.isInvalid() )
			return 1;

		// if this is valid, it has the priority
		if ( !this.isInvalid() && other.isInvalid() )
			return -1;

		// here the version are equal except for forced status
		// forced versions have the priority
		if ( this.isForced() )
			return -1;

		if ( other.isForced() )
			return 1;

		// else they are equal
		return 0;
	}
}
