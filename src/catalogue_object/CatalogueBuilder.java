package catalogue_object;

import dcf_webservice.ReserveLevel;

/**
 * Builder class for generating a catalogue step by step
 * @author avonva
 *
 */
public class CatalogueBuilder extends BaseObjectBuilder {

	String termCodeMask = null;
	String termCodeLength = null;
	String termMinCode = null;
	boolean acceptNonStandardCodes = true;  // default value
	boolean generateMissingCodes = false;   // default value
	String catalogueGroups = null;
	String dbFullPath = null;
	String backupDbPath = null;
	ReserveLevel reserveLevel = ReserveLevel.NONE;
	private String reserveUsername;
	private String reserveNote;
	boolean local = false;                  // default value
	int forcedCount = 0;

	/**
	 * Set the term code mask of the catalogue
	 * @param termCodeMask
	 */
	public CatalogueBuilder setTermCodeMask(String termCodeMask) {
		this.termCodeMask = termCodeMask;
		return this;
	}

	/**
	 * Set the term code length of the catalogue
	 * @param termCodeLength
	 */
	public CatalogueBuilder setTermCodeLength(String termCodeLength) {
		this.termCodeLength = termCodeLength;
		return this;
	}

	/**
	 * Set the minimun term code
	 * @param termMinCode
	 * @return
	 */
	public CatalogueBuilder setTermMinCode(String termMinCode) {
		this.termMinCode = termMinCode;
		return this;
	}

	/**
	 * Set if the the catalogue should accept or not standard codes
	 * @param acceptNonStandardCodes
	 * @return
	 */
	public CatalogueBuilder setAcceptNonStandardCodes(boolean acceptNonStandardCodes) {
		this.acceptNonStandardCodes = acceptNonStandardCodes;
		return this;
	}

	/**
	 * Set if the catalogue should generate missing codes
	 * @param generateMissingCodes
	 * @return
	 */
	public CatalogueBuilder setGenerateMissingCodes(boolean generateMissingCodes) {
		this.generateMissingCodes = generateMissingCodes;
		return this;
	}


	/**
	 * Set the catalogue groups, note that each group is $ separated (in case of multiple groups)!
	 * @param catalogueGroups
	 * @return
	 */
	public CatalogueBuilder setCatalogueGroups(String catalogueGroups) {
		this.catalogueGroups = catalogueGroups;
		return this;
	}

	/**
	 * Set if the catalogue is a local catalogue or not
	 * @param local
	 */
	public CatalogueBuilder setLocal(boolean local) {
		this.local = local;
		return this;
	}
	
	/**
	 * Set the forced count of the catalogue (number of times
	 * we had forced the catalogue editing mode since dcf
	 * was busy in a reserve operation)
	 * @param forcedCount
	 * @return
	 */
	public CatalogueBuilder setForcedCount ( int forcedCount ) {
		this.forcedCount = forcedCount;
		return this;
	}
	
	public CatalogueBuilder setReserveLevel( ReserveLevel reserveLevel ) {
		this.reserveLevel = reserveLevel;
		return this;
	}
	
	/**
	 * Set the reserve level by its name
	 * @param reserveLevel
	 * @return
	 */
	public CatalogueBuilder setReserveLevel( String reserveLevel ) {
		
		// default
		if ( reserveLevel == null )
			reserveLevel = ReserveLevel.NONE.name();
		
		this.reserveLevel = ReserveLevel.valueOf( reserveLevel );
		return this;
	}
	
	/**
	 * Set the user name of the user who reserved the catalogue
	 * @param reserveUsername
	 */
	public void setReserveUsername(String reserveUsername) {
		this.reserveUsername = reserveUsername;
	}
	
	/**
	 * Set the reserve note for reserve actions
	 * @param reserveNote
	 */
	public void setReserveNote(String reserveNote) {
		this.reserveNote = reserveNote;
	}

	/**
	 * Set the external reference to the db which contains
	 * the real data of the catalogue
	 * @param dbPath
	 */
	public CatalogueBuilder setDbFullPath(String dbFullPath) {
		this.dbFullPath = dbFullPath;
		return this;
	}

	/**
	 * Set the external reference to the db which contains
	 * a backup of the catalogue db before starting editing it.
	 * @param backupDbPath
	 */
	public CatalogueBuilder setBackupDbPath(String backupDbPath) {
		this.backupDbPath = backupDbPath;
		return this;
	}

	/**
	 * Create the catalogue
	 * @return
	 */
	public Catalogue build () {
		
		// create the catalogue object and return it
		return new Catalogue ( id, code, name, label, scopenotes, termCodeMask, termCodeLength, termMinCode,
				acceptNonStandardCodes, generateMissingCodes, version, lastUpdate, validFrom, 
				validTo, status, catalogueGroups, deprecated, dbFullPath, backupDbPath, local, forcedCount,
				reserveUsername, reserveLevel );
	}
}
