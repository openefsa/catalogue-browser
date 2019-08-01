package dcf_pending_request;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

import catalogue.Catalogue;
import catalogue.CatalogueVersion;
import catalogue.ReservedCatalogue;
import catalogue.VersionChecker;
import catalogue_browser_dao.CatalogueBackup;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.ForceCatEditDAO;
import catalogue_browser_dao.ICatalogueBackup;
import catalogue_browser_dao.ICatalogueDAO;
import catalogue_browser_dao.IForcedCatalogueDAO;
import catalogue_browser_dao.ReservedCatDAO;
import catalogue_object.Status.StatusValues;
import config.Config;
import config.Environment;
import dcf_manager.Dcf;
import dcf_manager.Dcf.DcfType;
import dcf_pending_request.PendingRequestActionsListener.ActionPerformed;
import dcf_pending_request.PendingRequestActionsListener.PendingRequestActionsEvent;
import dcf_user.User;
import import_catalogue.ImportException;
import sas_remote_procedures.XmlChangesService;
import sas_remote_procedures.XmlUpdateFile;
import sas_remote_procedures.XmlUpdateFileDAO;
import soap.UploadCatalogueFileImpl.PublishLevel;
import soap.UploadCatalogueFileImpl.ReserveLevel;
import version_manager.VersionComparator;

public class PendingRequestActions {

	private static final Logger LOGGER = LogManager.getLogger(PendingRequestActions.class);
	
	private Collection<PendingRequestActionsListener> listeners;
	
	private Environment env;
	private ICatalogueBackup backup;
	private ICatalogueDAO catDao; 
	private CatalogueEntityDAO<ReservedCatalogue> resDao;
	private CatalogueEntityDAO<XmlUpdateFile> xmlDao;
	private IForcedCatalogueDAO forcedDao;
	private ILastInternalVersionDownloader downloader;
	
	public PendingRequestActions(ICatalogueBackup backup, 
			ICatalogueDAO catDao, 
			CatalogueEntityDAO<ReservedCatalogue> resDao,
			CatalogueEntityDAO<XmlUpdateFile> xmlDao,
			IForcedCatalogueDAO forcedDao,
			ILastInternalVersionDownloader downloader,
			Environment env) {
		this.backup = backup;
		this.catDao = catDao;
		this.resDao = resDao;
		this.xmlDao = xmlDao;
		this.forcedDao = forcedDao;
		this.downloader = downloader;
		this.env = env;
		this.listeners = new ArrayList<>();
	}
	
	public PendingRequestActions() {
		this.backup = new CatalogueBackup();
		this.catDao = new CatalogueDAO();
		this.resDao = new ReservedCatDAO();
		this.xmlDao = new XmlUpdateFileDAO();
		this.forcedDao = new ForceCatEditDAO();
		this.downloader = new LastInternalVersionDownloader();
		
		this.env = Config.getEnvironment();
		
		this.listeners = new ArrayList<>();
	}
	
	/**
	 * Listen the action performed
	 * @param listener
	 */
	public void addListener(PendingRequestActionsListener listener) {
		this.listeners.add(listener);
	}
	
	public void notify(PendingRequestActionsEvent event) {
		for (PendingRequestActionsListener listener: listeners)
			listener.actionPerformed(event);
	}
	
	public Catalogue getLastVersion(String catalogueCode) {
		return catDao.getLastVersionByCode(catalogueCode, DcfType.fromEnvironment(env));
	}
	
	private boolean isLastInternalVersion(Catalogue catalogue, String dcfInternalVersion, 
			boolean incrementCatalogueVersionByOne) {
		
		// increment (fake) the internal version of the catalogue
		// this is the version which will be created
		// in dcf if the local internal version is indeed the dcf internal one
		CatalogueVersion localInternalVersion = new CatalogueVersion(catalogue.getVersion());
		
		if (incrementCatalogueVersionByOne)
			localInternalVersion.incrementInternal();
		
		// check if it is ok with the one in DCF
		VersionComparator comparator = new VersionComparator();
		int compare = comparator.compare(localInternalVersion.getVersion(), dcfInternalVersion);
		
		return compare >= 0;
	}
	
	/**
	 * Invalidate the catalogue version
	 * @param catalogue
	 */
	private void invalidate(Catalogue catalogue) {
		
		// add the NULL flag
		catalogue.getCatalogueVersion().invalidate();
		
		catalogue.setStatus(StatusValues.INVALID);
		
		// update version in the db
		catDao.update(catalogue);
		
		// remove the force editing
		forcedDao.removeForceEditing(catalogue);
	}
	
