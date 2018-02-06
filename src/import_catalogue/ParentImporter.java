package import_catalogue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueRelationDAO;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_browser_dao.ParentTermDAO;
import catalogue_object.Applicability;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;
import naming_convention.Headers;
import naming_convention.SpecialValues;
import open_xml_reader.ResultDataSet;
import term_code_generator.CodeGenerator;

/**
 * Import the term applicabilities contained in the term sheet into the database.
 * This class can manage also new terms which could be possibly created
 * in the {@link TermSheetImporter#importSheet()}. In fact,
 * here we manage also the possibility that a parent of a new term
 * can be also a new term itself.
 * @author avonva
 *
 */
public class ParentImporter extends SheetImporter<Applicability> {

	private static final Logger LOGGER = LogManager.getLogger(ParentImporter.class);
	
	private CatalogueRelationDAO<Applicability, Term, Hierarchy> dao;
	private Catalogue catalogue;
	private HashMap<String, Integer> termIds;
	private ArrayList<Hierarchy> hierarchies;
	
	// things for append
	private HashMap<String, String> newCodes;
	// temporary applicabilities
	private Collection<Applicability> tempAppl;

	public ParentImporter(CatalogueRelationDAO<Applicability, Term, Hierarchy> dao, Catalogue catalogue) throws SQLException {
		this.dao = dao;
		this.catalogue = catalogue;
		this.newCodes = new HashMap<>();
		this.tempAppl = new ArrayList<>();
		
		// get all the term ids of the database of the catalogue
		this.termIds = createIdHashMap ( catalogue, "TERM_ID", "TERM_CODE", "APP.TERM" );

		// get all the hierarchies of the catalogue
		HierarchyDAO hierDao = new HierarchyDAO( catalogue );
		this.hierarchies = hierDao.getAll();
	}
	
	public ParentImporter(Catalogue catalogue) throws SQLException {
		this(new ParentTermDAO(catalogue), catalogue);
	}
	
	/**
	 * Activate this method to manage also new terms (i.e. appended terms) into the 
	 * parent importer class. In particular, if a term with code
	 * or parent code containing {link CodeGenerator#TEMP_TERM_CODE}
	 * is encountered, its real code is taken from the {@code newCodes}
	 * hashmap (which was created before in
	 * the {@link TermSheetImporter#importSheet()} if new terms were
	 * encountered!)
	 * @param newCodes
	 */
	public void manageNewTerms( HashMap<String, String> newCodes ) {
		this.newCodes = newCodes;
	}

	@Override
	public Applicability getByResultSet(ResultDataSet rs) {
		return null;
	}

