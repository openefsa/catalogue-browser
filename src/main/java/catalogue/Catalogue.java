package catalogue;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;
import javax.xml.soap.SOAPException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_browser_dao.ParentTermDAO;
import catalogue_browser_dao.ReleaseNotesDAO;
import catalogue_browser_dao.ReservedCatDAO;
import catalogue_browser_dao.TermAttributeDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_generator.ThreadFinishedListener;
import catalogue_object.Applicability;
import catalogue_object.Attribute;
import catalogue_object.BaseObject;
import catalogue_object.Hierarchy;
import catalogue_object.HierarchyBuilder;
import catalogue_object.Mappable;
import catalogue_object.Nameable;
import catalogue_object.Status.StatusValues;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import catalogue_object.Version;
import data_transformation.BooleanConverter;
import data_transformation.DateTrimmer;
import dcf_manager.Dcf;
import dcf_manager.Dcf.DcfType;
import dcf_user.User;
import detail_level.DetailLevelDAO;
import detail_level.DetailLevelGraphics;
import global_manager.GlobalManager;
import i18n_messages.CBMessages;
import import_catalogue.CatalogueImporter.ImportFileFormat;
import import_catalogue.CatalogueImporterThread;
import progress_bar.IProgressBar;
import property.SorterCatalogueObject;
import soap.UploadCatalogueFileImpl.ReserveLevel;
import term_code_generator.CodeGenerator;
import term_code_generator.TermCodeException;
import term_type.TermType;
import term_type.TermTypeDAO;
import utilities.GlobalUtil;

