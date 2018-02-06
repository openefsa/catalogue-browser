package catalogue_object;

import java.sql.Timestamp;

/**
 * General object which is the baseline for term, hierarchy, attribute and catalogue
 * @author avonva
 *
 */
public class BaseObject implements Nameable {

	private int id;
	private String code;
	private String name;
	private String label;
	private String scopenotes;
	private Version version;
	private Timestamp lastUpdate;
	private Timestamp validFrom;
	private Timestamp validTo;
	private Status status;
	private boolean deprecated;
	
	public BaseObject () {}
	
	/**
	 * Initialize a base object
	 * @param id
	 * @param version
	 * @param lastUpdate
	 * @param validFrom
	 * @param validTo
	 * @param status
	 * @param deprecated
	 */
	public BaseObject( int id, String code, String name, String label, String scopenotes, 
			String version, Timestamp lastUpdate, Timestamp validFrom,
			Timestamp validTo, String status, boolean deprecated ) {

		this.id = id;
		this.code = code;
		this.name = name;
		this.label = label;
		this.scopenotes = scopenotes;
		this.version = new Version( version );
		this.lastUpdate = lastUpdate;
		this.validFrom = validFrom;
		this.validTo = validTo;
		this.status = new Status( status );
		this.deprecated = deprecated;
	}

	public void setId(int id) {
		this.id = id;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public void setScopenotes(String scopenotes) {
		this.scopenotes = scopenotes;
	}
	public void setRawVersion(Version version) {
		this.version = version;
	}
	public void setVersion(String version) {
		this.version = new Version( version );
	}
	public void setLastUpdate(Timestamp lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	public void setValidFrom(Timestamp validFrom) {
		this.validFrom = validFrom;
	}
	public void setValidTo(Timestamp validTo) {
		this.validTo = validTo;
	}
	public void setStatus(String status) {
		this.status = new Status( status );
	}
	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}

	public int getId() {
		return id;
	}
	public String getCode() {
		return code;
	}
	public String getName() {
		return name;
	}
	public String getLabel() {
		return label;
	}
	public String getScopenotes() {
		return scopenotes;
	}
	
	/**
	 * Get the raw object version which contains
	 * all the version information splitted 
	 * (major, minor and internal version are 
	 * subdivided in integers)
	 * @return
	 */
	public Version getRawVersion() {
		return version;
	}
	
	/**
	 * Get the version in string format
	 * the major, minor and internal are
	 * dot separated.
	 * @return the version of the base object
	 * if present, null otherwise
	 */
	public String getVersion() {
		if ( version != null )
			return version.getVersion();
		else return null;
	}
	public Timestamp getLastUpdate() {
		return lastUpdate;
	}
	public Timestamp getValidFrom() {
		return validFrom;
	}
	public Timestamp getValidTo() {
		return validTo;
	}
	/**
	 * Get the status in string format
	 * @return the status if found, null
	 * otherwise
	 */
	public String getStatus() {
		
		if ( status != null )
			return status.getStatus();
		
		return null;
	}
	
	public Status getStatusObject() {
		return this.status;
	}
	public Status getRawStatus() {
		return status;
	}
	public void setRawStatus(String status) {
		this.status = new Status(status);
	}
	public boolean isDeprecated() {
		return deprecated;
	}
}