	@Override
	public Collection<Applicability> getAllByResultSet(ResultDataSet rs) throws ImportException {

		Collection<Applicability> appls = new ArrayList<>();
		boolean addParent = true;

		// get the term code
		String termCode = rs.getString ( Headers.TERM_CODE );

		// if temp code we need to get the real code
		// of the term
		if ( CodeGenerator.isTempCode( termCode ) ) {
			termCode = newCodes.get( termCode );
			addParent = false;  // do not add a temporary applicability
		}
		
		// skip if no term code was found
		if ( termCode == null || termCode.isEmpty() )
			return null;
		
		// get the term id using the term code from the hashmap (global var)
		int termId = termIds.get( termCode );

		// for each attribute we create a record for the term attributes table
		for ( Hierarchy hierarchy : hierarchies ) {

			// get the parent term code 
			String parentCode = rs.getString ( 
					getHierarchyFieldName( hierarchy, Headers.SUFFIX_PARENT_CODE ) );
			
			// if temp code we need to get the real code
			// of the term
			if ( newCodes != null && CodeGenerator.isTempCode( parentCode ) ) {
				parentCode = newCodes.get( parentCode );
			}
			
			// next if no parent term is found
			if ( parentCode == null || parentCode.isEmpty() )
				continue;
			
			// ERROR! cannot set a term parent of itself
			if ( parentCode.equals( termCode ) ) {
				ImportException e = new ImportException(
						"ERROR: A TERM CANNOT BE PARENT OF ITSELF: term code " + termCode, 
						"X101");
				e.setData(termCode);
				throw e;
			}
			
			// check if we have the root term or not
			boolean isRoot = parentCode.equalsIgnoreCase( SpecialValues.NO_PARENT );

			// get the parent term id from the code
			Integer parentId = termIds.get( parentCode );
			
			// if not root and parent id not found => error
			if ( parentId == null && !isRoot ) {

				LOGGER.error ( "The parent term " + parentCode + 
						" is not present in the DB, please check!" );
				continue;
			}
			
			// get the term flag, NOTE this field is useless because it is always 1
			// if a parent code is defined
			//boolean flag = rs.getBoolean( 
				//	getHierarchyFieldName( hierarchy, Headers.SUFFIX_FLAG ), true );

			// get the term order
			int order = rs.getInt( 
					getHierarchyFieldName( hierarchy, Headers.SUFFIX_ORDER ), -1 );
			
			// get the term reportability
			boolean reportable = rs.getBoolean( 
					getHierarchyFieldName( hierarchy, Headers.SUFFIX_REPORT ), true );
			
			Applicability appl = createApplicability ( isRoot, termId, parentId, 
					hierarchy, order, reportable );
			
			// Add only real parents, not temporary ones!
			// because we need to define their order code
			if ( addParent )
				appls.add( appl );
			else  // add the applicability to the temporary ones
				tempAppl.add( appl );
		}
		
		return appls;
	}
	
	/**
	 * Create an applicability starting with the available information
	 * @param childId
	 * @param parentId
	 * @param hierarchy
	 * @param order
	 * @param reportable
	 * @return
	 */
	private Applicability createApplicability ( boolean isRoot, int childId, Integer parentId, 
			Hierarchy hierarchy, int order, boolean reportable ) {
		
		Term child = new Term( catalogue );
		child.setId( childId );
		
		Nameable parent;
		
		// if standard parent
		if ( !isRoot ) {
			
			// create parent term and assign id
			parent = new Term ( catalogue );
			( (Term) parent ).setId( parentId );
		}
		else // if hierarchy parent
			parent = new Hierarchy( catalogue );
		
		// create the applicability and return it
		Applicability appl = new Applicability( child, parent, 
				hierarchy, order, reportable );
		
		return appl;
	}
	
	/**
	 * Get the name of the column which contains the data related to the field "field".
	 * Example, we want the flag column related to the Reporting hierarchy => it returns reportingFlag
	 * if the hierarchy is the master, the convention says that we should search with the master keyword!
	 * In the previous example we would search masterFlag. The master code is variable, so we need this check.
	 * @param hierarchy
	 * @param field
	 * @return
	 */
	private String getHierarchyFieldName ( Hierarchy hierarchy, String field ) {
		
		String columnName;
		
		// if it is master hierarchy
		if ( hierarchy.getCode().equals( catalogue.getCode() ) )
			columnName = Headers.PREFIX_MASTER_CODE + field;
		else  // else, standard hierarchy
			columnName = hierarchy.getCode() + field; 
		
		return columnName;
	}

	@Override
	public void insert(Collection<Applicability> data) {
		dao.insert(data);
	}

	@Override
	public void end() {

		for ( Applicability appl : tempAppl ) {
			
			int order = appl.getOrder();
			
			// if order was not defined
			if ( order == -1 ) {

				Nameable parent = appl.getParentTerm();
				Hierarchy hierarchy = appl.getHierarchy();
				
				// get the first available order for the term in the hierarchy
				ParentTermDAO parentDao = new ParentTermDAO( catalogue );
				int newOrder = parentDao.getNextAvailableOrder( 
						parent, hierarchy );
				
				// set the applicability order
				appl.setOrder( newOrder );
				
				// we need a collection since insert works with
				// collections
				Collection<Applicability> applCol = new ArrayList<>();
				applCol.add( appl );
				
				// insert the applicability into the database
				// we need to insert one applicability at a time
				// otherwise the new order will be always the same
				insert ( applCol );
			}
		}
	}
}
