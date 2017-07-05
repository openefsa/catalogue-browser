package data_collection;

/**
 * A configuration which contains one of the data collection
 * variables. In particular it specifies the catalogue and 
 * the hierarchy which contains this variable.
 * @author avonva
 *
 */
public class CatalogueConfiguration {

	private int id = -1;
	private String dataElementName;
	private String catalogueCode;
	private String hierarchyCode;
	
	public CatalogueConfiguration( String dataElementName, 
			String catalogueCode, String hierarchyCode ) {
		this.dataElementName = dataElementName;
		this.catalogueCode = catalogueCode;
		this.hierarchyCode = hierarchyCode;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}
	/**
	 * Get the name of the data collection variable
	 * @return
	 */
	public String getDataElementName() {
		return dataElementName;
	}
	
	/**
	 * Get the code of the catalogue which contains
	 * the data collection variable
	 * @return
	 */
	public String getCatalogueCode() {
		return catalogueCode;
	}
	
	/**
	 * Get the code of the hierarchy which contains
	 * the data collection variable
	 * @return
	 */
	public String getHierarchyCode() {
		return hierarchyCode;
	}
	
	/**
	 * Import the configuration into the db
	 */
	public void makeImport( DataCollection dc, DCTable table ) {

		CatalogueConfigDAO configDao = new CatalogueConfigDAO();
		this.id = configDao.insert( this );

		// insert also the relationship among dc, table and config
		DCTableConfig tableConfig = new DCTableConfig( dc, table, this );
		
		DCTableConfigDAO tableConfigDao = new DCTableConfigDAO();
		tableConfigDao.insert( tableConfig );
	}
	
	@Override
	public String toString() {
		return "CAT CONFIG: id= " + (id == -1 ? "not defined yet" : id ) 
				+ "dataElemName=" + dataElementName
				+ ";catCode=" + catalogueCode
				+ ";hierCode=" + hierarchyCode;
	}
}
