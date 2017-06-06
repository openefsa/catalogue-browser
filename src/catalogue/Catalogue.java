package catalogue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.DatabaseManager;
import catalogue_browser_dao.ForceCatEditDAO;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_browser_dao.ParentTermDAO;
import catalogue_browser_dao.ReleaseNotesDAO;
import catalogue_browser_dao.ReservedCatDAO;
import catalogue_browser_dao.TermAttributeDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Applicability;
import catalogue_object.Attribute;
import catalogue_object.BaseObject;
import catalogue_object.Hierarchy;
import catalogue_object.HierarchyBuilder;
import catalogue_object.Mappable;
import catalogue_object.Status.StatusValues;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import catalogue_object.Version;
import data_transformation.BooleanConverter;
import data_transformation.DateTrimmer;
import dcf_manager.Dcf;
import dcf_manager.VersionFinder;
import dcf_pending_action.NewCatalogueInternalVersion;
import dcf_pending_action.PendingAction;
import dcf_pending_action.PendingActionDAO;
import dcf_user.User;
import dcf_webservice.ReserveLevel;
import detail_level.DetailLevelDAO;
import detail_level.DetailLevelGraphics;
import global_manager.GlobalManager;
import messages.Messages;
import term_code_generator.CodeGenerator;
import term_type.TermType;
import term_type.TermTypeDAO;
import utilities.GlobalUtil;

/**
 * Catalogue object, it contains the catalogue metadata, the catalogue
 * terms, hierarchies and attributes, term attributes and applicabilities.
 * @author avonva
 *
 */
public class Catalogue extends BaseObject implements Comparable<Catalogue>, Mappable, Cloneable {
	
	// date format of the catalogues
	public static final String ISO_8601_24H_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	public static final String NOT_APPLICABLE_VERSION = "Not applicable";
	public static final String LOCAL_CATALOGUE_STATUS = "Local catalogue";
	
	// the catalogue version is specialized, we need to
	// manage it separately
	CatalogueVersion version;
	
	// catalogue meta data
	private String termCodeMask;
	private int termCodeLength;
	private String termMinCode;
	private boolean acceptNonStandardCodes;
	private boolean generateMissingCodes;
	private String catalogueGroups;

	// external reference which locates the catalogue real data (not meta data)
	// this field is initialized when the catalogue meta data are inserted in the CATALOGUE table
	private String dbDir;       // db directory
	private String dbFullPath;  // db full path with filename
	private String backupDbPath; // path where it is located the backup of the catalogue db
	
	private boolean local;    // if the catalogue is a new local catalogue or not
	
	// boolean set to true if we start
	// a pending action on this catalogue
	private boolean requestingAction;
	
	// list of terms which are contained in the catalogue
	private HashMap< Integer, Term > terms;
	
	// cache for term ids to speed up finding
	// terms ids using term code without
	// iterating the entire collection of terms
	private HashMap< String, Integer > termsIds;
	
	// list of the hierarchies contained in the 
	// catalogue (both base and attribute hierarchies)
	private ArrayList< Hierarchy > hierarchies;
	
	// list of the attributes contained in the
	// catalogue (only definitions, not values)
	private ArrayList< Attribute > attributes;
	
	// list of possible implicit facets (i.e. categories) for the terms
	// could be empty if the catalogue does not have
	// any implicit facet
	private ArrayList< Attribute > facetCategories;
	
	// detail levels of the catalogue
	public ArrayList< DetailLevelGraphics > detailLevels;
	
	// term types of the catalogue (could be empty)
	public ArrayList< TermType > termTypes;
	
	// the catalogue release notes
	private ReleaseNotes releaseNotes;
	
	// default values for detail level and term type
	private DetailLevelGraphics defaultDetailLevel;
	private TermType defaultTermType;
	
	private int forcedCount;

	/**
	 * Constructor to create a catalogue object with all its variables
	 * 
	 * @param id
	 * @param code the catalogue code (unique)
	 * @param name the catalogue name (unique)
	 * @param label the catalogue label (text which is displayed)
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
	public Catalogue( int id, String code, String name, String label, 
			String scopenotes, String termCodeMask, String termCodeLength, 
			String termMinCode, boolean acceptNonStandardCodes, 
			boolean generateMissingCodes, String version,
			Timestamp lastUpdate, Timestamp validFrom, Timestamp validTo, 
			String status, String catalogueGroups, boolean deprecated, 
			String dbFullPath, String backupDbPath, boolean local, int forcedCount, 
			ReleaseNotes releaseNotes ) {

		// the id is not important for the catalogue
		super( id, code, name, label, scopenotes, version, lastUpdate, 
				validFrom, validTo, status, deprecated );
	
		// set the version of the catalogue with the
		// extended Version
		this.version = new CatalogueVersion( version );
		
		this.termCodeMask = termCodeMask;

		// convert the term code length into integer if possible
		try {
			this.termCodeLength = Integer.parseInt( termCodeLength );
		} catch (NumberFormatException e) {
			this.termCodeLength = 0;
		}

		this.termMinCode = termMinCode;

		this.acceptNonStandardCodes = acceptNonStandardCodes;
		this.generateMissingCodes = generateMissingCodes;

		this.catalogueGroups = catalogueGroups;

		this.dbFullPath = dbFullPath;
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
	}
	
	
	/**
	 * Load all the data related to the catalogue
	 * that is, hierarchies, terms, attributes,
	 * term attributes, applicabilities, detail
	 * levels and term types
	 */
	public void loadData() {
		refreshHierarchies();
		refreshTerms();
		refreshAttributes();
		refreshApplicabities();
		refreshTermAttributes();
		refreshDetailLevels();
		refreshTermTypes();
		refreshReleaseNotes();
	}
	
	/**
	 * Clear all the data of the catalogue
	 * that is, clear hierarchies, terms, attributes
	 * implicit facets, detail levels and term types
	 */
	public void clearData() {
		hierarchies.clear();
		attributes.clear();
		facetCategories.clear();
		terms.clear();
		detailLevels.clear();
		termTypes.clear();
	}


