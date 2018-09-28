package sas_remote_procedures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue_browser_dao.CatalogueEntityDAO;
import utilities.GlobalUtil;

public class XmlChangesService {

	private static final Logger LOGGER = LogManager.getLogger(XmlChangesService.class);
	
	public static final String TYPE_UPLOAD_XML_DATA = "UPLOAD_XML_CHANGES";
	public static final String CATALOGUE_ID_DATA_KEY = "catalogueId";
	
	/**
	 * The export format of the exported file as soon
	 * as the export is finished
	 */
	static final String START_FORMAT = ".start";
	
	/**
	 * The correct export format of the catalogue data
	 */
	static final String LOCAL_EXPORT_FORMAT = ".xlsx";
	
	/**
	 * The format of the file which will be produced
	 * by the sas procedure
	 */
	public static final String REMOTE_OUT_FORMAT = ".xml";
	
	/**
	 * The format of the lock file which will be created
	 * at the end of the process
	 */
	static final String END_FORMAT = ".end";
	
	/**
	 * The format of the lock file which will be created
	 * when the remote procedure starts processing the
	 * .xlsx file to create the .xml file
	 */
	static final String REMOTE_START_FORMAT = ".process.start";
	
	/**
	 * The format of the file which confirms that the
	 * procedure has terminated the conversion
	 */
	public static final String REMOTE_END_FORMAT = ".process.end";
	
	public File getXmlFileFromServer(XmlUpdateFile xmlFile) throws FileNotFoundException {
		
		LOGGER.info( "Downloading xml changes file from server" );
		
		String processEndFilename = SasRemotePaths.XML_UPDATES_CREATOR_INPUT_FOLDER + 
				xmlFile.getXmlFilename() + REMOTE_END_FORMAT;
		
		String remoteXmlFilename = SasRemotePaths.XML_UPDATES_CREATOR_UPDATE_FOLDER + 
				xmlFile.getXmlFilename() + REMOTE_OUT_FORMAT;
		
		File processEndFile = new File(processEndFilename);
		File remoteXmlFile = new File(remoteXmlFilename);
		
		LOGGER.info("Searching in server file=" + processEndFilename);
		
		if (!processEndFile.exists()) {
			throw new FileNotFoundException("The file=" + processEndFilename + " was not found in the server.");
		}
		
		LOGGER.info("Searching in server file=" + remoteXmlFile);
		if (!remoteXmlFile.exists()) {
			throw new FileNotFoundException("The file=" + remoteXmlFile + " was not found in the server.");
		}
		
		LOGGER.info("Xml updates file=" + remoteXmlFile + " was successfully found in the server");
		
		return remoteXmlFile;
	}

	/**
	 * Insert a xml file into the database
	 * @param dao
	 * @param xmlFile
	 */
	public void insert(CatalogueEntityDAO<XmlUpdateFile> dao, XmlUpdateFile xmlFile) {
		dao.insert(xmlFile);
	}
	
	/**
	 * Get by catalogue id
	 */
	public XmlUpdateFile getById(CatalogueEntityDAO<XmlUpdateFile> dao, int catalogueId) {
		return dao.getById(catalogueId);
	}
	
	/**
	 * Delete an update from the database
	 * @param dao
	 * @param xmlFile
	 * @throws IOException 
	 */
	public void delete(CatalogueEntityDAO<XmlUpdateFile> dao, XmlUpdateFile xmlFile) throws IOException {
		
		dao.remove(xmlFile);
		
		GlobalUtil.deleteFileCascade(SasRemotePaths.XML_UPDATES_CREATOR_INPUT_FOLDER 
				+ xmlFile.getXmlFilename() + REMOTE_END_FORMAT);
	}
	
	/**
	 * Update the xml file in the database
	 * @param dao
	 * @param xmlFile
	 */
	public void update(CatalogueEntityDAO<XmlUpdateFile> dao, XmlUpdateFile xmlFile) {
		dao.remove(xmlFile);
		dao.insert(xmlFile);
	}
}
