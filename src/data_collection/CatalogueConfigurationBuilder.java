package data_collection;

public class CatalogueConfigurationBuilder {

	private String dataElementName;
	private String catalogueCode;
	private String hierarchyCode;
	
	public void setDataElementName(String dataElementName) {
		this.dataElementName = dataElementName;
	}
	public void setCatalogueCode(String catalogueCode) {
		this.catalogueCode = catalogueCode;
	}
	public void setHierarchyCode(String hierarchyCode) {
		this.hierarchyCode = hierarchyCode;
	}
	
	public CatalogueConfiguration build() {
		return new CatalogueConfiguration ( dataElementName, 
				catalogueCode, hierarchyCode );
	}
}