	/**
	 * Clone the catalogue and return it
	 */
	public Catalogue clone () {

		// create the catalogue object and return it
		Catalogue catalogue = new Catalogue ( getId(), getCode(), getName(), 
				getLabel(), getScopenotes(), termCodeMask, 
				String.valueOf( termCodeLength ), termMinCode,
				acceptNonStandardCodes, generateMissingCodes, getVersion(), 
				getLastUpdate(), getValidFrom(), 
				getValidTo(), getStatus(), catalogueGroups, isDeprecated(), 
				dbFullPath, backupDbPath, local, 0, releaseNotes );
		
		return catalogue;
	}

	/**
	 * Open the current catalogue and load its data into ram
	 * Note that the user interface observes the changes in
	 * the current catalogue of the global manager, therefore
	 * when a current catalogue is set, the UI is automatically
	 * refreshed.
	 */
	public void open() {
		
		System.out.println ( "Opening " + this + " at " + dbFullPath );
		
		// load the catalogue data into RAM
		loadData();
		
		GlobalManager manager = GlobalManager.getInstance();
		
		// close the opened catalogue if there is one
		if ( manager.getCurrentCatalogue() != null )
			manager.getCurrentCatalogue().close();
		
		manager.setCurrentCatalogue( this );

		// refresh logging state
		GlobalUtil.refreshLogging();
	}
	
	/**
	 * Get the catalogue derby connection
	 * @return
	 */
	public String getShutdownDBURL() {
		return "jdbc:derby:" + dbFullPath + ";user=dbuser;password=dbuserpwd;shutdown=true";
	}
	
	/**
	 * Close the catalogue
	 */
	public void close() {
		
		System.out.println ( "Closing " + this + " at " + dbFullPath );
		
		// shutdown the connection, by default this operation throws an exception
		// but the command is correct! We close the connection since we close the db
		try {
			DriverManager.getConnection( getShutdownDBURL() );
		} catch (SQLException e) {
			System.out.println ( "System shutted down with code : " + e.getErrorCode() + " and state " + e.getSQLState() );
			System.out.println ( "Correct shutdown has code 45000 and state 08006" );
		}
		
		// clear data in ram
		clearData();
		
		// remove current catalogue
		GlobalManager manager = GlobalManager.getInstance();

		// if the current catalogue is the one we are
		// closing => set the current catalogue as null
		if ( manager.getCurrentCatalogue().sameAs( this ) )
			manager.setCurrentCatalogue( null );
	}
	
	/**
	 * Check if the current catalogue is opened
	 * or not
	 * @return
	 */
	public boolean isOpened () {
		GlobalManager manager = GlobalManager.getInstance();

		// if same of current return true
		return ( manager.getCurrentCatalogue().sameAs( this ) );
	}
	
	/**
	 * Refresh the UI with this catalogue. Note that
	 * you need to call {@link #open()} first
	 */
	public void refreshCatalogueUI () {

		GlobalManager manager = GlobalManager.getInstance();
		
		// if the opened catalogue is not this one => return
		if ( !manager.getCurrentCatalogue().sameAs( this ) ) {
			System.err.println( "Cannot refresh catalogue UI: catalogue not opened " + this );
			return;
		}
		
		// notify the observers
		manager.setCurrentCatalogue( this );
	}
	
	/**
	 * Refresh the hierarchies contents
	 */
	public void refreshHierarchies() {

		HierarchyDAO hierDao = new HierarchyDAO( this );
		
		// initialize the hierarchies
		hierarchies = hierDao.getAll();
	}
	
	/**
	 * Get an hierarchy by its id. If not found => null
	 * @param id
	 * @return
	 */
	public Hierarchy getHierarchyById ( int id ) {
		
		for ( Hierarchy h : hierarchies ) {
			
			if ( h.getId() == id )
				return h;
		}
		
		return null;
	}
	
	/**
	 * Get an hierarchy by its code. If not found => null
	 * @param code
	 * @return
	 */
	public Hierarchy getHierarchyByCode ( String code ) {
		
		for ( Hierarchy h : hierarchies ) {
			
			if ( h.getCode().equals( code ) )
				return h;
		}
		
		return null;
	}
	
	/**
	 * Create the master hierarchy starting from the 
	 * catalogue, since the master
	 * is actually the catalogue!
	 * @return
	 */
	public Hierarchy createMasterHierarchy() {
		
		HierarchyBuilder builder = new HierarchyBuilder();
		builder.setCatalogue( this );
		builder.setCode( getCode() );
		builder.setName( getName() );
		builder.setLabel( getLabel() );
		builder.setScopenotes( getScopenotes() );
		builder.setApplicability( "both" );
		builder.setMaster( true );
		builder.setStatus( getStatus() );
		builder.setGroups( getCatalogueGroups() );
		builder.setLastUpdate( getLastUpdate() );
		builder.setValidFrom( getValidFrom() );
		builder.setValidTo( getValidTo() );
		builder.setOrder( 0 );
		builder.setVersion( getVersion() );
		builder.setDeprecated( isDeprecated() );
		
		return builder.build();
	}
	
	/**
	 * Searches for the master hierarchy inside the hierarchies
	 * 
	 * @return
	 */
	public Hierarchy getMasterHierarchy() {

		// get the master hierarchy from the hierarchies list
		for ( Hierarchy hierarchy : hierarchies ) {
			
			// if we found the master stop and return the hierarchy
			if ( hierarchy.isMaster() )
				return hierarchy;
		}
		
		// return null if no hierarchy was found
		return null;
	}

	/**
	 * Check if there are hierarchies or not
	 * @return
	 */
	public boolean hasHierarchies() {
		return !hierarchies.isEmpty();
	}
	
	/**
	 * Check if we have attribute hierarchies or not (if they are present we can
	 * describe terms, otherwise no)
	 * @return
	 */
	public boolean hasAttributeHierarchies() {
		
		for ( Hierarchy hierarchy : hierarchies ) {
			if ( hierarchy.isFacet() )
				return true;
		}
		
		return false;
	}

	/**
	 * Refresh the attributes contents
	 */
	public void refreshAttributes() {

		AttributeDAO attrDao = new AttributeDAO( this );
		attributes = attrDao.getAll();

		// refresh also the cache of implicit facets
		facetCategories = attrDao.fetchAttributes( "catalogue", false );
	}
	
	/**
	 * Get an attribute by its id
	 * @param id
	 * @return
	 */
	public Attribute getAttributeById( int id ) {
		
		Attribute attr = null;
		
		for ( Attribute a : attributes ) {
			if ( a.getId() == id )
				attr = a;
		}
		
		return attr;
	}

