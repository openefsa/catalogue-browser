package catalogue_object;

import catalogue.Catalogue;

/**
 * Model and manage catalogues objects versions. It allows comparing
 * versions among them and incrementing their major, minor or internal values.
 * @author avonva
 *
 */
public class Version {
	
	private int major;            // major part of the version
	private int minor;            // minor part of the version
	private int internal;         // internal part of the version
	private boolean addInternal;  // if the internal version should be included in the version or not
	private boolean wrongFormat;  // if we had a wrong format in input

	public Version( String version ) {
		
		// if void parameter => return 0.0.0
		if ( version == null ) {
			major = minor = internal = 0;
			wrongFormat = true;
			return;
		}
		
		this.addInternal = false;
		
		// split the version pieces
		String[] versionUnpacked = version.split( "\\." );
		
		// if less than two elements => wrong format
		if ( versionUnpacked.length < 2 ) {
			wrongFormat = true;
			return;
		}
		
		// convert the version into integer pieces
		this.major = Integer.valueOf( versionUnpacked[0] );
		this.minor = Integer.valueOf( versionUnpacked[1] );
		
		// get also the internal version if present
		if ( versionUnpacked.length > 2 ) {
			
			// try to get the internal version if present
			// we need the try since it is possible that
			// instead a number we have a flag
			try {

				this.internal = Integer.valueOf( versionUnpacked[2] );
				// save that the internal version should be included
				addInternal = true;

			} catch ( NumberFormatException e ) {
			}
		}
	}
	
	/**
	 * Get the new version crafted by
	 * incrementing the major, minor and
	 * internal versions.
	 * @return the new version
	 */
	public String getVersion() {
		
		if ( wrongFormat )
			return Catalogue.NOT_APPLICABLE_VERSION;
		
		String newVersion = major + "." + minor;
		
		// if the internal version was set
		// add it
		if ( addInternal )
			newVersion = newVersion + "." + internal;
		
		return newVersion;
	}
	
	/**
	 * Get the major part of the version
	 * @return
	 */
	public int getMajor() {
		return major;
	}
	
	/**
	 * Increment major by 1
	 */
	public void incrementMajor() {
		major = major + 1;
		minor = 0;
		resetInternal();
	}
	
	/**
	 * Get the minor part of the version
	 * @return
	 */
	public int getMinor() {
		return minor;
	}
	
	/**
	 * Increment minor by 1
	 */
	public void incrementMinor() {
		minor = minor + 1;
		resetInternal();
	}
	
	/**
	 * Get the internal part of the version.
	 * @return the internal version if found,
	 * otherwise -1.
	 */
	public int getInternal() {
		return addInternal ? internal : -1;
	}
	
	/**
	 * Increment internal version by 1
	 */
	public void incrementInternal() {
		internal = internal + 1;
		addInternal = true;
	}
	
	/**
	 * Reset the internal version
	 */
	public void resetInternal() {
		internal = 0;
		addInternal = false;
	}
	
	/**
	 * Check if the version is an internal
	 * version or not
	 * @return
	 */
	public boolean isInternalVersion() {
		return addInternal;
	}
	
	/**
	 * If the version has a wrong format
	 * or not
	 * @return
	 */
	public boolean isWrongFormat() {
		return wrongFormat;
	}
	
	/**
	 * Compare the version of two catalogues
	 * @param the other version object
	 * @return an integer saying if the versions are equal or if
	 * the first version is older or newer.
	 */
	public int compareTo( Version version ) { 
		return compareTo ( version.getVersion() );
	}
	
	/**
	 * Compare the version of two catalogues
	 * @param the catalogue version string
	 * @return an integer saying if the versions are equal or if
	 * the first version is older or newer.
	 */
	public int compareTo( String version ) {
		
		// initialize another checker for the other catalogue
		Version other = new Version( version );
		
		if (this.isWrongFormat() && !other.isWrongFormat())
			return 1;
		
		if (!this.isWrongFormat() && other.isWrongFormat())
			return -1;

		int majorCheck = compareInteger( this.getMajor(), other.getMajor() );
		int minorCheck = compareInteger( this.getMinor(), other.getMinor() );
		int internalCheck = compareInteger( this.getInternal(), other.getInternal() );

		// if difference in major return it
		if ( majorCheck != 0 )
			return majorCheck;
		
		// if difference in minor return it
		if ( minorCheck != 0 )
			return minorCheck;
		
		// if difference in internal return it
		// note that if internal is disabled it is
		// set by default at 0, therefore there is
		// no problem in the check
		if ( internalCheck != 0 )
			return internalCheck;
		
		// else they are equal
		return 0;
	}
	
	/**
	 * Compare two integers
	 * @param i1
	 * @param i2
	 * @return
	 */
	private int compareInteger ( int i1, int i2 ) {
		
		if ( i1 > i2 )
			return -1;
		else if ( i1 < i2 )
			return 1;
		else
			return 0;
	}
	
	@Override
	public String toString() {
		return getVersion();
	}
	
	@Override
	public boolean equals(Object obj) {

		if ( !( obj instanceof Version ) )
			return super.equals(obj);
		
		// compare the string version
		return this.getVersion().equals( ( (Version) obj ).getVersion() );
	}
}
