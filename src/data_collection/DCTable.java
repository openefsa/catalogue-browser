package data_collection;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A table related to a data collection
 * @author avonva
 *
 */
public class DCTable {

	private int id = -1;
	private String name;
	private Collection<CatalogueConfiguration> configs;
	
	/**
	 * Create a data collection table
	 * @param name name of the table
	 */
	public DCTable( String name ) {
		this.name = name;
		this.configs = new ArrayList<>();
	}
	
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public Collection<CatalogueConfiguration> getConfigs() {
		return configs;
	}
	/**
	 * Set the data collection configurations
	 * @param config
	 */
	public void setConfigs(Collection<CatalogueConfiguration> configs) {
		this.configs = configs;
	}
	
	public void addConfig ( CatalogueConfiguration config ) {
		this.configs.add( config );
	}
	
	/**
	 * Import the table into the db
	 */
	public void makeImport( DataCollection dc ) {

		DCTableDAO tableDao = new DCTableDAO();
		
		// insert the table if not present
		if ( !tableDao.contains( this ) )
			this.id = tableDao.insert( this );
		else {  // else refresh the table id since we need it to insert DCTableConfig records
			DCTable table = tableDao.getByName( name );
			if ( table != null )
				this.id = table.getId();
		}
		
		// insert all the table configs
		for ( CatalogueConfiguration config : configs ) {
			config.makeImport( dc, this );
		}
	}
	
	@Override
	public String toString() {
		return "DC TABLE: id=" + (id == -1 ? "not defined yet" : id )
				+ ";name=" + name
				+ ";configs=" + configs;
	}
}