	/**
	 * Add a new term into the hashmap of terms
	 * @param id
	 * @param term
	 */
	public void addTerm( Term term ) {
		terms.put( term.getId(), term );
	}
	
	/**
	 * Check if some terms are present in the catalogue
	 * @return
	 */
	public boolean hasTerms () {
		return !terms.isEmpty();
	}
	

	/**
	 * Refresh the terms contents
	 */
	public void refreshTerms() {
		
		TermDAO termDao = new TermDAO( this );
		
		// initialize the terms
		terms = termDao.fetchTerms();
		
		termsIds = new HashMap<>();
		
		// update cache of ids
		for ( Term term : terms.values() ) {
			termsIds.put( term.getCode(), term.getId() );
		}
	}
	


	/**
	 * Refresh the parent child relationships of the catalogue terms
	 * Need to be called after {@linkplain Catalogue#refreshHierarchies} and
	 * {@linkplain Catalogue#refreshTerms}
	 */
	public void refreshApplicabities() {
		
		ParentTermDAO parentDao = new ParentTermDAO( this );
		
		for ( Applicability appl : parentDao.getAll() ) {
			Term term = appl.getChild();
			term.addApplicability( appl );
		}
	}
	
	/**
	 * Refresh the term attributes and their values.
	 * Need to be called after {@linkplain Catalogue#refreshTerms} and
	 * {@linkplain Catalogue#refreshAttributes}
	 */
	public void refreshTermAttributes() {
		
		// reset all the attributes of each term
		for ( Term term : terms.values() ) {
			term.clearAttributes();
		}
		
		// load the attributes values for the terms
		TermAttributeDAO taDao = new TermAttributeDAO( this );

		// set the term attributes to the terms
		for ( TermAttribute ta : taDao.getAll() ) {
			Term term = ta.getTerm();
			term.addAttribute( ta );
		}
	}
	
	/**
	 * Refresh the catalogue release notes
	 */
	public void refreshReleaseNotes() {
		ReleaseNotesDAO rnDao = new ReleaseNotesDAO( this );
		releaseNotes = rnDao.getReleaseNotes();
	}
	
	/**
	 * Get the catalogue release notes
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
		
		defaultDetailLevel = new DetailLevelGraphics( "", 
				Messages.getString( "DetailLevel.DefaultValue" ), null );
		
		// add void detail level
		detailLevels.add( defaultDetailLevel );
	}
	
	/**
	 * Get the default detail level of the catalogue
	 * @return
	 */
	public DetailLevelGraphics getDefaultDetailLevel() {
		return defaultDetailLevel;
	}
	
	/**
	 * Refresh the term types of the catalogue
	 * if there are some.
	 */
	public void refreshTermTypes() {
		
		TermTypeDAO ttDao = new TermTypeDAO( this );
		
		termTypes = ttDao.getAll();
		
		defaultTermType = new TermType( -1, "", 
				Messages.getString( "TermType.DefaultValue" ) );
		
		// add void term type
		termTypes.add( defaultTermType );
	}
	
	/**
	 * Get a term type by its id
	 * @param id
	 * @return
	 */
	public TermType getTermTypeById( int id ) {
		
		TermType tt = null;
		
		for ( TermType type : termTypes ) {
			if ( type.getId() == id )
				tt = type;
		}
		
		return tt;
	}
	
	/**
	 * Get the default term type of the catalogue
	 * @return
	 */
	public TermType getDefaultTermType() {
		return defaultTermType;
	}
	
	
	/**
	 * Check if the catalogue supports term types
	 * @return
	 */
	public boolean hasTermTypes () {
		return !termTypes.isEmpty();
	}
	
	/**
	 * Get all the hierarchies of the catalogue
	 * @return
	 */
	public ArrayList<Hierarchy> getHierarchies() {
		return hierarchies;
	}
	
	/**
	 * Get all the catalogue terms
	 * @return
	 */
	public Collection<Term> getTerms() {
		return terms.values();
	}
	
	/**
	 * Get all the catalogue attributes
	 * @return
	 */
	public ArrayList<Attribute> getAttributes() {
		return attributes;
	}
	
	/**
	 * Get all the implicit facets of the catalogue
	 * @return
	 */
	public ArrayList<Attribute> getFacetCategories() {
		return facetCategories;
	}
	
	
	/**
	 * Check if the catalogue has usable implicit facets categories or not
	 * @return
	 */
	public boolean hasImplicitFacetCategories () {
		return facetCategories != null && !facetCategories.isEmpty();
	}
	
	
	/**
	 * Get the detail levels of the catalogue
	 * @return
	 */
	public ArrayList<DetailLevelGraphics> getDetailLevels() {
		return detailLevels;
	}
	
	/**
	 * Get the term types of the catalogue. Could be empty.
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
	 * @param parent
	 * @param hierarchy
	 * @return
	 */
	public Term addNewTerm ( Term parent, Hierarchy hierarchy ) {
		
		// get the a new code for the term using the catalogue term code mask
		String code = CodeGenerator.getTermCode( termCodeMask );
		
		return addNewTerm ( code, parent, hierarchy );
	}
	
	/**
	 * Add a new term into the catalogue database as 
	 * child of the term "parent" in the selected hierarchy. The term is a complete
	 * term since we save the term, all its attributes and its applicability.
	 * @param code the code of the new term
	 * @param parent the parent of the new term
	 * @param hierarchy the hierarchy in which the term is added to the parent
	 * @return the new term
	 */
	public Term addNewTerm ( String code, Term parent, Hierarchy hierarchy ) {
		
		TermDAO termDao = new TermDAO( this );
		
		// get a new default term
		Term child = Term.getDefaultTerm( code );
		
		// insert the new term into the database and get the new term
		// with the id set
		int id = termDao.insert( child );
		child.setId( id );
		
		// initialize term attribute dao
		TermAttributeDAO taDao = new TermAttributeDAO( this );
		
		// insert the term attributes of the term
		taDao.updateByA1( child );

		ParentTermDAO parentDao = new ParentTermDAO( this );
		
		// get the first available order integer under the parent term
		// in the selected hierarchy
		int order = parentDao.getNextAvailableOrder( parent, hierarchy );
		
		// create the term applicability for the term in the selected hierarchy
		// we set the new term as child of the selected term
		// we set it to reportable as default
		Applicability appl = new Applicability( child, parent, 
				hierarchy, order, true );
		
		// add permanently the new applicability to the child
		child.addApplicability( appl, true );
		
		// update the involved terms in RAM
		termDao.update( child );
		termDao.update( parent );
		
		// add the term to the hashmap
		terms.put( id, child );
		
		// update also the ids cache
		termsIds.put( code, id );
		
		return child;
	}
	
