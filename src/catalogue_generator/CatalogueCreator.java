package catalogue_generator;

import java.io.IOException;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Hierarchy;
import dcf_manager.Dcf.DcfType;
import user_preferences.CataloguePreferenceDAO;

public class CatalogueCreator {

	/**
	 * Create a new local catalogue in the user pc. This catalogue is not official!
	 * Return the new catalogue object
	 * @throws DuplicatedCatalogueException 
	 * @throws IOException 
	 */
	public static Catalogue newLocalCatalogue ( String catalogueCode ) 
			throws DuplicatedCatalogueException, IOException {

		// replace spaces with underscores
		catalogueCode = catalogueCode.replaceAll(" ", "_" );

		// create a default catalogue object using the catalogue code as identifier
		Catalogue newCatalogue = Catalogue.getDefaultCatalogue( catalogueCode );

		CatalogueDAO catDao = new CatalogueDAO();
		
		Catalogue alreadyPresentCat = catDao.getCatalogue( newCatalogue.getCode(), 
				newCatalogue.getVersion(), DcfType.LOCAL );

		// if a local catalogue with the same code was already added => error!
		if ( alreadyPresentCat != null )
			throw new DuplicatedCatalogueException();

		// create database directory
		newCatalogue.createDbDir();

		// add the catalogue meta data to the database
		catDao.insert( newCatalogue );
		
		// create the standard database structure for
		// the new catalogue
		DatabaseManager.createCatalogueDatabase(newCatalogue.getDbPath());

		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( newCatalogue );
		
		// insert into the db the default user preferences values 
		// for the currently open catalogue
		prefDao.insertDefaultPreferences();
		
		// create the master hierarchy for a local catalogue
		Hierarchy master = newCatalogue.createMasterHierarchy();

		HierarchyDAO hierDao = new HierarchyDAO( newCatalogue );
		
		// insert it into the db
		hierDao.insert( master );
		
		return newCatalogue;
	}
}
