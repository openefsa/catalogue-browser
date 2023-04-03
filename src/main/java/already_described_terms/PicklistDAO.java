package already_described_terms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_object.Term;

/**
 * get picklist info from database
 * 
 * @author shahaal
 * @author avonva
 */
public class PicklistDAO implements CatalogueEntityDAO<Picklist> {

	private static final Logger LOGGER = LogManager.getLogger(PicklistDAO.class);

	private Catalogue catalogue;

	/**
	 * Initialize the picklist dao with the catalogue we want to communicate with
	 * 
	 * @param catalogue
	 */
	public PicklistDAO(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	/**
	 * Check if the db does not contain any picklist
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return getPicklistsCount() == 0;
	}

	/**
	 * Import a picklist and all its terms into the database
	 * 
	 * @param picklist
	 */
	public void importPicklist(Picklist picklist) {

		// create a new picklist record if it does not exist
		if (!hasPicklist(picklist))
			insert(picklist);

		// set the picklist id from the db
		picklist.setId(getPicklistFromCode(picklist.getCode()).getId());

		// delete all the picklist terms
		deletePicklistTerms(picklist);

		// insert into the database all the new picklist terms
		insertPicklistTerms(picklist);
	}

	/**
	 * Insert a picklist into the database
	 * 
	 * @param picklist
	 */
	public int insert(Picklist picklist) {

		int id = -1;

		String query = "insert into APP.PICKLIST (PICKLIST_CODE) values (?)";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			stmt.clearParameters();

			stmt.setString(1, picklist.getCode());

			stmt.executeUpdate();

			// update the terms ids with the ones given by the database
			try (ResultSet rs = stmt.getGeneratedKeys();) {

				if (rs.next())
					id = rs.getInt(1);

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return id;
	}

	/**
	 * Insert all the picklist term into the database
	 * 
	 * @param picklist
	 */
	private void insertPicklistTerms(Picklist picklist) {

		String query = "insert into APP.PICKLIST_TERM (PICKLIST_TERM_LEVEL, " + "PICKLIST_TERM_CODE, "
				+ "PICKLIST_BASETERM_CODE, " + "PICKLIST_TERM_LABEL, " + "PICKLIST_ID) values (?, ?, ?, ?, ?)";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			// for each picklist term add it into the database
			for (PicklistTerm term : picklist.getTerms()) {

				stmt.clearParameters();

				stmt.setInt(1, term.getLevel());
				stmt.setString(2, term.getCode());
				// get the base term code
				stmt.setString(3, term.getCode().split("#")[0]);
				stmt.setString(4, term.getLabel());
				stmt.setInt(5, picklist.getId());

				stmt.addBatch();
			}

			stmt.executeBatch();

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}
	}

	/**
	 * Check if a pick-list is already inserted into the database
	 * 
	 * @param picklist
	 * @return
	 */
	public boolean hasPicklist(Picklist picklist) {

		// number of picklist with same id or code in the database
		int count = 0;

		String query = "select count(*) from APP.PICKLIST where PICKLIST_ID = ? or PICKLIST_CODE = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, picklist.getId());
			stmt.setString(2, picklist.getCode());

			try (ResultSet rs = stmt.executeQuery();) {

				// get the count of the picklist
				if (rs.next())
					count = rs.getInt(1);

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		// return true if at least one picklist is found
		return count > 0;
	}

	/**
	 * TODO non funziona è da mettere l'id del termine, o il suo codice! Update a
	 * picklist
	 * 
	 * @param picklist
	 */
	public boolean update(Picklist picklist) {

		String query = "update APP.PICKLIST_TERM set PICKLIST_TERM_LEVEL = ?, PICKLIST_BASETERM_CODE = ?, "
				+ "PICKLIST_TERM_CODE = ?," + "PICKLIST_TERM_LABEL = ? where PICKLIST_ID = ? and PICKLIST_TERM_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			// for each picklist term
			for (PicklistTerm term : picklist.getTerms()) {

				stmt.clearParameters();

				stmt.setInt(1, term.getLevel());

				stmt.setString(2, term.getCode().split("#")[0]);

				stmt.setString(3, term.getCode());

				stmt.setString(4, term.getLabel());

				stmt.setInt(5, picklist.getId());
			}

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
	 * Get the picklist from the picklist code
	 * 
	 * @param code
	 * @return
	 */
	public Picklist getPicklistFromCode(String code) {

		Picklist picklist = null;

		String query = "select * from APP.PICKLIST where PICKLIST_CODE = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setString(1, code);

			try (ResultSet rs = stmt.executeQuery();) {

				// create the picklist
				if (rs.next())
					picklist = getByResultSet(rs);

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return picklist;
	}

	/**
	 * Delete all the picklist terms
	 * 
	 * @param code
	 * @return
	 */
	private void deletePicklistTerms(Picklist picklist) {

		String query = "delete from APP.PICKLIST_TERM where PICKLIST_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, picklist.getId());

			stmt.executeUpdate();

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}
	}

	/**
	 * Get all the picklists
	 * 
	 * @return
	 */
	public ArrayList<Picklist> getAll() {

		// output array
		ArrayList<Picklist> picklists = new ArrayList<>();

		String query = "select * from APP.PICKLIST";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query);
				ResultSet rs = stmt.executeQuery();) {

			// for each picklist
			while (rs.next()) {

				// create a new picklist
				Picklist picklist = getByResultSet(rs);

				// set the picklist terms
				picklist.setTerms(getPicklistTerms(picklist));

				// add the picklist to the output list
				picklists.add(picklist);
			}

			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return picklists;
	}

	/**
	 * Get the number of picklist which are stored into the database
	 * 
	 * @return
	 */
	public int getPicklistsCount() {

		// output integer
		int count = 0;

		String query = "select count(*) from APP.PICKLIST";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query);
				ResultSet rs = stmt.executeQuery();) {

			// get the count
			if (rs.next())
				count = rs.getInt(1);

			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return count;
	}

	/**
	 * Get all the terms of a pick-list. Return empty list if no terms found
	 * 
	 * @param picklist
	 * @return
	 */
	public ArrayList<PicklistTerm> getPicklistTerms(Picklist picklist) {
		
		// output array
		ArrayList<PicklistTerm> terms = new ArrayList<>();
		
		String query = "select * from APP.PICKLIST_TERM where PICKLIST_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();
			
			stmt.setInt(1, picklist.getId());

			try (ResultSet rs = stmt.executeQuery();) {

				// for each pick-list term
				while (rs.next()) {

					int level = rs.getInt("PICKLIST_TERM_LEVEL");
					String code = rs.getString("PICKLIST_TERM_CODE");
					String label = rs.getString("PICKLIST_TERM_LABEL");

					// add the current term to the output list
					PicklistTerm term = new PicklistTerm(catalogue, level, code, label);
					terms.add(term);
				}

				rs.close();
				
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return terms;
	}

	/**
	 * This method returns all the picklist terms which contains the "term" as base
	 * term, implicit facet or explicit facet
	 * 
	 * @param term
	 * @return
	 */
	public ArrayList<PicklistTerm> searchTermInPicklist(Picklist picklist, Term term) {

		// output array
		ArrayList<PicklistTerm> terms = new ArrayList<>();

		// first get the base term information using the base term codes
		// then get the implicit facets string related to the base term
		// then select all the terms which contains in their implicit facets or explicit
		// facets the
		// term passed in input.
		// TA.ATTR_VALUE contains the implicit facet string (baseTerm#F01.A05T6$...)
		// PT.PICKLIST_TERM_CODE contains the explicit facet string in the same format
		// of the implicit ones
		String query = "select PT.PICKLIST_TERM_LEVEL, PT.PICKLIST_TERM_CODE, PT.PICKLIST_TERM_LABEL "
				+ "from APP.PICKLIST_TERM as PT "
				+ "inner join APP.TERM as T on PT.PICKLIST_BASETERM_CODE = T.TERM_CODE "
				+ "inner join APP.TERM_ATTRIBUTE as TA on TA.TERM_ID = T.TERM_ID "
				+ "inner join APP.ATTRIBUTE as A on A.ATTR_ID = TA.ATTR_ID "
				+ "where PT.PICKLIST_ID = ? and A.ATTR_NAME = ? and ( TA.ATTR_VALUE like ? or PT.PICKLIST_TERM_CODE like ? )";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, picklist.getId());

			// we search in all the implicit facets of the term (inherited included)
			stmt.setString(2, "allFacets");
			stmt.setString(3, "%" + term.getCode() + "%");
			stmt.setString(4, "%" + term.getCode() + "%");

			try (ResultSet rs = stmt.executeQuery();) {

				// for each picklist term
				while (rs.next()) {

					int level = rs.getInt("PICKLIST_TERM_LEVEL");
					String code = rs.getString("PICKLIST_TERM_CODE");
					String label = rs.getString("PICKLIST_TERM_LABEL");

					// add the current term to the output list
					PicklistTerm picklistTerm = new PicklistTerm(catalogue, level, code, label);
					terms.add(picklistTerm);
				}

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return terms;
	}

	@Override
	public boolean remove(Picklist picklist) {

		// delete all the pick-list terms
		// to remove dependencies
		deletePicklistTerms(picklist);

		String query = "delete from APP.PICKLIST where PICKLIST_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, picklist.getId());
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
	public Picklist getById(int id) {

		return null;
	}

	@Override
	public Picklist getByResultSet(ResultSet rs) throws SQLException {

		int id = rs.getInt("PICKLIST_ID");
		String code = rs.getString("PICKLIST_CODE");

		// create a new picklist
		Picklist picklist = new Picklist(id, code);

		return picklist;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public List<Integer> insert(Iterable<Picklist> attrs) {

		return null;
	}

}