	/**
	 * Get a term by its id
	 * @param id the term id
	 */
	public Term getTermById( Integer id ) {
		
		Term term = terms.get( id );
		
		if ( term == null ) {
			System.err.println ( "Term with id " + id + " not found in catalogue " + this );
		}
		
		// get the term with the key
		// if not found => null
		return term;
	}
	
	/**
	 * Get a term by its code
	 * @param code
	 * @return
	 */
	public Term getTermByCode ( String code ) {

		// get the term id from the cache
		int id = termsIds.get( code );
		
		// get the term by its id
		return getTermById( id );
	}

	/**
	 * Check if the catalogue has the detail 
	 * level attribute or not
	 * @return
	 */
	public boolean hasDetailLevelAttribute () {
		
		for ( Attribute attr : attributes ) {
			if ( attr.isDetailLevel() )
				return true;
		}
		
		return false;
	}
	
	/**
	 * Check if the catalogue has the 
	 * term type attribute or not
	 * @return
	 */
	public boolean hasTermTypeAttribute () {
		
		for ( Attribute attr : attributes ) {
			if ( attr.isTermType() )
				return true;
		}
		
		return false;
	}
	
	/**
	 * Force the editing of the catalogue even if
	 * it was not reserved
	 * @param username who is forcing the editing
	 * @param level the editing level we want (MINOR or MAJOR)
	 * @return a copy of the catalogue on which we can edit
	 */
	public synchronized Catalogue forceEdit ( String username, ReserveLevel level ) {

		if ( level.isNone() ) {
			System.err.println( "Cannot force editing at level NONE" );
			return null;
		}
		
		// update the forced count for this catalogue
		forcedCount++;
		
		// update the forced count also in the db
		CatalogueDAO catDao = new CatalogueDAO();
		catDao.update( this );
		
		// version checker to modify the catalogue version
		// in a permament way
		VersionChecker versionChecker = new VersionChecker( this );
		
		Catalogue forcedCatalogue = versionChecker.force();
		
		System.out.println( "Editing forced by " 
				+ username + " for " + this );

		ForceCatEditDAO forceDao = new ForceCatEditDAO();
		forceDao.forceEditing( forcedCatalogue, username, level );
		
		return forcedCatalogue;
	}

	/**
	 * Confirm the version of the catalogue
	 * @param username
	 */
	public void confirmVersion () {
		
		System.out.println ( "Version confirmed for " + this );
		
		// remove forced flag
		version.confirm();
		
		System.out.println( "New version " + version.getVersion() );
		
		// remove the force editing since now
		// it is correctly enabled
		removeForceEdit();
		
		// update the version in the db
		CatalogueDAO catDao = new CatalogueDAO();
		catDao.update( this );
	}
	
	/**
	 * Set the current catalogue version as invalid,
	 * since it has changes made through forced editing
	 * and the reserve operation does not succeeded 
	 */
	public synchronized void invalidate() {
		
		System.out.println( "Invalid version for " + this );
		
		// add the NULL flag
		version.invalidate();
		
		System.out.println( "New version " + version.getVersion() );
		
		// update version in the db
		CatalogueDAO catDao = new CatalogueDAO();
		catDao.update( this );
	}
	
	/**
	 * Check if the version of the catalogue is invalid or not
	 * @return
	 */
	public boolean isInvalid() {
		return version.isInvalid();
	}

	/**
	 * Remove the forced editing from this catalogue
	 * related to the user we want
	 */
	private synchronized void removeForceEdit () {

		System.out.println( "Forced editing removed by " 
				+ User.getInstance() + " for " + this );
		
		ForceCatEditDAO forceDao = new ForceCatEditDAO();
		forceDao.removeForceEditing( this );
	}
	
	/**
	 * Get the forced editing level of this catalogue regarding
	 * the User identified by {@code username}.
	 * @param username
	 * @return the forced editing level, if NONE, no editing was forced
	 */
	public synchronized ReserveLevel getForcedEditLevel ( String username ) {
		
		ForceCatEditDAO forceDao = new ForceCatEditDAO();
		
		// we are forcing the editing if we have a forced editing
		// level greater than NONE
		return forceDao.getEditingLevel( this, username );
	}
	
	/**
	 * Check if the catalogue has forced
	 * editing or not by the user
	 * @param username
	 * @return
	 */
	public boolean isForceEdit( String username ) {

		// we are forcing the editing 
		// if we have a forced editing
		// level greater than NONE
		return getForcedEditLevel( username ).greaterThan( ReserveLevel.NONE );
	}

	/**
	 * update the status of the catalogue
	 * @param value
	 */
	public void setStatus ( StatusValues value ) {
		getRawStatus().markAs( value );
	}

	
	/**
	 * Reserve the current catalogue for the current user.
	 * Note that if you want to unreserve the catalogue you
	 * must use {@link #unreserve() }
	 * @param note the reservation note
	 * @param reserveLevel the reserve level needed. Both
	 * {@link ReserveLevel.MINOR} or 
	 * {@link ReserveLevel.MAJOR} are accepted.
	 * @return the new internal version of the catalogue if a new one is created
	 * or {@code this} catalogue if the version of the catalogue is simply confirmed
	 * (only for forced catalogues)
	 */
	public synchronized Catalogue reserve( String note, ReserveLevel reserveLevel ) {
		
		if ( reserveLevel.isNone() ) {
			System.err.println ( "You are reserving a catalogue with ReserveLevel.NONE "
					+ "as reserve level, use unreserve instead" );
			return null;
		}
		
		User user = User.getInstance();

		Catalogue newCatalogue;

		if ( this.isForceEdit( user.getUsername() ) ) {

			// we simply confirm the forced version and go on with that
			confirmVersion();
			newCatalogue = this;
		}
		else {
			// get a new internal version of the catalogue
			newCatalogue = newInternalVersion();
			System.out.println ( "Creating new internal version " + newCatalogue );
		}

		// reserve the catalogue into the db
		ReservedCatDAO resDao = new ReservedCatDAO();
		resDao.insert( new ReservedCatalogue(newCatalogue, 
				user.getUsername(), note, reserveLevel) );
		
		// update the status of the catalogue
		if ( reserveLevel.isMajor() )
			newCatalogue.setStatus( StatusValues.DRAFT_MAJOR_RESERVED );
		else if ( reserveLevel.isMinor() )
			newCatalogue.setStatus( StatusValues.DRAFT_MINOR_RESERVED );
		
		// the reserve operation is finished
		setRequestingAction( false );
		
		return newCatalogue;
	}
	
