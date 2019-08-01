package dcf_pending_request;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

import config.Environment;
import dcf_user.User;
import import_catalogue.CatalogueImporter;
import import_catalogue.CatalogueImporter.ImportFileFormat;
import import_catalogue.ImportException;
import soap.ExportCatalogueFile;

public class LastInternalVersionDownloader implements ILastInternalVersionDownloader {
	
	@Override
	public void downloadAndImport(String catalogueCode, Environment env)
			throws SOAPException, ImportException, IOException, SQLException {
		
		// download the file
		ExportCatalogueFile request = new ExportCatalogueFile();
		
		File file = request.exportLastInternalVersion(env, User.getInstance(), catalogueCode);
		
		if (file == null || !file.exists())
			throw new FileNotFoundException("The file containing the last internal version of the catalogue cannot be found");
		
		// import the last internal version into the database
		CatalogueImporter importer = new CatalogueImporter(file.getPath(), ImportFileFormat.XML);
		try {
			importer.makeImport();
		} catch (TransformerException | XMLStreamException | OpenXML4JException | SAXException e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}
}
