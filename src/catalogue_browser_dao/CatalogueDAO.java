package catalogue_browser_dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue.CatalogueBuilder;
import dcf_manager.Dcf.DcfType;
import dcf_user.User;
import sas_remote_procedures.XmlUpdateFileDAO;
import utilities.GlobalUtil;

/**
 * DAO used to manage catalogues (Catalogue table of the database is considered)
 * 
 * @author avonva
 * @author shahaal
 */
public class CatalogueDAO implements ICatalogueDAO {

	private static final Logger LOGGER = LogManager.getLogger(CatalogueDAO.class);

	@Override
	public void setCatalogue(Catalogue catalogue) {
	}

	/**
	 * Insert a new catalogue into the main catalogues database. Moreover, the
	 * catalogue database is also created.
	 * 
	 * @param catalogue
	 */
	public synchronized int insert(Catalogue catalogue) {

		int id = -1;
		String query = "insert into app.CATALOGUE (" + "CAT_DCF_TYPE," + "CAT_VERSION," + "CAT_CODE," + "CAT_NAME,"
				+ "CAT_LABEL," + "CAT_SCOPENOTE," + "CAT_TERM_CODE_MASK," + "CAT_TERM_CODE_LENGTH,"
				+ "CAT_TERM_MIN_CODE," + "CAT_ACCEPT_NON_STANDARD_CODES," + "CAT_GENERATE_MISSING_CODES,"
				+ "CAT_STATUS," + "CAT_GROUPS," + "CAT_LAST_UPDATE," + "CAT_VALID_FROM," + "CAT_VALID_TO,"
				+ "CAT_DEPRECATED," + "CAT_IS_LOCAL," + "CAT_DB_PATH," + "CAT_DB_BACKUP_PATH,"
				+ "CAT_FORCED_COUNT ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			// set the query parameters
			stmt.setString(1, catalogue.getCatalogueType().toString());
			stmt.setString(2, catalogue.getVersion());
			stmt.setString(3, catalogue.getCode());
			stmt.setString(4, catalogue.getName());
			stmt.setString(5, catalogue.getLabel());
			stmt.setString(6, catalogue.getScopenotes());
			stmt.setString(7, catalogue.getTermCodeMask());
			stmt.setInt(8, catalogue.getTermCodeLength());
			stmt.setString(9, catalogue.getTermMinCode());
			stmt.setBoolean(10, catalogue.isAcceptNonStandardCodes());
			stmt.setBoolean(11, catalogue.isGenerateMissingCodes());
			stmt.setString(12, catalogue.getStatus());
			stmt.setString(13, catalogue.getCatalogueGroups());

			stmt.setNull(14, java.sql.Types.TIMESTAMP); // last update, we do not know this value

			if (catalogue.getValidFrom() != null)
				stmt.setTimestamp(15, GlobalUtil.toSQLTimestamp(catalogue.getValidFrom()));
			else
				stmt.setNull(15, java.sql.Types.TIMESTAMP);

			stmt.setNull(16, java.sql.Types.TIMESTAMP); // validTo, we do not know this value

			stmt.setBoolean(17, catalogue.isDeprecated());

			// set if the catalogue is local or not
			stmt.setBoolean(18, catalogue.isLocal());

			stmt.setString(19, catalogue.getDbPath());

			stmt.setString(20, catalogue.getBackupDbPath());

			stmt.setInt(21, catalogue.getForcedCount());

			// execute the query
			stmt.execute();

			// get the generated id and return it
			try (ResultSet rs = stmt.getGeneratedKeys();) {
				if (rs.next()) {

					// get the new id from the db
					id = rs.getInt(1);

					// set the id to the catalogue object
					catalogue.setId(id);
				}

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return id;
	}

	/**
	 * Add the meta data of the catalogue to the meta data table The meta data table
	 * contains the meta data of the catalogues and the external db path where the
	 * real data are actually stored for each catalogue
	 * 
	 * @param catalogue
	 * @param folder    the folder where the catalogue database should be created
	 * @return the db path where the catalogue data are stored
	 */
	public boolean update(Catalogue catalogue) {

		String query = "update APP.CATALOGUE set " + "CAT_DCF_TYPE = ?," + "CAT_VERSION = ?," + "CAT_CODE = ?,"
				+ "CAT_NAME = ?," + "CAT_LABEL = ?," + "CAT_SCOPENOTE = ?," + "CAT_TERM_CODE_MASK = ?,"
				+ "CAT_TERM_CODE_LENGTH = ?," + "CAT_TERM_MIN_CODE = ?," + "CAT_ACCEPT_NON_STANDARD_CODES = ?,"
				+ "CAT_GENERATE_MISSING_CODES = ?," + "CAT_STATUS = ?," + "CAT_GROUPS = ?," + "CAT_LAST_UPDATE = ?,"
				+ "CAT_VALID_FROM = ?," + "CAT_VALID_TO = ?," + "CAT_DEPRECATED = ?," + "CAT_IS_LOCAL = ?,"
				+ "CAT_DB_PATH = ?," + "CAT_DB_BACKUP_PATH = ?," + "CAT_FORCED_COUNT = ? " + " where CAT_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			// set the query parameters
			stmt.setString(1, catalogue.getCatalogueType().toString());
			stmt.setString(2, catalogue.getVersion());
			stmt.setString(3, catalogue.getCode());
			stmt.setString(4, catalogue.getName());
			stmt.setString(5, catalogue.getLabel());
			stmt.setString(6, catalogue.getScopenotes());
			stmt.setString(7, catalogue.getTermCodeMask());
			stmt.setInt(8, catalogue.getTermCodeLength());
			stmt.setString(9, catalogue.getTermMinCode());
			stmt.setBoolean(10, catalogue.isAcceptNonStandardCodes());
			stmt.setBoolean(11, catalogue.isGenerateMissingCodes());
			stmt.setString(12, catalogue.getStatus());
			stmt.setString(13, catalogue.getCatalogueGroups());

			stmt.setNull(14, java.sql.Types.TIMESTAMP); // last update, we do not know this value

			if (catalogue.getValidFrom() != null)
				stmt.setTimestamp(15, GlobalUtil.toSQLTimestamp(catalogue.getValidFrom()));
			else
				stmt.setNull(15, java.sql.Types.TIMESTAMP);

			stmt.setNull(16, java.sql.Types.TIMESTAMP); // validTo, we do not know this value

			stmt.setBoolean(17, catalogue.isDeprecated());

			// set if the catalogue is local or not
			stmt.setBoolean(18, catalogue.isLocal());

			stmt.setString(19, catalogue.getDbPath());
			stmt.setString(20, catalogue.getBackupDbPath());

			stmt.setInt(21, catalogue.getForcedCount());

			stmt.setInt(22, catalogue.getId());

			// execute the query
			stmt.executeUpdate();

			// close the statement
			stmt.close();

			// close the connection
			con.close();

			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return false;
	}

	/**
	 * Delete a catalogue from the local database (both metadata and data)
	 * 
	 * @param catalogue
	 */
	public boolean delete(Catalogue catalogue) {

		String query = "delete from APP.CATALOGUE where CAT_ID = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			// remove dependencies first
			XmlUpdateFileDAO xmlDao = new XmlUpdateFileDAO();
			xmlDao.removeById(catalogue.getId());

			ForceCatEditDAO forcedDao = new ForceCatEditDAO();
			forcedDao.remove(catalogue);

			stmt.setInt(1, catalogue.getId());

			// execute the query and close the connections
			stmt.executeUpdate();

			stmt.close();

			con.close();

			return true;

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return false;
	}

	/**
	 * Given a result set with catalogues meta data inside it, this function will
	 * create a catalogue object taking the meta data from the result set The
	 * function takes the current item of the results set and tries to get the
	 * catalogue data. Use a while(rs.next) loop for a result set with more than one
	 * catalogue
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public Catalogue getByResultSet(ResultSet rs) throws SQLException {

		// create a catalogue using a builder
		CatalogueBuilder builder = new CatalogueBuilder();

		// set the catalogue meta data and create the catalogue object
		builder.setId(rs.getInt("CAT_ID"));
		builder.setCatalogueType(DcfType.valueOf(rs.getString("CAT_DCF_TYPE")));
		builder.setVersion(rs.getString("CAT_VERSION"));
		builder.setCode(rs.getString("CAT_CODE"));
		builder.setName(rs.getString("CAT_NAME"));
		builder.setLabel(rs.getString("CAT_LABEL"));
		builder.setScopenotes(rs.getString("CAT_SCOPENOTE"));
		builder.setTermCodeMask(rs.getString("CAT_TERM_CODE_MASK"));
		builder.setTermCodeLength(rs.getString("CAT_TERM_CODE_LENGTH"));
		builder.setTermMinCode(rs.getString("CAT_TERM_MIN_CODE"));
		builder.setAcceptNonStandardCodes(rs.getBoolean("CAT_ACCEPT_NON_STANDARD_CODES"));
		builder.setGenerateMissingCodes(rs.getBoolean("CAT_GENERATE_MISSING_CODES"));
		builder.setStatus(rs.getString("CAT_STATUS"));
		builder.setCatalogueGroups(rs.getString("CAT_GROUPS"));

		// set the dates with the adequate checks
		java.sql.Timestamp ts = rs.getTimestamp("CAT_LAST_UPDATE");

		if (ts != null)
			builder.setLastUpdate(ts);

		ts = rs.getTimestamp("CAT_VALID_FROM");
		if (ts != null)
			builder.setValidFrom(ts);

		ts = rs.getTimestamp("CAT_VALID_TO");
		if (ts != null)
			builder.setValidTo(ts);

		builder.setDeprecated(rs.getBoolean("CAT_DEPRECATED"));
		builder.setDbPath(rs.getString("CAT_DB_PATH"));
		builder.setBackupDbPath(rs.getString("CAT_DB_BACKUP_PATH"));

		builder.setLocal(rs.getBoolean("CAT_IS_LOCAL"));

		builder.setForcedCount(rs.getInt("CAT_FORCED_COUNT"));

		// return the catalogue
		return builder.build();
	}

	/**
	 * Remove all the records of the catalogue database
	 * 
	 * @param con
	 * @throws SQLException
	 * @throws NoCatalogueOpenException
	 */
	public void deleteContents(Catalogue catalogue) throws SQLException {

		try (Connection con = catalogue.getConnection(); Statement stmt = con.createStatement();) {

			stmt.execute("DELETE FROM APP.PICKLIST_TERM");
			stmt.execute("DELETE FROM APP.RECENT_TERM");
			stmt.execute("DELETE FROM APP.RELEASE_NOTES_OP");
			stmt.execute("DELETE FROM APP.SEARCH_OPT");
			stmt.execute("DELETE FROM APP.PICKLIST");
			stmt.execute("DELETE FROM APP.PREFERENCE");
			stmt.execute("DELETE FROM APP.PARENT_TERM");
			stmt.execute("DELETE FROM APP.TERM_ATTRIBUTE");
			stmt.execute("DELETE FROM APP.TERM_TYPE");
			stmt.execute("DELETE FROM APP.ATTRIBUTE");
			stmt.execute("DELETE FROM APP.HIERARCHY");
			stmt.execute("DELETE FROM APP.TERM");

			stmt.close();
			con.close();
		}
	}

	/**
	 * Compress the database to avoid fragmentation
	 */
	public void compress(Catalogue catalogue) {

		// This will fail, if there are dependencies

		try (Connection con = catalogue.getConnection();
				CallableStatement cs = con.prepareCall("CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)");) {

			cs.setString(1, "APP");

			cs.setShort(3, (short) 1);

			cs.setString(2, "PARENT_TERM");
			cs.execute();
			cs.setString(2, "TERM_ATTRIBUTE");
			cs.execute();
			cs.setString(2, "TERM");
			cs.execute();
			cs.setString(2, "ATTRIBUTE");
			cs.execute();
			cs.setString(2, "HIERARCHY");
			cs.execute();
			cs.setString(2, "PICKLIST_TERM");
			cs.execute();
			cs.setString(2, "RECENT_TERM");
			cs.execute();
			cs.setString(2, "RELEASE_NOTES_OP");
			cs.execute();
			cs.setString(2, "SEARCH_OPT");
			cs.execute();
			cs.setString(2, "PICKLIST");
			cs.execute();
			cs.setString(2, "PREFERENCE");
			cs.execute();
			cs.setString(2, "TERM_TYPE");
			cs.execute();

			cs.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}
	}

	/**
	 * Check if a catalogue is already inserted into the db or not
	 * 
	 * @param catalogue
	 */
	public boolean contains(Catalogue catalogue) {

		boolean found = false;

		// we check the catalogue code and then we check the version if the catalogue is
		// not local
		// note that if the catalogue is local the version check is automatically
		// eliminated
		String query = "select * from APP.CATALOGUE " + "where CAT_CODE = ? and CAT_VERSION = ? and CAT_DCF_TYPE = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.clearParameters();

			stmt.setString(1, catalogue.getCode());
			stmt.setString(2, catalogue.getVersion());
			stmt.setString(3, catalogue.getCatalogueType().toString());

			try (ResultSet rs = stmt.executeQuery();) {

				// get if something was found
				found = rs.next();

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException exception) {
			exception.printStackTrace();
		}

		return found;
	}

	/**
	 * Get the last release of the selected catalogue either for test catalogue or
	 * production ones
	 * 
	 * @param catalogue
	 * @return
	 */
	public Catalogue getLastVersionByCode(String code, DcfType catType) {

		// output
		Catalogue lastVersion = null;

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(
						"select * from APP.CATALOGUE " + "where CAT_CODE = ? and CAT_DCF_TYPE = ?");) {

			// get all the catalogues codes related to the last version of each catalogue.
			// QUERY:
			// Get first for each catalogue the max version of it. Then get all the
			// information related to the
			// last release of the catalogue using a self join on cat code and max_version
			// we get also the information related to the reserved catalogues using a LEFT
			// join

			stmt.setString(1, code);
			stmt.setString(2, catType.toString());

			// execute the query
			try (ResultSet rs = stmt.executeQuery();) {

				ArrayList<Catalogue> catalogues = new ArrayList<>();

				// get the catalogues data and add them to the output array list
				while (rs.next())
					catalogues.add(getByResultSet(rs));

				// if no catalogues return
				if (catalogues.isEmpty())
					return null;

				// avoid computation for unique catalogues
				if (catalogues.size() == 1)
					lastVersion = catalogues.get(0);
				else {
					// sort the catalogues according to their version
					catalogues.sort(new Comparator<Catalogue>() {
						public int compare(Catalogue o1, Catalogue o2) {

							boolean inv1 = o1.getCatalogueVersion().isInvalid();
							boolean inv2 = o2.getCatalogueVersion().isInvalid();

							boolean older = o1.isOlder(o2);

							// if first invalid => second before
							if (inv1 && !inv2)
								return 1;

							// if second invalid => first before
							if (!inv1 && inv2)
								return -1;

							if (older)
								return 1;
							else
								return -1;
						};
					});

					// get the most recent catalogue
					lastVersion = catalogues.get(0);
				}

				rs.close();

			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return lastVersion;
	}

	/**
	 * Get the catalogues which are the last version of the ones which were
	 * downloaded locally. We use this feature to check if there is an update of a
	 * catalogue
	 * 
	 * @param catalogueType specify TEST to get all the last release of the test
	 *                      catalogues, or PRODUCTION for the ones of production
	 * @return
	 */
	public ArrayList<Catalogue> getLastReleaseCatalogues(DcfType catalogueType) {

		// output array
		ArrayList<Catalogue> catalogues = new ArrayList<>();

		String query = "select * " + "from APP.CATALOGUE C " + "inner join ( "
				+ "select MAX(CAT_VALID_FROM) as MAX_VF, CAT_CODE " + "from APP.CATALOGUE " + "group by CAT_CODE) TEMP "
				+ "on C.CAT_CODE = TEMP.CAT_CODE and C.CAT_VALID_FROM = TEMP.MAX_VF " + "where C.CAT_DCF_TYPE = ?";

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			// get all the catalogues codes related to the last version of each catalogue.
			// QUERY:
			// Get first for each catalogue the max version of it. Then get all the
			// information related to the
			// last release of the catalogue using a self join on cat code and max_version
			stmt.setString(1, catalogueType.toString());

			// here we have a table which contains only the rows related to the
			// last version of the catalogue!

			// execute the query
			try (ResultSet rs = stmt.executeQuery();) {

				// get the catalogues data and add them to the output array list
				while (rs.next())
					catalogues.add(getByResultSet(rs));

				rs.close();

			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return catalogues;
	}

	/**
	 * Retrieve all the meta data of all the catalogues (of one type)
	 * 
	 * @param catalogueType the type of the catalogues which we want to consider
	 * @return
	 */
	public ArrayList<Catalogue> getMyCatalogues(DcfType catalogueType) {

		String query = "select * from APP.CATALOGUE where CAT_DCF_TYPE = ?" + "or CAT_DCF_TYPE = ?";

		// output array
		ArrayList<Catalogue> catalogues = new ArrayList<>();

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, catalogueType.toString());
			stmt.setString(2, DcfType.LOCAL.toString());

			// get the query results
			try (ResultSet rs = stmt.executeQuery();) {

				User user = User.getInstance();

				// get the catalogues data and add them to the output array list
				while (rs.next()) {

					Catalogue catalogue = getByResultSet(rs);

					// do not consider the cat user catalogue if we are not a cm
					if (catalogue.isCatUsersCatalogue() && !user.isCatManager())
						continue;

					catalogues.add(getByResultSet(rs));
				}

				rs.close();

			}

			stmt.close();
			con.close();

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
			return null;
		}

		return catalogues;
	}

	/**
	 * Get the catalogue metadata starting from the catalogue code and version
	 * 
	 * @param catalogueCode
	 * @return
	 */
	public Catalogue getCatalogue(String catalogueCode, String catalogueVersion, DcfType catType) {

		String query = "select * from APP.CATALOGUE t1 " + "where t1.CAT_CODE = ? and t1.CAT_VERSION = ?"
				+ "and t1.CAT_DCF_TYPE = ?";

		Catalogue cat;

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			// set the catalogue code and version parameters for the statement
			stmt.setString(1, catalogueCode);
			stmt.setString(2, catalogueVersion);
			stmt.setString(3, catType.toString());

			// get the results
			try (ResultSet rs = stmt.executeQuery();) {

				// get the catalogue data and add them to the output array list
				if (rs.next())
					cat = getByResultSet(rs);
				else // return null otherwise
					cat = null;

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
			return null;
		}

		return cat;
	}

	@Override
	public boolean remove(Catalogue object) {
		return delete(object);
	}

	@Override
	public Catalogue getById(int id) {

		String query = "select * from APP.CATALOGUE t1 " + "where t1.CAT_ID = ?";

		Catalogue cat;

		try (Connection con = DatabaseManager.getMainDBConnection();
				PreparedStatement stmt = con.prepareStatement(query);) {

			// set the catalogue code and version parameters for the statement
			stmt.setInt(1, id);

			// get the results
			try (ResultSet rs = stmt.executeQuery();) {

				// get the catalogue data and add them to the output array list
				if (rs.next())
					cat = getByResultSet(rs);
				else // return null otherwise
					cat = null;

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
			return null;
		}
		return cat;
	}

	@Override
	public Collection<Catalogue> getAll() {

		return null;
	}

	@Override
	public List<Integer> insert(Iterable<Catalogue> attrs) {
		return null;
	}
}