/**
 * Catalogue object, it contains the catalogue metadata, the catalogue terms,
 * hierarchies and attributes, term attributes and applicabilities.
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class Catalogue extends BaseObject implements Comparable<Catalogue>, Mappable, Cloneable, IDcfCatalogue {

	private static final Logger LOGGER = LogManager.getLogger(Catalogue.class);

	// date format of the catalogues
	public static final String ISO_8601_24H_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

	// version and status of local catalogues
	public static final String NOT_APPLICABLE_VERSION = "Not applicable";
	public static final String LOCAL_CATALOGUE_STATUS = "Local catalogue";

	// the catalogue version is specialized, we need to
	// manage it separately from the standard Version
	CatalogueVersion version;

	// catalogue meta data
	private String termCodeMask;
	private int termCodeLength;
	private String termMinCode;
	private boolean acceptNonStandardCodes;
	private boolean generateMissingCodes;
	private String catalogueGroups;
	private DcfType catalogueType; // is the catalogue a test or production catalogue?

	private String dbPath;
	private String backupDbPath; // path where it is located the backup of the catalogue db

	private boolean local; // if the catalogue is a new local catalogue or not

	// list of terms which are contained in the catalogue
	private HashMap<Integer, Term> terms;

	// cache for term ids to speed up finding
	// terms ids using term code without
	// iterating the entire collection of terms
	private HashMap<String, Integer> termsIds;

	// list of the hierarchies contained in the
	// catalogue (both base and attribute hierarchies)
	private ArrayList<Hierarchy> hierarchies;

	// list of the attributes contained in the
	// catalogue (only definitions, not values)
	private ArrayList<Attribute> attributes;

	// list of possible implicit facets (i.e. categories) for the terms
	// could be empty if the catalogue does not have
	// any implicit facet
	private ArrayList<Attribute> facetCategories;

	// detail levels of the catalogue
	public ArrayList<DetailLevelGraphics> detailLevels;

	// term types of the catalogue (could be empty)
	public ArrayList<TermType> termTypes;

	// the catalogue release notes
	private ReleaseNotes releaseNotes;

	// default values for detail level and term type
	private DetailLevelGraphics defaultDetailLevel;
	private TermType defaultTermType;

	private int forcedCount;

	public Catalogue() {
	}

	/**
	 * Constructor to create a catalogue object with all its variables
	 * 
	 * @param id
	 * @param code                   the catalogue code (unique)
	 * @param name                   the catalogue name (unique)
	 * @param label                  the catalogue label (text which is displayed)
	 * @param scopenotes
	 * @param termCodeMask
	 * @param termCodeLength
	 * @param termMinCode
	 * @param acceptNonStandardCodes
	 * @param generateMissingCodes
	 * @param version
	 * @param lastUpdate
	 * @param validFrom
	 * @param validTo
	 * @param status
	 * @param catalogueGroups
	 * @param deprecated
	 * @param dbFullPath
	 * @param backupDbPath
	 * @param local
	 * @param forcedCount
	 */
	public Catalogue(int id, DcfType catalogueType, String code, String name, String label, String scopenotes,
			String termCodeMask, String termCodeLength, String termMinCode, boolean acceptNonStandardCodes,
			boolean generateMissingCodes, String version, Timestamp lastUpdate, Timestamp validFrom, Timestamp validTo,
			String status, String catalogueGroups, boolean deprecated, String dbPath, String backupDbPath,
			boolean local, int forcedCount, ReleaseNotes releaseNotes) {

		// the id is not important for the catalogue
		super(id, code, name, label, scopenotes, version, lastUpdate, validFrom, validTo, status, deprecated);

		this.catalogueType = catalogueType;

		// set the version of the catalogue with the
		// extended Version
		this.version = new CatalogueVersion(version);

		this.termCodeMask = termCodeMask;

		// convert the term code length into integer if possible
		try {
			this.termCodeLength = Integer.parseInt(termCodeLength);
		} catch (NumberFormatException e) {
			LOGGER.error("Error, not integer ", e);
			e.printStackTrace();
			
			this.termCodeLength = 0;
		}

		this.termMinCode = termMinCode;
		this.acceptNonStandardCodes = acceptNonStandardCodes;
		this.generateMissingCodes = generateMissingCodes;
		this.catalogueGroups = catalogueGroups;
		this.dbPath = dbPath;
		this.backupDbPath = backupDbPath;
		this.local = local;
		this.forcedCount = forcedCount;
		this.releaseNotes = releaseNotes;

		// initialize memory for data
		terms = new HashMap<>();
		hierarchies = new ArrayList<>();
		attributes = new ArrayList<>();
		facetCategories = new ArrayList<>();
		detailLevels = new ArrayList<>();
		termTypes = new ArrayList<>();
		termsIds = new HashMap<>();
	}

	/**
	 * Refresh the catalogue metadata contents
	 */
	public void refresh() {
		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue recent = catDao.getById(getId());

		this.setCode(recent.getCode());
		this.setCatalogueVersion(recent.getCatalogueVersion());
		this.setName(recent.getName());
		this.setLabel(recent.getLabel());
		this.setScopenotes(recent.getScopenotes());
		this.termCodeMask = recent.getTermCodeMask();
		this.termCodeLength = recent.getTermCodeLength();
		this.termMinCode = recent.getTermMinCode();
		this.acceptNonStandardCodes = recent.isAcceptNonStandardCodes();
		this.generateMissingCodes = recent.isGenerateMissingCodes();
		this.setLastUpdate(recent.getLastUpdate());
		this.setValidFrom(recent.getValidFrom());
		this.setValidTo(recent.getValidTo());
		this.setStatus(recent.getStatus());
		this.catalogueGroups = recent.getCatalogueGroups();
		this.setDeprecated(recent.isDeprecated());
		this.forcedCount = recent.getForcedCount();
		this.local = recent.isLocal();
		this.backupDbPath = recent.getBackupDbPath();
		this.releaseNotes = recent.getReleaseNotes();
	}

	/**
	 * Load all the data related to the catalogue that is, hierarchies, terms,
	 * attributes, term attributes, applicabilities, detail levels and term types
	 */
	public void loadData() {

		// thread to load small data
		Thread baseThread = new Thread(new Runnable() {
			@Override
			public void run() {
				refreshHierarchies();
				refreshAttributes();
				refreshTermTypes();
				refreshDetailLevels();
				refreshReleaseNotes();
			}
		});

		// Thread to load terms
		Thread termThread = new Thread(new Runnable() {
			@Override
			public void run() {
				refreshTerms();
			}
		});

		// load terms and base information
		baseThread.start();
		termThread.start();

		// wait to finish, since the applicabilities
		// and term attributes need the terms in the
		// RAM memory
		try {
			baseThread.join();
			termThread.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		// refresh applicabilities and term attributes in parallel
		Thread applThread = new Thread(new Runnable() {
			@Override
			public void run() {
				refreshApplicabities();
			}
		});

		Thread taThread = new Thread(new Runnable() {
			@Override
			public void run() {
				refreshTermAttributes();
			}
		});

		applThread.start();
		taThread.start();

		// wait to finish
		try {
			taThread.join();
			applThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			LOGGER.error("Refresh failed for catalogue=" + this, e);
		}
	}

	/**
	 * Clear all the data of the catalogue that is, clear hierarchies, terms,
	 * attributes implicit facets, detail levels and term types
	 */
	public void clearData() {

		hierarchies.clear();
		attributes.clear();
		facetCategories.clear();

		if (terms != null) {
			for (Term term : terms.values()) {
				term.clear();
			}

			terms.clear();
		}

		detailLevels.clear();
		termTypes.clear();
		termsIds.clear();

		if (releaseNotes != null)
			releaseNotes.clear();
	}

	/**
	 * Clone the catalogue and return it
	 */
	public Catalogue cloneNewVersion(String newVersion) {

		// create the catalogue object and return it
		Catalogue catalogue = new Catalogue(getId(), catalogueType, getCode(), getName(), getLabel(), getScopenotes(),
				termCodeMask, String.valueOf(termCodeLength), termMinCode, acceptNonStandardCodes, generateMissingCodes,
				newVersion, getLastUpdate(), getValidFrom(), getValidTo(), getStatus(), catalogueGroups, isDeprecated(),
				null, backupDbPath, local, 0, releaseNotes);

		return catalogue;
	}

	/**
	 * Set the type of the catalogue. Test or production.
	 * 
	 * @param catalogueType
	 */
	public void setCatalogueType(DcfType catalogueType) {
		this.catalogueType = catalogueType;
	}

	/**
	 * Check if the catalogue is a test or a production catalogue
	 * 
	 * @return
	 */
	public DcfType getCatalogueType() {
		return catalogueType;
	}

	/**
	 * Open the current catalogue and load its data into ram Note that the user
	 * interface observes the changes in the current catalogue of the global
	 * manager, therefore when a current catalogue is set, the UI is automatically
	 * refreshed.
	 */
	public void open() {

		GlobalManager manager = GlobalManager.getInstance();

		// close the opened catalogue if there is one
		if (manager.getCurrentCatalogue() != null)
			manager.getCurrentCatalogue().closeQuitely();

		// load the catalogue data into RAM
		loadData();

		manager.setCurrentCatalogue(this);

	}

	/**
	 * Get the catalogue derby connection
	 * 
	 * @return
	 */
	public String getShutdownDBURL() {
		return "jdbc:derby:" + getDbPath() + ";user=dbuser;password=dbuserpwd;shutdown=true";
	}

	/**
	 * Close the connection with the catalogue db
	 */
	public void closeConnection() {

		// shutdown the connection, by default this operation throws an exception
		// but the command is correct! We close the connection since we close the db
		try {
			DriverManager.getConnection(getShutdownDBURL());
		} catch (SQLException e) {
			LOGGER.info("System shutted down with code : " + e.getErrorCode() + " and state " + e.getSQLState());
			LOGGER.info("Correct shutdown has code 45000 and state 08006 or XJ004");
		}
	}

	/**
	 * Close the catalogue
	 */
	public void close() {

		LOGGER.info("Closing " + this + " at " + getDbPath());

		// remove current catalogue
		GlobalManager manager = GlobalManager.getInstance();

		Catalogue current = manager.getCurrentCatalogue();

		// if the current catalogue is the one we are
		// closing => set the current catalogue as null
		if (current != null && current.sameAs(this))
			manager.setCurrentCatalogue(null);

		closeQuitely();
	}

	/**
	 * Close the catalogue without notifying the observers of the global manager
	 */
	public void closeQuitely() {
		// clear data in ram
		clearData();
		closeConnection();
	}

	/**
	 * Check if the current catalogue is opened or not
	 * 
	 * @return
	 */
	public boolean isOpened() {
		GlobalManager manager = GlobalManager.getInstance();

		Catalogue current = manager.getCurrentCatalogue();

		// if the opened catalogue is not this one => return
		if (current != null && current.getId() == this.getId()) {
			return true;
		}

		return false;
	}

	/**
	 * Refresh the UI with this catalogue. Note that you need to call
	 * {@link #open()} first
	 */
	public void refreshCatalogueUI() {

		GlobalManager manager = GlobalManager.getInstance();

		// if the opened catalogue is not this one => return
		if (!manager.getCurrentCatalogue().sameAs(this)) {
			LOGGER.warn("Cannot refresh catalogue UI: catalogue not opened " + this);
			return;
		}

		// notify the observers
		manager.setCurrentCatalogue(this);
	}

	/**
	 * Refresh the hierarchies contents
	 */
	public void refreshHierarchies() {

		HierarchyDAO hierDao = new HierarchyDAO(this);

		// initialize the hierarchies
		hierarchies = hierDao.getAll();
	}

	/**
	 * Get an hierarchy by its id. If not found => null
	 * 
	 * @param id
	 * @return
	 */
	public Hierarchy getHierarchyById(int id) {
		if(hasHierarchies()) {
			for (Hierarchy h : hierarchies) {
	
				if (h.getId() == id)
					return h;
			}
		}
		return null;
	}

	/**
	 * Get an hierarchy by its code. If not found => null
	 * 
	 * @param code
	 * @return
	 */
	public Hierarchy getHierarchyByCode(String code) {
		if(hasHierarchies()) {
			for (Hierarchy h : hierarchies) {
	
				if (h.getCode().equals(code))
					return h;
			}
		}

		return null;
	}

	/**
	 * Create the master hierarchy starting from the catalogue, since the master is
	 * actually the catalogue!
	 * 
	 * @return
	 */
	public Hierarchy createMasterHierarchy() {

		HierarchyBuilder builder = new HierarchyBuilder();
		builder.setCatalogue(this);
		builder.setCode(getCode());
		builder.setName(getName());
		builder.setLabel(getLabel());
		builder.setScopenotes(getScopenotes());
		builder.setApplicability("both");
		builder.setMaster(true);
		builder.setStatus(getStatus());
		builder.setGroups(getCatalogueGroups());
		builder.setLastUpdate(getLastUpdate());
		builder.setValidFrom(getValidFrom());
		builder.setValidTo(getValidTo());
		builder.setOrder(0);
		builder.setVersion(getVersion());
		builder.setDeprecated(isDeprecated());

		return builder.build();
	}

	/**
	 * Searches for the master hierarchy inside the hierarchies
	 * 
	 * @return
	 */
	public Hierarchy getMasterHierarchy() {

		// return if hierarchies is null
		if (hierarchies == null || hierarchies.isEmpty())
			return null;

		// get the master hierarchy from the hierarchies list
		for (Hierarchy hierarchy : hierarchies) {

			// if we found the master stop and return the hierarchy
			if (hierarchy.isMaster())
				return hierarchy;
		}

		// return null if no hierarchy was found
		return null;
	}

	/**
	 * Check if there are hierarchies or not
	 * 
	 * @return
	 */
	public boolean hasHierarchies() {
		return hierarchies != null && !hierarchies.isEmpty();
	}

	/**
	 * Check if we have attribute hierarchies or not (if they are present we can
	 * describe terms, otherwise no)
	 * 
	 * @return
	 */
	public boolean hasAttributeHierarchies() {

		for (Hierarchy hierarchy : hierarchies) {
			if (hierarchy.isFacet())
				return true;
		}

		return false;
	}

	/**
	 * Refresh the attributes contents
	 */
	public void refreshAttributes() {

		AttributeDAO attrDao = new AttributeDAO(this);
		attributes = attrDao.getAll();

		// refresh also the cache of implicit facets
		facetCategories = attrDao.getFacetCategories();
	}

	/**
	 * Get an attribute by its id
	 * 
	 * @param id
	 * @return
	 */
	public Attribute getAttributeById(int id) {

		Attribute attr = null;

		for (Attribute a : attributes) {
			if (a.getId() == id)
				attr = a;
		}

		return attr;
	}

	/**
	 * Get an attribute by its name
	 * 
	 * @author shahaal
	 * @param name
	 * @return
	 */
	public Attribute getAttributeByName(String name) {

		Attribute attr = null;

		for (Attribute a : attributes) {
			if (a.getName().equals(name))
				attr = a;
		}

		return attr;
	}

	/**
	 * Get an attribute by its code
	 * 
	 * @author shahaal
	 * @param code
	 * @return
	 */
	public Attribute getAttributeByCode(String code) {

		Attribute attr = null;

		for (Attribute a : attributes) {
			if (a.getCode().equals(code))
				attr = a;
		}

		return attr;
	}

	/**
	 * the method find the implicit facet attribute
	 * 
	 * @author shahaal
	 * @return
	 */
	public Attribute findImplicitFacetsAttribute() {

		for (Attribute attr : getAttributes()) {
			if (attr.isImplicitFacet()) {
				return attr;
			}
		}

		return null;
	}

	/**
	 * Add a new term into the hashmap of terms
	 * 
	 * @param id
	 * @param term
	 */
	public void addTerm(Term term) {
		terms.put(term.getId(), term);
	}

	/**
	 * Check if some terms are present in the catalogue
	 * 
	 * @return
	 */
	public boolean hasTerms() {
		return terms != null && !terms.isEmpty();
	}

	/**
	 * Refresh the terms contents
	 */
	public void refreshTerms() {

		TermDAO termDao = new TermDAO(this);

		// initialise the terms
		terms = termDao.fetchTerms();

		termsIds = new HashMap<>();

		// update cache of ids
		for (Term term : terms.values()) {
			termsIds.put(term.getCode(), term.getId());
		}
	}

	/**
	 * Refresh the parent child relationships of the catalogue terms Need to be
	 * called after {@linkplain Catalogue#refreshHierarchies} and
	 * {@linkplain Catalogue#refreshTerms}
	 */
	public void refreshApplicabities() {

		// remove applicabilities
		for (Term term : terms.values())
			term.clearApplicabilities();

		ParentTermDAO parentDao = new ParentTermDAO(this);

		// add applicabilities
		for (Applicability appl : parentDao.getAll()) {
			Term term = appl.getChild();
			term.addApplicability(appl);
		}
	}

	/**
	 * Refresh the term attributes and their values. Need to be called after
	 * {@linkplain Catalogue#refreshTerms} and
	 * {@linkplain Catalogue#refreshAttributes}
	 */
	public void refreshTermAttributes() {

		// reset all the attributes of each term
		for (Term term : terms.values()) {
			term.clearAttributes();
		}

		// load the attributes values for the terms
		TermAttributeDAO taDao = new TermAttributeDAO(this);

		// set the term attributes to the terms
		for (TermAttribute ta : taDao.getAll()) {
			Term term = ta.getTerm();
			term.addAttribute(ta);
		}
	}

	/**
	 * Refresh the catalogue release notes
	 */
	public void refreshReleaseNotes() {
		ReleaseNotesDAO rnDao = new ReleaseNotesDAO(this);
		releaseNotes = rnDao.getReleaseNotes();
	}

	/**
	 * Get the catalogue release notes
	 * 
	 * @return
	 */
	public ReleaseNotes getReleaseNotes() {
		return releaseNotes;
	}

	/**
	 * Refresh the detail levels of the catalogue
	 */
	public void refreshDetailLevels() {

		DetailLevelDAO detailDao = new DetailLevelDAO();

		detailLevels = detailDao.getAll();

		defaultDetailLevel = new DetailLevelGraphics("", CBMessages.getString("DetailLevel.DefaultValue"), null);

		// add void detail level
		detailLevels.add(defaultDetailLevel);
	}

	/**
	 * Get the default detail level of the catalogue
	 * 
	 * @return
	 */
	public DetailLevelGraphics getDefaultDetailLevel() {
		return defaultDetailLevel;
	}

	/**
	 * Refresh the term types of the catalogue if there are some.
	 */
	public void refreshTermTypes() {

		TermTypeDAO ttDao = new TermTypeDAO(this);

		termTypes = ttDao.getAll();

		defaultTermType = new TermType(-1, "", CBMessages.getString("TermType.DefaultValue"));

		// add void term type
		termTypes.add(defaultTermType);
	}

	/**
	 * Get a term type by its id
	 * 
	 * @param id
	 * @return
	 */
	public TermType getTermTypeById(int id) {

		TermType tt = null;

		for (TermType type : termTypes) {
			if (type.getId() == id)
				tt = type;
		}

		return tt;
	}

	/**
	 * Get the default term type of the catalogue
	 * 
	 * @return
	 */
	public TermType getDefaultTermType() {
		return defaultTermType;
	}

	/**
	 * Check if the catalogue supports term types
	 * 
	 * @return
	 */
	public boolean hasTermTypes() {
		// since term types always have the
		// none term type, the list is empty
		// if we have one or less term types
		return !(termTypes.size() <= 1);
	}

	/**
	 * Check if the catalogue has some implicit generic attributes
	 * 
	 * @return
	 */
	public boolean hasGenericAttributes() {
		AttributeDAO attrDao = new AttributeDAO(this);
		return !attrDao.fetchGeneric().isEmpty();
	}

	/**
	 * Get all the hierarchies of the catalogue
	 * 
	 * @return
	 */
	public ArrayList<Hierarchy> getHierarchies() {
		return hierarchies;
	}

	/**
	 * Get all the facet hierarchies of the catalogue
	 * 
	 * @return
	 */
	public ArrayList<Hierarchy> getFacetHierarchies() {
		ArrayList<Hierarchy> facets = new ArrayList<>();
		for (Hierarchy hierarchy : hierarchies) {
			if (hierarchy.isFacet())
				facets.add(hierarchy);
		}

		// sort facets
		SorterCatalogueObject sorter = new SorterCatalogueObject();
		Collections.sort(facets, sorter);

		return facets;
	}

	/**
	 * Get all the catalogue terms
	 * 
	 * @return
	 */
	public Collection<Term> getTerms() {
		return terms.values();
	}

	/**
	 * Get all the catalogue attributes
	 * 
	 * @return
	 */
	public ArrayList<Attribute> getAttributes() {
		return attributes;
	}

	/**
	 * Get the generic attributes of the catalogue
	 * 
	 * @return
	 */
	public Collection<Attribute> getGenericAttributes() {

		Collection<Attribute> attrs = new ArrayList<>();
		for (Attribute attr : attributes) {
			if (attr.isGeneric())
				attrs.add(attr);
		}

		return attrs;
	}

	/**
	 * Get all the implicit facets of the catalogue
	 * 
	 * @return
	 */
	public ArrayList<Attribute> getFacetCategories() {
		return facetCategories;
	}

	/**
	 * Get all the in use facet categories
	 * 
	 * @return
	 */
	public Collection<Attribute> getInUseFacetCategories() {

		Collection<Attribute> categories = new ArrayList<>();
		Collection<Hierarchy> notUsed = getNotUsedHierarchies();

		if (facetCategories == null)
			return categories;

		// for each category
		for (Attribute attr : facetCategories) {

			// if is not present in the not used hierarchies
			// add it
			if (!notUsed.contains(attr.getHierarchy()))
				categories.add(attr);
		}

		return categories;
	}

	/**
	 * Check if the catalogue has usable implicit facets categories or not
	 * 
	 * @return
	 */
	public boolean hasImplicitFacetCategories() {
		return facetCategories != null && !facetCategories.isEmpty();
	}

	/**
	 * Get the detail levels of the catalogue
	 * 
	 * @return
	 */
	public ArrayList<DetailLevelGraphics> getDetailLevels() {
		return detailLevels;
	}

	/**
	 * Get the term types of the catalogue. Could be empty.
	 * 
	 * @return
	 */
	public ArrayList<TermType> getTermTypes() {
		return termTypes;
	}

	/**
	 * Check if the catalogue has data or not
	 */
	public boolean isEmpty() {

		// if there are not hierarchies and terms
		// then we have no data
		// so the catalogue is a new one
		return !hasHierarchies() && !hasTerms();
	}

	/**
	 * Add a new term using the term code mask as code generator
	 * 
	 * @param parent
	 * @param hierarchy
	 * @return the new term
	 * @throws TermCodeException
	 */
	public Term addNewTerm(Nameable parent, Hierarchy hierarchy) throws TermCodeException {

		// get the a new code for the term using the catalogue term code mask
		CodeGenerator generator = new CodeGenerator();
		String code = generator.getTermCode(termCodeMask);

		return addNewTerm(code, parent, hierarchy);
	}

	/**
	 * Add a new term into the catalogue database as child of the term "parent" in
	 * the selected hierarchy. The term is a complete term since we save the term,
	 * all its attributes and its applicability.
	 * 
	 * @author shahaal
	 * @param code             the code of the new term
	 * @param parent           the parent of the new term
	 * @param hierarchy        the hierarchy in which the term is added to the
	 *                         parent
	 * @param termExtendedName
	 * @param scopeNotes
	 * @param scientificName
	 * @return the new term
	 */
	public Term addNewTerm(String code, Nameable parent, Hierarchy hierarchy, String termExtendedName,
			String scopeNotes, String scientificName) {

		TermDAO termDao = new TermDAO(this);

		// get a new default term
		Term child = Term.getDefaultTerm(code);

		// set terms string values
		child.setName(termExtendedName);
		child.setDisplayAs("");
		child.setScopenotes(scopeNotes);

		// set term level of detail E stands for ExtendedTerm
		child.setDetailLevelValue("E");

		// split the scientific name
		TermAttribute ta = TermAttribute.getDefaultTermAttribute(child);
		ta.setValue(scientificName);

		// get the names in scientific name field (delim = $)
		for (String name : ta.getRepeatableValues()) {
			TermAttribute taTemp = TermAttribute.getDefaultTermAttribute(child);
			taTemp.setValue(name);
			child.addAttribute(taTemp);
		}

		// insert the new term into the database and get the id
		int id = termDao.insert(child);
		child.setId(id);

		// initialise term attribute dao
		TermAttributeDAO taDao = new TermAttributeDAO(this);

		// insert the term attributes of the term
		taDao.updateByA1(child);

		ParentTermDAO parentDao = new ParentTermDAO(this);

		/*
		 * comment for removing specific auto insertion ONLY for MASTER & Hierarchy id
		 * (based on order)
		 */
		ArrayList<Hierarchy> hierarchies = new ArrayList<>();
		hierarchies.add(hierarchy);// master
		// hierarchies.add(getHierarchyById(9));//botanical
		hierarchies.add(getHierarchyById(11));// source

		for (Hierarchy h : hierarchies) {

			// get the first available order integer under the parent term
			// in the selected hierarchy
			int order = parentDao.getNextAvailableOrder(parent, h);

			// create the term applicability for the term in the selected hierarchy
			// we set the new term as child of the selected term
			// we set it to reportable as default
			Applicability appl = new Applicability(child, parent, h, order, true);

			// add permanently the new applicability to the child
			child.addApplicability(appl, true);

		}

		// update the involved terms in RAM
		termDao.update(child);

		if (parent instanceof Term)
			termDao.update((Term) parent);

		// add the term to the hashmap
		terms.put(id, child);

		// update also the ids cache
		termsIds.put(code, id);

		return child;
	}

	/**
	 * Add a new term into the catalogue database as child of the term "parent" in
	 * the selected hierarchy. The term is a complete term since we save the term,
	 * all its attributes and its applicability.
	 * 
	 * @param code      the code of the new term
	 * @param parent    the parent of the new term
	 * @param hierarchy the hierarchy in which the term is added to the parent
	 * @return the new term
	 */
	public Term addNewTerm(String code, Nameable parent, Hierarchy hierarchy) {

		TermDAO termDao = new TermDAO(this);

		// get a new default term
		Term child = Term.getDefaultTerm(code);

		// insert the new term into the database and get the new term
		// with the id set
		int id = termDao.insert(child);
		child.setId(id);

		// initialize term attribute dao
		TermAttributeDAO taDao = new TermAttributeDAO(this);

		// insert the term attributes of the term
		taDao.updateByA1(child);

		ParentTermDAO parentDao = new ParentTermDAO(this);

		// get the first available order integer under the parent term
		// in the selected hierarchy
		int order = parentDao.getNextAvailableOrder(parent, hierarchy);

		// create the term applicability for the term in the selected hierarchy
		// we set the new term as child of the selected term
		// we set it to reportable as default
		Applicability appl = new Applicability(child, parent, hierarchy, order, true);

		// add permanently the new applicability to the child
		child.addApplicability(appl, true);

		// update the involved terms in RAM
		termDao.update(child);

		if (parent instanceof Term)
			termDao.update((Term) parent);

		// add the term to the hashmap
		terms.put(id, child);

		// update also the ids cache
		termsIds.put(code, id);

		return child;
	}

	/**
	 * Get a term by its id
	 * 
	 * @param id the term id
	 */
	public Term getTermById(Integer id) {

		Term term = terms.get(id);

		if (term == null) {
			LOGGER.error("Term with id " + id + " not found in catalogue " + this);
		}

		// get the term with the key
		// if not found => null
		return term;
	}

	/**
	 * Get a term by its code
	 * 
	 * @param code
	 * @return
	 */
	public Term getTermByCode(String code) {

		// get the term id from the cache
		int id = termsIds.get(code);

		// get the term by its id
		return getTermById(id);
	}

	/**
	 * Check if the catalogue has the detail level attribute or not
	 * 
	 * @return
	 */
	public boolean hasDetailLevelAttribute() {

		for (Attribute attr : attributes) {
			if (attr.isDetailLevel())
				return true;
		}

		return false;
	}

	/**
	 * Check if the catalogue has the term type attribute or not
	 * 
	 * @return
	 */
	public boolean hasTermTypeAttribute() {

		for (Attribute attr : attributes) {
			if (attr.isTermType())
				return true;
		}

		return false;
	}

	/**
	 * Check if the version of the catalogue is invalid or not
	 * 
	 * @return
	 */
	public boolean isInvalid() {
		return version.isInvalid();
	}

	/**
	 * update the status of the catalogue
	 * 
	 * @param value
	 */
	public void setStatus(StatusValues value) {
		getRawStatus().markAs(value);
	}

	/**
	 * Check if the catalogue can be reserved by the current user. To check
	 * unreservability, please use {@link isUnreservable}
	 * 
	 * @return
	 */
	public boolean isReservable() {

		CatalogueStatus prob = getCatalogueStatus();

		return prob == CatalogueStatus.NONE;
	}

	/**
	 * Enum used to get the specific problem which is blocking a reserve action on
	 * this catalogue
	 * 
	 * @author avonva
	 *
	 */
	public enum CatalogueStatus {
		NONE, INVALID, PENDING_ACTION_ONGOING, RESERVED_BY_CURRENT, RESERVED_BY_OTHER, NOT_LAST, LOCAL, DEPRECATED,
		FORCED_EDIT
	};

	/**
	 * Get the status of the catalogue
	 * 
	 * @return
	 */
	public CatalogueStatus getCatalogueStatus() {

		CatalogueStatus problem = CatalogueStatus.NONE;

		// if the catalogue is reserved by someone which is not me
		// then we cannot reserve
		boolean reservedByOther = !isReservedBy(User.getInstance()) && isReserved();

		// no problem if no user had reserved the catalogue
		// and the catalogue is the last available
		// version of the catalogue and the catalogue
		// is not local and it is not deprecated
		if (isInvalid())
			problem = CatalogueStatus.INVALID;
		// else if ( isRequestingAction() )
		// problem = CatalogueStatus.PENDING_ACTION_ONGOING;
		else if (reservedByOther)
			problem = CatalogueStatus.RESERVED_BY_OTHER;
		else if (isReservedBy(User.getInstance()))
			problem = CatalogueStatus.RESERVED_BY_CURRENT;
		else if (hasUpdate())
			problem = CatalogueStatus.NOT_LAST;
		else if (local)
			problem = CatalogueStatus.LOCAL;
		else if (isDeprecated())
			problem = CatalogueStatus.DEPRECATED;
		// else if ( isForceEdit( username ) )
		// problem = CatalogueStatus.FORCED_EDIT;

		return problem;
	}

	/**
	 * Get the catalogue term code mask if there is one
	 * 
	 * @return
	 */
	public String getTermCodeMask() {
		return termCodeMask;
	}

	/**
	 * Get the catalogue term code length
	 * 
	 * @return
	 */
	public int getTermCodeLength() {
		return termCodeLength;
	}

	/**
	 * Get the code which is the starting point for creating new codes
	 * 
	 * @return
	 */
	public String getTermMinCode() {
		return termMinCode;
	}

	/**
	 * Check if the catalogue accepts non standard codes for the terms
	 * 
	 * @return
	 */
	public boolean isAcceptNonStandardCodes() {
		return acceptNonStandardCodes;
	}

	/**
	 * Check if the catalogue can generate missing codes or not
	 * 
	 * @return
	 */
	public boolean isGenerateMissingCodes() {
		return generateMissingCodes;
	}

	/**
	 * Get the catalogue groups (single string $ separated)
	 * 
	 * @return
	 */
	public String getCatalogueGroups() {
		return catalogueGroups;
	}

	/**
	 * Set if the catalogue is a local catalogue or not
	 * 
	 * @param local
	 */
	public void setLocal(boolean local) {
		this.local = local;
	}

	/**
	 * Is the database local? True if the database was created through the command
	 * "new local catalogue", false otherwise
	 * 
	 * @return
	 */
	public boolean isLocal() {
		return local;
	}

	/**
	 * Get the reserved catalogue object if present.
	 * 
	 * @return
	 */
	private ReservedCatalogue getReservedCatalogue() {

		ReservedCatDAO resDao = new ReservedCatDAO();

		return resDao.getById(getId());
	}

	/**
	 * Check if the catalogue is reserved or not
	 * 
	 * @return
	 */
	public boolean isReserved() {

		// if the catalogue is present into the
		// reserved catalogue table it is reserved
		return getReservedCatalogue() != null;
	}

	/**
	 * Check if the catalogue is reserved or not by the user with username passed in
	 * input
	 * 
	 * @return
	 */
	public boolean isReservedBy(User user) {

		ReservedCatalogue rc = getReservedCatalogue();

		// if not present the catalogue is not reserved
		if (rc == null)
			return false;

		// check that the user who reserved the catalogue
		// is the one passed in input
		return rc.getUsername().equals(user.getUsername());
	}

	/**
	 * Set the number of times that we have forced the editing of this catalogue
	 * 
	 * @param forcedCount
	 */
	public void setForcedCount(int forcedCount) {
		this.forcedCount = forcedCount;
	}

	/**
	 * Get how many times we had forced the editing of this catalogue.
	 * 
	 * @return
	 */
	public int getForcedCount() {
		return forcedCount;
	}

	/**
	 * Get the reserve level of the catalogue if present
	 * 
	 * @return
	 */
	public ReserveLevel getReserveLevel() {

		ReserveLevel level = null;

		ReservedCatalogue rc = getReservedCatalogue();

		// if catalogue is reserved get the level
		if (rc != null)
			level = rc.getLevel();

		return level;
	}

	/**
	 * Get the username of the user who reserved the catalogue (if there is one)
	 * 
	 * @return
	 */
	public String getReserveUsername() {

		String username = null;

		ReservedCatalogue rc = getReservedCatalogue();

		// if catalogue is reserved get the level
		if (rc != null)
			username = rc.getUsername();

		return username;
	}

	/**
	 * Get the reserve note if present
	 * 
	 * @return
	 */
	public String getReserveNote() {

		String note = null;
		ReservedCatalogue rc = getReservedCatalogue();

		if (rc != null)
			note = rc.getNote();

		return note;
	}

	/**
	 * Set the catalogue version
	 * 
	 * @param version
	 */
	public void setCatalogueVersion(CatalogueVersion version) {
		this.version = version;
	}

	/**
	 * Get the catalogue version
	 * 
	 * @return
	 */
	public CatalogueVersion getCatalogueVersion() {
		return version;
	}

	/**
	 * Get the catalogue version in string format
	 */
	@Override
	public String getVersion() {
		if (version != null)
			return version.getVersion();
		else
			return null;
	}

	@Override
	public void setVersion(String version) {
		setCatalogueVersion(new CatalogueVersion(version));
	}

	@Override
	public void setRawVersion(Version version) {
		LOGGER.error("setRawVersion not supported by Catalogue, use setCatalogueVersion instead");
	}

	@Override
	public Version getRawVersion() {
		LOGGER.error("getRawVersion not supported by Catalogue, use getCatalogueVersion instead");
		return null;
	}

	/**
	 * Get the directory which contains the database of the catalogue
	 * 
	 * @return
	 */
	/*
	 * public String getDBDir() { return dbDir; }
	 */

	/**
	 * Get the backup path
	 * 
	 * @return
	 */
	public String getBackupDbPath() {
		return backupDbPath;
	}

	/**
	 * Set the backup db path (the db which will be used as backup of this catalogue
	 * if needed)
	 * 
	 * @param backupDbPath
	 */
	public void setBackupDbPath(String backupDbPath) {
		this.backupDbPath = backupDbPath;
	}

	/**
	 * Get then main folder which contains the catalogue database, which can be the
	 * local folder, the production folder or test folder
	 * 
	 * @return
	 */
	public String getDbMainDir() {

		String folder = null;

		// if local catalogue => local cat dir, otherwise main cat dir
		// depending on the catalogue type
		if (local)
			folder = DatabaseManager.LOCAL_CAT_DB_FOLDER;
		else if (catalogueType == DcfType.PRODUCTION)
			folder = DatabaseManager.PRODUCTION_CAT_DB_FOLDER;
		else
			folder = DatabaseManager.TEST_CAT_DB_FOLDER;

		return folder;
	}

	/**
	 * Build the full db path of the catalogue with default dir settings create also
	 * the directory in which the db will be created
	 * 
	 * @param catalogue
	 * @return
	 */
	public boolean createDbDir() {
		return GlobalUtil.createDirectory(getDbPath());
	}

	/**
	 * Get the db folder with a file object
	 * 
	 * @return
	 */
	public File getDbFolder() {
		File file = new File(getDbPath());
		return file;
	}

	/**
	 * Get the catalogue db path
	 * 
	 * @return
	 */
	public String getDbPath() {

		if (dbPath == null)
			dbPath = computeDbPath();

		return dbPath;
	}

	/**
	 * Get the complete database path of the catalogue
	 * 
	 * @return
	 */
	private String computeDbPath() {

		String path = getDbMainDir() + System.getProperty("file.separator") + "CAT_" + getCode() + "_DB"
				+ System.getProperty("file.separator");

		// if local catalogue => we return only the db code as name
		if (local)
			return path + getCode();

		// if instead we have an official catalogue, set also the version

		// name of the catalogue db
		String dbName = getCode() + "_VERSION_" + version.getVersion();

		return path + dbName;
	}

	/**
	 * Get the full path of the db ( directory + dbname ) and set it as the path of
	 * the catalogue (we build it!)
	 * 
	 * @return
	 */
	/*
	 * public String buildDBFullPath( String dbDir ) {
	 * 
	 * dbFullPath = getDbFullPath ( dbDir, getCode(), getVersion(), local ); return
	 * dbFullPath; }
	 */
	/**
	 * Set the full db path directly
	 * 
	 * @param dbFullPath
	 */
	/*
	 * public void setDbFullPath(String dbFullPath) { this.dbFullPath = dbFullPath;
	 * }
	 */

	/**
	 * Get the db full path using the catalogues main directory, the catalogue
	 * code/version and if the catalogue is local or not For non local catalogues
	 * the folder will be code + "_VERSION_" + version for local catalogues instead
	 * we use only the code (the version is not applicable)
	 * 
	 * @param dbDir,  the folder in which we will create the catalogue folder and
	 *                then the database
	 * @param code
	 * @param version
	 * @param local
	 * @return
	 */
	/*
	 * public static String getDbFullPath ( String dbDir, String code, String
	 * version, boolean local ) {
	 * 
	 * // if local catalogue => we return only the db code as name if ( local )
	 * return dbDir + System.getProperty( "file.separator" ) + code;
	 * 
	 * // if instead we have an official catalogue, set also the version
	 * 
	 * // name of the catalogue db String dbName = code + "_VERSION_" + version;
	 * 
	 * // create the full path and assign it return dbDir + System.getProperty(
	 * "file.separator" ) + dbName; }
	 */

	/**
	 * Get the path of the Db of the catalogue null if it was not set (you have to
	 * use buildDBFullPath first!) see also {@link #buildDBFullPath( String dbDir )
	 * buildDBFullPath}
	 * 
	 * @return
	 */
	/*
	 * public String getDbFullPath() { return dbFullPath; }
	 */

	/**
	 * Get the default hierarchy for this catalogue, default is the master The
	 * master hierarchy can be hidden to users. For this reason it is necessary to
	 * define a new default hierarchy in these cases. The default hierarchy is
	 * usually the master hierarchy, but this can be overridden setting after the
	 * catalogue scopenote: $hideMasterWith=newDefaultHierarchyCode If it is not
	 * found, we return the master hierarchy
	 * 
	 * @return the default hierarchy
	 */
	public Hierarchy getDefaultHierarchy() {

		// set the initial value for the default hierarchy
		Hierarchy defaultHierarchy = getMasterHierarchy();

		String hierarchyCode = getTokenByKey("defaultHierarchy");

		if (hierarchyCode == null)
			return defaultHierarchy;

		// get the hierarchy
		Hierarchy temp = getHierarchyByCode(hierarchyCode);

		if (temp != null)
			defaultHierarchy = temp;

		return defaultHierarchy;
	}

	/**
	 * Get a token value from the catalogue scopenotes. Syntax of scopenotes tokens:
	 * scopenotes$[tokenKey=value1,value2,...] If multiple tokens are used the
	 * syntax is the following:
	 * scopenotes$[tokenKey1=value1,value2,...][tokenKey2=value1,value2,...]...
	 * 
	 * @param key the key of the token we are interested in
	 * @return the list of token values comma separated
	 */
	private String getTokenByKey(String key) {

		// split based on $
		String[] split = getScopenotes().split("\\$");

		// if only one element return (we have only the scopenotes)
		if (split.length <= 1)
			return null;

		// get pattern of not used hierarchies
		split = split[1].split("\\[" + key);

		if (split.length <= 1)
			return null;

		// remove ] from pattern and get only the interested part
		split = split[1].split("\\]");

		// if no elements return
		if (split.length < 0)
			return null;

		// remove equal sign and spaces
		String codes = split[0];
		codes = codes.replaceAll("=", "");
		codes = codes.trim();

		return codes;
	}

	/**
	 * Get all the hierarchies which are currently in use
	 * 
	 * @return
	 */
	public Collection<Hierarchy> getInUseHierarchies() {

		Collection<Hierarchy> inUse = new ArrayList<>();

		// get all hierarchies
		inUse.addAll(hierarchies);

		// remove the not used
		for (Hierarchy notUsed : getNotUsedHierarchies())
			inUse.remove(notUsed);

		return inUse;
	}

	/**
	 * Get the hierarchies which should not be showed to the read only used. Syntax
	 * in the catalogue scopenotes: scopenotes$[notUsedHierarchies=code1,code2,...]
	 * 
	 * @return
	 */
	public Collection<Hierarchy> getNotUsedHierarchies() {

		Collection<Hierarchy> notUsed = new ArrayList<>();

		// if catalogue manager show everything
		if (User.getInstance().isCatManager())
			return notUsed;

		// not used also for deprecated hierarchies
		for (Hierarchy h : hierarchies) {
			if (h.isDeprecated()) {
				notUsed.add(h);
			}
		}

		String codes = getTokenByKey("notUsedHierarchies");

		if (codes == null)
			return notUsed;

		// for each hierarchy comma separated
		StringTokenizer st = new StringTokenizer(codes, ",");
		while (st.hasMoreTokens()) {

			String hierarchyCode = st.nextToken();

			// return the default hierarchy
			Hierarchy temp = getHierarchyByCode(hierarchyCode);

			if (temp == null) {
				LOGGER.error("Catalogue scopenote - " + "NotUsedHierarchies: Hierarchy with code " + hierarchyCode
						+ " not found!");
				continue;
			}

			// add the not used hierarchy
			if (!notUsed.contains(temp))
				notUsed.add(temp);
		}

		return notUsed;
	}

	/**
	 * Get a catalogue field value using the DB columns names column name
	 * 
	 * @param key
	 * @return
	 */
	public String getValueByKey(String key) {

		String value = "";

		switch (key) {
		case "CAT_CODE":
			value = getCode();
			break;
		case "CAT_NAME":
			value = getName();
			break;
		case "CAT_LABEL":
			value = getLabel();
			break;
		case "CAT_SCOPENOTE":
			value = getScopenotes();
			break;
		case "CAT_TERM_CODE_MASK":
			value = termCodeMask;
			break;
		case "CAT_TERM_CODE_LENGTH":
			if (termCodeLength == 0)
				value = "";
			else
				value = String.valueOf(termCodeLength);
			break;
		case "CAT_TERM_MIN_CODE":
			value = termMinCode;
			break;
		case "CAT_ACCEPT_NON_STANDARD_CODES":
			value = String.valueOf(acceptNonStandardCodes);
			break;
		case "CAT_GENERATE_MISSING_CODES":
			value = String.valueOf(generateMissingCodes);
			break;
		case "CAT_VERSION":
			value = getVersion();
			break;
		case "CAT_GROUPS":
			value = catalogueGroups;
			break;
		case "CAT_LAST_UPDATE":
			if (getLastUpdate() != null)
				value = DateTrimmer.dateToString(getLastUpdate());
			break;
		case "CAT_VALID_FROM":
			if (getValidFrom() != null)
				value = DateTrimmer.dateToString(getValidFrom());
			break;
		case "CAT_VALID_TO":
			if (getValidTo() != null)
				value = DateTrimmer.dateToString(getValidTo());
			break;
		case "CAT_STATUS":
			value = getStatus();
			break;
		case "CAT_DEPRECATED":
			value = BooleanConverter.toNumericBoolean(String.valueOf(isDeprecated()));
			break;
		case "CAT_RN_DESCRIPTION":
			if (releaseNotes != null && releaseNotes.getDescription() != null)
				value = releaseNotes.getDescription();
			break;
		case "CAT_RN_VERSION_DATE":
			if (releaseNotes != null && releaseNotes.getDate() != null)
				value = DateTrimmer.dateToString(releaseNotes.getDate());
			break;
		case "CAT_RN_INTERNAL_VERSION":
			if (releaseNotes != null && releaseNotes.getInternalVersion() != null)
				value = releaseNotes.getInternalVersion();
			break;
		case "CAT_RN_INTERNAL_VERSION_NOTE":
			if (releaseNotes != null && releaseNotes.getInternalVersionNote() != null)
				value = releaseNotes.getInternalVersionNote();
			break;
		default:
			break;
		}

		return value;
	}

	/**
	 * Create a default catalogue object (for new catalogues)
	 * 
	 * @param catalogueCode
	 * @return
	 */
	public static Catalogue getDefaultCatalogue(String catalogueCode) {

		CatalogueBuilder builder = new CatalogueBuilder();
		builder.setCode(catalogueCode);
		builder.setName(catalogueCode);
		builder.setLabel(catalogueCode);
		builder.setVersion(NOT_APPLICABLE_VERSION);
		builder.setStatus(LOCAL_CATALOGUE_STATUS);

		// this is ignored for local catalogues,
		// but it is easier to manage the case
		// assigning a value to the variable
		builder.setCatalogueType(DcfType.LOCAL);

		builder.setLocal(true);

		return builder.build();
	}

	/**
	 * Get the last downloaded version of the catalogue
	 * 
	 * @return
	 */
	public Catalogue getLastVersion() {

		CatalogueDAO catDao = new CatalogueDAO();

		return catDao.getLastVersionByCode(getCode(), catalogueType);
	}

	/**
	 * Get the dummy cat users catalogue. We use this catalogue to provide the right
	 * authorization accesses to users
	 * 
	 * @return
	 */
	public static Catalogue getCatUsersCatalogue() {

		String code = "CATUSERS";

		CatalogueBuilder builder = new CatalogueBuilder();
		builder.setCode(code);
		builder.setName(code);
		builder.setLabel(code);
		builder.setCatalogueType(Dcf.dcfType);
		builder.setVersion("");

		return builder.build();
	}

	/**
	 * Check if the catalogue is the cat users catalogue we hide this catalogue from
	 * read only users.
	 * 
	 * @return
	 */
	public boolean isCatUsersCatalogue() {
		return getCode().equals("CATUSERS");
	}

	/**
	 * Check if the catalogue is the MTX catalogue or not If we have the MTX then
	 * several conditions will apply, such as the enabling of the business rules
	 * 
	 * @return
	 */
	public boolean isMTXCatalogue() {
		Hierarchy h = getDefaultHierarchy();
		boolean hasReporting = h != null && h.toString().contains("Reporting hierarchy");
		boolean containsMTX = getCode().equals("MTX") || (isLocal() && getCode().contains("MTX_"));
		return containsMTX || hasReporting;
	}

	/**
	 * Get the newest version of the catalogue (if there is one, otherwise null)
	 * 
	 * @return
	 */
	public Catalogue getUpdate() {

		if (isLastRelease())
			return null;

		return getLastRelease();
	}

	/**
	 * Get the last release of the catalogue. If we already have the last release
	 * then it will be returned as it is.
	 * 
	 * @return
	 */
	public Catalogue getLastRelease() {

		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue lastLocalVersion = catDao.getLastVersionByCode(getCode(), catalogueType);

		Catalogue lastPublishedRelease = Dcf.getLastPublishedRelease(this);

		// if not found return the local version (for local catalogues)
		if (lastPublishedRelease == null)
			return lastLocalVersion;

		// if local < published
		if (lastLocalVersion.isOlder(lastPublishedRelease))
			return lastPublishedRelease;
		else
			return lastLocalVersion;
	}

	/**
	 * Check if this catalogue is the last PUBLISHED release or not.
	 * 
	 * @return
	 */
	public boolean isLastRelease() {

		// get the last update
		Catalogue last = getLastRelease();

		// if this catalogue is older than the last
		// release => this is not the last release
		if (last != null && this.isOlder(last)) {
			return false;
		}

		return true;
	}

	public void increaseForcedCount() {
		this.forcedCount++;
	}

	/**
	 * Download the catalogue from the dcf and save it on the disk
	 * 
	 * @throws SOAPException
	 * @return the file where the downloaded file is placed
	 */
	public File download() throws SOAPException, AttachmentNotFoundException {

		LOGGER.info("Downloading " + this);

		// ask for exporting catalogue to the dcf
		// export the catalogue and save its attachment into an xml file
		Dcf dcf = new Dcf();

		File file = dcf.exportCatalogue(this);

		if (file == null || !file.exists()) {
			throw new AttachmentNotFoundException();
		}

		// set the catalogue type according to the dcf one
		catalogueType = Dcf.dcfType;

		return file;
	}

	/**
	 * Import a catalogue in .xml format
	 * 
	 * @param file
	 * @param progressBar
	 * @param doneListener
	 */
	public void makeXmlImport(final File file, IProgressBar progressBar, double maxProgress,
			final ThreadFinishedListener doneListener) {

		LOGGER.info("make Xml Import");
		
		CatalogueImporterThread importCat = new CatalogueImporterThread(file, ImportFileFormat.XML);

		if (progressBar != null)
			importCat.setProgressBar(progressBar, maxProgress);

		// set the listener
		importCat.addDoneListener(new ThreadFinishedListener() {

			@Override
			public void finished(Thread thread, int code, Exception exception) {

				// TODO remove temporary downloaded xml file if needed
				try {
					GlobalUtil.deleteFileCascade(file);
				} catch (IOException e) {
					LOGGER.error("Error during delete ", e);
					e.printStackTrace();
				}

				if (doneListener != null)
					doneListener.finished(thread, code, null);
			}
		});

		importCat.start();
	}

	/**
	 * Download a catalogue and import it
	 * 
	 * @param progressBar
	 * @param doneListener
	 * @return
	 * @throws SOAPException
	 * @throws AttachmentNotFoundException
	 */
	public boolean downloadAndImport(IProgressBar progressBar, double maxProgress, ThreadFinishedListener doneListener)
			throws SOAPException, AttachmentNotFoundException {

		// download the catalogue
		File catalogueXml;

		catalogueXml = download();

		// stop if not existing
		if (!catalogueXml.exists()) {
			return false;
		}

		// import the catalogue
		makeXmlImport(catalogueXml, progressBar, maxProgress, doneListener);

		return true;
	}

	/**
	 * Check if the contents of the catalogue are correct and follows the catalogue
	 * rules
	 * 
	 * @return
	 */
	public Term isDataCorrect() {

		Term incorrectTerm = null;

		for (Term term : terms.values()) {
			if (!term.isDataCorrect())
				incorrectTerm = term;
		}

		return incorrectTerm;
	}

	/**
	 * Check if this catalogue is an older version of the catalogue passed in input.
	 * 
	 * @param catalogue
	 * @return true if this catalogue is older than the other, false otherwise.
	 */
	public boolean isOlder(Catalogue catalogue) {

		// local catalogues don't have a version so
		// we cannot say if one is older than another
		if (this.isLocal() || catalogue.isLocal())
			return false;

		// check if the catalogues have the same code
		boolean sameCode = this.equals(catalogue);

		boolean olderVersion = version.compareTo(catalogue.getCatalogueVersion()) > 0;

		return sameCode && olderVersion;
	}

	/**
	 * Open the db connection with the currently open catalogue
	 * 
	 * @author shahaal
	 * @return
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {

		Connection con = DriverManager.getConnection(getDbUrl());
		return con;
	}

	/**
	 * Get the catalogue db connection
	 * 
	 * @return
	 */
	public String getDbUrl() {
		return "jdbc:derby:" + getDbPath() + ";user=dbuser;password=dbuserpwd";
	}

	/**
	 * Is a new version present in the dcf? Return the new version if present,
	 * otherwise null
	 * 
	 * @return
	 */
	public boolean hasUpdate() {
		return getUpdate() != null;
	}

	/**
	 * Check if the last release of the catalogue is already downloaded
	 * 
	 * @return
	 */
	public boolean isLastReleaseAlreadyDownloaded() {

		// if the catalogue has not any update => we have the last
		// version and so we have already downloaded it
		if (!hasUpdate())
			return true;

		// check if we already have that update in our local db
		// it can happen that a user opens a older version of the catalogue
		// but already have downloaded the new version

		boolean alreadyDownloaded = false;

		// get the last release of the catalogue
		Catalogue lastRelease = getUpdate();

		CatalogueDAO catDao = new CatalogueDAO();

		// check if the last release is already downloaded
		// if the last release is present into the local catalogues then return true
		for (Catalogue cat : catDao.getMyCatalogues(catalogueType)) {

			if (cat.getCode().equals(lastRelease.getCode()) && cat.getVersion().equals(lastRelease.getVersion())) {
				alreadyDownloaded = true;
				break;
			}
		}
		;

		return alreadyDownloaded;
	}

	/**
	 * Override the to string method to print easily the catalogue
	 */
	@Override
	public String toString() {
		return getCode() + " " + getVersion();
	}

	/**
	 * Order the current catalogue with another one by label name and by version
	 * 
	 * @param cat
	 * @return
	 */
	@Override
	public int compareTo(Catalogue cat) {

		if (getLabel().equalsIgnoreCase(cat.getLabel())) {

			// compare the versions if equal label

			return version.compareTo(cat.getCatalogueVersion());
		}

		return getLabel().compareTo(cat.getLabel());
	}

	/**
	 * Check if this catalogue is the same as the one passed in input (both in code
	 * and version)
	 * 
	 * @param catalogue
	 * @return true if equal
	 */
	public boolean sameAs(Catalogue catalogue) {

		boolean sameCode = getCode().equals(catalogue.getCode());
		boolean sameVers = getVersion().equals(catalogue.getVersion());

		return sameCode && sameVers;
	}

	/**
	 * Decide when a catalogue is the same as another one the catalogue code
	 * identifies the catalogue (without version)
	 */
	@Override
	public boolean equals(Object cat) {
		boolean sameCode = getCode().equals(((Catalogue) cat).getCode());
		return sameCode;
	}

	@Override
	public void setTermCodeMask(String termCodeMask) {
		this.termCodeMask = termCodeMask;
	}

	@Override
	public void setTermCodeLength(int termCodeLength) {
		this.termCodeLength = termCodeLength;
	}

	@Override
	public void setTermMinCode(String termMinCode) {
		this.termMinCode = termMinCode;
	}

	@Override
	public void setAcceptNonStandardCodes(boolean acceptNonStandardCodes) {
		this.acceptNonStandardCodes = acceptNonStandardCodes;
	}

	@Override
	public void setGenerateMissingCodes(boolean generateMissingCodes) {
		this.generateMissingCodes = generateMissingCodes;
	}

	@Override
	public void setCatalogueGroups(String catalogueGroups) {
		this.catalogueGroups = catalogueGroups;
	}
}
