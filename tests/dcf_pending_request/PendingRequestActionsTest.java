package dcf_pending_request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import catalogue.Catalogue;
import catalogue.CatalogueVersion;
import catalogue.ReleaseNotesOperation;
import catalogue.ReservedCatalogue;
import catalogue_browser_dao.AttributeDAOMock;
import catalogue_browser_dao.CatalogueBackupMock;
import catalogue_browser_dao.CatalogueDAOMock;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.CatalogueRelationDAO;
import catalogue_browser_dao.ForcedCatalogueDAOMock;
import catalogue_browser_dao.HierarchyDAOMock;
import catalogue_browser_dao.ICatalogueBackup;
import catalogue_browser_dao.IForcedCatalogueDAO;
import catalogue_browser_dao.LastInternalVersionDownloaderMock;
import catalogue_browser_dao.NotesDAOMock;
import catalogue_browser_dao.ParentTermDAOMock;
import catalogue_browser_dao.ReservedCatalogueDAOMock;
import catalogue_browser_dao.TermAttributeDAOMock;
import catalogue_browser_dao.TermDAOMock;
import catalogue_browser_dao.XmlUpdateFileDAOMock;
import catalogue_object.Applicability;
import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import config.Environment;
import dcf_manager.Dcf.DcfType;
import dcf_user.User;
import import_catalogue.ImportException;
import sas_remote_procedures.SasRemotePaths;
import sas_remote_procedures.XmlChangesService;
import sas_remote_procedures.XmlUpdateFile;
import soap.UploadCatalogueFileImpl.PublishLevel;
import soap.UploadCatalogueFileImpl.ReserveLevel;
import version_manager.VersionComparator;

public class PendingRequestActionsTest {

	private CatalogueDAOMock catDao;
	private ReservedCatalogueDAOMock resDao;
	private IForcedCatalogueDAO forcedDao;
	private CatalogueEntityDAO<XmlUpdateFile> xmlDao;
	private CatalogueEntityDAO<Attribute> attrDao;
	private CatalogueEntityDAO<Hierarchy> hierDao; 
	private CatalogueEntityDAO<Term> termDao;
	private CatalogueRelationDAO<TermAttribute, Term, Attribute> taDao;
	private CatalogueRelationDAO<Applicability, Term, Hierarchy> parentDao;
	private CatalogueEntityDAO<ReleaseNotesOperation> notesDao;
	private ILastInternalVersionDownloader downloader;
	private ICatalogueBackup backup;
	
	private PendingRequestActions actions;
	
	private String catalogueCode;
	private String username;
	
	@Before
	public void init() {
		
		this.catalogueCode = "AMRPROG";  // needed this code for using also test-files, do not change
		this.username = "avonva";
		
		User.getInstance().login("avonva", "Ab123456");
		
		this.backup = new CatalogueBackupMock();
		this.catDao = new CatalogueDAOMock();
		this.attrDao = new AttributeDAOMock();
		this.hierDao = new HierarchyDAOMock();
		this.termDao = new TermDAOMock();
		this.taDao = new TermAttributeDAOMock();
		this.parentDao = new ParentTermDAOMock();
		this.notesDao = new NotesDAOMock();
		this.resDao = new ReservedCatalogueDAOMock();
		this.xmlDao = new XmlUpdateFileDAOMock();
		this.forcedDao = new ForcedCatalogueDAOMock();
		this.downloader = new LastInternalVersionDownloaderMock(catDao, attrDao, hierDao, 
				termDao, taDao, parentDao, notesDao);
		
		this.actions = new PendingRequestActions(backup, catDao, resDao, 
				xmlDao, forcedDao, downloader, Environment.TEST);
	}
	
	public void getLastVersion() {
		Catalogue last = actions.getLastVersion(catalogueCode);
		System.out.println(last);
	}
	
