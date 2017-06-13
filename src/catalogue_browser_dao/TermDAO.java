package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;

import catalogue.Catalogue;
import catalogue_object.Term;

/**
 * Class to manage all the databse interactions with the Term table.
 * @author avonva
 *
 */
public class TermDAO implements CatalogueEntityDAO<Term> {
	
	private Catalogue catalogue;
	
	/**
	 * Initialize term dao with the catalogue
	 * we want to communicate with
	 * @param catalogue
	 */
	public TermDAO( Catalogue catalogue ) {
		this.catalogue = catalogue;
	}
	
	/**
	 * Insert a single term into the db
	 * @param t
	 * @return
	 */
	public int insert ( Term t ) {
		
		ArrayList<Term> terms = new ArrayList<>();
		terms.add( t );
		
		return insertTerms ( terms ).get(0);
	}

	
	/**
	 * Insert a batch of new terms into the catalogue database
	 * return the term object with the ID field set.
	 * @param t
	 * @return
	 */
	public synchronized ArrayList<Integer> insertTerms ( Collection<Term> terms ) {
		
		ArrayList<Integer> ids = new ArrayList<>();
		
		Connection con = null;
		
		String query = "insert into APP.TERM (TERM_CODE, TERM_EXTENDED_NAME, "
				+ "TERM_SHORT_NAME, TERM_SCOPENOTE, TERM_DEPRECATED, TERM_LAST_UPDATE, "
				+ "TERM_VALID_FROM, TERM_VALID_TO, TERM_STATUS ) values (?, ?, ?, ?, ?, ?, ?, ?, ? )";
		
		try {
			
			con = catalogue.getConnection();
			
			PreparedStatement stmt = con.prepareStatement( query, Statement.RETURN_GENERATED_KEYS );

			for ( Term t : terms ) {
				
				stmt.clearParameters();
				
				stmt.setString( 1, t.getCode() );
				stmt.setString( 2, t.getName() );
				stmt.setString( 3, t.getShortName() );
				stmt.setString( 4, t.getScopenotes() );
				stmt.setBoolean( 5, t.isDeprecated() );

				if ( t.getLastUpdate() != null )
					stmt.setTimestamp( 6, t.getLastUpdate() );
				else
					stmt.setNull( 6, java.sql.Types.TIMESTAMP );

				if ( t.getValidTo() != null )
					stmt.setTimestamp( 7, t.getValidTo() );
				else
					stmt.setNull( 7, java.sql.Types.TIMESTAMP );

				if ( t.getValidTo() != null )
					stmt.setTimestamp( 8, t.getValidTo() );
				else
					stmt.setNull( 8, java.sql.Types.TIMESTAMP );

				stmt.setString( 9, t.getStatus() );

				stmt.addBatch();
			}
			
			stmt.executeBatch();
			
			// get an instance of the global manager
			//GlobalManager manager = GlobalManager.getInstance();
			
			// get the current catalogue
			//Catalogue currentCat = manager.getCurrentCatalogue();
			
			// TODO TODO TODO
			// only if the opened catalogue is the catalogue we are modifying then
			// refresh the Terms hash map
			//if ( currentCat != null && catalogue.equals( currentCat ) ) {

			if ( !terms.isEmpty() ) {

				// update the terms ids with the ones given by the database
				ResultSet rs = stmt.getGeneratedKeys();
				//int count = 0;

				while ( rs.next() ) {

					ids.add( rs.getInt(1) );

					//Term term = terms.get( count );
					//term.setId( rs.getInt( 1 ) );

					// add the term to the Terms hashmap
					//catalogue.addTerm( term );

					//count++;
				}

				rs.close();
				//}
			}

			stmt.close();
			con.close();
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return ids;
	}
	
	/**
	 * Update the term fields in the DB. The last update field is modified to NOW
	 * @param t, the term to be updated
	 * @return the updated term
	 */
	public boolean update ( Term t ) {

		Connection con = null;
		
		try {
			
			// open the connection
			con = catalogue.getConnection();
			
			PreparedStatement stmt = con.prepareStatement( "update APP.TERM set TERM_CODE = ?, TERM_EXTENDED_NAME = ?, "
							+ "TERM_SHORT_NAME = ?, TERM_SCOPENOTE = ?, TERM_DEPRECATED = ?, TERM_LAST_UPDATE = ?,"
							+ "TERM_VALID_FROM = ?, TERM_VALID_TO = ?, TERM_STATUS = ? where TERM_ID = ?" );
			
			stmt.clearParameters();
			
			stmt.setString( 1, t.getCode() );
			stmt.setString( 2, t.getName() );
			stmt.setString( 3, t.getShortName() );
			stmt.setString( 4, t.getScopenotes() );
			stmt.setBoolean( 5, t.isDeprecated() );
			
			// set timestamps or null if needed
			Calendar cal = Calendar.getInstance();
			
			// set the last update to now
			stmt.setTimestamp( 6, new Timestamp( cal.getTimeInMillis() ) );

			if ( t.getValidFrom() != null )
				stmt.setTimestamp( 7, t.getValidFrom() );
			else
				stmt.setNull( 7, Types.TIMESTAMP );
			
			
			if ( t.getValidTo() != null )
				stmt.setTimestamp( 8, t.getValidTo() );
			else
				stmt.setNull( 8, Types.TIMESTAMP );

			stmt.setString( 9, t.getStatus() );
			
			stmt.setInt( 10, t.getId() );
			
			// execute the statement
			stmt.executeUpdate();
			
			// close statement
			stmt.close();
			
			// close the connection
			con.close();
			
			return true;
			
		} catch ( SQLException sqle ) {
			sqle.printStackTrace();
		}

		return false;
	}

	
	// update the term in the hashmap of terms (which is used to 
	// store in RAM the terms). Use this to refresh term field in ram
	// once they are modified
	public void updateTermInRAM ( Term term ) {
		catalogue.addTerm( term );
	}

	/**
	 * Get all the terms
	 * @return
	 */
	public Collection< Term > getAll() {
		return fetchTerms().values();
	}
	
	/**
	 * Get all the catalogue terms ordered by their code
	 * All term attributes are also fetched
	 * All applicabilities are also fetched
	 * @return
	 */
	public HashMap< Integer, Term > fetchTerms () {

		HashMap< Integer, Term > terms = new HashMap<>();

		Connection con = null;

		String query = "select * from APP.TERM";

		try {

			// prepare the connection with the currently open catalogue
			con = catalogue.getConnection();

			// prepare the statement and its parameters
			Statement stmt = con.createStatement();

			// get the results
			ResultSet rs = stmt.executeQuery( query );

			// save all the terms
			while ( rs.next() ) {

				Term term = getByResultSet( rs );
				terms.put( term.getId(), term );
			}

			rs.close();
			stmt.close();
			con.close();

			return terms;

		} catch ( SQLException sqle ) {
			sqle.printStackTrace();
			return null;
		}
	}

	/**
	 * Retrieve a term from the database using its code
	 * @param code
	 * @return
	 */
	public Term getByCode ( String code ) {
		
		Connection con = null;

		String sqlstr = "select TERM_ID " + " from  APP.TERM " + " where upper( TERM_CODE ) = ? ";

		try {
			con = catalogue.getConnection();
			
			PreparedStatement stmt = con.prepareStatement( sqlstr );

			stmt.clearParameters();
			
			/* I want to retrieve the first level under the root */
			stmt.setString( 1, code.toUpperCase() );
			
			ResultSet rs = stmt.executeQuery();

			Term term = null;

			if ( rs.next() )
				term = catalogue.getTermById( rs.getInt( "TERM_ID" ) );
			
			rs.close();
			stmt.close();
			con.close();
			
			return term;

		} catch ( SQLException sqle ) {
			sqle.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Retrieve a term from the database using its name ( which is unique in the catalogue )
	 * @param code
	 * @return
	 */
	public Term getByName ( String extendedName ) {

		Connection con = null;

		String sqlstr = "select * from APP.TERM where upper( TERM_EXTENDED_NAME ) = ? ";

		try {
			con = catalogue.getConnection();
			
			PreparedStatement stmt = con.prepareStatement( sqlstr );

			stmt.clearParameters();
			
			/* I want to retrieve the first level under the root */
			stmt.setString( 1, extendedName.toUpperCase() );
			
			ResultSet rs = stmt.executeQuery();
			
			Term term = null;
			
			if ( rs.next() )
				term = catalogue.getTermById( rs.getInt( "TERM_ID" ) );
			
			rs.close();
			stmt.close();
			con.close();
			
			return term;

		} catch ( SQLException sqle ) {
			sqle.printStackTrace();
			return null;
		}
	}

	
	
	/**
	 * Create a new term object starting from the result set
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public Term getByResultSet( ResultSet rs ) throws SQLException {
		return getByResultSet ( new Term( catalogue ), rs, true );
	}
	
	/**
	 * add to a pre-existing term object all its fields contained in the result set
	 * @param t
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public Term getByResultSet( Term t, ResultSet rs, boolean setId ) throws SQLException {
		
		// set the term id if required
		if ( setId )
			t.setId( rs.getInt( "TERM_ID" ) );
		
		// set the term code
		t.setCode( rs.getString( "TERM_CODE" ) );

		// set the term extended name
		t.setName( rs.getString( "TERM_EXTENDED_NAME" ) );

		// set the term short name
		t.setShortName( rs.getString( "TERM_SHORT_NAME" ) );

		// set the term scopenotes
		t.setScopenotes( rs.getString( "TERM_SCOPENOTE" ) );
		
		// set if the term is deprecated
		t.setDeprecated( rs.getBoolean( "TERM_DEPRECATED" ) );

		// set last update of the term
		t.setLastUpdate( rs.getTimestamp( "TERM_LAST_UPDATE" ) );
		
		// set valid from
		t.setValidFrom( rs.getTimestamp( "TERM_VALID_FROM" ) );
		
		// set valid to
		t.setValidTo( rs.getTimestamp( "TERM_VALID_TO" ) );
		
		// set status
		t.setStatus( rs.getString( "TERM_STATUS" ) );

		return t;
	}
	
	/**
	 * check if the term name was already used before by other terms in the DB
	 * (Different from the one considered by the termCode)
	 * @param termName
	 * @param extended, should extended name or short name be considered?
	 * @return
	 */
	public boolean isTermNameUnique ( String termCode, String termName, boolean extended ) {
		
		Connection con = null;
		PreparedStatement stmt=null;
		ResultSet rs = null;
		
		String sqlstr = "select TERM_ID from APP.TERM where ";
		
		// check on the correct field
		if ( extended )
			sqlstr = sqlstr + "TERM_EXTENDED_NAME = ? ";
		else
			sqlstr = sqlstr + "TERM_SHORT_NAME = ? ";
		
		sqlstr = sqlstr + "and TERM_CODE <> ?";

		try {
			
			con = catalogue.getConnection();
			stmt = con.prepareStatement( sqlstr );
			stmt.clearParameters();
			
			stmt.setString( 1, termName );
			stmt.setString( 2, termCode );
			rs = stmt.executeQuery();
			
			boolean noDupl = !rs.next();
			
			rs.close();
			stmt.close();
			con.close();
			
			// if there is a record in the next => a term with the same name 
			// but different code actually exists
			return noDupl;
			
		} catch ( SQLException e ) {
			e.printStackTrace();
			return false;
		}
	}


	@Override
	public boolean remove(Term object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Term getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}
}
