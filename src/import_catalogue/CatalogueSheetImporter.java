package import_catalogue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import excel_file_management.ResultDataSet;

public class CatalogueSheetImporter extends SheetImporter<Catalogue> {

	// the path where the db of the catalogue should be created
	private String dbPath;
	
	// the new catalogue
	private Catalogue catalogue;
	
	/**
	 * Initialize the catalogue sheet importer
	 * @param dbPath the path where the db of the catalogue should be created
	 * @param data the catalogue sheet data
	 */
	public CatalogueSheetImporter( String dbPath, ResultDataSet data ) {
		super(data);
	}

	@Override
	public Catalogue getByResultSet(ResultDataSet rs) {

		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue catalogue = null;

		try {
			catalogue = catDao.getCatalogueFromExcel ( rs );
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return catalogue;
	}

	@Override
	public Collection<Catalogue> getAllByResultSet(ResultDataSet rs) {
		return null;
	}

	@Override
	public void insert(Collection<Catalogue> data) {

		if ( data.isEmpty() )
			return;

		// get the catalogue and save it as global variable
		Iterator<Catalogue> iter = data.iterator();
		catalogue = iter.next();
		
		// if anything was found => create a new catalogue
		// as default we create the catalogue using the official folder 
		// and the catalogue code and version
		// obtained from the excel sheet
		if ( dbPath == null )
			dbPath = catalogue.buildDBFullPath( DatabaseManager.OFFICIAL_CAT_DB_FOLDER + 
					System.getProperty("file.separator") + "CAT_" + catalogue.getCode() + "_DB" );

		// update the db path of the catalogue
		catalogue.setDbFullPath( dbPath );

		// try to connect to the database. If it is not present we have an exception and thus we
		// create the database starting from scrach
		try {

			Connection con = catalogue.getConnection();
			con.close();

			// if no exception was thrown => the database exists and we have to delete it

			// delete the content of the old catalogue database
			System.out.println( "Deleting the database located in " + dbPath );

			//updateProgressBar( 10, Messages.getString("ImportExcelXLSX.DeleteDBLabel") );

			CatalogueDAO catDao = new CatalogueDAO();
			catDao.deleteDBRecords ( catalogue );

			// set the id to the catalogue
			int id = catDao.getCatalogue( catalogue.getCode(), 
					catalogue.getVersion() ).getId();

			catalogue.setId( id );
		}
		catch ( SQLException e ) {

			// otherwise the database does not exist => we create it

			System.out.println ( "Add " + catalogue.getLabel() + " - " + catalogue.getVersion() +
					" data to the CATALOGUE table");

			CatalogueDAO catDao = new CatalogueDAO();

			// set the id to the catalogue
			int id = catDao.insert( catalogue );

			catalogue.setId( id );

			// create the standard database structure for
			// the new catalogue
			catDao.createDBTables( catalogue.getDbFullPath() );
		}
	}
	
	/**
	 * Get the imported catalogue. Note that you
	 * should call {@link #importSheet()} before
	 * this. Otherwise you will get null.
	 * @return
	 */
	public Catalogue getImportedCatalogue() {
		return catalogue;
	}
}