	@Test
	public void reserveSuccessAndUnreserveHavingLastInternalVersion() throws SOAPException, TransformerException, 
		IOException, XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {
		
		Catalogue startingPoint = new Catalogue();
		startingPoint.setCode(catalogueCode);
		startingPoint.setVersion("1.0");
		catDao.insert(startingPoint);
		
		String dcfLastInternal = "1.0.1";
		
		assertEquals(catDao.getAll().size(), 1);
		
		actions.reserveCompletedBeforeForcing(catalogueCode, dcfLastInternal, 
				ReserveLevel.MINOR, "reservation note", username);
		
		// catalogue should be reserved
		
		assertEquals(catDao.getAll().size(), 2);  // new version created
		
		Catalogue lastVersion = catDao.getLastVersionByCode(catalogueCode, null);
		
		// new internal version created
		assertEquals("1.0.1", lastVersion.getVersion());
		
		ReservedCatalogue rs = resDao.getById(lastVersion.getId());
		
		// present => reserved
		assertNotNull(rs);
		assertEquals("1.0.1", rs.getCatalogue().getVersion());
		
		// new version should be equal to the one in dcf
		VersionComparator comparator = new VersionComparator();
		int compare = comparator.compare(rs.getCatalogue().getVersion(), dcfLastInternal);
		assertEquals(compare, 0);
		
		actions.unreserveCompleted(catalogueCode, username);
		
		rs = resDao.get(catalogueCode);
		
		// not present => unreserved
		assertNull(rs);
	}
	
	@Test
	public void reserveMinorForced() {

		Catalogue startingPoint = new Catalogue();
		startingPoint.setCode(catalogueCode);
		startingPoint.setVersion("1.0");
		startingPoint.setRawStatus("PUBLISHED MINOR");
		catDao.insert(startingPoint);
		
		assertEquals(catDao.getAll().size(), 1);		
		
		actions.forceReserve(catalogueCode, ReserveLevel.MINOR, username);
		
		ReserveLevel forcedLevel = forcedDao.getEditingLevel(actions.getLastVersion(catalogueCode), username);
		
		// should be forced
		assertEquals(forcedLevel, ReserveLevel.MINOR);
		
		// should have created the temporary version
		assertEquals(catDao.getAll().size(), 2);
		
		CatalogueVersion version = catDao.getLastVersionByCode(catalogueCode, null).getCatalogueVersion();
		
		// is the version forced?
		boolean forced = version.isForced();

		assertEquals(version.getVersion().contains(CatalogueVersion.FORCED_VERSION), true);
		assertEquals(forced, true);
	}
	
	@Test
	public void forcedReserveMinorFailed() {

		Catalogue startingPoint = new Catalogue();
		startingPoint.setCode(catalogueCode);
		startingPoint.setVersion("1.0");
		startingPoint.setRawStatus("PUBLISHED MINOR");
		catDao.insert(startingPoint);
		
		// force the reserve of a catalogue
		actions.forceReserve(catalogueCode, ReserveLevel.MINOR, username);

		assertEquals(catDao.getAll().size(), 2);  // new version should be created
		
		// make the reserve failing
		actions.reserveFailedAfterForcedReserve(catalogueCode);
		
		Catalogue forcedVersion = catDao.getLastInvalidVersion(catalogueCode);
		
		assertEquals(catDao.getAll().size(), 2);  // no new version should be created
		assertEquals(forcedVersion.getCatalogueVersion().isInvalid(), true);
		assertEquals(forcedVersion.getVersion().contains(CatalogueVersion.INVALID_VERSION), true);
		
		// force the reserve of a catalogue again
		actions.forceReserve(catalogueCode, ReserveLevel.MINOR, username);
		Catalogue forcedVersion2 = catDao.getLastVersionByCode(catalogueCode, null);
		assertEquals(catDao.getAll().size(), 3);
		assertEquals(forcedVersion2.getCatalogueVersion().isForced(), true);

		// mark the catalogue again as invalid
		actions.reserveFailedAfterForcedReserve(catalogueCode);
		assertEquals(catDao.getAll().size(), 3);
		assertEquals(forcedVersion2.getCatalogueVersion().isInvalid(), true);
	}
	
	@Test
	public void doubleForcedReserve() {

		Catalogue startingPoint = new Catalogue();
		startingPoint.setCode(catalogueCode);
		startingPoint.setVersion("1.0");
		startingPoint.setRawStatus("PUBLISHED MINOR");
		catDao.insert(startingPoint);
		
		boolean forced = actions.forceReserve(catalogueCode, ReserveLevel.MINOR, username);
		
		assertEquals(forced, true);
		
		forced = actions.forceReserve(catalogueCode, ReserveLevel.MAJOR, username);
		
		assertEquals(forced, false);
		
		assertEquals(catDao.getAll().size(), 2);
		
		System.out.println("Double forced reserve db=" + catDao.getAll());
	}
	
