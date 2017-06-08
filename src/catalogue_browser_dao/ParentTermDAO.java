package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import catalogue.Catalogue;
import catalogue_object.Applicability;
import catalogue_object.BaseObject;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;

/**
 * Class to manage the communication with the table "parent_term" of the db
 * the relationship between parent and children in a defined
 * hierarchy is called "Applicability"
 * @author avonva
 *
 */
public class ParentTermDAO implements CatalogueRelationDAO<Applicability, Term, Hierarchy> {

	private Catalogue catalogue;
	
	/**
	 * Initialize the parent term dao with the
	 * catalogue we want to communicate with
	 * @param catalogue
	 */
	public ParentTermDAO( Catalogue catalogue ) {
		this.catalogue = catalogue;
	}
	
	
	/**
	 * Add a new applicability in the database
	 * @return
	 */
	public int insert ( Applicability appl ) {
		
		Collection<Applicability> appls = new ArrayList<>();
		appls.add( appl );
		
		ArrayList<Integer> ids = insert ( appls );
		if ( ids.isEmpty() )
			return -1;
		
		return ids.get( 0 );
		/*
		int id = -1;
		
		Connection con;
		
		String query = "insert into APP.PARENT_TERM (TERM_ID, HIERARCHY_ID, PARENT_TERM_ID, TERM_ORDER, TERM_REPORTABLE)"
				+ "values (?, ?, ?, ?, ?)";
		
		try {

			// get the connection
			con = catalogue.getConnection();

			// prepare the query
			PreparedStatement stmt = con.prepareStatement( query, Statement.RETURN_GENERATED_KEYS );

			stmt.clearParameters();

			// Create a new record with the term and its parent in the selected hierarchy
			stmt.setInt ( 1, appl.getChild().getId() );
			stmt.setInt ( 2, appl.getHierarchy().getId() );
			
			// set the parent (the term if term, otherwise null if hierarchy)
			if ( appl.getParentTerm() instanceof Term )
				stmt.setInt ( 3, ( (Term) appl.getParentTerm() ).getId() );
			else
				stmt.setNull (3, java.sql.Types.INTEGER );
			
			stmt.setInt     ( 4, appl.getOrder() );
			stmt.setBoolean ( 5, appl.isReportable() );
			
			stmt.executeUpdate();

			ResultSet rs = stmt.getGeneratedKeys();
			if ( rs.next() )
				id = rs.getInt(1);
			
			rs.close();
			stmt.close();
			con.close();
			

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return id;*/
	}
	