	/**
	 * Confirm the catalogue version which was
	 * previously forced to temporary
	 * @param catalogue
	 */
	private void confirm(Catalogue catalogue) {
		
		// confirm the temporary version
		LOGGER.info ("Version confirmed for " + this);
		
		// remove forced flag
		catalogue.getCatalogueVersion().confirm();
		
		catalogue.setStatus(StatusValues.INTERNAL_VERSION);

		LOGGER.info("Forced editing removed by " 
				+ User.getInstance() + " for " + this);

		// remove the force editing
		forcedDao.removeForceEditing(catalogue);
		catDao.update(catalogue);
	}
	
	private Catalogue importLastInternalVersion(String catalogueCode) throws SOAPException, TransformerException, 
		IOException, XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {
		
		notify(new PendingRequestActionsEvent(ActionPerformed.LIV_IMPORT_STARTED, catalogueCode, ""));
		
		// download the last internal version and import it
		downloader.downloadAndImport(catalogueCode, env);
		
		// fetch the last internal version
		// and update the catalogue reference
		Catalogue lastInternalVersion = getLastVersion(catalogueCode);
		
		notify(new PendingRequestActionsEvent(ActionPerformed.LIV_IMPORTED, catalogueCode, "", "",
				lastInternalVersion.getCode(), lastInternalVersion.getVersion()));
		
		return lastInternalVersion;
	}

	/**
	 * Reserve successfully completed after the catalogue
	 * was reserved in a forced way.
	 * @return true if catalogue confirmed, false if invalidated
	 * @param catalogueCode
	 * @param dcfInternalVersion
	 * @param level
	 * @param reservationNote
	 * @param username
	 * @throws ImportException 
	 * @throws SQLException 
	 * @throws SAXException 
	 * @throws OpenXML4JException 
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws TransformerException 
	 * @throws SOAPException 
	 */
	public boolean reserveCompletedAfterForcedReserve(String catalogueCode, String dcfInternalVersion, 
			ReserveLevel level, String reservationNote, String username) 
					throws SOAPException, TransformerException, IOException, XMLStreamException, 
					OpenXML4JException, SAXException, SQLException, ImportException {
		
		Catalogue catalogue = getLastVersion(catalogueCode);
		
		boolean isLast = isLastInternalVersion(catalogue, dcfInternalVersion, false);

		if (!isLast) {
			
			String oldVersion = catalogue.getVersion();
			
			// invalidate the forced version
			invalidate(catalogue);
			
			notify(new PendingRequestActionsEvent(ActionPerformed.TEMP_CAT_INVALIDATED_LIV, 
					catalogueCode, oldVersion, catalogue.getVersion()));
			
			// download the last internal one
			Catalogue lastInternalVersion = importLastInternalVersion(catalogueCode);
			
			// create a copy of it with internal version increased 
			// and mark it as reserved
			Catalogue newVersion = markAsReservedVersion(lastInternalVersion, username, reservationNote, level);
			
			notify(new PendingRequestActionsEvent(ActionPerformed.NEW_INTERNAL_VERSION_CREATED, 
					catalogueCode, dcfInternalVersion, newVersion.getVersion()));
		}
		else {
			
			String oldVersion = catalogue.getVersion();
			confirm(catalogue);
			
			// reserve the catalogue into the db
			resDao.insert(new ReservedCatalogue(catalogue, 
					username, reservationNote, level));
			
			notify(new PendingRequestActionsEvent(ActionPerformed.TEMP_CAT_CONFIRMED, 
					catalogueCode, oldVersion, catalogue.getVersion()));
		}
		
		return isLast;
	}
	