	/**
	 * Unreserve the catalogue
	 */
	public synchronized void unreserve () {

		// update the status of the catalogue
		if ( getReserveLevel().isMajor() )
			this.setStatus( StatusValues.DRAFT_MAJOR_UNRESERVED );
		else if ( getReserveLevel().isMinor() )
			this.setStatus( StatusValues.DRAFT_MINOR_UNRESERVED );
		
		// unreserve the catalogue in the database
		ReservedCatDAO resDao = new ReservedCatDAO();
		ReservedCatalogue rc = resDao.getById( getId() );
		
		if ( rc != null )
			resDao.remove( rc );
		else
			System.err.println( "Catalogue already unreserved " + this );
		
		// update the catalogue status
		CatalogueDAO catDao = new CatalogueDAO();
		catDao.update( this );
		
		// the unreserve operation is finished
		setRequestingAction( false );
	}
	
	/**
	 * Publish a minor release of the catalogue
	 * @return the new version of the catalogue
	 */
	public Catalogue publishMinor () {
		
		// version checker to modify the catalogue version
		// in a permament way
		VersionChecker versionChecker = new VersionChecker( this );
		
		Catalogue newCatalogue = versionChecker.publishMinor();
		
		newCatalogue.setStatus( StatusValues.PUBLISHED_MINOR );
		
		return newCatalogue;
	}
	
	/**
	 * Publish a minor release of the catalogue
	 * @return the new version of the catalogue
	 */
	public Catalogue publishMajor () {
		
		// version checker to modify the catalogue version
		// in a permament way
		VersionChecker versionChecker = new VersionChecker( this );
		
		Catalogue newCatalogue = versionChecker.publishMajor();
		
		newCatalogue.setStatus( StatusValues.PUBLISHED_MAJOR );
		
		return newCatalogue;
	}
	
	/**
	 * Publish a new internal version of the catalogue
	 * @return the new version of the catalogue
	 */
	public Catalogue newInternalVersion () {
		
		// version checker to modify the catalogue version
		// in a permament way
		VersionChecker versionChecker = new VersionChecker( this );
		
		return versionChecker.newInternalVersion();
	}

	
	/**
	 * Set if the catalogue is being reserved or 
	 * unreserved
	 * @param reserving
	 */
	public void setRequestingAction(boolean requestingAction) {
		this.requestingAction = requestingAction;
	}
	
	/**
	 * Check if a pending action regarding this
	 * catalogue is being operated
	 * @return
	 */
	public boolean isRequestingAction() {
		
		PendingActionDAO prDao = new PendingActionDAO();
		Collection<PendingAction> pas = prDao.getByCatalogue ( this );
		return !pas.isEmpty() || requestingAction;
	}
	
	/**
	 * Check if the catalogue can be reserved by the current user.
	 * To check unreservability, please use {@link isUnreservable}
	 * @return
	 */
	public boolean isReservable () {
		
		CatalogueStatus prob = getCatalogueStatus();

		return prob == CatalogueStatus.NONE;
	}
	
	/**
	 * Enum used to get the specific problem which
	 * is blocking a reserve action on this catalogue
	 * @author avonva
	 *
	 */
	public enum CatalogueStatus {
		NONE,
		INVALID,
		PENDING_ACTION_ONGOING,
		RESERVED_BY_CURRENT,
		RESERVED_BY_OTHER,
		NOT_LAST,
		LOCAL,
		DEPRECATED,
		FORCED_EDIT
	};
	
	/**
	 * Get the status of the catalogue
	 * @return
	 */
	public CatalogueStatus getCatalogueStatus() {

		CatalogueStatus problem = CatalogueStatus.NONE;
		
		String username = User.getInstance().getUsername();
		
		// if the catalogue is reserved by someone which is not me
		// then we cannot reserve
		boolean reservedByOther = !isReservedBy( User.getInstance() ) 
				&& isReserved();
		
		// no problem if no user had reserved the catalogue
		// and the catalogue is the last available
		// version of the catalogue and the catalogue
		// is not local and it is not deprecated
		if ( isInvalid() )
			problem = CatalogueStatus.INVALID;
		else if ( isRequestingAction() )
			problem = CatalogueStatus.PENDING_ACTION_ONGOING;
		else if ( reservedByOther )
			problem = CatalogueStatus.RESERVED_BY_OTHER;
		else if ( isReservedBy ( User.getInstance() ) )
			problem = CatalogueStatus.RESERVED_BY_CURRENT;
		else if ( hasUpdate() )
			problem = CatalogueStatus.NOT_LAST;
		else if ( local )
			problem = CatalogueStatus.LOCAL;
		else if ( isDeprecated() )
			problem = CatalogueStatus.DEPRECATED;
		else if ( isForceEdit( username ) )
			problem = CatalogueStatus.FORCED_EDIT;
		
		return problem;
	}
	
	/**
	 * Check if the catalogue can be unreserved by the current user.
	 * To check reservability, please use {@link isReservable}
	 * @return
	 */
	public boolean isUnreservable () {

		User user = User.getInstance();
		
		// if the user had reserved the catalogue
		// if this is the last release
		// and if no force editing is applied
		if ( isReservedBy( user ) 
				&& isLastRelease() 
				&& !isForceEdit( user.getUsername() ) )
			return true;
		
		return false;
	}
	
	/**
	 * Check if the catalogue can be published
	 * @return
	 */
	public boolean canBePublished () {
		
		// can be published if not reserved and
		// if not in forced editing
		boolean ok = !isReserved() && 
				!isForceEdit( User.getInstance().getUsername() );
		
		return ok;
	}
	/**
	 * Get the catalogue term code mask if there is one
	 * @return
	 */
	public String getTermCodeMask() {
		return termCodeMask;
	}

