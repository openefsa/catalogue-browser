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
import catalogue_object.AttributeBuilder;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import naming_convention.SpecialValues;

/**
 * Class to manage all the database interactions with the attribute table
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class AttributeDAO implements CatalogueEntityDAO<Attribute> {

	private static final Logger LOGGER = LogManager.getLogger(AttributeDAO.class);

	private Catalogue catalogue;

	/**
	 * Initialize the dao for the selected catalogue
	 * 
	 * @param catalogue
	 */
	public AttributeDAO(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	/**
	 * Insert a single attribute into the database
	 * 
	 * @param attr
	 * @return
	 */
	public synchronized int insert(Attribute attr) {

		Collection<Attribute> attrs = new ArrayList<>();
		attrs.add(attr);

		List<Integer> ids = insert(attrs);

		if (ids.isEmpty())
			return -1;

		return ids.get(0);
	}

	/**
	 * Insert a batch of attributes in the db of the selected catalogue
	 * 
	 * @param hierarchy
	 * @return
	 */
	public synchronized List<Integer> insert(Iterable<Attribute> attrs) {

		ArrayList<Integer> ids = new ArrayList<>();

		// set the query to insert a new attribtue
		String query = "insert into APP.ATTRIBUTE (ATTR_CODE, ATTR_NAME , "
				+ "ATTR_LABEL, ATTR_SCOPENOTE, ATTR_REPORTABLE, ATTR_VISIBLE, ATTR_SEARCHABLE,"
				+ "ATTR_ORDER, ATTR_TYPE, ATTR_MAX_LENGTH, ATTR_PRECISION, ATTR_SCALE,"
				+ "ATTR_CAT_CODE, ATTR_SINGLE_REPEATABLE, ATTR_INHERITANCE,"
				+ "ATTR_UNIQUENESS, ATTR_TERM_CODE_ALIAS, ATTR_LAST_UPDATE, ATTR_VALID_FROM,"
				+ "ATTR_VALID_TO, ATTR_STATUS, ATTR_DEPRECATED, ATTR_VERSION ) values ("
				+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			stmt.clearParameters();

			for (Attribute attr : attrs) {

				stmt.setString(1, attr.getCode());
				stmt.setString(2, attr.getName());
				stmt.setString(3, attr.getLabel());
				stmt.setString(4, attr.getScopenotes());
				stmt.setString(5, attr.getReportable());
				stmt.setBoolean(6, attr.isVisible());
				stmt.setBoolean(7, attr.isSearchable());
				stmt.setInt(8, attr.getOrder());
				stmt.setString(9, attr.getType());
				stmt.setInt(10, attr.getMaxLength());
				stmt.setInt(11, attr.getPrecision());
				stmt.setInt(12, attr.getScale());
				stmt.setString(13, attr.getCatalogueCode());
				stmt.setString(14, attr.getSingleOrRepeatable());
				stmt.setString(15, attr.getInheritance());
				stmt.setBoolean(16, attr.isUniqueness());
				stmt.setBoolean(17, attr.isTermCodeAlias());

				if (attr.getLastUpdate() != null)
					stmt.setTimestamp(18, attr.getLastUpdate());
				else
					stmt.setNull(18, java.sql.Types.TIMESTAMP);

				if (attr.getValidFrom() != null)
					stmt.setTimestamp(19, attr.getValidFrom());
				else
					stmt.setNull(19, java.sql.Types.TIMESTAMP);

				if (attr.getValidTo() != null)
					stmt.setTimestamp(20, attr.getValidTo());
				else
					stmt.setNull(20, java.sql.Types.TIMESTAMP);

				stmt.setString(21, attr.getStatus());

				stmt.setBoolean(22, attr.isDeprecated());

				stmt.setString(23, attr.getVersion());

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
	 * Remove an attribute from the database
	 * 
	 * @param attr
	 * @return
	 */
	public boolean remove(Attribute attr) {

		// initialize term attribute dao
		TermAttributeDAO taDao = new TermAttributeDAO(catalogue);

		// remove all the dependencies
		taDao.removeByA2(attr);

		// set the query to insert a new attribute
		String query = "delete from APP.ATTRIBUTE where ATTR_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			// execute the query

			stmt.clearParameters();

			stmt.setInt(1, attr.getId());

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
	public boolean update(Attribute attr) {

		// get all the hierarchies
		String query = "update APP.ATTRIBUTE set ATTR_CODE = ?, ATTR_NAME = ?, ATTR_LABEL = ?,"
				+ "ATTR_SCOPENOTE = ?, ATTR_REPORTABLE = ?, ATTR_VISIBLE = ?, ATTR_SEARCHABLE = ?,"
				+ "ATTR_ORDER = ?, ATTR_TYPE = ?, ATTR_MAX_LENGTH = ?, ATTR_PRECISION = ?, ATTR_SCALE = ?,"
				+ "ATTR_CAT_CODE = ?, ATTR_SINGLE_REPEATABLE = ?, ATTR_INHERITANCE = ?, ATTR_UNIQUENESS = ?,"
				+ "ATTR_TERM_CODE_ALIAS = ?, ATTR_LAST_UPDATE = ?, ATTR_VALID_FROM = ?,"
				+ "ATTR_VALID_TO = ?, ATTR_STATUS = ?, ATTR_DEPRECATED = ?, ATTR_VERSION = ? where ATTR_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setString(1, attr.getCode());
			stmt.setString(2, attr.getName());
			stmt.setString(3, attr.getLabel());
			stmt.setString(4, attr.getScopenotes());
			stmt.setString(5, attr.getReportable());
			stmt.setBoolean(6, attr.isVisible());
			stmt.setBoolean(7, attr.isSearchable());
			stmt.setInt(8, attr.getOrder());
			stmt.setString(9, attr.getType());
			stmt.setInt(10, attr.getMaxLength());
			stmt.setInt(11, attr.getPrecision());
			stmt.setInt(12, attr.getScale());
			stmt.setString(13, attr.getCatalogueCode());
			stmt.setString(14, attr.getSingleOrRepeatable());
			stmt.setString(15, attr.getInheritance());
			stmt.setBoolean(16, attr.isUniqueness());
			stmt.setBoolean(17, attr.isTermCodeAlias());

			if (attr.getLastUpdate() != null)
				stmt.setTimestamp(18, attr.getLastUpdate());
			else
				stmt.setNull(18, java.sql.Types.TIMESTAMP);

			if (attr.getValidFrom() != null)
				stmt.setTimestamp(19, attr.getValidFrom());
			else
				stmt.setNull(19, java.sql.Types.TIMESTAMP);

			if (attr.getValidTo() != null)
				stmt.setTimestamp(20, attr.getValidTo());
			else
				stmt.setNull(20, java.sql.Types.TIMESTAMP);

			stmt.setString(21, attr.getStatus());
			stmt.setBoolean(22, attr.isDeprecated());
			stmt.setString(23, attr.getVersion());

			stmt.setInt(24, attr.getId());

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
	 * Get an attribute by its id
	 * 
	 * @param attrCode
	 * @return
	 */
	public Attribute getById(int id) {

		// output attribute
		Attribute attr = null;

		// get all the catalogue attributes and add to the implicit facets list
		String query = "select * from APP.ATTRIBUTE where ATTR_ID = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setInt(1, id);

			// add the results to the output array
			try (ResultSet rs = stmt.executeQuery()) {

				// create an attribute from the result set
				// if attribute found
				if (rs.next())
					attr = getByResultSet(rs);
				
				rs.close();
			}
			
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return attr;
	}

	/**
	 * Get an attribute by its code
	 * 
	 * @param attrCode
	 * @return
	 */
	public Attribute getByCode(String attrCode) {

		// output attribute
		Attribute attr = null;

		// get all the catalogue attributes and add to the implicit facets list
		String query = "select * from APP.ATTRIBUTE where ATTR_CODE = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, attrCode);

			// add the results to the output array
			try (ResultSet rs = stmt.executeQuery();) {

				// create an attribute from the result set
				// if attribute found
				if (rs.next())
					attr = getByResultSet(rs);
				
				rs.close();
			}
			
			stmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return attr;
	}

	/**
	 * Get an attribute by its name
	 * 
	 * @param attrCode
	 * @return
	 */
	public Attribute getByName(String attrName) {

		// output attribute
		Attribute attr = null;

		// get all the catalogue attributes and add to the implicit facets list
		String query = "select * from APP.ATTRIBUTE where ATTR_NAME = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, attrName);

			// add the results to the output array
			try (ResultSet rs = stmt.executeQuery();) {

				// create an attribute from the result set
				// if attribute found
				if (rs.next())
					attr = getByResultSet(rs);
				
				rs.close();
			}
			
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return attr;
	}

	/**
	 * Get all attributes from the database
	 * 
	 * @return
	 */
	public ArrayList<Attribute> getAll() {

		return fetchAttributes("-all-", false);
	}

	/**
	 * Get all the facet categories of the catalogue
	 * 
	 * @return
	 */
	public ArrayList<Attribute> getFacetCategories() {

		ArrayList<Attribute> facetCategories = new ArrayList<>();

		ArrayList<Attribute> catAttrs = fetchAttributes("catalogue", false);

		for (Attribute attr : catAttrs) {

			if (attr.isFacetCategory())
				facetCategories.add(attr);
		}

		return facetCategories;
	}

	/**
	 * Get all the attributes which are not facet categories
	 * 
	 * @return
	 */
	public ArrayList<Attribute> getNonFacetAttributes() {

		ArrayList<Attribute> attrs = new ArrayList<>();

		ArrayList<Attribute> catAttrs = getAll();

		for (Attribute attr : catAttrs) {

			if (!attr.isFacetCategory())
				attrs.add(attr);
		}

		return attrs;
	}

	/**
	 * Get all the non catalogue attributes
	 */
	public ArrayList<Attribute> fetchNonCatalogueAttributes() {
		return fetchAttributes("catalogue", true);
	}

	/**
	 * Fetch attributes, specify a type for selecting only a subset of attributes
	 * set "-all-" for fetching all the attributes Possible types: xs:string,
	 * xs:integer, xs:decimal, xs:boolean, catalogue Moreover you can choose to
	 * select all the attributes except for one type. In this case you have to
	 * insert in the attrType the attribute type that you want to exclude, and then
	 * set the exclude boolean to true+ The exclude boolean is ignored if attrType =
	 * "-all-"
	 * 
	 * @return
	 */
	public ArrayList<Attribute> fetchAttributes(String attrType, boolean exclude) {

		ArrayList<String> attrTypes = new ArrayList<>();
		attrTypes.add(attrType);

		return fetchAttributes(attrTypes, exclude);
	}

	/**
	 * Fetch attributes, specify a type for selecting only a subset of attributes
	 * set "-all-" for fetching all the attributes Possible types: xs:string,
	 * xs:integer, xs:decimal, xs:boolean, catalogue We can also set multiple types!
	 * (if we want only integer and boolean for example) Moreover you can choose to
	 * select all the attributes except for one type. In this case you have to
	 * insert in the attrType the attribute type that you want to exclude, and then
	 * set the exclude boolean to true The exclude boolean is ignored if attrType =
	 * "-all-"
	 * 
	 * @param attrTypes the type of attributes to select or remove (depending on the
	 *                  exclude boolean)
	 * @param exclude   false if we want to select all the attributes which have a
	 *                  type contained in attrTypes, false to exclude them
	 * @return {@code ArrayList<Attribute>} containing the selected attributes
	 */

	public ArrayList<Attribute> fetchAttributes(ArrayList<String> attrTypes, boolean exclude) {

		// output array
		ArrayList<Attribute> attrs = new ArrayList<>();

		// get all the catalogue attributes and add to the implicit facets list
		String query = "select * from APP.ATTRIBUTE";

		// if there is an attr type
		if (!attrTypes.contains("-all-")) {

			// exclude the type if required, otherwise search for the match
			String op = exclude ? "<>" : "=";

			query = query + " where ";

			for (int i = 0; i < attrTypes.size(); i++) {
				query = query + "APP.ATTRIBUTE.ATTR_TYPE " + op + " '" + attrTypes.get(i) + "'";

				// add and clause if it is not the last one
				if (i < attrTypes.size() - 1)
					query = query + " and ";
			}
		}

		// order
		query = query + " order by ATTR_ORDER";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setFetchSize(200);
			
			// add the results to the output array
			try (ResultSet rs = stmt.executeQuery();) {

				while (rs.next()) {

					// create an attribute from the result set
					Attribute attr = getByResultSet(rs);

					// add the attribute to the output array
					attrs.add(attr);

				}
				
				rs.close();
			}

			stmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return attrs;
	}

	/**
	 * Create an Attribute object starting from the DB result set
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public Attribute getByResultSet(ResultSet rs) throws SQLException {

		// get the attribute builder
		AttributeBuilder builder = new AttributeBuilder();

		// set all the parameters
		builder.setCatalogue(catalogue);
		builder.setId(rs.getInt("ATTR_ID"));
		builder.setCode(rs.getString("ATTR_CODE"));
		builder.setName(rs.getString("ATTR_NAME"));
		builder.setLabel(rs.getString("ATTR_LABEL"));
		builder.setScopenotes(rs.getString("ATTR_SCOPENOTE"));
		builder.setReportable(rs.getString("ATTR_REPORTABLE"));
		builder.setVisible(rs.getBoolean("ATTR_VISIBLE"));
		builder.setSearchable(rs.getBoolean("ATTR_SEARCHABLE"));
		builder.setOrder(rs.getInt("ATTR_ORDER"));
		builder.setType(rs.getString("ATTR_TYPE"));
		builder.setMaxLength(rs.getInt("ATTR_MAX_LENGTH"));
		builder.setPrecision(rs.getInt("ATTR_PRECISION"));
		builder.setScale(rs.getInt("ATTR_SCALE"));
		builder.setCatalogueCode(rs.getString("ATTR_CAT_CODE"));
		builder.setSingleOrRepeatable(rs.getString("ATTR_SINGLE_REPEATABLE"));
		builder.setInheritance(rs.getString("ATTR_INHERITANCE"));
		builder.setUniqueness(rs.getBoolean("ATTR_UNIQUENESS"));
		builder.setTermCodeAlias(rs.getBoolean("ATTR_TERM_CODE_ALIAS"));
		builder.setStatus(rs.getString("ATTR_STATUS"));
		builder.setLastUpdate(rs.getTimestamp("ATTR_LAST_UPDATE"));
		builder.setValidFrom(rs.getTimestamp("ATTR_VALID_FROM"));
		builder.setValidTo(rs.getTimestamp("ATTR_VALID_TO"));
		builder.setDeprecated(rs.getBoolean("ATTR_DEPRECATED"));
		builder.setVersion(rs.getString("ATTR_VERSION"));

		return builder.build();
	}

	/**
	 * Fetch all the generic attributes, that is, we exclude implicit facets, all
	 * facets, detail levels, term types and all the catalogue attributes
	 * 
	 * @return
	 */
	public ArrayList<Attribute> fetchGeneric() {

		// output array
		ArrayList<Attribute> attrs = new ArrayList<>();

		// get all the generic attributes
		String query = "select * from APP.ATTRIBUTE where ATTR_NAME <> ? "
				+ "and ATTR_NAME <> ? and ATTR_NAME <> ? and ATTR_NAME <> ? and ATTR_TYPE <> ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setString(1, SpecialValues.ALL_FACETS_NAME);
			stmt.setString(2, SpecialValues.IMPLICIT_FACETS_NAME);
			stmt.setString(3, SpecialValues.DETAIL_LEVEL_NAME);
			stmt.setString(4, SpecialValues.TERM_TYPE_NAME);
			stmt.setString(5, Attribute.catalogueTypeName);

			// add the results to the output array
			try (ResultSet rs = stmt.executeQuery();) {

				while (rs.next()) {

					// create an attribute from the result set
					Attribute attr = getByResultSet(rs);

					// add the attribute to the output array
					attrs.add(attr);

				}
				
				rs.close();
			}
			
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return attrs;
	}

	/**
	 * Get all the attributes names which can be added to a term (non repeatable
	 * attributes can be added only one time!)
	 * 
	 * @param t
	 * @return
	 */
	public ArrayList<Attribute> getApplicableAttributes(Term t) {

		ArrayList<Attribute> attrs = fetchGeneric();
		ArrayList<TermAttribute> termAttrs = t.getGenericAttributes();

		// remove non repeatable attributes which are already present
		// into the term attributes
		for (TermAttribute ta : termAttrs) {
			if (!ta.getAttribute().isRepeatable()) {
				attrs.remove(ta.getAttribute());
			}
		}

		return attrs;
	}

	/**
	 * Get all the attributes labels which can be added to a term (non repeatable
	 * attributes can be added only one time!)
	 * 
	 * @param t
	 * @return
	 */
	public ArrayList<String> getApplicableAttributesLabel(Term t) {

		ArrayList<Attribute> attrs = getApplicableAttributes(t);

		// get the remaining attributes names
		ArrayList<String> attrsNames = new ArrayList<>();

		for (int i = 0; i < attrs.size(); i++) {
			attrsNames.add(attrs.get(i).getLabel());
		}

		return attrsNames;
	}
}
