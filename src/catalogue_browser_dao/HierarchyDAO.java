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
import catalogue_object.Hierarchy;
import catalogue_object.HierarchyBuilder;

/**
 * Class to manage all the database interactions regarding the hierarchy table
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class HierarchyDAO implements CatalogueEntityDAO<Hierarchy> {

	private static final Logger LOGGER = LogManager.getLogger(HierarchyDAO.class);

	private Catalogue catalogue;

	/**
	 * Initialize the hierarchy dao with the catalogue we want to communicate with.
	 * 
	 * @param catalogue
	 */
	public HierarchyDAO(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	/**
	 * Insert a new hierarchy into the database of the current catalogue
	 * 
	 * @param hierarchy the new hierarchy
	 * @return
	 */
	public int insert(Hierarchy hierarchy) {

		Collection<Hierarchy> hierarchies = new ArrayList<>();
		hierarchies.add(hierarchy);

		List<Integer> ids = insert(hierarchies);

		if (ids.isEmpty())
			return -1;

		return ids.get(0);
	}

	/**
	 * Insert a batch of new hierarchies into the database of the selected catalogue
	 * 
	 * @param hierarchy
	 * @return
	 */
	public synchronized List<Integer> insert(Iterable<Hierarchy> hierarchies) {

		ArrayList<Integer> ids = new ArrayList<>();

		// get all the hierarchies
		String query = "insert into APP.HIERARCHY ( HIERARCHY_CODE, HIERARCHY_NAME, HIERARCHY_LABEL,"
				+ "HIERARCHY_SCOPENOTE, HIERARCHY_APPLICABILITY, HIERARCHY_ORDER, HIERARCHY_STATUS, HIERARCHY_IS_MASTER,"
				+ "HIERARCHY_LAST_UPDATE, HIERARCHY_VALID_FROM, HIERARCHY_VALID_TO, "
				+ "HIERARCHY_DEPRECATED, HIERARCHY_GROUPS, HIERARCHY_VERSION )"
				+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			stmt.clearParameters();

			for (Hierarchy hierarchy : hierarchies) {

				stmt.setString(1, hierarchy.getCode());
				stmt.setString(2, hierarchy.getName());
				stmt.setString(3, hierarchy.getLabel());
				stmt.setString(4, hierarchy.getScopenotes());
				stmt.setString(5, hierarchy.getApplicability());
				stmt.setInt(6, hierarchy.getOrder());
				stmt.setString(7, hierarchy.getStatus());
				stmt.setBoolean(8, hierarchy.isMaster());

				if (hierarchy.getLastUpdate() != null)
					stmt.setTimestamp(9, hierarchy.getLastUpdate());
				else
					stmt.setNull(9, java.sql.Types.TIMESTAMP);

				if (hierarchy.getValidFrom() != null)
					stmt.setTimestamp(10, hierarchy.getValidFrom());
				else
					stmt.setNull(10, java.sql.Types.TIMESTAMP);

				if (hierarchy.getValidTo() != null)
					stmt.setTimestamp(11, hierarchy.getValidTo());
				else
					stmt.setNull(11, java.sql.Types.TIMESTAMP);

				stmt.setBoolean(12, hierarchy.isDeprecated());

				stmt.setString(13, hierarchy.getGroups());

				stmt.setString(14, hierarchy.getVersion());

				stmt.addBatch();
			}

			stmt.executeBatch();

			// update the terms ids with the ones given by the database
			try (ResultSet rs = stmt.getGeneratedKeys();) {

				if (rs != null) {
					while (rs.next())
						ids.add(rs.getInt(1));
					
					rs.close();
				}
			}

			stmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return ids;
	}

	/**
	 * Remove a hierarchy and all the applicabilities related to it
	 * 
	 * @param attr
	 * @return
	 */
	public boolean remove(Hierarchy hierarchy) {

		ParentTermDAO parentDao = new ParentTermDAO(catalogue);

		// first remove the applicabilities (dependency on the hierarchy we want to
		// delete)
		parentDao.removeByA2(hierarchy);

		// remove the relationships between the terms and the hierarchy
		String query = "delete from APP.HIERARCHY where HIERARCHY_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setInt(1, hierarchy.getId());

			stmt.executeUpdate();

			stmt.close();
			con.close();

			return true;

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}
		return false;
	}

	/**
	 * update a hierarchy of the database
	 * 
	 * @param hierarchy
	 * @return
	 */
	public boolean update(Hierarchy hierarchy) {

		// get all the hierarchies
		String query = "update APP.HIERARCHY set HIERARCHY_CODE = ?, HIERARCHY_NAME = ?, HIERARCHY_LABEL = ?,"
				+ "HIERARCHY_SCOPENOTE = ?, HIERARCHY_APPLICABILITY = ?, HIERARCHY_ORDER = ?, HIERARCHY_STATUS = ?,"
				+ "HIERARCHY_IS_MASTER = ?, HIERARCHY_LAST_UPDATE = ?, HIERARCHY_VALID_FROM = ?, HIERARCHY_VALID_TO = ?,"
				+ "HIERARCHY_DEPRECATED = ?, HIERARCHY_GROUPS = ?, HIERARCHY_VERSION = ?" + "where HIERARCHY_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setString(1, hierarchy.getCode());
			stmt.setString(2, hierarchy.getName());
			stmt.setString(3, hierarchy.getLabel());
			stmt.setString(4, hierarchy.getScopenotes());
			stmt.setString(5, hierarchy.getApplicability());
			stmt.setInt(6, hierarchy.getOrder());
			stmt.setString(7, hierarchy.getStatus());
			stmt.setBoolean(8, hierarchy.isMaster());

			if (hierarchy.getLastUpdate() != null)
				stmt.setTimestamp(9, hierarchy.getLastUpdate());
			else
				stmt.setNull(9, java.sql.Types.TIMESTAMP);

			if (hierarchy.getValidFrom() != null)
				stmt.setTimestamp(10, hierarchy.getValidFrom());
			else
				stmt.setNull(10, java.sql.Types.TIMESTAMP);

			if (hierarchy.getValidTo() != null)
				stmt.setTimestamp(11, hierarchy.getValidTo());
			else
				stmt.setNull(11, java.sql.Types.TIMESTAMP);

			stmt.setBoolean(12, hierarchy.isDeprecated());

			stmt.setString(13, hierarchy.getGroups());
			stmt.setString(14, hierarchy.getVersion());

			stmt.setInt(15, hierarchy.getId());

			stmt.executeUpdate();

			stmt.close();
			con.close();

			return true;

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return false;
	}

	/**
	 * Fetch a single database hierarchies (hierarchies and facets)
	 * 
	 * @return
	 */
	public Hierarchy getById(int id) {

		// get all the hierarchies
		String query = "select * from APP.HIERARCHY where HIERARCHY_ID = ?";

		Hierarchy hierarchy = null;
		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();
			stmt.setInt(1, id);

			try (ResultSet rs = stmt.executeQuery();) {

				// get the hierarchy if it was found
				if (rs.next())
					hierarchy = getByResultSet(rs);
				
				rs.close();
				
			}
			
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return hierarchy;
	}

	/**
	 * Get hierarchy by code
	 * 
	 * @return
	 */
	public Hierarchy getByCode(String code) {

		// get all the hierarchies
		String query = "select * from APP.HIERARCHY where HIERARCHY_CODE = ?";

		Hierarchy hierarchy = null;
		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();
			stmt.setString(1, code);

			try (ResultSet rs = stmt.executeQuery();) {
				// get the hierarchy if it was found
				if (rs.next())
					hierarchy = getByResultSet(rs);
				
				rs.close();
			}
			
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return hierarchy;
	}

	/**
	 * Get a hierarchy from the result set of a query
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public Hierarchy getByResultSet(ResultSet rs) throws SQLException {

		HierarchyBuilder builder = new HierarchyBuilder();

		builder.setCatalogue(catalogue);
		builder.setId(rs.getInt("HIERARCHY_ID"));
		builder.setCode(rs.getString("HIERARCHY_CODE"));
		builder.setName(rs.getString("HIERARCHY_NAME"));
		builder.setLabel(rs.getString("HIERARCHY_LABEL"));
		builder.setScopenotes(rs.getString("HIERARCHY_SCOPENOTE"));
		builder.setApplicability(rs.getString("HIERARCHY_APPLICABILITY"));
		builder.setOrder(rs.getInt("HIERARCHY_ORDER"));
		builder.setStatus(rs.getString("HIERARCHY_STATUS"));
		builder.setLastUpdate(rs.getTimestamp("HIERARCHY_LAST_UPDATE"));
		builder.setValidFrom(rs.getTimestamp("HIERARCHY_VALID_FROM"));
		builder.setValidTo(rs.getTimestamp("HIERARCHY_VALID_TO"));
		builder.setDeprecated(rs.getBoolean("HIERARCHY_DEPRECATED"));
		builder.setMaster(rs.getBoolean("HIERARCHY_IS_MASTER"));
		builder.setGroups(rs.getString("HIERARCHY_GROUPS"));
		builder.setVersion(rs.getString("HIERARCHY_VERSION"));

		return builder.build();
	}

	/**
	 * Fetch all the database hierarchies, return empty if no hierarchy is found
	 * 
	 * @return
	 */
	public ArrayList<Hierarchy> getAll() {

		// output array
		ArrayList<Hierarchy> hierarchies = new ArrayList<>();

		// get all the hierarchies
		String query = "select * from APP.HIERARCHY order by HIERARCHY_ORDER";
		  
		try (Connection con = catalogue.getConnection()) {

			PreparedStatement stmt = con.prepareStatement(query);
			stmt.setFetchSize(200);
			
			ResultSet rs = stmt.executeQuery();
			
			// add all the hierarchies
			while (rs.next())
				hierarchies.add(getByResultSet(rs));

			rs.close();
			stmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return hierarchies;
	}
}
