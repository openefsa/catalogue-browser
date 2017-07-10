package import_catalogue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

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
import ui_progress_bar.IProgressBar;
import ui_progress_bar.ProgressSettings;
import ui_search_bar.SearchOptionDAO;
import user_preferences.CataloguePreferenceDAO;
import utilities.GlobalUtil;

/**
 * Import an entire catalogue workbook (xslx) into the database
 * @author avonva
 *
 */
public class CatalogueWorkbookImporter {
	
	// set this to import a local catalogue
	private Catalogue openedCat;
	private CatalogueSheetImporter catImp;
	private TermSheetImporter termImp;
	private IProgressBar progressBar;
	private double maxProgress;
	
	/**
	 * Set this to true if the catalogue you
	 * are importing an .xlsx in a catalogue
	 * (the one opened in the main panel)
	 * in order to override its data
	 * @param openedCat
	 */
	public void setOpenedCatalogue( Catalogue openedCat ) {
		this.openedCat = openedCat;
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
	public void importWorkbook( IProgressBar progressBar, String filename, double maxProgress ) 
			throws IOException, XMLStreamException, OpenXML4JException, 
			SAXException, SQLException {
		
		this.progressBar = progressBar;
		this.maxProgress = maxProgress;
		
		// get the excel data
		final WorkbookReader workbookReader = new WorkbookReader( filename );
		
		// import catalogue
		System.out.println( "Import catalogue sheet" );
		progressBar.setLabel( Messages.getString("Import.Catalogue") );
		catImp = importCatalogueSheet( workbookReader );
		
		Catalogue importedCat = catImp.getImportedCatalogue();
		String catExcelCode = catImp.getExcelCode();
		
		// import hierarchies
		System.out.println( "Import hierarchy sheet" );
		progressBar.setLabel( Messages.getString("Import.Hierarchy") );
		importHierarchySheet ( workbookReader, importedCat, catExcelCode );

		// import attributes
		System.out.println( "Import attribute sheet" );
		progressBar.setLabel( Messages.getString("Import.Attribute") );
		importAttributeSheet ( workbookReader, importedCat );
		
		// import terms
		System.out.println( "Import term sheet" );
		progressBar.setLabel( Messages.getString("Import.Term") );
		termImp = importTermSheet ( workbookReader, importedCat );
		
		// import term attributes and parent
		System.out.println( "Import term attributes and parents sheet" );
		progressBar.setLabel( Messages.getString("Import.TermAttrParent") );
		importTermRelations ( workbookReader, importedCat, termImp.getNewCodes() );

		// import the release note sheet
		System.out.println( "Import release notes sheet" );
		progressBar.setLabel( Messages.getString("Import.ReleaseNotes") );
		importReleaseNotes ( workbookReader, importedCat );
		
		// close the connection with excel reader
		workbookReader.close();
		
		// insert default preferences
		// after having imported the excel, we can insert the default preferences
		System.out.println ( "Creating default preferences" );
		progressBar.setLabel( Messages.getString("Import.Preferences") );
		
		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( importedCat );
		prefDao.insertDefaultPreferences();
		
		// insert the default search options
		SearchOptionDAO optDao = new SearchOptionDAO ( importedCat );
		optDao.insertDefaultSearchOpt();

		// add progress
		double prog = ProgressSettings.getProgress( 
				ProgressSettings.DEFAULT_PREF, maxProgress );
		progressBar.addProgress( prog );

		System.out.println( importedCat + " successfully imported in " + importedCat.getDbPath() );
		
		// clear temporary files
		GlobalUtil.clearTempDir();
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
	private CatalogueSheetImporter importCatalogueSheet ( WorkbookReader workbookReader ) 
			throws InvalidFormatException, IOException, XMLStreamException {
		
		// get the catalogue sheet and check if the catalogues are compatible
		// (the open catalogue and the one we want to import)
		workbookReader.processSheetName( Headers.CAT_SHEET_NAME );

		ResultDataSet sheetData = workbookReader.next();
		
		CatalogueSheetImporter catImp = new CatalogueSheetImporter();
		
		double prog = ProgressSettings.getProgress( ProgressSettings.CAT_SHEET, maxProgress );
		catImp.setProgressBar( progressBar, 
				workbookReader.getRowCount(), prog );
		
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

		// get the attribute sheet
		workbookReader.processSheetName( Headers.ATTR_SHEET_NAME );

		ResultDataSet sheetData = workbookReader.next();
		
		AttributeSheetImporter attrImp = new 
				AttributeSheetImporter( catalogue );
		
		double prog = ProgressSettings.getProgress( ProgressSettings.ATTR_SHEET, maxProgress );
		attrImp.setProgressBar(progressBar, 
				workbookReader.getRowCount(), prog);
		
		// start the import
		attrImp.importData( sheetData );
		
		
		// import the term types related to the attributes
		TermTypeImporter ttImp = new TermTypeImporter( catalogue );
		ttImp.importSheet();
	}
	
	/**
	 * Import the hierarchy sheet. Note that you need to set
	 * the master hierarchy code, since if we are overriding
	 * an already existing catalogue which has its own master
	 * code, we cannot use it, we need the master hierarchy
	 * code contained in the excel sheet to get the master
	 * hierarchy data correctly.
	 * @param workbookReader
	 * @param catalogue
	 * @param catExcelCode the master hierarchy code
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	private void importHierarchySheet ( WorkbookReader workbookReader, 
			Catalogue catalogue, String catExcelCode ) 
					throws InvalidFormatException, IOException, XMLStreamException {

		// get the hierarchy sheet
		workbookReader.processSheetName( Headers.HIER_SHEET_NAME );
		
		ResultDataSet sheetData = workbookReader.next();
		
		HierarchySheetImporter hierImp = new 
				HierarchySheetImporter( catalogue, catExcelCode );
		
		double prog = ProgressSettings.getProgress( ProgressSettings.HIER_SHEET, maxProgress );
		hierImp.setProgressBar(progressBar, 
				workbookReader.getRowCount(), prog);
		
		// start the import
		hierImp.importData( sheetData );
	}
	
	/**
	 * Import a sheet in a smarter way. In particular, two parallel
	 * threads are started. The first thread reads the data from the
	 * workbookReader object. The second thread writes the read data
	 * into the database. Note that the first thread reads the data
	 * also while the second one is writing the data of the previous
	 * batch. This results in improved performances, since delay times
	 * are reduced. Note that this method requires that you set a
	 * batch size for the workbookReader (see {@link WorkbookReader#setBatchSize(int)}, 
	 * otherwise the data would not be separable, since they are
	 * all contained in a single big batch.
	 * @param workbookReader the reader with a sheet already loaded
	 * @param importer the sheet importer
	 * @throws XMLStreamException
	 * @throws CloneNotSupportedException
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 */
	private void importQuickly ( WorkbookReader workbookReader, String sheetName,
			int batchSize, final SheetImporter<?> importer ) throws XMLStreamException, 
			InvalidFormatException, IOException {
		
		QuickImporter quickImp = new QuickImporter( workbookReader, batchSize ) {
			
			@Override
			public void importData(ResultDataSet rs) {
				importer.importData( rs );
			}
		};
		
		quickImp.importSheet();
	}

	/**
	 * Import the entire term sheet into the db. Note that you have
	 * to import the hierarchies and the attributes before importing
	 * this sheet. See {@link #importAttributeSheet(WorkbookReader, Catalogue)}
	 * and {@link #importHierarchySheet(WorkbookReader, Catalogue, String)}.
	 * @param workbookReader
	 * @param catalogue
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws SQLException 
	 */
	private TermSheetImporter importTermSheet ( WorkbookReader workbookReader, Catalogue catalogue ) 
			throws InvalidFormatException, IOException, XMLStreamException, SQLException {

		final int batchSize = 100;

		// get the hierarchy sheet
		workbookReader.processSheetName( Headers.TERM_SHEET_NAME );
		
		TermSheetImporter termImp = new TermSheetImporter( catalogue );
		
		double prog = ProgressSettings.getProgress( ProgressSettings.TERM_SHEET, maxProgress );
		termImp.setProgressBar(progressBar, 
				workbookReader.getRowCount(), prog);
		
		// import terms in a quick way
		importQuickly ( workbookReader, Headers.TERM_SHEET_NAME, batchSize, termImp );
		
		return termImp;
	}
	
	/**
	 * Import term attributes and term parents
	 * @param workbookReader
	 * @param catalogue
	 * @param newCodes
	 * @throws SQLException
	 * @throws InvalidFormatException
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	private void importTermRelations ( WorkbookReader workbookReader,
			Catalogue catalogue, HashMap<String,String> newCodes ) 
					throws SQLException, InvalidFormatException, XMLStreamException, IOException {

		final int batchSize = 100;
		
		// note that we need to have imported the terms to import
		// term attributes and parent terms!
		// import term attributes and parent terms in a parallel way
		// since they are independent processes
		QuickParentAttributesImporter tapImporter = new QuickParentAttributesImporter( catalogue, 
				workbookReader, Headers.TERM_SHEET_NAME, batchSize );
		
		double progTa = ProgressSettings.getProgress( 
				ProgressSettings.TERM_ATTR_SHEET, maxProgress );
		
		double progParent = ProgressSettings.getProgress( 
				ProgressSettings.PARENT_SHEET, maxProgress );
		
		tapImporter.setAttributeProgressBar(progressBar, progTa);
		tapImporter.setParentProgressBar(progressBar, progParent);
		
		tapImporter.manageNewTerms( newCodes );
		
		tapImporter.importSheet();
	}
	
	/**
	 * Import the release notes
	 * @param workbookReader
	 * @param catalogue
	 */
	private void importReleaseNotes ( WorkbookReader workbookReader, Catalogue catalogue ) {

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
			
			double prog = ProgressSettings.getProgress( 
					ProgressSettings.NOTES_SHEET, maxProgress );

			notesImp.setProgressBar(progressBar, 
					workbookReader.getRowCount(), prog);
			
			notesImp.importData( sheetData );
			
			workbookReader.getSheetParser().close();
			sheetData.close();
			
		} catch ( Exception e ) {
			System.err.println( "Release notes not found for " + catalogue );
		}
	}
}