	@Test
	public void failedForcedReserveWithoutForcedCatalogue() {

		Catalogue startingPoint = new Catalogue();
		startingPoint.setCode(catalogueCode);
		startingPoint.setVersion("1.0");
		startingPoint.setRawStatus("PUBLISHED MINOR");
		catDao.insert(startingPoint);
		
		actions.reserveFailedAfterForcedReserve(catalogueCode);

		assertEquals(catDao.getAll().size(), 1);
		
		System.out.println("failedForcedReserveWithoutForcedCatalogue db=" + catDao.getAll());
	}
	
	@Test
	public void forcedReserveSuccessHavingLastInternalVersion() throws SOAPException, TransformerException, 
	IOException, XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {

		Catalogue startingPoint = new Catalogue();
		startingPoint.setCode(catalogueCode);
		startingPoint.setVersion("1.0");
		startingPoint.setRawStatus("PUBLISHED MINOR");
		catDao.insert(startingPoint);
		
		String dcfInternalVersion = "1.0.1";
		
		// force version
		actions.forceReserve(catalogueCode, ReserveLevel.MINOR, username);

		assertEquals(catDao.getLastVersionByCode(catalogueCode, null).getVersion().equals("1.0.1.1.TEMP"), true);
		
		// confirm the version
		actions.reserveCompletedAfterForcedReserve(catalogueCode, dcfInternalVersion, 
				ReserveLevel.MINOR, "reservation note", username);
		
		Catalogue last = catDao.getLastVersionByCode(catalogueCode, null);
		
		assertEquals(catDao.getAll().size(), 2);
		assertEquals(last.getVersion().equals("1.0.1"), true);
		
		ReservedCatalogue reservedCat = resDao.getById(last.getId());
		assertNotNull(reservedCat);
		assertEquals(ReserveLevel.MINOR, reservedCat.getLevel());
		
		System.out.println("forcedReserveSuccessHavingLastInternalVersion db=" + catDao.getAll());
	}
	
	@Test
	public void forcedReserveSuccessButNotHavingLastInternalVersion() throws SOAPException, TransformerException, 
	IOException, XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {
		
		Catalogue startingPoint = new Catalogue();
		startingPoint.setCode(catalogueCode);
		startingPoint.setVersion("1.0");
		startingPoint.setRawStatus("PUBLISHED MINOR");
		catDao.insert(startingPoint);
		
		String dcfInternalVersion = "1.3.5";
		
		// force version
		actions.forceReserve(catalogueCode, ReserveLevel.MINOR, username);

		assertEquals(catDao.getLastVersionByCode(catalogueCode, null).getVersion().equals("1.0.1.1.TEMP"), true);
		
		// confirm the version
		boolean isCorrect = actions.reserveCompletedAfterForcedReserve(catalogueCode, dcfInternalVersion, 
				ReserveLevel.MINOR, "reservation note", username);
		
		assertFalse(isCorrect); // we do not have the last internal version!
		
		// last internal version downloaded and copy created and reserved
		// plus the invalidated catalogue and the starting catalogue
		assertEquals(4, catDao.getAll().size());
		
		Catalogue lastInvalidVersion = catDao.getLastInvalidVersion(catalogueCode);

		assertTrue(lastInvalidVersion.getVersion().equals("1.0.1.1.NULL"));
		
		// the last internal version must be imported
		Catalogue lastInternalVersion = catDao.getCatalogue(catalogueCode, "1.3.5", DcfType.TEST);
		assertNotNull(lastInternalVersion);
		
		Catalogue reservedVersion = actions.getLastVersion(catalogueCode);
		
		// a new version should be created
		assertEquals("1.3.6", reservedVersion.getVersion());
		
		// and it should be reserved locally
		assertNotNull(resDao.getById(reservedVersion.getId()));
		
		System.out.println("forcedReserveSuccessButNotHavingLastInternalVersion db=" + catDao.getAll());
	}
	
