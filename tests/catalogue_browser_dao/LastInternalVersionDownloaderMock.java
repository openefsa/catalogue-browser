package catalogue_browser_dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

import catalogue.ReleaseNotesOperation;
import catalogue_object.Applicability;
import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import config.Environment;
import dcf_pending_request.ILastInternalVersionDownloader;
import import_catalogue.CatalogueImporter;
import import_catalogue.CatalogueImporter.ImportFileFormat;
import import_catalogue.ImportException;

public class LastInternalVersionDownloaderMock implements ILastInternalVersionDownloader {

	private ICatalogueDAO catDao;
	private CatalogueEntityDAO<Attribute> attrDao;
	private CatalogueEntityDAO<Hierarchy> hierDao; 
	private CatalogueEntityDAO<Term> termDao;
	private CatalogueRelationDAO<TermAttribute, Term, Attribute> taDao;
	private CatalogueRelationDAO<Applicability, Term, Hierarchy> parentDao;
	private CatalogueEntityDAO<ReleaseNotesOperation> notesDao;
	
	public LastInternalVersionDownloaderMock(ICatalogueDAO catDao, 
			CatalogueEntityDAO<Attribute> attrDao,
			CatalogueEntityDAO<Hierarchy> hierDao,
			CatalogueEntityDAO<Term> termDao,
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
	
	@Override
	public void downloadAndImport(String catalogueCode, Environment env)
			throws SOAPException, ImportException, IOException, SQLException {
		
		// download the file
		ExportCatalogueFileMock request = new ExportCatalogueFileMock();
		
		File file = request.exportLastInternalVersion();
		
		if (file == null || !file.exists())
			throw new FileNotFoundException("The file containing the last internal version of the catalogue cannot be found");
		
		// import the last internal version into the database
		CatalogueImporter importer = new CatalogueImporter(file.getPath(), ImportFileFormat.XML);
		importer.setDaos(catDao, attrDao, hierDao, termDao, taDao, parentDao, notesDao);
		try {
			importer.makeImport();
		} catch (TransformerException | XMLStreamException | OpenXML4JException | SAXException e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}	
}
