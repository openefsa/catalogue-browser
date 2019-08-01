package data_collection;

/**
 * A configuration which contains one of the data collection
 * variables. In particular it specifies the catalogue and 
 * the hierarchy which contains this variable.
 * @author avonva
 *
 */
public class CatalogueConfiguration extends DcfCatalogueConfig {

	public CatalogueConfiguration() {
		super();
	}
	
	public CatalogueConfiguration(String dataElementName, 
			String catalogueCode, String hierarchyCode) {
		super(dataElementName, catalogueCode, hierarchyCode);
	}
	
	/**
	 * Import the configuration into the db
	 */
	public void makeImport( DataCollection dc, DCTable table ) {

		CatalogueConfigDAO configDao = new CatalogueConfigDAO();
		int id = configDao.insert( this );
		
		this.setId(id);

		// insert also the relationship among dc, table and config
		DCTableConfig tableConfig = new DCTableConfig( dc, table, this );
		
		DCTableConfigDAO tableConfigDao = new DCTableConfigDAO();
		tableConfigDao.insert( tableConfig );
	}
}
