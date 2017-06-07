package import_catalogue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import catalogue.Catalogue;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_browser_dao.ParentTermDAO;
import catalogue_object.Applicability;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;
import excel_file_management.ResultDataSet;

public class ParentImporter extends SheetImporter<Applicability> {

	private Catalogue catalogue;
	private HashMap<String, Integer> termIds;
	private ArrayList<Hierarchy> hierarchies;

	public ParentImporter( Catalogue catalogue, 
			ResultDataSet termData ) throws SQLException {

		super( termData );
		this.catalogue = catalogue;

		// get all the term ids
		termIds = createIdHashMap ( catalogue, "TERM_ID", "TERM_CODE", "APP.TERM" );

		// get all the hierarchies of the catalogue
		HierarchyDAO hierDao = new HierarchyDAO( catalogue );
		hierarchies = hierDao.getAll();
	}

	@Override
	public Applicability getByResultSet(ResultDataSet rs) {
		return null;
	}

	@Override
	public Collection<Applicability> getAllByResultSet(ResultDataSet rs) {

		Collection<Applicability> appls = new ArrayList<>();

		// get the term code
		String termCode = rs.getString ( "termCode" );

		// skip if no term code was found
		if ( termCode.isEmpty() )
			return null;

		// get the term id using the term code from the hashmap (global var)
		int termId = termIds.get( termCode );

		// for each attribute we create a record for the term attributes table
		for ( Hierarchy hierarchy : hierarchies ) {

			// get the parent term code 
			String parentCode = rs.getString ( getHierarchyFieldName( hierarchy, "ParentCode" ) );
			
			// next if no parent term is found
			if ( parentCode == null || parentCode.isEmpty() )
				continue;
			
			// check if we have the root term or not
			boolean isRoot = parentCode.equalsIgnoreCase( "ROOT" );

			// get the parent term id from the code
			Integer parentId = termIds.get( parentCode );
			
			// if not root and parent id not found => error
			if ( parentId == null && !isRoot ) {

				System.err.println ( "The parent term " + parentCode + 
						" is not present in the DB, please check!" );
				continue;
			}
			
			// get the term flag
			//boolean flag = rs.getBoolean( 
				//	getHierarchyFieldName( hierarchy, "Flag" ), true );
			
			// get the term order
			int order = rs.getInt( 
					getHierarchyFieldName( hierarchy, "Order" ), 0 );
			
			// get the term reportability
			boolean reportable = rs.getBoolean( 
					getHierarchyFieldName( hierarchy, "Reportable" ), true );
			
			Applicability appl = createApplicability ( isRoot, termId, parentId, 
					hierarchy, order, reportable );
			
			appls.add( appl );
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
			columnName = Hierarchy.MASTER_HIERARCHY_CODE + field;
		else  // else, standard hierarchy
			columnName = hierarchy.getCode() + field; 
		
		return columnName;
	}

	@Override
	public void insert(Collection<Applicability> data) {
		ParentTermDAO parentDao = new ParentTermDAO ( catalogue );
		parentDao.insert( data );
	}

}
