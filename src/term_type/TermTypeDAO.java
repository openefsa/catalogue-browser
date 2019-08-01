package term_type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueEntityDAO;

/**
 * Dao to communicate with the term type table
 * @author avonva
 * @author shahaal
 *
 */
public class TermTypeDAO implements CatalogueEntityDAO<TermType> {

	private static final Logger LOGGER = LogManager.getLogger(TermTypeDAO.class);
	
	private Catalogue catalogue;

	/**
	 * Initialize the term type dao with the catalogue
	 * we want to communicate with.
	 * @param catalogue
	 */
	public TermTypeDAO( Catalogue catalogue ) {
		this.catalogue = catalogue;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}
	
	/**
	 * Get all the term types from the catalogue db
	 * @return
	 */
	public ArrayList< TermType > getAll () {

		ArrayList< TermType >  values = new ArrayList<>();

		// get all the distinct values of the attribute and return them
		// we get the term type in the order they were inserted in
		String query = "select * from APP.TERM_TYPE";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();
			stmt.setFetchSize(1000);
			
			// get the results
			try(ResultSet rs = stmt.executeQuery();) {
	
				// get all the detail levels
				while ( rs.next() ) {
					TermType tt = getByResultSet( rs );
					values.add( tt );
				}
				
				rs.close();
			}
			
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return values;
	}

	/**
	 * Insert a batch of term types
	 * @param termTypes
	 */
	public synchronized List<Integer> insert (Iterable<TermType> termTypes) {

		String query = "insert into APP.TERM_TYPE (TERM_TYPE_CODE, TERM_TYPE_LABEL) values (?, ?)";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			// for each term type
			for ( TermType type : termTypes ) {

				// clear the parameters
				stmt.clearParameters();

				// add the term type code
				stmt.setString( 1, type.getCode() );

				// add the term type description
				stmt.setString( 2, type.getLabel() );

				// add the batch
				stmt.addBatch();
			}

			// insert all the term types into the database
			stmt.executeBatch();

			// close the connection
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}
		
		return null;
	}

	@Override
	public int insert(TermType object) {
		
		return -1;
	}

	@Override
	public boolean remove(TermType object) {
		
		return true;
	}

	@Override
	public boolean update(TermType object) {
		
		return true;
	}

	@Override
	public TermType getById(int id) {
		
		return null;
	}

	@Override
	public TermType getByResultSet(ResultSet rs) throws SQLException {

		int id = rs.getInt( "TERM_TYPE_ID" );
		String code = rs.getString( "TERM_TYPE_CODE" );
		String name = rs.getString( "TERM_TYPE_LABEL" );

		TermType termType = new TermType( id, code, name );

		return termType;
	}
}
