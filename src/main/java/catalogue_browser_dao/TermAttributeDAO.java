package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_object.Attribute;
import catalogue_object.Term;
import catalogue_object.TermAttribute;

/**
 * Class to manage all the database interactions with the term attributes table.
 * The term attributes table is the relation between attribtues and terms, it
 * answers to the question: which attributes are owned by each term and which
 * are their values?
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class TermAttributeDAO implements CatalogueRelationDAO<TermAttribute, Term, Attribute> {

	private static final Logger LOGGER = LogManager.getLogger(TermAttributeDAO.class);

	private Catalogue catalogue;

	/**
	 * Initialize the dao with the catalogue to be interrogated
	 * 
	 * @param catalogue
	 */
	public TermAttributeDAO(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public int insert(TermAttribute ta) {

		Collection<TermAttribute> tas = new ArrayList<>();
		tas.add(ta);
		List<Integer> ids = insert(tas);

		if (ids.isEmpty())
			return -1;

		return ids.get(0);
	}

	public synchronized List<Integer> insert(Iterable<TermAttribute> tas) {

		ArrayList<Integer> ids = new ArrayList<>();

		// create the base query for each record
		String query = "INSERT INTO APP.TERM_ATTRIBUTE (TERM_ID, ATTR_ID, " + "ATTR_VALUE ) VALUES (" + "?, ?, ? )";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			con.setAutoCommit(false);

			// get the records one by one and insert them into the database
			for (TermAttribute ta : tas) {

				// set the term id
				stmt.setInt(1, ta.getTerm().getId());

				// set the attribute id
				stmt.setInt(2, ta.getAttribute().getId());

				// set the value parameter
				stmt.setString(3, ta.getValue());

				// add the record to the batch
				stmt.addBatch();
			}

			// execute the batch of insertions
			stmt.executeBatch();

			// get all the ids
			try (ResultSet rs = stmt.getGeneratedKeys();) {

				if (rs != null) {
					while (rs.next())
						ids.add(rs.getInt(1));

					rs.close();
				}
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

	@Override
	public boolean remove(TermAttribute object) {
		return true;
	}

	/**
	 * Update the value of the term attribute related to the term
	 * 
	 * @param termId
	 * @param ta
	 * @return
	 */
	public boolean update(TermAttribute ta) {

		// get all the hierarchies
		String query = "update APP.TERM_ATTRIBUTE set ATTR_VALUE = ? where ATTR_ID = ? and TERM_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setString(1, ta.getValue());
			stmt.setInt(2, ta.getAttribute().getId());
			stmt.setInt(3, ta.getTerm().getId());

			stmt.executeUpdate();

			stmt.close();
			con.close();

			return true;

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public TermAttribute getById(int id) {

		return null;
	}

	/**
	 * Get all the term attributes contained in the db
	 * 
	 * @author shahaal
	 */
	public ArrayList<TermAttribute> getAll() {

		ArrayList<TermAttribute> tas = new ArrayList<>();

		// get all the parent terms and hierarchies
		String query = "select * from APP.ATTRIBUTE A inner join APP.TERM_ATTRIBUTE TA on A.ATTR_ID = TA.ATTR_ID";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query)) {

			stmt.setFetchSize(200);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {

				// create the term attribute object
				TermAttribute ta = getByResultSet(rs);

				tas.add(ta);
			}

			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return tas;
	}

	/**
	 * Get all the attributes of a single term from the selected catalogue
	 * 
	 * @param t
	 * @return
	 */
	public ArrayList<TermAttribute> getByA1(Term term) {

		ArrayList<TermAttribute> attributes = new ArrayList<>();

		// get the term with id = the input term id
		String query = "select A.*, TA.TERM_ATTR_ID, TA.ATTR_VALUE  " + "from APP.TERM_ATTRIBUTE TA "
				+ "inner join APP.ATTRIBUTE A on TA.ATTR_ID = A.ATTR_ID " + "where TA.TERM_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, term.getId());

			// get the results
			try (ResultSet rs = stmt.executeQuery()) {

				AttributeDAO attrDao = new AttributeDAO(catalogue);

				while (rs.next()) {

					// get the attribute
					Attribute attribute = attrDao.getByResultSet(rs);

					// get the term attribute id
					int id = rs.getInt("TERM_ATTR_ID");

					// get the attribute value
					String value = rs.getString("ATTR_VALUE");

					// create the term attribute with attribute and value
					TermAttribute attr = new TermAttribute(id, term, attribute, value);

					// add the attribute
					attributes.add(attr);
				}

				rs.close();

			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return attributes;
	}

	@Override
	public Collection<TermAttribute> getByA2(Attribute object) {

		return null;
	}

	/**
	 * Remove all the term attributes related to a single term from the db
	 * 
	 * @param con
	 * @param ta
	 * @return
	 */
	public boolean removeByA1(Term term) {

		// create insert query
		String query = "delete from APP.TERM_ATTRIBUTE where TERM_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, term.getId());

			// remove all the term attributes related to that term
			stmt.executeUpdate();

			stmt.close();
			con.close();

			return true;
		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Remove all the term attributes related to an attribute (used to remove an
	 * attribute from the database)
	 * 
	 * @param con
	 * @param ta
	 * @return
	 */
	public boolean removeByA2(Attribute attribute) {

		// create insert query
		String query = "delete from APP.TERM_ATTRIBUTE where ATTR_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, attribute.getId());

			// remove all the term attributes related to that term
			stmt.executeUpdate();

			stmt.close();
			con.close();

			return true;
		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Update all the term attributes of the term, first all the term attributes are
	 * removed and then we insert them using the one contained in the term object
	 * 
	 * @param term we update the term attributes of this term
	 */
	public boolean updateByA1(Term term) {

		// get the compacted term attributes
		ArrayList<TermAttribute> attrs = term.getAttributes();

		String query = "insert into APP.TERM_ATTRIBUTE (TERM_ID, ATTR_ID, ATTR_VALUE) values (?, ?, ?)";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			// remove all the term attributes to refresh them
			removeByA1(term);
			
			// for each attribute create a batch update
			for (TermAttribute ta : attrs) {

				// set the parameters
				stmt.setInt(1, term.getId());
				
				stmt.setInt(2, ta.getAttribute().getId());

				// set the value
				stmt.setString(3, ta.getValue());

				// add the query as batch update
				stmt.addBatch();
			}

			// execute the batch update
			stmt.executeBatch();

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

	/**
	 * Update the term attributes which has as attribute the selected one.
	 * 
	 * @param attribute
	 */
	public boolean updateByA2(Attribute attribute) {
		return true;
	}

	/**
	 * Get the term attribute from the result set. The result set should contain the
	 * information of term id, attribute id and attribute value
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public TermAttribute getByResultSet(ResultSet rs) throws SQLException {

		AttributeDAO attrDao = new AttributeDAO(catalogue);

		// get the attribute
		Attribute attribute = attrDao.getByResultSet(rs);

		// get the term from the hash map
		Term term = catalogue.getTermById(rs.getInt("TERM_ID"));

		// get the term attribute id
		int id = rs.getInt("TERM_ATTR_ID");

		// get the value of the attribute
		String value = rs.getString("ATTR_VALUE");

		// create the term attribute object
		TermAttribute ta = new TermAttribute(id, term, attribute, value);

		return ta;
	}
}
