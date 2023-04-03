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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_object.Term;

/**
 * Class to manage all the databse interactions with the Term table.
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class TermDAO implements CatalogueEntityDAO<Term> {

	private static final Logger LOGGER = LogManager.getLogger(TermDAO.class);

	private Catalogue catalogue;

	/**
	 * Initialize term dao with the catalogue we want to communicate with
	 * 
	 * @param catalogue
	 */
	public TermDAO(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	/**
	 * Insert a single term into the db
	 * 
	 * @param t
	 * @return
	 */
	public int insert(Term t) {

		List<Term> terms = new ArrayList<>();
		terms.add(t);

		return insert(terms).get(0);
	}

	/**
	 * Insert a batch of new terms into the catalogue database return the term
	 * object with the ID field set.
	 * 
	 * @param t
	 * @return
	 */
	public synchronized List<Integer> insert(Iterable<Term> terms) {

		ArrayList<Integer> ids = new ArrayList<>();

		String query = "insert into APP.TERM (TERM_CODE, TERM_EXTENDED_NAME, "
				+ "TERM_SHORT_NAME, TERM_SCOPENOTE, TERM_DEPRECATED, TERM_LAST_UPDATE, "
				+ "TERM_VALID_FROM, TERM_VALID_TO, TERM_STATUS, TERM_VERSION ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			con.setAutoCommit(false);

			for (Term t : terms) {

				stmt.clearParameters();

				stmt.setString(1, t.getCode());
				stmt.setString(2, t.getName());
				stmt.setString(3, t.getShortName(false));
				stmt.setString(4, t.getScopenotes());
				stmt.setBoolean(5, t.isDeprecated());

				if (t.getLastUpdate() != null)
					stmt.setTimestamp(6, t.getLastUpdate());
				else
					stmt.setNull(6, java.sql.Types.TIMESTAMP);

				if (t.getValidFrom() != null)
					stmt.setTimestamp(7, t.getValidFrom());
				else
					stmt.setNull(7, java.sql.Types.TIMESTAMP);

				if (t.getValidTo() != null)
					stmt.setTimestamp(8, t.getValidTo());
				else
					stmt.setNull(8, java.sql.Types.TIMESTAMP);

				stmt.setString(9, t.getStatus());
				stmt.setString(10, t.getVersion());

				stmt.addBatch();
			}

			stmt.executeBatch();

			// if empty
			// update the terms ids with the ones given by the database
			try (ResultSet rs = stmt.getGeneratedKeys();) {

				while (rs.next())
					ids.add(rs.getInt(1));

				rs.close();
			}

			stmt.close();

			con.commit();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return ids;
	}

	/**
	 * Update the term fields in the DB. The last update field is modified to NOW
	 * 
	 * @param t, the term to be updated
	 * @return the updated term
	 */
	public boolean update(Term t) {

		String query = "update APP.TERM set TERM_CODE = ?, TERM_EXTENDED_NAME = ?, "
				+ "TERM_SHORT_NAME = ?, TERM_SCOPENOTE = ?, TERM_DEPRECATED = ?, TERM_LAST_UPDATE = ?,"
				+ "TERM_VALID_FROM = ?, TERM_VALID_TO = ?, TERM_STATUS = ?, TERM_VERSION = ? where TERM_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setString(1, t.getCode());
			stmt.setString(2, t.getName());
			stmt.setString(3, t.getShortName(false));
			stmt.setString(4, t.getScopenotes());
			stmt.setBoolean(5, t.isDeprecated());

			// set timestamps or null if needed
			Calendar cal = Calendar.getInstance();

			// set the last update to now
			stmt.setTimestamp(6, new Timestamp(cal.getTimeInMillis()));

			if (t.getValidFrom() != null)
				stmt.setTimestamp(7, t.getValidFrom());
			else
				stmt.setNull(7, Types.TIMESTAMP);

			if (t.getValidTo() != null)
				stmt.setTimestamp(8, t.getValidTo());
			else
				stmt.setNull(8, Types.TIMESTAMP);

			stmt.setString(9, t.getStatus());

			stmt.setString(10, t.getVersion());

			stmt.setInt(11, t.getId());

			// execute the statement
			stmt.executeUpdate();

			// close statement
			stmt.close();

			// close the connection
			con.close();

			return true;

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return false;
	}

	// update the term in the hashmap of terms (which is used to
	// store in RAM the terms). Use this to refresh term field in ram
	// once they are modified
	public void updateTermInRAM(Term term) {
		catalogue.addTerm(term);
	}

	/**
	 * Get all the terms
	 * 
	 * @return
	 */
	public Collection<Term> getAll() {
		return fetchTerms().values();
	}

	/**
	 * Get all the catalogue terms ordered by their code All term attributes are
	 * also fetched All applicabilities are also fetched
	 * 
	 * @return
	 */
	public HashMap<Integer, Term> fetchTerms() {

		HashMap<Integer, Term> terms = new HashMap<>();

		String query = "select * from APP.TERM";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query)) {

			stmt.setFetchSize(2000);
			
			ResultSet rs = stmt.executeQuery();
			
			// save all the terms
			while (rs.next()) {

				Term term = getByResultSet(rs);
				terms.put(term.getId(), term);
			}

			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
			return null;
		}

		return terms;
	}

	/**
	 * Retrieve a term from the database using its code
	 * 
	 * @param code
	 * @return
	 */
	public Term getByCode(String code) {

		String query = "select TERM_ID " + " from  APP.TERM " + " where upper( TERM_CODE ) = ? ";

		Term term = null;
		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			/* I want to retrieve the first level under the root */
			stmt.setString(1, code.toUpperCase());

			try (ResultSet rs = stmt.executeQuery();) {

				if (rs.next())
					term = catalogue.getTermById(rs.getInt("TERM_ID"));

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
			return null;
		}

		return term;
	}

	/**
	 * Retrieve a term from the database using its name ( which is unique in the
	 * catalogue )
	 * 
	 * @param code
	 * @return
	 */
	public Term getByName(String extendedName) {

		String query = "select * from APP.TERM where upper( TERM_EXTENDED_NAME ) = ? ";

		Term term = null;
		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			/* I want to retrieve the first level under the root */
			stmt.setString(1, extendedName.toUpperCase());

			try (ResultSet rs = stmt.executeQuery();) {

				if (rs.next())
					term = catalogue.getTermById(rs.getInt("TERM_ID"));

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
			return null;
		}

		return term;
	}

	/**
	 * Create a new term object starting from the result set
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public Term getByResultSet(ResultSet rs) throws SQLException {
		return getByResultSet(new Term(catalogue), rs, true);
	}

	/**
	 * add to a pre-existing term object all its fields contained in the result set
	 * 
	 * @param t
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public Term getByResultSet(Term t, ResultSet rs, boolean setId) throws SQLException {

		// set the term id if required
		if (setId)
			t.setId(rs.getInt("TERM_ID"));

		// set the term code
		t.setCode(rs.getString("TERM_CODE"));

		// set the term extended name
		t.setName(rs.getString("TERM_EXTENDED_NAME"));

		// set the term short name
		t.setDisplayAs(rs.getString("TERM_SHORT_NAME"));

		// set the term scopenotes
		t.setScopenotes(rs.getString("TERM_SCOPENOTE"));

		// set if the term is deprecated
		t.setDeprecated(rs.getBoolean("TERM_DEPRECATED"));

		// set last update of the term
		t.setLastUpdate(rs.getTimestamp("TERM_LAST_UPDATE"));

		// set valid from
		t.setValidFrom(rs.getTimestamp("TERM_VALID_FROM"));

		// set valid to
		t.setValidTo(rs.getTimestamp("TERM_VALID_TO"));

		// set status
		t.setStatus(rs.getString("TERM_STATUS"));

		// set version
		t.setVersion(rs.getString("TERM_VERSION"));

		return t;
	}

	/**
	 * check if the term name was already used before by other terms in the DB
	 * (Different from the one considered by the termCode)
	 * 
	 * @param termName
	 * @param extended, should extended name or short name be considered?
	 * @return
	 */
	public boolean isTermNameUnique(String termCode, String termName, boolean extended) {

		// true if empty name
		if (termName.isEmpty())
			return true;

		String query = "select TERM_ID from APP.TERM where ";

		// check on the correct field
		if (extended)
			query = query + "TERM_EXTENDED_NAME = ? ";
		else
			query = query + "TERM_SHORT_NAME = ? ";

		query = query + "and TERM_CODE <> ?";

		boolean noDupl = true;
		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setString(1, termName);
			stmt.setString(2, termCode);

			try (ResultSet rs = stmt.executeQuery();) {
				noDupl = !rs.next();

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
			return false;
		}

		// if there is a record in the next => a term with the same name
		// but different code actually exists
		return noDupl;
	}

	@Override
	public boolean remove(Term object) {

		return false;
	}

	@Override
	public Term getById(int id) {

		return null;
	}
}
