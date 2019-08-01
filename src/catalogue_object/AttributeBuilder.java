package catalogue_object;

/**
 * Builder to build the attribute class step by step
 * @author avonva
 *
 */
public class AttributeBuilder extends CatalogueObjectBuilder {

	private int order;
	private int maxLength;
	private int precision;
	private int scale;
	private String reportable;
	private String type;
	private String catalogueCode;
	private String singleOrRepeatable;
	private String inheritance;
	private boolean visible;
	private boolean searchable;
	private boolean uniqueness;
	private boolean termCodeAlias;
	
	/**
	 * Build the attribute
	 * @return
	 */
	public Attribute build () {
		
		return new Attribute( catalogue, id, code, name, label, scopenotes, reportable, 
				visible, searchable, order, type, maxLength, precision, scale,
				catalogueCode, singleOrRepeatable, inheritance, uniqueness, 
				termCodeAlias, status, version, lastUpdate, validFrom, validTo, deprecated );
	}
	
	public void setReportable(String reportable) {
		this.reportable = reportable;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}
	
	public void setPrecision(int precision) {
		this.precision = precision;
	}
	
	public void setScale(int scale) {
		this.scale = scale;
	}
	
	public void setCatalogueCode(String catalogueCode) {
		this.catalogueCode = catalogueCode;
	}
	
	public void setSingleOrRepeatable(String singleOrRepeatable) {
		this.singleOrRepeatable = singleOrRepeatable;
	}
	
	public void setInheritance(String inheritance) {
		this.inheritance = inheritance;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}
	
	public void setUniqueness(boolean uniqueness) {
		this.uniqueness = uniqueness;
	}
	
	public void setTermCodeAlias(boolean termCodeAlias) {
		this.termCodeAlias = termCodeAlias;
	}
}