	/**
	 * Get the catalogue term code length
	 * @return
	 */
	public int getTermCodeLength() {
		return termCodeLength;
	}

	/**
	 * Get the code which is the starting point for
	 * creating new codes
	 * @return
	 */
	public String getTermMinCode() {
		return termMinCode;
	}

	/**
	 * Check if the catalogue accepts non standard codes
	 * for the terms
	 * @return
	 */
	public boolean isAcceptNonStandardCodes() {
		return acceptNonStandardCodes;
	}

	/**
	 * Check if the catalogue can generate
	 * missing codes or not
	 * @return
	 */
	public boolean isGenerateMissingCodes() {
		return generateMissingCodes;
	}

	/**
	 * Get the catalogue groups (single string $ separated)
	 * @return
	 */
	public String getCatalogueGroups() {
		return catalogueGroups;
	}

	/**
	 * Get the directory which contains the database 
	 * of the catalogue
	 * @return
	 */
	public String getDBDir() {
		return dbDir;
	}
	
	/**
	 * Get the backup path
	 * @return
	 */
	public String getBackupDbPath() {
		return backupDbPath;
	}
	
	/**
	 * Set the backup db path (the db which will be
	 * used as backup of this catalogue if needed)
	 * @param backupDbPath
	 */
	public void setBackupDbPath(String backupDbPath) {
		this.backupDbPath = backupDbPath;
	}
	
	
	
	/**
	 * Build the full db path of the catalogue with default dir settings
	 * create also the directory in which the db will be created
	 * @param catalogue
	 * @return
	 */
	public String createDbDir () {
		
		// if local catalogue => local cat dir, otherwise main cat dir
		String folder = isLocal() ? DatabaseManager.LOCAL_CAT_DB_FOLDER : DatabaseManager.OFFICIAL_CAT_DB_FOLDER;

		String dbDirectory = DatabaseManager.generateDBDirectory( folder, this );

		String dbFullPath = buildDBFullPath( dbDirectory );  // full path of the db from the directory
		
		return dbFullPath;
	}
	
	/**
	 * Get the full path of the db ( directory + dbname ) and set it
	 * as the path of the catalogue (we build it!)
	 * @return
	 */
	public String buildDBFullPath( String dbDir ) {

		dbFullPath = getDbFullPath ( dbDir, getCode(), getVersion(), local );
		return dbFullPath;
	}
	
	/**
	 * Set the full db path directly
	 * @param dbFullPath
	 */
	public void setDbFullPath(String dbFullPath) {
		this.dbFullPath = dbFullPath;
	}
	
	
	/**
	 * Is the database local? True if the database was created
	 * through the command "new local catalogue", false otherwise
	 * @return
	 */
	public boolean isLocal() {
		return local;
	}
	
	/**
	 * Get the reserved catalogue object if present.
	 * @return
	 */
	private ReservedCatalogue getReservedCatalogue() {
		
		ReservedCatDAO resDao = new ReservedCatDAO();
		
		return resDao.getById( getId() );
	}
	
	/**
	 * Check if the catalogue is reserved or not
	 * @return
	 */
	public boolean isReserved() {
		
		// if the catalogue is present into the
		// reserved catalogue table it is reserved
		return getReservedCatalogue() != null;
	}
	
	/**
	 * Check if the catalogue is reserved or not
	 * by the user with username passed in input
	 * @return
	 */
	public boolean isReservedBy( User user ) {
		
		ReservedCatalogue rc = getReservedCatalogue();
		
		// if not present the catalogue is not reserved
		if ( rc == null )
			return false;
		
		// check that the user who reserved the catalogue
		// is the one passed in input
		return rc.getUsername().equals( user.getUsername() );
	}
	
	/**
	 * Set the number of times that we have forced the
	 * editing of this catalogue
	 * @param forcedCount
	 */
	public void setForcedCount(int forcedCount) {
		this.forcedCount = forcedCount;
	}
	
	/**
	 * Get how many times we had forced the editing
	 * of this catalogue.
	 * @return
	 */
	public int getForcedCount() {
		return forcedCount;
	}
	
	
	/**
	 * Get the reserve level of the catalogue
	 * if present
	 * @return
	 */
	public ReserveLevel getReserveLevel () {
		
		ReserveLevel level = ReserveLevel.NONE;
		
		ReservedCatalogue rc = getReservedCatalogue();
		
		// if catalogue is reserved get the level
		if ( rc != null )
			level = rc.getLevel();
		
		return level;
	}
	
	/**
	 * Get the username of the user who reserved 
	 * the catalogue (if there is one)
	 * @return
	 */
	public String getReserveUsername() {
		
		String username = null;

		ReservedCatalogue rc = getReservedCatalogue();

		// if catalogue is reserved get the level
		if ( rc != null )
			username = rc.getUsername();

		return username;
	}

	/**
	 * Get the reserve note if present
	 * @return
	 */
	public String getReserveNote() {
		
		String note = null;
		ReservedCatalogue rc = getReservedCatalogue();
		
		if ( rc != null )
			note = rc.getNote();
		
		return note;
	}
	
	
	/**
	 * Set the catalogue version
	 * @param version
	 */
	public void setCatalogueVersion ( CatalogueVersion version ) {
		this.version = version;
	}
	
	/**
	 * Get the catalogue version
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
		if ( version != null )
			return version.getVersion();
		else
			return null;
	}
	
	@Override
	public void setVersion(String version) {
		setCatalogueVersion ( new CatalogueVersion ( version ) );
	}
	
	@Override
	public void setRawVersion(Version version) {
		System.err.println( "setRawVersion not supported by Catalogue, use setCatalogueVersion instead" );
	}

	@Override
	public Version getRawVersion() {
		System.err.println( "getRawVersion not supported by Catalogue, use getCatalogueVersion instead" );
		return null;
	}
	
	/**
	 * Get the db full path using the catalogues main directory, 
	 * the catalogue code/version and if the catalogue is local or not
	 * For non local catalogues the folder will be   code + "_VERSION_" + version
	 * for local catalogues instead we use only the code (the version is not applicable)
	 * @param dbDir, the folder in which we will create the catalogue folder and then the database
	 * @param code
	 * @param version
	 * @param local
	 * @return
	 */
	public static String getDbFullPath ( String dbDir, String code, String version, boolean local ) {

		// if local catalogue => we return only the db code as name
		if ( local )
			return dbDir + System.getProperty( "file.separator" ) + code;

		// if instead we have an official catalogue, set also the version

		// name of the catalogue db
		String dbName = code + "_VERSION_" + version;

		// create the full path and assign it
		return dbDir + System.getProperty( "file.separator" ) + dbName;
	}
	
	
	/**
	 * Get the path of the Db of the catalogue
	 * null if it was not set (you have to use buildDBFullPath first!)
	 * see also {@link #buildDBFullPath( String dbDir ) buildDBFullPath}
	 * @return
	 */
	public String getDbFullPath() {
		return dbFullPath;
	}

