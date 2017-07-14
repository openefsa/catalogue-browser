package test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import dcf_manager.Dcf.DcfType;
import import_catalogue.CatalogueWorkbookImporter;

public class ImportTest {
	
	public static void main ( String[] args ) throws IOException, XMLStreamException, 
	OpenXML4JException, SAXException, SQLException {

		DatabaseManager.startMainDB();

		CatalogueDAO catDao = new CatalogueDAO();
		ArrayList<Catalogue> cats = catDao.getMyCatalogues( DcfType.LOCAL );
		Catalogue catalogue = null;
		for ( Catalogue cat : cats ) {
			if ( cat.getCode().equals( "MTX" ) )
				catalogue = cat;
		}

		CatalogueWorkbookImporter importer = new CatalogueWorkbookImporter();
		importer.setOpenedCatalogue( catalogue );
		//importer.importWorkbook( 
		//		"C:\\Users\\avonva\\Desktop\\MTX_8.7.xlsx");
	}
}
