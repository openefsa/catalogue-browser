package ui_search_bar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_object.Attribute;
import term_type.TermType;
import term_type.TermTypeDAO;
import user_preferences.OptionType;
import user_preferences.SearchOption;

/**
 * Manage interaction with the SEARCH_OPT table
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class SearchOptionDAO implements CatalogueEntityDAO<SearchOption> {

	private static final Logger LOGGER = LogManager.getLogger(SearchOptionDAO.class);

	Catalogue catalogue;

	/**
	 * Instantiate the search option dao with the catalogue we are working with
	 * 
	 * @param catalogue
	 */
	public SearchOptionDAO(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	/**
	 * Insert a new search options
	 */
	@Override
	public int insert(SearchOption opt) {

		String query = "insert into APP.SEARCH_OPT (OBJ_ID, OBJ_TYPE, SEARCH_OPT_ENABLED) values (?,?,?) ";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			stmt.setInt(1, opt.getId());
			stmt.setString(2, opt.getType().toString());
			stmt.setBoolean(3, opt.isEnabled());

			stmt.executeUpdate();

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return opt.getId();
	}

	/**
	 * Not implemented yet
	 */
	@Override
	public boolean remove(SearchOption object) {

		return false;
	}

	/**
	 * Update the state of a search option (enabled/disabled)
	 */
	@Override
	public boolean update(SearchOption opt) {

		String query = "update APP.SEARCH_OPT set SEARCH_OPT_ENABLED = ? where OBJ_ID = ? and OBJ_TYPE = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setBoolean(1, opt.isEnabled());
			stmt.setInt(2, opt.getId());
			stmt.setString(3, opt.getType().toString());

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
	 * Not implemented yet
	 */
	@Override
	public SearchOption getById(int id) {

		return null;
	}

	@Override
	public SearchOption getByResultSet(ResultSet rs) throws SQLException {

		int id = rs.getInt("OBJ_ID");
		boolean enabled = rs.getBoolean("SEARCH_OPT_ENABLED");
		OptionType type = OptionType.valueOf(rs.getString("OBJ_TYPE"));

		SearchOption opt = new SearchOption(catalogue, id, enabled, type);
		return opt;
	}

	/**
	 * Get all the search options
	 */
	@Override
	public Collection<SearchOption> getAll() {

		Collection<SearchOption> opts = new ArrayList<>();

		String query = "select * from APP.SEARCH_OPT";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query);
				ResultSet rs = stmt.executeQuery();) {

			while (rs.next()) 
				opts.add(getByResultSet(rs));
			
			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return opts;
	}

	/**
	 * Get the ENABLED search options of a single type.
	 * 
	 * @param type
	 * @return
	 */
	public ArrayList<SearchOption> getEnabledByType(OptionType type) {

		ArrayList<SearchOption> opts = getByType(type);

		// remove all the disabled search options
		Iterator<SearchOption> iterator = opts.listIterator();

		while (iterator.hasNext()) {

			if (!iterator.next().isEnabled())
				iterator.remove();
		}

		return opts;
	}

	/**
	 * Get the search options of a single type.
	 * 
	 * @param type
	 * @return
	 */
	public ArrayList<SearchOption> getByType(OptionType type) {

		ArrayList<SearchOption> opts = new ArrayList<>();

		String query = "select * from APP.SEARCH_OPT where OBJ_TYPE = ?";

		try (Connection con = catalogue.getConnection(); PreparedStatement stmt = con.prepareStatement(query);) {

			stmt.setString(1, type.toString());

			try (ResultSet rs = stmt.executeQuery();) {

				while (rs.next())
					opts.add(getByResultSet(rs));

				rs.close();
			}

			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return opts;
	}

	/**
	 * Insert the default search options in the catalogue database
	 */
	public void insertDefaultSearchOpt() {

		TermTypeDAO typeDao = new TermTypeDAO(catalogue);
		AttributeDAO attrDao = new AttributeDAO(catalogue);

		for (TermType tt : typeDao.getAll()) {
			insert(new SearchOption(catalogue, tt.getId(), true, OptionType.TERM_TYPE));
		}

		// add only generic attribute
		for (Attribute attr : attrDao.fetchGeneric()) {
			insert(new SearchOption(catalogue, attr.getId(), true, OptionType.ATTRIBUTE));
		}
	}

	@Override
	public List<Integer> insert(Iterable<SearchOption> attrs) {

		return null;
	}
}