	/**
	 * Get the default hierarchy for this catalogue, default is the master
	 * The master hierarchy can be hidden to users. For this reason it is necessary to define a
	 * new default hierarchy in these cases. The default hierarchy is usually the master hierarchy, but this
	 * can be overridden setting after the catalogue scopenote: $hideMasterWith=newDefaultHierarchyCode
	 * If it is not found, we return the master hierarchy
	 * @return the default hierarchy
	 */
	public Hierarchy getDefaultHierarchy () {

		// set the initial value for the default hierarchy
		Hierarchy defaultHierarchy = getMasterHierarchy();

		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// if we are in editing, the default hierarchy is the master hierarchy
		if ( !manager.isReadOnly() )
			return defaultHierarchy;

		StringTokenizer st = new StringTokenizer( getScopenotes(), "$" );
		while ( st.hasMoreTokens() ) {

			String token = st.nextToken();

			// remove spaces
			token = token.trim();

			if ( token.toLowerCase().contains( "[hideMasterWith".toLowerCase() ) ) {

				String[] split = token.split( "=.*]" );

				// go to the next iteration if wrong split
				if ( split.length != 2 )
					continue;

				// return the default hierarchy
				Hierarchy temp = getHierarchyByCode( split[1] );

				if ( temp != null )
					defaultHierarchy = temp;
			}
		}
		return defaultHierarchy;
	}


	/**
	 * Get a catalogue field value using the DB columns names
	 * column name
	 * @param key
	 * @return
	 */
	public String getValueByKey( String key ) {

		String value = "";

		switch ( key ) {
		case "CAT_CODE":
			value = getCode(); break;
		case "CAT_NAME":
			value = getName(); break;
		case "CAT_LABEL":
			value = getLabel(); break;
		case "CAT_SCOPENOTE":
			value = getScopenotes(); break;
		case "CAT_TERM_CODE_MASK":
			value = termCodeMask; break;
		case "CAT_TERM_CODE_LENGTH":
			value = String.valueOf( termCodeLength ); break;
		case "CAT_TERM_MIN_CODE":
			value = termMinCode; break;
		case "CAT_ACCEPT_NON_STANDARD_CODES":
			value = String.valueOf( acceptNonStandardCodes ); break;
		case "CAT_GENERATE_MISSING_CODES":
			value = String.valueOf( generateMissingCodes ); break;
		case "CAT_VERSION":
			value = getVersion(); break;
		case "CAT_GROUPS":
			value = catalogueGroups; break;
		case "CAT_LAST_UPDATE":
			if ( getLastUpdate() != null )
				value = DateTrimmer.dateToString( getLastUpdate() ); 
			break;
		case "CAT_VALID_FROM":
			if ( getValidFrom() != null )
				value = DateTrimmer.dateToString( getValidFrom() ); 
			break;
		case "CAT_VALID_TO":
			if ( getValidTo() != null )
				value = DateTrimmer.dateToString( getValidTo() );
			break;
		case "CAT_STATUS":
			value = getStatus(); break;
		case "CAT_DEPRECATED":
			value = BooleanConverter.toNumericBoolean( String.valueOf( isDeprecated() ) ); break;
		case "CAT_RN_DESCRIPTION":
			System.out.println( "Description " + releaseNotes.getDescription() );
			if ( releaseNotes != null && releaseNotes.getDescription() != null )
				value = releaseNotes.getDescription();
			break;
		case "CAT_RN_VERSION_DATE":
			if ( releaseNotes != null && releaseNotes.getDate() != null )
				value = DateTrimmer.dateToString( releaseNotes.getDate() ); 
			break;
		case "CAT_RN_INTERNAL_VERSION":
			if ( releaseNotes != null && releaseNotes.getInternalVersion() != null )
				value = releaseNotes.getInternalVersion();
			break;
		case "CAT_RN_INTERNAL_VERSION_NOTE":
			if ( releaseNotes != null && releaseNotes.getInternalVersionNote() != null )
				value = releaseNotes.getInternalVersionNote();
			break;
		default:
			break;
		}

		return value;
	}
	
	/**
	 * Create a default catalogue object (for new catalogues)
	 * @param catalogueCode
	 * @return
	 */
	public static Catalogue getDefaultCatalogue( String catalogueCode ) {

		CatalogueBuilder builder = new CatalogueBuilder();
		builder.setCode( catalogueCode );
		builder.setName( catalogueCode );
		builder.setLabel( catalogueCode );
		builder.setVersion( NOT_APPLICABLE_VERSION );
		builder.setStatus( LOCAL_CATALOGUE_STATUS );
		builder.setLocal( true );

		return builder.build();
	}
	
	/**
	 * Get the last downloaded version of the catalogue
	 * @return
	 */
	public Catalogue getLastVersion() {
		
		CatalogueDAO catDao = new CatalogueDAO();
		
		return catDao.getLastVersionByCode( getCode() );
	}
	
	/**
	 * Get the dummy cat users catalogue. We use this catalogue
	 * to provide the right authorization accesses to users
	 * @return
	 */
	public static Catalogue getCatUsersCatalogue() {
		
		String code = "CATUSERS";
		
		CatalogueBuilder builder = new CatalogueBuilder();
		builder.setCode( code );
		builder.setName( code );
		builder.setLabel( code );
		builder.setVersion( "" );
		
		return builder.build();
	}
	
	/**
	 * Check if the catalogue is the cat users catalogue
	 * we hide this catalogue from read only users.
	 * @return
	 */
	public boolean isCatUsersCatalogue() {
		return getCode().equals ( "CATUSERS" );
	}
	
