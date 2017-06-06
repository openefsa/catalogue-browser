package catalogue_object;

import java.sql.Timestamp;

/**
 * Builder for the general catalogue object. It must be extended to match more
 * specific needs (as builders for catalogue, attributes...)
 * @author avonva
 *
 */
public abstract class BaseObjectBuilder {

	protected int id;
	protected String code;
	protected String version;
	protected String name = "";
	protected String label = "";
	protected String scopenotes= "";
	protected Timestamp lastUpdate = null;
	protected Timestamp validFrom = null;
	protected Timestamp validTo = null;
	protected String status = null;
	protected boolean deprecated = false;
	
	/**
	 * Set the id of the object
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Set the code of the object
	 * @param code
	 * @return
	 */
	public BaseObjectBuilder setCode(String code) {
		this.code = code;
		return this;
	}
	
	/**
	 * Set the name of the object
	 * @param name
	 * @return
	 */
	public BaseObjectBuilder setName(String name) {
		this.name = name;
		return this;
	}
	
	/**
	 * Set the label of the object
	 * @param label
	 * @return
	 */
	public BaseObjectBuilder setLabel(String label) {
		this.label = label;
		return this;
	}
	
	/**
	 * Set the scopenotes of the object
	 * @param scopeNote
	 * @return
	 */
	public BaseObjectBuilder setScopenotes(String scopenotes) {
		this.scopenotes = scopenotes;
		return this;
	}
	
	/**
	 * Set the version of the object
	 * @param version
	 * @return
	 */
	public BaseObjectBuilder setVersion(String version) {
		this.version = version;
		return this;
	}
	
	/**
	 * Set the last update of the object
	 * @param lastUpdate
	 * @return
	 */
	public BaseObjectBuilder setLastUpdate(Timestamp lastUpdate) {
		this.lastUpdate = lastUpdate;
		return this;
	}
	
	/**
	 * Set the valid from of the object
	 * @param validFrom
	 * @return
	 */
	public BaseObjectBuilder setValidFrom(Timestamp validFrom) {
		this.validFrom = validFrom;
		return this;
	}
	
	/**
	 * Set the valid to of the object
	 * @param validTo
	 * @return
	 */
	public BaseObjectBuilder setValidTo(Timestamp validTo) {
		this.validTo = validTo;
		return this;
	}
	
	/**
	 * Set the status of the object
	 * @param status
	 * @return
	 */
	public BaseObjectBuilder setStatus(String status) {
		this.status = status;
		return this;
	}
	
	/**
	 * Set it the object is deprecated or not
	 * @param deprecated
	 * @return
	 */
	public BaseObjectBuilder setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}
	
	/**
	 * Create the object from the builder class, the child class
	 * needs to specify which type of catalogue object is created
	 * @return
	 */
	public abstract BaseObject build();
}
