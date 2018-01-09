package data_collection;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Hierarchy;
import dcf_manager.Dcf;

/**
 * Relation object among {@link DataCollection}, {@link DCTable}
 * and {@link DcfCatalogueConfig} in the db.
 * @author avonva
 *
 */
public class DCTableConfig {

	private static final String MASTER_HIERARCHY_CODE = "master_Hierarchy";
	
	private DataCollection dc;
	private DCTable table;
	private DcfCatalogueConfig config;
	
	public DCTableConfig( DataCollection dc, DCTable table, DcfCatalogueConfig config ) {
		this.dc = dc;
		this.table = table;
		this.config = config;
	}
	
	/**
	 * Get the data collection related to this configuration
	 * @return
	 */
	public DataCollection getDc() {
		return dc;
	}
	
	/**
	 * Get the fact table related to this configuration
	 * @return
	 */
	public DCTable getTable() {
		return table;
	}
	
	/**
	 * Get the configuration object
	 * @return
	 */
	public DcfCatalogueConfig getConfig() {
		return config;
	}
	
	/**
	 * Get the catalogue linked to this configuration
	 * @return
	 */
	public Catalogue getCatalogue() {

		String catCode = config.getCatalogueCode();
		
		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue catalogue = catDao.getLastVersionByCode( catCode, Dcf.dcfType );
		
		return catalogue;
	}
	
	/**
	 * Get the hierarchy linked to this configuration
	 * @return
	 */
	public Hierarchy getHierarchy() {

		// change the current hierarchy with the selected one
		String hierCode = config.getHierarchyCode();
		
		Catalogue catalogue = getCatalogue();
		
		if ( catalogue == null )
			return null;
		
		HierarchyDAO hierDao = new HierarchyDAO( catalogue );
		
		// special case, if master hierarchy get
		// catalogue code as master
		if ( hierCode.equals( MASTER_HIERARCHY_CODE ) )
			hierCode = catalogue.getCode();
		
		return hierDao.getByCode( hierCode );
	}
	
	@Override
	public String toString() {
		return "DCTableConfig: " + dc + ";" + table + ";" + config;
	}
}