	/**
	 * Check if the catalogue is the MTX catalogue or not
	 * If we have the MTX then several conditions will apply, such as the 
	 * enabling of the business rules
	 * @return
	 */
	public boolean isMTXCatalogue() {
		return getCode().equals( "MTX" );
	}
	
	/**
	 *  Get the newest version of the catalogue 
	 *  (if there is one, otherwise null)
	 * @return
	 */
	public Catalogue getUpdate () {

		if ( isLastRelease() )
			return null;
		
		return getLastRelease();
	}
	
	/**
	 * Get the last release of the catalogue. If we
	 * already have the last release then it will be
	 * returned as it is.
	 * @return
	 */
	public Catalogue getLastRelease() {
		
		CatalogueDAO catDao = new CatalogueDAO();
		Catalogue lastLocalVersion = catDao.getLastVersionByCode( getCode() );
		
		Catalogue lastPublishedRelease = Dcf.getLastPublishedRelease( this );
		
		// if not found return the local version (for local catalogues)
		if ( lastPublishedRelease == null )
			return lastLocalVersion;
		
		// if local < published
		if ( lastLocalVersion.isOlder( lastPublishedRelease ) )
			return lastPublishedRelease;
		else
			return lastLocalVersion;
	}
	
	/**
	 * Check if this catalogue is the last PUBLISHED release
	 * or not.
	 * @return
	 */
	public boolean isLastRelease() {
		
		// get the last update
		Catalogue last = getLastRelease();

		// if this catalogue is older than the last
		// release => this is not the last release
		if ( last != null && this.isOlder( last ) ) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check if the catalogue is the last
	 * INTERNAL VERSION or not.
	 * @return the new catalogue internal version if there is one,
	 * null otherwise
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 */
	public NewCatalogueInternalVersion getLastInternalVersion() throws IOException, 
	TransformerException, ParserConfigurationException, SAXException {

		String format = ".xml";
		String filename = "temp_" + getCode();
		String input = filename + format;
		String output = filename + "_version" + format;

		Dcf dcf = new Dcf();

		// export the internal version in the file
		boolean written = dcf.exportCatalogueInternalVersion( 
				getCode(), input );

		// if no internal version is retrieved we have
		// the last version of the catalogue
		if ( !written )
			return null;

		VersionFinder finder = new VersionFinder( input, output );

		// compare the catalogues versions
		CatalogueVersion intVersion = new CatalogueVersion ( finder.getVersion() );

		// if the downloaded version is newer than the one we
		// are working with => we are using an old version
		if ( intVersion.compareTo( version ) < 0 ) {

			// save the new version of the catalogue
			NewCatalogueInternalVersion newVersion = 
					new NewCatalogueInternalVersion( getCode(), 
							finder.getVersion(), input );

			return newVersion;
		}

		return null;
	}
	
	/**
	 * Check if this catalogue is an older version
	 * of the catalogue passed in input.
	 * @param catalogue
	 * @return true if this catalogue is older than the other,
	 * false otherwise.
	 */
	public boolean isOlder ( Catalogue catalogue ) {
		
		// local catalogues don't have a version so
		// we cannot say if one is older than another
		if ( this.isLocal() || catalogue.isLocal() )
			return false;
		
		// check if the catalogues have the same code
		boolean sameCode = this.equals( catalogue );
		
		boolean olderVersion = version.compareTo( catalogue.getCatalogueVersion() ) > 0;

		return sameCode && olderVersion;
	}

	/**
	 * Open the db connection with the currently open catalogue
	 * @return
	 * @throws SQLException
	 */
	public Connection getConnection () throws SQLException {
		
		Connection con = DriverManager.getConnection( getDbUrl() );
		
		return con;
	}

	/**
	 * Get the catalogue db connection
	 * @return
	 */
	public String getDbUrl() {
		return "jdbc:derby:" + dbFullPath + ";user=dbuser;password=dbuserpwd";
	}
	

	/**
	 * Is a new version present in the dcf?
	 * Return the new version if present, otherwise null
	 * @return
	 */
	public boolean hasUpdate () {
		return getUpdate() != null;
	}
	
	/**
	 * Check if the last release of the catalogue is already downloaded
	 * @return
	 */
	public boolean isLastReleaseAlreadyDownloaded () {

		// if the catalogue has not any update => we have the last 
		// version and so we have already downloaded it
		if ( !hasUpdate() )
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
		for ( Catalogue cat : catDao.getLocalCatalogues() ) {

			if ( cat.getCode().equals( lastRelease.getCode() ) && 
					cat.getVersion().equals( lastRelease.getVersion() ) ) {
				alreadyDownloaded = true;
				break;
			}
		};

		return alreadyDownloaded;
	}
	
	/**
	 * Check if the master hierarchy should be hidden or not
	 * @return
	 */
	public boolean isMasterHierarchyHidden () {
		
		User user = User.getInstance();
		
		// if we are in read only mode and the default hierarchy 
		// is not the master hierarchy
		// then we have to hide the master hierarchy
		return user.canEdit( this ) && 
				!getDefaultHierarchy().isMaster();
	}
	
	/**
	 * Override the to string method to print easily the catalogue
	 */
	@Override
	public String toString() {
		return "CATALOGUE: " + getCode() + ", version " + getVersion();
	}


	/**
	 * Order the current catalogue with another one by label name
	 * and by version
	 * @param cat
	 * @return
	 */
	@Override
	public int compareTo( Catalogue cat ) {
		
		if ( getLabel().equals( cat.getLabel() ) ) {
			
			// compare the versions if equal label

			return version.compareTo( cat.getCatalogueVersion() );
		}
		
		return getLabel().compareTo( cat.getLabel() );
	}

	/**
	 * Check if this catalogue is the same as the one
	 * passed in input (both in code and version)
	 * @param catalogue
	 * @return true if equal
	 */
	public boolean sameAs ( Catalogue catalogue ) {
		
		boolean sameCode = getCode().equals( catalogue.getCode() );
		boolean sameVers = getVersion().equals( catalogue.getVersion() );
		
		return sameCode && sameVers;
	}
	
	/**
	 * Decide when a catalogue is the same as another one 
	 * the catalogue code identifies the catalogue (without version)
	 */
	@Override
	public boolean equals( Object cat ) {
		boolean sameCode = getCode().equals( ( (Catalogue) cat ).getCode() );
		return sameCode;
	}
}


