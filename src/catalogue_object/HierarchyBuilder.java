package catalogue_object;

/**
 * Builder to create a hierarchy object step by step
 * @author avonva
 *
 */
public class HierarchyBuilder extends CatalogueObjectBuilder {
	
	private int order;
	private String applicability;
	private String groups;
	private boolean master = false;
	
	/**
	 * Build the hierarchy
	 * @return
	 */
	public Hierarchy build() {
		return new Hierarchy( catalogue, id, code, name, label, scopenotes, applicability, 
				order, status, master, version, lastUpdate, validFrom, validTo, deprecated, groups);
	}

	/**
	 * Set the applicability of the hierarchy (base, attribute, both)
	 * @param applicability
	 */
	public void setApplicability(String applicability) {
		this.applicability = applicability;
	}
	
	/**
	 * Set the order of the hierarchy
	 * @param order
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Set if the hierarchy is the master or not
	 * @param master
	 */
	public void setMaster( boolean master ) {
		this.master = master;
	}

	/**
	 * Set the hierarchy groups
	 * @param groups
	 */
	public void setGroups(String groups) {
		this.groups = groups;
	}
}
