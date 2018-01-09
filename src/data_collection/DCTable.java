package data_collection;

/**
 * A table related to a data collection
 * @author avonva
 *
 */
public class DCTable extends DcfDCTable {

	public DCTable() {}
	
	public DCTable(String name) {
		setName(name);
	}
	
	/**
	 * Import the table into the db
	 */
	public void makeImport( DataCollection dc ) {

		DCTableDAO tableDao = new DCTableDAO();
		
		// insert the table if not present
		if ( !tableDao.contains( this ) ) {
			int id = tableDao.insert( this );
			this.setId(id);
		}
		else {  // else refresh the table id since we need it to insert DCTableConfig records
			DCTable table = tableDao.getByName( getName() );
			if ( table != null ) {
				int id = table.getId();
				this.setId(id);
			}
		}
		
		// insert all the table configs
		for ( IDcfCatalogueConfig config : getConfigs() ) {
			
			CatalogueConfiguration catConfig = (CatalogueConfiguration) config;
			
			catConfig.makeImport( dc, this );
		}
	}
}
