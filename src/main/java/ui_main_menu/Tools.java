package ui_main_menu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import catalogue_browser_dao.DatabaseManager;
import config.Config;
import config.Environment;
import dcf_user.User;
import pending_request.DcfPendingRequestsList;
import pending_request.IDcfPendingRequestsList;
import pending_request.IPendingRequest;
import pending_request.PendingRequestDao;
import sas_remote_procedures.XmlChangesService;
import sas_remote_procedures.XmlUpdateFile;
import sas_remote_procedures.XmlUpdateFileDAO;
import soap.DetailedSOAPException;
import soap.UploadCatalogueFileImpl;
import soap.UploadCatalogueFileImpl.PublishLevel;
import soap.UploadCatalogueFileImpl.ReserveLevel;
import soap.UploadCatalogueFilePersistentImpl;
import ui_main_panel.BrowserPendingRequestWorker;
import user.IDcfUser;

/**
 * Actions performed from the user interface in the tools menu.
 * @author avonva
 * @author shahaal
 */
public class Tools {
	
	/**
	 * Reserve a catalogue
	 * @param level minor/major
	 * @param catalogueCode
	 * @param description
	 * @throws SQLException
	 * @throws IOException
	 * @throws DetailedSOAPException
	 */
	public void reserve(ReserveLevel level, String catalogueCode, String description) 
			throws SQLException, IOException, DetailedSOAPException {
		
		IPendingRequest request = getSender().reserve(User.getInstance(), 
				getEnvironment(), level, catalogueCode, description);
		
		// start polling the dcf
		BrowserPendingRequestWorker.getInstance().startPendingRequests(request);
	}
	
	/**
	 * Unreserve a catalogue
	 * @param catalogueCode
	 * @param description
	 * @throws SQLException
	 * @throws IOException
	 * @throws DetailedSOAPException
	 */
	public void unreserve(String catalogueCode, String description) 
			throws SQLException, IOException, DetailedSOAPException {
		
		IPendingRequest request = getSender().unreserve(User.getInstance(), 
				getEnvironment(), catalogueCode, description);
		
		// start polling the dcf
		BrowserPendingRequestWorker.getInstance().startPendingRequests(request);
	}
	
	/**
	 * Publish a catalogue
	 * @param level publish minor/major
	 * @param catalogueCode
	 * @throws SQLException
	 * @throws IOException
	 * @throws DetailedSOAPException
	 */
	public void publish(PublishLevel level, String catalogueCode) 
			throws SQLException, IOException, DetailedSOAPException {
		
		IPendingRequest request = getSender().publish(User.getInstance(), 
				getEnvironment(), level, catalogueCode);
		
		// start polling the dcf
		BrowserPendingRequestWorker.getInstance().startPendingRequests(request);
	}
	
	/**
	 * Upload .xml changes to dcf. The .xml file must be generated
	 * by the SAS procedure.
	 * @param attachment
	 * @param catalogueCode
	 * @throws DetailedSOAPException
	 * @throws IOException
	 */
	public void uploadXmlData(int catalogueDatabaseId, String catalogueCode) 
			throws DetailedSOAPException, FileNotFoundException, IOException {
		
		XmlChangesService xmlService = new XmlChangesService();
		XmlUpdateFile xmlUpdatesFile = xmlService.getById(new XmlUpdateFileDAO(), catalogueDatabaseId);
		
		// get the file from the server
		File file = xmlService.getXmlFileFromServer(xmlUpdatesFile);
		
		// read the file and send it to the dcf
		String attachment = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
		
		Map<String, String> data = new HashMap<>();
		data.put(XmlChangesService.CATALOGUE_ID_DATA_KEY, String.valueOf(catalogueDatabaseId));
		data.put(UploadCatalogueFileImpl.CATALOGUE_CODE_DATA_KEY, catalogueCode);
		
		// upload the xml file to dcf
		IPendingRequest request = getSender().uploadCatalogueFile(User.getInstance(), 
				getEnvironment(), attachment, XmlChangesService.TYPE_UPLOAD_XML_DATA, data);

		// start polling the dcf
		BrowserPendingRequestWorker.getInstance().startPendingRequests(request);
	}
	
	/**
	 * Start pending requests of an user
	 * @param user
	 * @throws SQLException
	 * @throws IOException
	 */
	public void startUserPendingRequests(IDcfUser user) throws SQLException, IOException {
		
		IDcfPendingRequestsList<IPendingRequest> output = new DcfPendingRequestsList();
		getSender().getUserPendingRequests(user, output);
		
		for (IPendingRequest request: output)
			BrowserPendingRequestWorker.getInstance().startPendingRequests(request);
	}
	
	/**
	 * Get object which is used to send request to dcf
	 * @return
	 */
	private UploadCatalogueFilePersistentImpl getSender() {
		
		String dbUrl = DatabaseManager.createMainDBURL();
		
		PendingRequestDao<IPendingRequest> dao = new PendingRequestDao<>(dbUrl);
		
		UploadCatalogueFilePersistentImpl uploadCatFile = 
				new UploadCatalogueFilePersistentImpl(dao);
		
		return uploadCatFile;
	}
	
	/**
	 * Get the type of dcf
	 * @return
	 */
	private Environment getEnvironment() {
		Environment env = Config.getEnvironment();
		return env;
	}
}