	/**
	 * Reserve failed after the catalogue was reserved
	 * in a forced way
	 */
	public boolean reserveFailedAfterForcedReserve(String catalogueCode) {
		Catalogue catalogue = getLastVersion(catalogueCode);
		
		if (!catalogue.getCatalogueVersion().isForced())
			return false;
		
		String oldVersion = catalogue.getVersion();
		
		invalidate(catalogue);
		
		notify(new PendingRequestActionsEvent(ActionPerformed.TEMP_CAT_INVALIDATED_NO_RESERVE, 
				catalogueCode, oldVersion, catalogue.getVersion()));
		
		return true;
	}

	
	/**
	 * Reserve a catalogue in the db
	 * @param catalogueCode
	 * @param lastInternalVersion the last internal version present in DCF
	 * @param level
	 * @param reservationNote
	 * @param username
	 * @throws SOAPException 
	 * @throws ImportException 
	 * @throws SQLException 
	 * @throws SAXException 
	 * @throws OpenXML4JException 
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws TransformerException 
	 */
	public void reserveCompletedBeforeForcing(String catalogueCode, String dcfInternalVersion, 
			ReserveLevel level, String reservationNote, String username) 
					throws SOAPException, TransformerException, IOException, XMLStreamException, 
					OpenXML4JException, SAXException, SQLException, ImportException {
		
		LOGGER.info("Reserve completed in DCF. Creating new local internal version for editing");
		
		Catalogue catalogue = getLastVersion(catalogueCode);
		
		// if not last internal (dcf internal version is newer)
		if (!isLastInternalVersion(catalogue, dcfInternalVersion, true)) {
			
			LOGGER.info("Catalogue=" + catalogue + " is not the last internal version (which is=" 
					+ dcfInternalVersion + "). Downloading it.");
			
			catalogue = importLastInternalVersion(catalogueCode);
		}
		
		Catalogue newVersion = markAsReservedVersion(catalogue, username, reservationNote, level);
		
		notify(new PendingRequestActionsEvent(ActionPerformed.NEW_INTERNAL_VERSION_CREATED, 
				catalogueCode, catalogue.getVersion(), newVersion.getVersion()));
	}
	
	/**
	 * Mark the catalogue as reserved in the local machine
	 * It also creates a new internal version of the catalogue,
	 * which is returned back by the method
	 * @param catalogue
	 * @param username
	 * @param reservationNote
	 * @param level
	 * @return
	 */
	private Catalogue markAsReservedVersion(Catalogue catalogue, String username, 
			String reservationNote, ReserveLevel level) {
		
		// version checker, increase version
		// and create backup
		VersionChecker versionChecker = new VersionChecker(backup, catDao, catalogue);
		catalogue = versionChecker.newInternalVersion();

		LOGGER.info("The new reserved local internal version of the catalogue is=" + catalogue);
		
		// reserve the catalogue into the db
		resDao.insert(new ReservedCatalogue(catalogue, 
				username, reservationNote, level));
		
		return catalogue;
	}
	
	/**
	 * Reserve a catalogue in a forced way
	 * @param catalogueCode
	 * @param level
	 * @param username
	 */
	public boolean forceReserve(String catalogueCode, ReserveLevel level, String username) {
		
		Catalogue catalogue = getLastVersion(catalogueCode);
		
		// avoid double forced
		if (catalogue.getCatalogueVersion().isForced())
			return false;
		
		LOGGER.info("DCF is busy. Creating a FORCED fake local internal version for editing");
		
		String oldVersion = catalogue.getVersion();
		
		// update the forced count for this catalogue
		catalogue.increaseForcedCount();

		// update the forced count also in the db
		catDao.update(catalogue);

		// version checker to modify the catalogue version
		// and to create backup
		VersionChecker versionChecker = new VersionChecker(backup, catDao, catalogue);
		Catalogue forcedCatalogue = versionChecker.force();
		
		// set the new status
		forcedCatalogue.setStatus(StatusValues.TEMPORARY);

		// save catalogue as forced
		forcedDao.forceEditing(forcedCatalogue, username, level);
		
		notify(new PendingRequestActionsEvent(ActionPerformed.TEMP_CAT_CREATED, 
				catalogueCode, oldVersion, forcedCatalogue.getVersion()));
		
		return true;
	}
	
	/**
	 * Unreserve a catalogue in db
	 * @param catalogueCode
	 * @param username
	 * @return if the catalogue was unreserved successfully in the database
	 */
	public boolean unreserveCompleted(String catalogueCode, String username) {
		
		LOGGER.info("Unreserve completed in DCF");
		
		// TODO : should i change the status of the catalogue?
		
		Catalogue catalogue = getLastVersion(catalogueCode);
		
		// unreserve the catalogue in the database
		ReservedCatalogue rc = resDao.getById(catalogue.getId());

		if (rc != null)
			return resDao.remove(rc);
		
		return false;
	}
	
	/**
	 * Publish a catalogue in db
	 * @param catalogueCode
	 * @param level
	 */
	public void publishCompleted(String catalogueCode, PublishLevel level) {
		LOGGER.info("Publish completed in DCF");
		
		// refresh getCataloguesList
		Dcf dcf = new Dcf();
		dcf.refreshCatalogues();
	}
	
	public void uploadXmlChangesCompleted(int catalogueId) throws IOException {
		
		XmlUpdateFile xmlFile = xmlDao.getById(catalogueId);

		if (xmlFile == null)
			return;
		
		XmlChangesService service = new XmlChangesService();
		service.delete(xmlDao, xmlFile);
	}
}
