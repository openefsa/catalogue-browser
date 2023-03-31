package import_catalogue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

import catalogue.Catalogue;
import catalogue.ReleaseNotesOperation;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.CatalogueRelationDAO;
import catalogue_browser_dao.ICatalogueDAO;
import catalogue_object.Applicability;
import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import progress_bar.IProgressBar;
import utilities.GlobalUtil;
import xml_to_excel.XmlCatalogueToExcel;
import zip_manager.ZipManager;

public class CatalogueImporter {

	private static final Logger LOGGER = LogManager.getLogger(CatalogueImporter.class);

	private String filename; // path of the file
	private ImportFileFormat format; // the format of the file
	private Catalogue openedCat;
	private IProgressBar progressBar;
	private double maxProgress;
	private double preprocProgress;

	// list of temporary files which need to
	// be deleted at the end of the process
	private ArrayList<String> garbage;

	private ICatalogueDAO catDao;
	private CatalogueEntityDAO<Attribute> attrDao;
	private CatalogueEntityDAO<Hierarchy> hierDao;
	private CatalogueEntityDAO<Term> termDao;
	private CatalogueRelationDAO<TermAttribute, Term, Attribute> taDao;
	private CatalogueRelationDAO<Applicability, Term, Hierarchy> parentDao;
	private CatalogueEntityDAO<ReleaseNotesOperation> notesDao;

	/**
	 * Enumerator to specify the format of the file we want to import into the
	 * catalogue database
	 * 
	 * @author avonva
	 *
	 */
	public enum ImportFileFormat {
		ECF, XML, XLSX;
	}

	/**
	 * Initialize the import thread
	 * 
	 * @param filename path of the file we want to import
	 * @param format   in which format is the file that we want to import
	 */
	public CatalogueImporter(String filename, ImportFileFormat format, IProgressBar progressBar, double maxProgress) {

		this.filename = filename;
		this.format = format;
		this.garbage = new ArrayList<>();
		this.progressBar = progressBar;
		this.maxProgress = maxProgress;
	}

	public CatalogueImporter(String filename, ImportFileFormat format) {
		this(filename, format, null, 100);
	}

	public void setDaos(ICatalogueDAO catDao, CatalogueEntityDAO<Attribute> attrDao,
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
	 * Import the file
	 * 
	 * @throws TransformerException
	 * @throws SQLException
	 * @throws SAXException
	 * @throws OpenXML4JException
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws ImportException
	 */
	public void makeImport() throws TransformerException, IOException, XMLStreamException, OpenXML4JException,
			SAXException, SQLException, ImportException {

		// 5% of progress bar for preprocessing
		this.preprocProgress = maxProgress * 5 / 100;
		if (progressBar != null)
			progressBar.addProgress(preprocProgress);

		switch (format) {
		case ECF:
			importEcf(filename);
			break;

		case XML:
			importXml(filename);
			break;

		case XLSX:
			importXlsx(filename);
			break;

		default:
			break;
		}
	}

	/**
	 * Process an ecf file and extract the xml catalogue contained in it.
	 * 
	 * @param filename
	 * @return the created xml file
	 */
	private String processEcf(String filename) {

		String outputFile = null;

		try {

			File inputFile = new File(filename);

			String outputFolder = GlobalUtil.getTempDir() + inputFile.getName() + "_unzip";

			// unzip the ecf file into the xml
			ZipManager.extractFolder(filename, outputFolder);

			final File unzippedFolder = new File(outputFolder);

			if (unzippedFolder.listFiles().length <= 0) {
				LOGGER.error("Wrong file format, " + "cannot find the xml file inside the .ecf");
				return null;
			}

			// add the unzipped folder to the garbage to
			// delete it at the end of the process
			garbage.add(unzippedFolder.getAbsolutePath());

			// get the xml file from the folder
			File xmlFile = unzippedFolder.listFiles()[0];

			outputFile = xmlFile.getAbsolutePath();

			return outputFile;

		} catch (IOException e1) {
			LOGGER.error("Error while processing Ecf files", e1);
			e1.printStackTrace();
		}

		return null;
	}

	/**
	 * Process an .xml file to create a .xlsx catalogue file
	 * 
	 * @param filename xml filename
	 * @return the created xlsx file
	 * @throws TransformerException
	 */
	private String processXml(String filename) throws TransformerException {

		String outputFilename = filename + ".xlsx";

		// convert the xml into an excel
		XmlCatalogueToExcel converter = new XmlCatalogueToExcel(filename, outputFilename);

		// do the conversion
		converter.convertXmlToExcel();

		LOGGER.info("Processed xml filename: " + outputFilename);
		return outputFilename;
	}

	/**
	 * Import an .ecf catalogue
	 * 
	 * @param filename the absolute path of the .ecf file
	 * @throws TransformerException
	 * @throws SQLException
	 * @throws SAXException
	 * @throws OpenXML4JException
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws ImportException
	 */
	private void importEcf(String filename) throws TransformerException, IOException, XMLStreamException,
			OpenXML4JException, SAXException, SQLException, ImportException {

		String xmlFile = processEcf(filename);

		// at the end of the process delete the
		// .xml temporary file
		garbage.add(xmlFile);

		// import the .xml file
		importXml(xmlFile);
	}

	/**
	 * Import a .xml catalogue
	 * 
	 * @param filename the absolute path of the .xml catalogue
	 * @throws TransformerException
	 * @throws SQLException
	 * @throws SAXException
	 * @throws OpenXML4JException
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws ImportException
	 */
	private void importXml(String filename) throws TransformerException, IOException, XMLStreamException,
			OpenXML4JException, SAXException, SQLException, ImportException {

		String xlsxFile = processXml(filename);

		// at the end of the process delete the
		// temporary xlsx file
		garbage.add(xlsxFile);

		// import the xlsx catalogue
		importXlsx(xlsxFile);
	}

	/**
	 * Import a .xlsx catalogue
	 * 
	 * @param filename the absolute path of the .xlsx catalogue
	 * @throws SQLException
	 * @throws SAXException
	 * @throws OpenXML4JException
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws ImportException
	 */
	private void importXlsx(final String filename)
			throws IOException, XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {

		// instantiate the workbook importer and set
		// some settings
		CatalogueWorkbookImporter importer = null;

		if (this.attrDao == null)
			importer = new CatalogueWorkbookImporter();
		else
			importer = new CatalogueWorkbookImporter(catDao, attrDao, hierDao, termDao, taDao, parentDao, notesDao);

		if (openedCat != null)
			importer.setOpenedCatalogue(openedCat);

		// import the catalogue contained in the
		// xlsx file into the specified path (db path)
		importer.importWorkbook(progressBar, filename, maxProgress - preprocProgress);

		// delete all the temporary files
		deleteGarbage();
	}

	/**
	 * Delete all the file in the garbage
	 * 
	 * @throws IOException
	 */
	private void deleteGarbage() {
		for (String filename : garbage) {
			try {
				GlobalUtil.deleteFileCascade(new File(filename));
			} catch (IOException e) {
				LOGGER.error("Error while deleting files", e);
				e.printStackTrace();
			}
		}
	}

	public void setOpenedCat(Catalogue openedCat) {
		this.openedCat = openedCat;
	}
}
