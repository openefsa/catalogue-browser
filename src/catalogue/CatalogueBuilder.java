package catalogue;

import catalogue_object.BaseObjectBuilder;
import dcf_manager.Dcf.DcfType;

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
	String backupDbPath = null;
	String dbPath = null;
	boolean local = false;                  // default value
	int forcedCount = 0;
	DcfType catalogueType;
	ReleaseNotes releaseNotes;

	/**
	 * Set if the catalogue is a production or a test catalogue
	 * @param catalogueType
	 */
	public void setCatalogueType(DcfType catalogueType) {
		this.catalogueType = catalogueType;
	}
	
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

	/**
	 * Set the external reference to the db which contains
	 * a backup of the catalogue db before starting editing it.
	 * @param backupDbPath
	 */
	public CatalogueBuilder setBackupDbPath(String backupDbPath) {
		this.backupDbPath = backupDbPath;
		return this;
	}
	
	public void setDbPath(String dbPath) {
		this.dbPath = dbPath;
	}

	/**
	 * Set the release notes of the catalogue
	 * @param releaseNotes
	 * @return
	 */
	public CatalogueBuilder setReleaseNotes(ReleaseNotes releaseNotes) {
		this.releaseNotes = releaseNotes;
		return this;
	}
	
	/**
	 * Create the catalogue
	 * @return
	 */
	public Catalogue build () {
		
		// create the catalogue object and return it
		return new Catalogue ( id, catalogueType, code, name, label, scopenotes, 
				termCodeMask, termCodeLength, termMinCode,
				acceptNonStandardCodes, generateMissingCodes, version, lastUpdate, validFrom, 
				validTo, status, catalogueGroups, deprecated, dbPath, backupDbPath, local, forcedCount, 
				releaseNotes );
	}
}
