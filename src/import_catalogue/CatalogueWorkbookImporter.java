package import_catalogue;

import java.io.IOException;
import java.sql.SQLException;

import javax.xml.stream.XMLStreamException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

import catalogue.Catalogue;
import catalogue_browser_dao.ReleaseNotesDAO;
import messages.Messages;
import naming_convention.Headers;
import open_xml_reader.ResultDataSet;
import open_xml_reader.WorkbookReader;
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
	private Catalogue openedCat;
	
	private FormProgressBar progressBar;
	
	/**
	 * Set this to true if the catalogue you
	 * are importing is a local catalogue 
	 * see {@link Catalogue#isLocal()}
	 * @param local
	 */
	public void setOpenedCatalogue( Catalogue openedCat ) {
		this.openedCat = openedCat;
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
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws OpenXML4JException 
	 * @throws SQLException 
	 * @throws Exception
	 */
	public void importWorkbook( String dbPath, String filename ) 
			throws IOException, XMLStreamException, OpenXML4JException, 
			SAXException, SQLException {

		updateProgressBar( 1, Messages.getString("ImportExcelXLSX.ReadingData") );
		
		// get the excel data
		WorkbookReader workbookReader = new WorkbookReader( filename );
		
		CatalogueSheetImporter catImp = importCatalogueSheet( workbookReader, dbPath );
		
		// get the imported catalogue (if it was local it is the same)
		Catalogue catalogue = catImp.getImportedCatalogue();
		String catExcelCode = catImp.getExcelCode();
		
		// import hierarchies
		importHierarchySheet ( workbookReader, catalogue, catExcelCode );
		
		// import attributes
		importAttributeSheet ( workbookReader, catalogue );
	
		// import terms
		importTermSheet ( workbookReader, catalogue );
		
		// import the release note sheet
		importReleaseNotes ( workbookReader, catalogue );

		// close the connection
		workbookReader.close();

		// after having imported the excel, we can insert the default preferences
		System.out.println ( "Insert default preferences values for " + catalogue  + " in " +
				catalogue.getDbPath() );
		
		// insert default preferences
		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( catalogue );
		prefDao.insertDefaultPreferences();
		
		// insert the default search options
		SearchOptionDAO optDao = new SearchOptionDAO ( catalogue );
		optDao.insertDefaultSearchOpt();
		
		// end process
		if ( progressBar != null )
			progressBar.close();
		
		System.out.println( catalogue + " successfully imported in " + catalogue.getDbPath() );
	}
	
	/**
	 * Import the catalogue sheet
	 * @param workbookReader
	 * @param dbPath
	 * @return
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	private CatalogueSheetImporter importCatalogueSheet ( WorkbookReader workbookReader, String dbPath ) 
			throws InvalidFormatException, IOException, XMLStreamException {
		
		System.out.println( "Importing catalogue sheet" );
		updateProgressBar( 1, Messages.getString("ImportExcelXLSX.ImportCatalogue") );
		
		// get the catalogue sheet and check if the catalogues are compatible
		// (the open catalogue and the one we want to import)
		workbookReader.processSheetName( Headers.CAT_SHEET_NAME );

		ResultDataSet sheetData = workbookReader.next();
		
		CatalogueSheetImporter catImp = 
				new CatalogueSheetImporter( dbPath );
		
		if ( openedCat != null )
			catImp.setOpenedCatalogue ( openedCat );
		
		catImp.importData( sheetData );
		
		return catImp;
	}
	
	/**
	 * Import the attribute sheet
	 * @param workbookReader
	 * @param catalogue
	 * @throws XMLStreamException
	 * @throws InvalidFormatException
	 * @throws IOException
	 */
	private void importAttributeSheet( WorkbookReader workbookReader, Catalogue catalogue ) 
			throws XMLStreamException, InvalidFormatException, IOException {
		
		System.out.println( "Importing attribute sheet" );
		updateProgressBar( 2, Messages.getString("ImportExcelXLSX.ImportAttributeLabel") );
		
		// get the attribute sheet
		workbookReader.processSheetName( Headers.ATTR_SHEET_NAME );

		ResultDataSet sheetData = workbookReader.next();
		
		AttributeSheetImporter attrImp = new 
				AttributeSheetImporter( catalogue );
		// start the import
		attrImp.importData( sheetData );
		
		
		// import the term types related to the attributes
		TermTypeImporter ttImp = new TermTypeImporter( catalogue );
		ttImp.importSheet();
	}
	
	/**
	 * Import the hierarchy sheet
	 * @param workbookReader
	 * @param catalogue
	 * @param catExcelCode
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	private void importHierarchySheet ( WorkbookReader workbookReader, 
			Catalogue catalogue, String catExcelCode ) 
					throws InvalidFormatException, IOException, XMLStreamException {
		
		System.out.println( "Importing hierarchy sheet" );
		updateProgressBar( 2, Messages.getString("ImportExcelXLSX.ImportHierarchyLabel") );
		
		// get the hierarchy sheet
		workbookReader.processSheetName( Headers.HIER_SHEET_NAME );
		
		ResultDataSet sheetData = workbookReader.next();
		
		HierarchySheetImporter hierImp = new 
				HierarchySheetImporter( catalogue );
		hierImp.setMasterCode( catExcelCode );
		
		// start the import
		hierImp.importData( sheetData );
	}
	
	/**
	 * Import the entire term sheet into the db
	 * @param workbookReader
	 * @param catalogue
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws SQLException 
	 */
	private void importTermSheet ( WorkbookReader workbookReader, Catalogue catalogue ) 
			throws InvalidFormatException, IOException, XMLStreamException, SQLException {

		final int batchSize = 2000;
		
		System.out.println( "Importing term sheet" );
		updateProgressBar( 15, Messages.getString("ImportExcelXLSX.ImportTermLabel") );
		
		// get the term sheet
		workbookReader.processSheetName( Headers.TERM_SHEET_NAME );
		workbookReader.setBatchSize( batchSize );
		
		TermSheetImporter termImp = new 
				TermSheetImporter( catalogue );
		
		// read the first batch
		while ( workbookReader.hasNext() ) {
			ResultDataSet data = workbookReader.next();
			termImp.importData( data );
		}

		System.out.println( "Importing term attributes" );
		updateProgressBar( 25, Messages.getString("ImportExcelXLSX.ImportTermAttrLabel") );
		
		// import term attributes
		TermAttributeImporter taImp = new 
				TermAttributeImporter( catalogue );
		taImp.manageNewTerms( termImp.getNewCodes() );
		
		// get the term sheet
		workbookReader.processSheetName( Headers.TERM_SHEET_NAME );
		workbookReader.setBatchSize( batchSize );

		// read the first batch
		while ( workbookReader.hasNext() ) {
			ResultDataSet data = workbookReader.next();
			taImp.importData( data );
		}
		
		System.out.println( "Importing parent terms" );
		updateProgressBar( 25, Messages.getString("ImportExcelXLSX.ImportTermParents") );
		
		// import parent terms
		ParentImporter parentImp = new ParentImporter( catalogue );
		
		// manage also new terms (i.e. the append function )
		parentImp.manageNewTerms( termImp.getNewCodes() );
		
		// get the term sheet
		workbookReader.processSheetName( Headers.TERM_SHEET_NAME );
		workbookReader.setBatchSize( batchSize );
		
		// read the first batch
		while ( workbookReader.hasNext() ) {
			ResultDataSet data = workbookReader.next();
			parentImp.importData( data );
		}
	}
	
	/**
	 * Import the release notes
	 * @param workbookReader
	 * @param catalogue
	 */
	private void importReleaseNotes ( WorkbookReader workbookReader, Catalogue catalogue ) {
		
		System.out.println( "Importing release notes sheet" );
		updateProgressBar( 5, Messages.getString("ImportExcelXLSX.ImportReleaseNotes") );
		
		// add the catalogue information
		ReleaseNotesDAO notesDao = new ReleaseNotesDAO( catalogue );
		if ( catalogue.getReleaseNotes() != null )
			notesDao.insert( catalogue.getReleaseNotes() );
		
		// import the release notes operations
		try {

			workbookReader.processSheetName( Headers.NOTES_SHEET_NAME );
			
			ResultDataSet sheetData = workbookReader.next();
			
			NotesSheetImporter notesImp = new 
					NotesSheetImporter( catalogue );
			notesImp.importData( sheetData );
			
		} catch ( Exception e ) {
			System.err.println( "Release notes not found for " + catalogue );
		}
	}
}