	@Test
	public void reserveSuccessWithoutHavingLastInternalVersion() throws SOAPException, TransformerException, 
		IOException, XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {
		
		Catalogue startingPoint = new Catalogue();
		startingPoint.setCode(catalogueCode);
		startingPoint.setVersion("1.0");
		catDao.insert(startingPoint);
		
		String dcfLastInternal = "1.3.6";
		
		assertEquals(1, catDao.getAll().size());
		
		actions.reserveCompletedBeforeForcing(catalogueCode, dcfLastInternal, 
				ReserveLevel.MINOR, "reservation note", username);
		
		// a new version of the catalogue should be downloaded and imported
		// and then a new version copy of it should be created
		
		Catalogue last = catDao.getLastVersionByCode(catalogueCode, null);
		
		assertEquals("1.3.6", last.getVersion());  // the new version as the one in dcf
		assertEquals(3, catDao.getAll().size());  // new version created and a copy found
		
		System.out.println("reserveSuccessWithoutHavingLastInternalVersion db=" + catDao.getAll());
	}
	
	@Test
	public void uploadXmlChangesFileCompletedSuccessfully() throws IOException {
		
		Catalogue catalogue = new Catalogue();
		catalogue.setCode(catalogueCode);
		catalogue.setVersion("1.0");
		int id = catDao.insert(catalogue);
		catalogue.setId(id);
		
		String filename = "xml-updates-filename";
		File file = new File(SasRemotePaths.XML_UPDATES_CREATOR_INPUT_FOLDER 
				+ filename + XmlChangesService.REMOTE_END_FORMAT);
		
		file.createNewFile();  // create the file
		
		assertEquals(true, file.exists());
		
		// a fake xml updates file in the db
		xmlDao.insert(new XmlUpdateFile(catalogue, filename));
		
		// when completed
		actions.uploadXmlChangesCompleted(id);
		
		// the file should be deleted from server
		assertEquals(false, file.exists());
		
		// and from database
		assertEquals(true, xmlDao.getById(id) == null);
	}
	
	@Test(expected = IOException.class)
	public void uploadXmlChangesFileProcessEndFileNotFound() throws IOException {
		
		Catalogue catalogue = new Catalogue();
		catalogue.setCode(catalogueCode);
		catalogue.setVersion("1.0");
		int id = catDao.insert(catalogue);
		catalogue.setId(id);
		
		String filename = "xml-updates-filename";
		File file = new File(SasRemotePaths.XML_UPDATES_CREATOR_INPUT_FOLDER 
				+ filename + XmlChangesService.REMOTE_END_FORMAT);
		
		assertEquals(false, file.exists());
		
		// a fake xml updates file in the db
		xmlDao.insert(new XmlUpdateFile(catalogue, filename));
		
		// when completed
		actions.uploadXmlChangesCompleted(id);
	}
	
	@Test
	public void unreserveCompleted() {
		
		Catalogue catalogue = new Catalogue();
		catalogue.setCode(catalogueCode);
		catalogue.setVersion("1.0");
		int id = catDao.insert(catalogue);
		catalogue.setId(id);
		
		resDao.insert(new ReservedCatalogue(catalogue, username, "reservation note", ReserveLevel.MAJOR));
		actions.unreserveCompleted(catalogueCode, username);
		
		assertNull(resDao.get(catalogueCode));
	}
	
	@Test
	public void publishMinorCompleted() {
		
		Catalogue catalogue = new Catalogue();
		catalogue.setCode(catalogueCode);
		catalogue.setVersion("1.0");
		int id = catDao.insert(catalogue);
		catalogue.setId(id);
		
		actions.publishCompleted(catalogueCode, PublishLevel.MINOR);
		
		// no versions created
		assertEquals(1, catDao.getAll().size());
	}
	
	@Test
	public void publishMajorCompleted() {
		
		Catalogue catalogue = new Catalogue();
		catalogue.setCode(catalogueCode);
		catalogue.setVersion("1.0");
		int id = catDao.insert(catalogue);
		catalogue.setId(id);
		
		actions.publishCompleted(catalogueCode, PublishLevel.MAJOR);
		
		// no versions created
		assertEquals(1, catDao.getAll().size());
	}
}
