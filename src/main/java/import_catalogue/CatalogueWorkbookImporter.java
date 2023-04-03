package import_catalogue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

import catalogue.Catalogue;
import catalogue.ReleaseNotesOperation;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.CatalogueRelationDAO;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_browser_dao.ICatalogueDAO;
import catalogue_browser_dao.ParentTermDAO;
import catalogue_browser_dao.ReleaseNotesOperationDAO;
import catalogue_browser_dao.TermAttributeDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Applicability;
import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import i18n_messages.CBMessages;
import naming_convention.Headers;
import open_xml_reader.ResultDataSet;
import open_xml_reader.WorkbookReader;
import progress_bar.IProgressBar;
import ui_search_bar.SearchOptionDAO;
import user_preferences.CataloguePreferenceDAO;
import utilities.GlobalUtil;

/**
 * Import an entire catalogue workbook (xslx) into the database
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class CatalogueWorkbookImporter {

	private static final Logger LOGGER = LogManager.getLogger(CatalogueWorkbookImporter.class);

	private ICatalogueDAO catDao;
	private CatalogueEntityDAO<Attribute> attrDao;
	private CatalogueEntityDAO<Hierarchy> hierDao;
	private CatalogueEntityDAO<Term> termDao;
	private CatalogueRelationDAO<TermAttribute, Term, Attribute> taDao;
	private CatalogueRelationDAO<Applicability, Term, Hierarchy> parentDao;
	private CatalogueEntityDAO<ReleaseNotesOperation> notesDao;

	// set this to import a local catalogue
	private Catalogue openedCat;
	private IProgressBar progressBar;
	private double maxProgress;

	public CatalogueWorkbookImporter() {
		this.catDao = new CatalogueDAO();
		this.attrDao = null;
		this.hierDao = null;
		this.termDao = null;
		this.taDao = null;
		this.parentDao = null;
		this.notesDao = null;
	}

	public CatalogueWorkbookImporter(ICatalogueDAO catDao, CatalogueEntityDAO<Attribute> attrDao,
			CatalogueEntityDAO<Hierarchy> hierDao, CatalogueEntityDAO<Term> termDao,
			CatalogueRelationDAO<TermAttribute, Term, Attribute> taDao,
			CatalogueRelationDAO<Applicability, Term, Hierarchy> parentDao,
			CatalogueEntityDAO<ReleaseNotesOperation> notesDao) {
		this.catDao = catDao;
		this.attrDao = attrDao;
		this.hierDao = hierDao;
		this.termDao = termDao;
		this.taDao = taDao;
		this.parentDao = parentDao;
		this.notesDao = notesDao;
	}

	/**
	 * Set this to true if the catalogue you are importing an .xlsx in a catalogue
	 * (the one opened in the main panel) in order to override its data
	 * 
	 * @param openedCat
	 */
	public void setOpenedCatalogue(Catalogue openedCat) {
		this.openedCat = openedCat;
	}

	private void initDaos(Catalogue catalogue) {

		if (this.attrDao == null) {
			this.attrDao = new AttributeDAO(catalogue);
			this.hierDao = new HierarchyDAO(catalogue);
			this.termDao = new TermDAO(catalogue);
			this.notesDao = new ReleaseNotesOperationDAO(catalogue);
			this.taDao = new TermAttributeDAO(catalogue);
			this.parentDao = new ParentTermDAO(catalogue);
		}

		this.hierDao.setCatalogue(catalogue);
		this.attrDao.setCatalogue(catalogue);
		this.termDao.setCatalogue(catalogue);
		this.taDao.setCatalogue(catalogue);
		this.parentDao.setCatalogue(catalogue);
		this.notesDao.setCatalogue(catalogue);
	}

	/**
	 * Import the workbook
	 * 
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws SAXException
	 * @throws OpenXML4JException
	 * @throws SQLException
	 * @throws ImportException
	 * @throws Exception
	 */
	public void importWorkbook(IProgressBar pb, String filename, double maxProgress)
			throws IOException, XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {

		this.progressBar = pb;
		this.maxProgress = maxProgress;

		// get the excel data
		try (WorkbookReader workbookReader = new WorkbookReader(filename)) {

			// import catalogue
			LOGGER.info("Import catalogue sheet");

			if (progressBar != null)
				progressBar.setLabel(CBMessages.getString("Import.Catalogue"));

			CatalogueSheetImporter catImp = importCatalogueSheet(workbookReader);

			Catalogue importedCat = catImp.getImportedCatalogue();
			String catExcelCode = catImp.getExcelCode();

			// prepare daos to import data
			this.initDaos(importedCat);

			// import hierarchies
			LOGGER.info("Import hierarchy sheet");

			if (progressBar != null)
				progressBar.setLabel(CBMessages.getString("Import.Hierarchy"));

			importHierarchySheet(workbookReader, importedCat, catExcelCode);

			// import attributes
			LOGGER.info("Import attribute sheet");
			if (progressBar != null)
				progressBar.setLabel(CBMessages.getString("Import.Attribute"));
			importAttributeSheet(workbookReader, importedCat);

			// import terms
			LOGGER.info("Import term sheet");
			if (progressBar != null)
				progressBar.setLabel(CBMessages.getString("Import.Term"));
			TermSheetImporter termImp = importTermSheet(workbookReader, importedCat);

			// import term attributes and parent
			LOGGER.info("Import term attributes and parents sheet");
			if (progressBar != null)
				progressBar.setLabel(CBMessages.getString("Import.TermAttrParent"));
			importTermRelations(workbookReader, importedCat, termImp.getNewCodes());

			// import the release note sheet
			LOGGER.info("Import release notes sheet");
			if (progressBar != null)
				progressBar.setLabel(CBMessages.getString("Import.ReleaseNotes"));
			importReleaseNotes(workbookReader, importedCat);

			// close the connection with excel reader
			workbookReader.close();

			// insert default preferences
			// after having imported the excel, we can insert the default preferences
			LOGGER.info("Creating default preferences");
			if (progressBar != null)
				progressBar.setLabel(CBMessages.getString("Import.Preferences"));

			CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(importedCat);
			prefDao.insertDefaultPreferences();

			// insert the default search options
			SearchOptionDAO optDao = new SearchOptionDAO(importedCat);
			optDao.insertDefaultSearchOpt();

			if (progressBar != null) {
				// add progress
				double prog = ProgressSettings.getProgress(ProgressSettings.DEFAULT_PREF, maxProgress);
				progressBar.addProgress(prog);
			}

			LOGGER.info(importedCat + " successfully imported in " + importedCat.getDbPath());

			// clear temporary files
			GlobalUtil.clearTempDir();
		}
	}

	/**
	 * Import the catalogue sheet
	 * 
	 * @author shahaal
	 * @author avonva
	 * @param workbookReader
	 * @param dbPath
	 * @return
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws ImportException
	 */
	private CatalogueSheetImporter importCatalogueSheet(WorkbookReader workbookReader)
			throws InvalidFormatException, IOException, XMLStreamException, ImportException {
		
		LOGGER.info("Importing catalogue sheet");

		// get the catalogue sheet and check if the catalogues are compatible
		// (the open catalogue and the one we want to import)
		workbookReader.processSheetName(Headers.CAT_SHEET_NAME);

		ResultDataSet sheetData = workbookReader.next();

		CatalogueSheetImporter catImp = new CatalogueSheetImporter(catDao);

		if (progressBar != null) {
			double prog = ProgressSettings.getProgress(ProgressSettings.CAT_SHEET, maxProgress);
			catImp.setProgressBar(progressBar, workbookReader.getRowCount(), prog);
		}

		if (openedCat != null)
			catImp.setOpenedCatalogue(openedCat);

		catImp.importData(sheetData);

		// refresh catalogue in ram
		openedCat = catImp.getImportedCatalogue();

		// solve memory leak
		sheetData.close();

		return catImp;
	}

	/**
	 * Import the attribute sheet
	 * 
	 * @author shahaal
	 * @author avonva
	 * @param workbookReader
	 * @param catalogue
	 * @throws XMLStreamException
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws ImportException
	 */
	private void importAttributeSheet(WorkbookReader workbookReader, Catalogue catalogue)
			throws XMLStreamException, InvalidFormatException, IOException, ImportException {
		
		LOGGER.info("Importing attribute sheet");

		// get the attribute sheet
		workbookReader.processSheetName(Headers.ATTR_SHEET_NAME);

		ResultDataSet sheetData = workbookReader.next();

		AttributeSheetImporter attrImp = new AttributeSheetImporter(attrDao, catalogue);

		if (progressBar != null) {
			double prog = ProgressSettings.getProgress(ProgressSettings.ATTR_SHEET, maxProgress);
			attrImp.setProgressBar(progressBar, workbookReader.getRowCount(), prog);
		}

		// start the import
		attrImp.importData(sheetData);

		// import the term types related to the attributes
		TermTypeImporter ttImp = new TermTypeImporter(catalogue);
		ttImp.importSheet();

		// solve memory leak
		sheetData.close();
	}

	/**
	 * Import the hierarchy sheet. Note that you need to set the master hierarchy
	 * code, since if we are overriding an already existing catalogue which has its
	 * own master code, we cannot use it, we need the master hierarchy code
	 * contained in the excel sheet to get the master hierarchy data correctly.
	 * 
	 * @author shahaal
	 * @author avonva
	 * @param workbookReader
	 * @param catalogue
	 * @param catExcelCode   the master hierarchy code
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws ImportException
	 */
	private void importHierarchySheet(WorkbookReader workbookReader, Catalogue catalogue, String catExcelCode)
			throws InvalidFormatException, IOException, XMLStreamException, ImportException {
		
		LOGGER.info("Importing hierarchy sheet");

		// get the hierarchy sheet
		workbookReader.processSheetName(Headers.HIER_SHEET_NAME);

		ResultDataSet sheetData = workbookReader.next();

		HierarchySheetImporter hierImp = new HierarchySheetImporter(hierDao, catalogue, catExcelCode);

		if (progressBar != null) {
			double prog = ProgressSettings.getProgress(ProgressSettings.HIER_SHEET, maxProgress);
			hierImp.setProgressBar(progressBar, workbookReader.getRowCount(), prog);
		}

		// start the import
		hierImp.importData(sheetData);

		// solve memory leak
		sheetData.close();
	}

	/**
	 * Import a sheet in a smarter way. In particular, two parallel threads are
	 * started. The first thread reads the data from the workbookReader object. The
	 * second thread writes the read data into the database. Note that the first
	 * thread reads the data also while the second one is writing the data of the
	 * previous batch. This results in improved performances, since delay times are
	 * reduced. Note that this method requires that you set a batch size for the
	 * workbookReader (see {@link WorkbookReader#setBatchSize(int)}, otherwise the
	 * data would not be separable, since they are all contained in a single big
	 * batch.
	 * 
	 * @param workbookReader the reader with a sheet already loaded
	 * @param importer       the sheet importer
	 * @throws XMLStreamException
	 * @throws CloneNotSupportedException
	 * @throws IOException
	 * @throws InvalidFormatException
	 * @throws ImportException
	 */
	private void importQuickly(WorkbookReader workbookReader, String sheetName, int batchSize,
			final SheetImporter<?> importer)
			throws XMLStreamException, InvalidFormatException, IOException, ImportException {

		QuickImporter quickImp = new QuickImporter(workbookReader, batchSize) {

			@Override
			public void importData(ResultDataSet rs) throws ImportException {
				importer.importData(rs);
			}
		};

		quickImp.importSheet();
	}

	/**
	 * Import the entire term sheet into the db. Note that you have to import the
	 * hierarchies and the attributes before importing this sheet. See
	 * {@link #importAttributeSheet(WorkbookReader, Catalogue)} and
	 * {@link #importHierarchySheet(WorkbookReader, Catalogue, String)}.
	 * 
	 * @param workbookReader
	 * @param catalogue
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws SQLException
	 * @throws ImportException
	 */
	private TermSheetImporter importTermSheet(WorkbookReader workbookReader, Catalogue catalogue)
			throws InvalidFormatException, IOException, XMLStreamException, SQLException, ImportException {
		
		LOGGER.info("Importing term sheet");

		final int batchSize = 100;

		// get the hierarchy sheet
		workbookReader.processSheetName(Headers.TERM_SHEET_NAME);

		TermSheetImporter termImp = new TermSheetImporter(termDao, catalogue);

		if (progressBar != null) {
			double prog = ProgressSettings.getProgress(ProgressSettings.TERM_SHEET, maxProgress);
			termImp.setProgressBar(progressBar, workbookReader.getRowCount(), prog);
		}

		// import terms in a quick way
		importQuickly(workbookReader, Headers.TERM_SHEET_NAME, batchSize, termImp);

		return termImp;
	}

	/**
	 * Import term attributes and term parents
	 * 
	 * @param workbookReader
	 * @param catalogue
	 * @param newCodes
	 * @throws SQLException
	 * @throws InvalidFormatException
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws ImportException
	 */
	private void importTermRelations(WorkbookReader workbookReader, Catalogue catalogue,
			HashMap<String, String> newCodes)
			throws SQLException, InvalidFormatException, XMLStreamException, IOException, ImportException {
		
		LOGGER.info("Importing term relations");
		
		final int batchSize = 100;

		// note that we need to have imported the terms to import
		// term attributes and parent terms!
		// import term attributes and parent terms in a parallel way
		// since they are independent processes
		QuickParentAttributesImporter tapImporter = new QuickParentAttributesImporter(taDao, parentDao, catalogue,
				workbookReader, Headers.TERM_SHEET_NAME, batchSize);

		if (progressBar != null) {
			double progTa = ProgressSettings.getProgress(ProgressSettings.TERM_ATTR_SHEET, maxProgress);
			double progParent = ProgressSettings.getProgress(ProgressSettings.PARENT_SHEET, maxProgress);

			tapImporter.setAttributeProgressBar(progressBar, progTa);
			tapImporter.setParentProgressBar(progressBar, progParent);
		}

		tapImporter.manageNewTerms(newCodes);
		tapImporter.importSheet();
	}

	/**
	 * Import the release notes
	 * 
	 * @param workbookReader
	 * @param catalogue
	 */
	private void importReleaseNotes(WorkbookReader workbookReader, Catalogue catalogue) {

		LOGGER.info("Importing release notes");
		
		// import the release notes operations
		try {

			workbookReader.processSheetName(Headers.NOTES_SHEET_NAME);

			ResultDataSet sheetData = workbookReader.next();

			NotesSheetImporter notesImp = new NotesSheetImporter(notesDao);

			if (progressBar != null) {
				double prog = ProgressSettings.getProgress(ProgressSettings.NOTES_SHEET, maxProgress);

				notesImp.setProgressBar(progressBar, workbookReader.getRowCount(), prog);
			}

			notesImp.importData(sheetData);

			workbookReader.getSheetParser().close();
			sheetData.close();

		} catch (Exception e) {
			LOGGER.error("Release notes not found for " + catalogue, e);
			e.printStackTrace();
		}
	}
}