	public ArrayList<Integer> insert ( Collection<Applicability> appls ) {
		
		ArrayList<Integer> ids = new ArrayList<>();
		
		Connection con;
		
		String query = "insert into APP.PARENT_TERM (TERM_ID, HIERARCHY_ID, "
				+ "PARENT_TERM_ID, TERM_ORDER, TERM_REPORTABLE, TERM_FLAG)"
				+ "values (?, ?, ?, ?, ?, ?)";
		
		try {

			// get the connection
			con = catalogue.getConnection();

			// prepare the query
			PreparedStatement stmt = con.prepareStatement( query, Statement.RETURN_GENERATED_KEYS );

			for ( Applicability appl : appls ) {
				
				stmt.clearParameters();

				// Create a new record with the term and its parent in the selected hierarchy
				stmt.setInt ( 1, appl.getChild().getId() );
				stmt.setInt ( 2, appl.getHierarchy().getId() );

				// set the parent (the term if term, otherwise null if hierarchy)
				if ( appl.getParentTerm() instanceof Term )
					stmt.setInt ( 3, ( (Term) appl.getParentTerm() ).getId() );
				else
					stmt.setNull (3, java.sql.Types.INTEGER );

				stmt.setInt     ( 4, appl.getOrder() );
				stmt.setBoolean ( 5, appl.isReportable() );

				// flag is true since the applicability exists
				stmt.setBoolean( 6, true );

				stmt.addBatch();
			}
			
			stmt.executeBatch();

			ResultSet rs = stmt.getGeneratedKeys();
			if ( rs != null ) {
				while ( rs.next() )
					ids.add( rs.getInt( 1 ) );
				rs.close();
			}
			
			stmt.close();
			con.close();
			

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return ids;
	}

	/**
	 * Remove an applicability from the database
	 */
	public boolean remove ( Applicability appl ) {
		return remove( appl.getHierarchy(), appl.getParentTerm(), appl.getChild() );
	}
	
	/**
	 * Remove the applicability related to the parent-child in the selected
	 * hierarchy
	 * @param hierarchy
	 * @param parent
	 * @param child
	 * @return
	 */
	public boolean remove ( Hierarchy hierarchy, Nameable parent, Term child ) {

		Connection con = null;
		String query = null;
		
		// remove the relationships between the parent term in the hierarchy
		if ( parent instanceof Term )  // if we have a term we search a parent id
			query = "delete from APP.PARENT_TERM where HIERARCHY_ID = ? and TERM_ID = ? and PARENT_TERM_ID = ?";
		else // if we have a hierarchy as parent we search where id = null
			query = "delete from APP.PARENT_TERM where HIERARCHY_ID = ? and TERM_ID = ? and PARENT_TERM_ID is null";
		
		try {

			// get the connection
			con = catalogue.getConnection();

			// execute the query
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.clearParameters();
			
			stmt.setInt ( 1,  hierarchy.getId() );
			stmt.setInt ( 2,  child.getId() );
			
			// if we have a parent term get its id, otherwise we check where it is null
			if ( parent instanceof Term )
				stmt.setInt ( 3, ( (Term) parent ).getId() );
			
			stmt.executeUpdate();

			stmt.close();
			con.close();
			
			return true;

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Update an applicabicability
	 * @param appl
	 * @return
	 */
	public boolean update ( Applicability appl ) {
		return update( appl.getHierarchy(), appl.getChild(), appl.isReportable() );
	}
	
	/**
	 * Update the applicability of a term in the chosen hierarchy
	 * @param hierarchy
	 * @param term
	 * @param reportable
	 * @return
	 */
	public boolean update ( Hierarchy hierarchy, Term term, boolean reportable ) {
		
		Connection con;
		
		String query = "update APP.PARENT_TERM P set TERM_REPORTABLE = ? where HIERARCHY_ID = ? and TERM_ID = ?";
		
		try {

			// get the connection
			con = catalogue.getConnection();

			// prepare the query
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.clearParameters();

			// set if the term is reportable in the selected hierarchy
			stmt.setBoolean ( 1, reportable );
			stmt.setInt ( 2, hierarchy.getId() );
			stmt.setInt ( 3, term.getId() );
			
			stmt.executeUpdate();

			stmt.close();
			con.close();
			
			return true;

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return false;
	}

	/**
	 * Get an applicability from the result set, we need term id,
	 * hierarchy id and parent term id. Then we need also term order and term
	 * reportable.
	 */
	public Applicability getByResultSet(ResultSet rs) throws SQLException {
		
		// get the child term using its id from the catalogue
		Term childTerm = catalogue.getTermById ( rs.getInt( "TERM_ID" ) );
		
		// get the hierarchy using its id from the catalogue
		Hierarchy hierarchy = catalogue.getHierarchyById( rs.getInt( "HIERARCHY_ID" ) );
		
		// get the parent term
		int parentId = rs.getInt ( "PARENT_TERM_ID" );
		
		BaseObject parentTerm = null;
		
		// if the field is not null ( as convention 0 is returned if null is retrieved )
		if ( parentId != 0 ) {
			// then create the parent term
			parentTerm = catalogue.getTermById ( parentId );
		}
		else {
			// set the hierarchy as parent if the element is root
			parentTerm = hierarchy;
		}
		
		// get the order
		int order = rs.getInt( "TERM_ORDER" );
		
		// get reportability
		boolean reportable = rs.getBoolean( "TERM_REPORTABLE" );
		
		// create the applicability
		Applicability appl = new Applicability( childTerm, parentTerm, hierarchy, order, reportable);
		
		return appl;
	}
	
	/**
	 * Get all the applicabilities contained in 
	 * the database of the catalogue
	 */
	public Collection<Applicability> getAll() {
		
		ArrayList<Applicability> appls = new ArrayList<>();
		
		Connection con = null;

		// get all the parent terms and hierarchies ( we join with term to retrieve the parent
		// term information )
		// Note that we use a left join in order to maintain also terms which does not have
		// a parent (i.e. parent_term_id = null)! 
		// These terms will refer directly to the hierarchy they belong to (as parent)
		String query = "select * from APP.PARENT_TERM P left join APP.TERM T on P.PARENT_TERM_ID = T.TERM_ID";

		try {
			
			con = catalogue.getConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			ResultSet rs = stmt.executeQuery();

			// analyze results
			while ( rs.next() ) {
				Applicability appl = getByResultSet( rs );
				appls.add( appl );
			}
			
			rs.close();
			stmt.close();
			con.close();
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return appls;
	}

	/**
	 * Remove all the parent term relationships related to the hierarchy 
	 * Used when we remove an entire hierarchy, we remove the dependencies
	 * @param attr
	 * @return
	 */
	public boolean removeByA2 ( Hierarchy hierarchy ) {

		Connection con = null;
		
		// remove the relationships between the terms and the hierarchy
		String query = "delete from APP.PARENT_TERM where HIERARCHY_ID = ?";
		
		try {

			// get the connection
			con = catalogue.getConnection();

			// execute the query
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.clearParameters();
			
			stmt.setInt ( 1,  hierarchy.getId() );
			
			stmt.executeUpdate();

			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	

	/**
	 * Get the first level nodes of the tree considering the selected "hierarchy"
	 * @param hierarchy
	 * @return
	 */
	public ArrayList< Term > getFirstLevelNodes ( Hierarchy hierarchy, boolean hideDeprecatedTerms,
			boolean hideNonReportableTerms ) {
		return getChildren( null, hierarchy, hideDeprecatedTerms, hideNonReportableTerms );
	}

	/**
	 * Get all the children of the term in the chosen hierarchy. Set hideDeprecatedTerms to true to remove the 
	 * deprecated terms. Set hideNonReportableTerms to true to remove all the non reportable leaf and the non reportable
	 * terms which have only non reportable children (considering the entire subtree).
	 * @param t
	 * @param hierarchy
	 * @return
	 */
	public ArrayList< Term > getChildren ( Nameable t, Hierarchy hierarchy, boolean hideDeprecatedTerms,
			boolean hideNonReportableTerms ) {
		
		// output list
		ArrayList< Term > children = new ArrayList< Term >();
		
		Connection con = null;

		String query = "select T.TERM_ID "
				+ " from APP.PARENT_TERM as P inner join APP.TERM as T on (P.TERM_ID = T.TERM_ID) "
				+ " inner join APP.PARENT_TERM as P2 on (T.TERM_ID = P2.TERM_ID) "
				+ " where P.HIERARCHY_ID = ? and P2.HIERARCHY_ID = ? ";
		
		// if null it is a hierarchy
		if ( t != null )
			query = query + " and P.PARENT_TERM_ID = ? ";
		else
			query = query + " and P.PARENT_TERM_ID is null ";
		
		
		// if we want to hide deprecated terms we select only the non deprecated
		if ( hideDeprecatedTerms )
			query = query + " and T.TERM_DEPRECATED = false ";
		
		// order results
		query = query + "order by P2.TERM_ORDER, T.TERM_EXTENDED_NAME";

		try {
			con = catalogue.getConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			stmt.clearParameters();

			stmt.setInt( 1, hierarchy.getId() );
			stmt.setInt( 2, hierarchy.getId() );
			
			// if it is a term set its id
			if ( t != null && t instanceof Term )
				stmt.setInt( 3, ( (Term) t ).getId() );
			
			ResultSet rs = stmt.executeQuery();
			
			Term child;
			
			// for each child
			while ( rs.next() ) {
				
				// get the term from the hash map
				child = catalogue.getTermById ( rs.getInt( "TERM_ID" ) );

				// add the child to the output list
				
				boolean canAdd = true;
				
				// if we want to hide non reportable terms, we check if the term has reportable children in the current hierarchy
				// and if it is reportable. If it has not reportable children and it is not reportable
				// we hide it!
				if ( hideNonReportableTerms )
					canAdd = child.isReportable( hierarchy ) || child.hasReportableChildren( hierarchy );
				
				// if we can add, then add the child
				if ( canAdd )
					children.add( child );
				
			}
			
			rs.close();
			stmt.close();
			con.close();
			
		} catch ( SQLException sqle ) {
			sqle.printStackTrace();
			return null;
		}
		
		return children;
	}

	
	
	
	/**
	 * Get the max order for the parent children in the hierarchy selected
	 * @param parent, the term which will be considered as parent, we check all its children order integer
	 * @param hierarchy, the hierarchy in which we take the parent children
	 * @return
	 */
	public int getMaxOrder ( Nameable parent, Hierarchy hierarchy ) {
		
		// output, default is zero
		int maxOrder = 0;
		
		Connection con;

		String query = " select max(TERM_ORDER) as MAX_ORDER from APP.PARENT_TERM where HIERARCHY_ID = ? and PARENT_TERM_ID = ?";

		try {

			// get the connection
			con = catalogue.getConnection();

			// prepare the query
			PreparedStatement stmt = con.prepareStatement( query );

			stmt.clearParameters();

			// Create a new record with the term and its parent in the selected hierarchy
			stmt.setInt ( 1, hierarchy.getId() );
			
			// if parent is a term set its id
			if ( parent instanceof Term )
				stmt.setInt ( 2, ( (Term) parent ).getId() );
			else  // if it is a hierarchy
				stmt.setNull( 2, java.sql.Types.INTEGER );
			
			ResultSet rs = stmt.executeQuery();
			
			// Get the max order
			if ( rs.next() )
				maxOrder = rs.getInt( "MAX_ORDER" );

			rs.close();
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
			return maxOrder;
		}

		return maxOrder;
	}
	
	/**
	 * Get the next available order integer for a new term which will be added into the
	 * hierarchy as child of the parent term. 
	 * @param parent, the term which will be considered as parent, we check all its children order integer
	 * @param hierarchy, the hierarchy in which we take the parent children
	 * @return
	 */
	public int getNextAvailableOrder ( Nameable parent , Hierarchy hierarchy ) {
		return getMaxOrder(parent, hierarchy) + 1;
	}
	

	/**
	 * Shift all the selected terms order integer by an offset in the selected hierarchy
	 * Used for drag n drop functionalities
	 * @param source
	 * @param target
	 * @param hierarchy
	 */
	public void shiftTerms ( ArrayList<Term> sources, Hierarchy hierarchy, int offset ) {
		
		Connection con;
		
		String query = "update APP.PARENT_TERM P set P.TERM_ORDER = P.TERM_ORDER + ? "
				+ "where P.TERM_ID = ? and P.HIERARCHY_ID = ?";
		
		try {
			
			con = catalogue.getConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			// for each source term we add an offset to their order
			for ( Term source : sources ) {
				
				stmt.clearParameters();
				
				// prepare the parameters of the query
				stmt.setInt( 1, offset );
				stmt.setInt( 2, source.getId() );
				stmt.setInt( 3, hierarchy.getId() );
			
				// add the batch
				stmt.addBatch();
			}
			
			// execute update
			stmt.executeBatch();

			stmt.close();
			con.close();
			
		}
		catch ( SQLException e ) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * Update the term order of a term in a hierarchy
	 * @param term
	 * @param hierarchy
	 * @param order
	 */
	public void updateTermOrder ( Term term, Hierarchy hierarchy, int order ) {
		
		Connection con;
		
		String query = "UPDATE APP.PARENT_TERM set TERM_ORDER = ? where TERM_ID = ? and HIERARCHY_ID = ? ";

		try {

			con = catalogue.getConnection();

			PreparedStatement stmt = con.prepareStatement( query );

			stmt.clearParameters();

			// set the order
			stmt.setInt( 1, order );
			stmt.setInt( 2, term.getId() );
			stmt.setInt( 3, hierarchy.getId() );
			
			// execute query
			stmt.executeUpdate();

			stmt.close();
			con.close();
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Add an integer offset to a subset of the children of the parent term in the hierarchy
	 * We can select a subset of children fixing a minimum/maximum order integer
	 * @param parent
	 */
	public void addOrderOffset ( Term parent, Hierarchy hierarchy, String operator, int childOrder, int offset ) {
		
		Connection con;
		
		String query = "UPDATE APP.PARENT_TERM "
				+ "set TERM_ORDER = TERM_ORDER + ? "
				+ "where PARENT_TERM_ID = ? and HIERARCHY_ID = ? ";
	
		query = query + "and TERM_ORDER " + operator + " ? ";
		
		
		try {

			con = catalogue.getConnection();

			PreparedStatement stmt = con.prepareStatement( query );

			stmt.clearParameters();

			// set the order offset
			stmt.setInt( 1, offset );
			
			if ( parent != null )
				stmt.setInt( 2, parent.getId() );
			else  // otherwise null
				stmt.setNull( 2, java.sql.Types.INTEGER );
			
			stmt.setInt( 3, hierarchy.getId() );
			stmt.setInt( 4, childOrder );
			
			// execute query
			stmt.executeUpdate();

			stmt.close();
			con.close();
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Swap the order integer of two terms in the selected hierarchy
	 * @param source
	 * @param target
	 * @param hierarchy
	 */
	public void swapTermOrder ( Term source, Term target, Hierarchy hierarchy ) {
		
		Connection con;
		
		String query = "update APP.PARENT_TERM P set P.TERM_ORDER = ? "
				+ "where P.TERM_ID = ? and P.HIERARCHY_ID = ?";
		
		try {
			
			con = catalogue.getConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			stmt.clearParameters();
			
			// set the target order as the source order
			stmt.setInt( 1, source.getOrder( hierarchy ) );
			stmt.setInt( 2, target.getId() );
			stmt.setInt( 3, hierarchy.getId() );
			
			stmt.addBatch();
			
			// set the source order as the target order
			stmt.setInt( 1, target.getOrder( hierarchy ) );
			stmt.setInt( 2, source.getId() );
			stmt.setInt( 3, hierarchy.getId() );
			
			stmt.addBatch();
			
			// execute update
			stmt.executeBatch();

			stmt.close();
			con.close();
			
		}
		catch ( SQLException e ) {
			e.printStackTrace();
		}	
	}

	@Override
	public Applicability getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Applicability> getByA1(Term object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Applicability> getByA2(Hierarchy object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeByA1(Term object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateByA1(Term object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateByA2(Hierarchy object) {
		// TODO Auto-generated method stub
		return false;
	}
}
