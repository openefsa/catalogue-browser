package import_catalogue;

import catalogue.Catalogue;
import catalogue_browser_dao.ReleaseNotesDAO;
import messages.Messages;
import naming_convention.Headers;
import open_xml_reader.ResultDataSet;
import open_xml_reader.XLSXFormat;
import ui_progress_bar.FormProgressBar;
import ui_search_bar.SearchOptionDAO;
import user_preferences.CataloguePreferenceDAO;

/**
 * Import an entire catalogue workbook (xslx) into the database
 * @author avonva
 *
 */
public class CatalogueWorkbookImporter {
	
	// set this to import a local catalogue
	private Catalogue localCat;
	
	private FormProgressBar progressBar;
	
	/**
	 * Set this to true if the catalogue you
	 * are importing is a local catalogue 
	 * see {@link Catalogue#isLocal()}
	 * @param local
	 */
	public void setLocal( Catalogue localCat ) {
		this.localCat = localCat;
	}
	
	
	/**
	 * Set a progress bar for the import process.
	 * @param progressBar, the progress bar which is displayed in the main UI
	 */
	public void setProgressBar ( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Update the progress bar progress and label
	 * @param progress
	 * @param label
	 */
	private void updateProgressBar ( int progress, String label ) {
		
		if ( progressBar == null )
			return;
		
		progressBar.addProgress( progress );
		progressBar.setLabel( label );
	}
	
	/**
	 * Import the workbook
	 * @throws Exception
	 */
	public void importWorkbook( String dbPath, String filename ) throws Exception {

		updateProgressBar( 1, Messages.getString("ImportExcelXLSX.ReadingData") );
		
		// get the excel data
		XLSXFormat rawData = new XLSXFormat( filename );
		
		System.out.println( "Importing catalogue sheet" );
		updateProgressBar( 1, Messages.getString("ImportExcelXLSX.ImportCatalogue") );
		
		// get the catalogue sheet and check if the catalogues are compatible
		// (the open catalogue and the one we want to import)
		ResultDataSet sheetData = rawData.processSheetName( Headers.CAT_SHEET_NAME );

		CatalogueSheetImporter catImp = 
				new CatalogueSheetImporter( dbPath, sheetData );
		
		if ( localCat != null )
			catImp.setLocalCatalogue ( localCat );
		
		catImp.importSheet();
		
		// get the imported catalogue (if it was local it is the same)
		Catalogue catalogue = catImp.getImportedCatalogue();
		String catExcelCode = catImp.getExcelCode();
		
		System.out.println( "Importing hierarchy sheet" );
		updateProgressBar( 10, Messages.getString("ImportExcelXLSX.ImportHierarchyLabel") );
		
		// get the hierarchy sheet
		sheetData = rawData.processSheetName( Headers.HIER_SHEET_NAME );
		HierarchySheetImporter hierImp = new 
				HierarchySheetImporter( catalogue, sheetData );
		hierImp.setMasterCode( catExcelCode );
		
		// start the import
		hierImp.importSheet();
		
		
		System.out.println( "Importing attribute sheet" );
		updateProgressBar( 10, Messages.getString("ImportExcelXLSX.ImportAttributeLabel") );
		
		// get the attribute sheet
		sheetData = rawData.processSheetName( Headers.ATTR_SHEET_NAME );

		AttributeSheetImporter attrImp = new 
				AttributeSheetImporter( catalogue, sheetData );
		
		// start the import
		attrImp.importSheet();
		
		
		System.out.println( "Importing term sheet" );
		updateProgressBar( 20, Messages.getString("ImportExcelXLSX.ImportTermLabel") );
		
		// get the term sheet
		sheetData = rawData.processSheetName( Headers.TERM_SHEET_NAME );
		TermSheetImporter termImp = new 
				TermSheetImporter( catalogue, sheetData );
		
		termImp.importSheet( 500 );
		
		
		// restart term data scanning from first
		sheetData.initScan();
		
		System.out.println( "Importing term attributes" );
		updateProgressBar( 15, Messages.getString("ImportExcelXLSX.ImportTermAttrLabel") );
		
		// import term attributes
		TermAttributeImporter taImp = new 
				TermAttributeImporter( catalogue, sheetData );
		taImp.importSheet( 2000 );
		
		// import the term types related to the attributes
		TermTypeImporter ttImp = new TermTypeImporter( catalogue );
		ttImp.importSheet();
		
		
		// restart term data scanning from first
		sheetData.initScan();
		
		System.out.println( "Importing parent terms" );
		updateProgressBar( 15, 
				Messages.getString("ImportExcelXLSX.ImportTermParents") );
		
		// import parent terms
		ParentImporter parentImp = new 
				ParentImporter( catalogue, sheetData );
		parentImp.importSheet( 2000 );
		
		
		System.out.println( "Importing release notes sheet" );
		
		updateProgressBar( 15, 
				Messages.getString("ImportExcelXLSX.ImportReleaseNotes") );
		
		// add the catalogue information
		ReleaseNotesDAO notesDao = new ReleaseNotesDAO( catalogue );
		if ( catalogue.getReleaseNotes() != null )
			notesDao.insert( catalogue.getReleaseNotes() );
		
		// import the release notes operations
		try {

			sheetData = rawData.processSheetName( Headers.NOTES_SHEET_NAME );
			NotesSheetImporter notesImp = new 
					NotesSheetImporter( catalogue, sheetData );
			notesImp.importSheet();
			
		} catch ( Exception e ) {
			System.err.println( "Release notes not found for " + catalogue );
		}

		// close the connection
		rawData.close();

		
		// after having imported the excel, we can insert the default preferences
		System.out.println ( "Insert default preferences values into the database" );
		
		// insert default preferences
		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( catalogue );
		prefDao.insertDefaultPreferences();
		
		// insert the default search options
		SearchOptionDAO optDao = new SearchOptionDAO ( catalogue );
		optDao.insertDefaultSearchOpt();
		
		// end process
		if ( progressBar != null )
			progressBar.close();
		
		System.out.println( catalogue + " successfully imported in " + catalogue.getDbFullPath() );
	}
}
