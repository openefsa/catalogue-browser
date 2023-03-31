package ui_search_bar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import term_type.TermType;
import user_preferences.OptionType;
import user_preferences.SearchOption;

/**
 * Queries used to make searches
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class SearchDAO {

	private static final Logger LOGGER = LogManager.getLogger(SearchDAO.class);

	private Catalogue catalogue;
	private Term rootTerm;

	/**
	 * Initialize the search dao with the catalogue we want to communicate with
	 * 
	 * @param catalogue
	 */
	public SearchDAO(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	/**
	 * Set a root term which restricts the search results. In particular, only the
	 * terms which are children of the root term will be maintained in the results.
	 * 
	 * @param term
	 */
	public void setRootTerm(Term term) {
		this.rootTerm = term;
	}

	/**
	 * Search the text as keyword(s) to find terms in a subset of hierarchies.
	 * 
	 * @param text        the entire text we want to search ( as "goat milk",
	 *                    "bovine" ... )
	 * @param type        the search type we want to perform see
	 *                    {@linkplain SearchType}
	 * @param hierarchies filter the results, only terms belonging to one of these
	 *                    hierarchies will be inserted in the results
	 * @return list of terms which matched the conditions
	 */
	public ArrayList<Term> startSearch(String text, SearchType type, Hierarchy hierarchy) {

		ArrayList<String> keywords;

		// if exact match, we use the entire text as single keyword
		if (type == SearchType.EXACT_MATCH) {

			keywords = new ArrayList<>();
			keywords.add(text);

		} else {

			// otherwise, if any or all words we
			// compute all the keywords which
			// are space-separated
			keywords = new ArrayList<String>(Arrays.asList(text.split(" ")));
		}

		return search(keywords, type, hierarchy);
	}

	/**
	 * Perform a search in the database terms and term attributes possibly filtering
	 * by hierarchies and term types (if present in the db)
	 * 
	 * @param keywords    set of keywords to be searched
	 * @param type        the search method we want to use. See
	 *                    {@linkplain SearchType }
	 * @param hierarchies subset of hierarchies in which searching terms (empty =
	 *                    global search)
	 * @return
	 */
	private ArrayList<Term> search(ArrayList<String> keywords, SearchType type, Hierarchy hierarchy) {

		ArrayList<Term> terms = new ArrayList<>();

		ArrayList<Integer> ids1 = findByCodeOrName(keywords, type);
		ArrayList<Integer> ids2 = findByAttribute(keywords, type);

		// create a set to combine the results avoiding duplicated
		Set<Integer> uniqueIds = new HashSet<>();
		uniqueIds.addAll(ids1);
		uniqueIds.addAll(ids2);

		// filter by term type and hierarchy
		for (Integer id : uniqueIds) {

			Term term = catalogue.getTermById(id);

			// Hide the term if not in use
			if (!term.isInUse())
				continue;

			// Skip elements which are not children of the
			// root term if it was set
			if (rootTerm != null && !term.hasAncestor(rootTerm, hierarchy)) {
				continue;
			}

			// if the term type of the term is searchable and also one
			// of the hierarchy of the term is searchable, add it
			if (isTypeSearchable(term) && hasHierachySearchable(term, hierarchy)) {
				terms.add(term);
			}
		}

		return terms;
	}

	/**
	 * Check if the term type of the term is one of the selected in the user
	 * settings
	 * 
	 * @param term
	 * @return
	 */
	@SuppressWarnings("unlikely-arg-type")
	private boolean isTypeSearchable(Term term) {

		// if the catalogue does not use term types
		// we return true as default to avoid blocking
		// search operations
		if (!catalogue.hasTermTypes())
			return true;

		// if the term does not have a term type
		// we include it in the results as default
		if (term.getTermType() == null)
			return true;

		// get the searchable term types
		Collection<TermType> types = getSearchableTermTypes();

		// return true if the term type is contained in the
		// searchable term types
		return types.contains(term.getTermType());
	}

	/**
	 * Check if the term has an applicable hierarchy which is contained in the
	 * searchable hierarchies
	 * 
	 * @param term
	 * @param searchableHierarchies
	 * @return
	 */
	private boolean hasHierachySearchable(Term term, Hierarchy hierarchy) {

		// if no searchable hierarchies => we search globally
		// therefore we return true
		// if ( searchableHierarchies.isEmpty() )
		// return true;

		// if no applicable hierarchy => we do not return it
		if (term.getApplicableHierarchies().isEmpty())
			return false;

		// if the hierarchy is contained in the applicable hierarchies of the term
		// then found
		for (Hierarchy termHierarchy : term.getApplicableHierarchies()) {

			if (hierarchy.equals(termHierarchy)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Get all the ids of the terms which matched the keywords by their name or code
	 * with the selected search type.
	 * 
	 * @param keywords
	 * @param type
	 * @return
	 */
	private ArrayList<Integer> findByCodeOrName(ArrayList<String> keywords, SearchType type) {

		ArrayList<Integer> termIds = new ArrayList<>();

		// if no keyword return empty
		if (keywords.isEmpty())
			return termIds;

		// main query, select terms based on hierarchies and attributes
		String query = "select distinct(TERM_ID) from APP.TERM where ";

		// filter on name and code
		int count = 0;
		for (String key : keywords) {

			String value = key.toUpperCase();

			String keyUp = "'" + value + "'";
			String keyLike = "'%" + value + "%'";

			query = query + " ( upper(TERM_EXTENDED_NAME) like " + keyLike;
			query = query + " or upper(TERM_CODE) = " + keyUp + " ) ";
			// query = query + " upper(TERM_CODE) = " + keyUp;

			// go to the next keyword
			count++;

			// if it is not the last keyword add also
			// the conjunction operator
			if (count < keywords.size())
				query = query + " " + getLogicalOp(type) + " ";

		}

		// execute the query
		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query);
				ResultSet rs = stmt.executeQuery();) {

			// get all the terms ids
			while (rs.next())
				termIds.add(rs.getInt("TERM_ID"));

			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return termIds;
	}

	/**
	 * Get all the terms ids which match the keywords by the attributes
	 * 
	 * @param keyword
	 * @param type
	 * @return list of terms ids which have term attributes which matched the
	 *         keywords with the selected search method
	 */
	private ArrayList<Integer> findByAttribute(ArrayList<String> keywords, SearchType type) {

		ArrayList<Integer> termIds = new ArrayList<>();

		// if no keyword return empty
		if (keywords.isEmpty())
			return termIds;

		// get all the attributes ids related to the
		// attributes we are allowed to search in
		String attrIds = getAttributeFilter();

		// get all the term attributes ids which match
		// the keywords
		String taIds = getTermAttrFilter(keywords, type);

		// if no attributes matched => return empty
		// if no term attributes matched => return empty
		if (attrIds.isEmpty() || taIds.isEmpty())
			return termIds;

		// here for sure we have at least one attribute id and one
		// term attribute id, therefore we can use the IN clause
		// without worrying about empty lists

		// filter on the searchable attributes, get all the term attributes
		// which have as attribute one contained in the searchable attributes
		// vector
		String query = "select distinct (TERM_ID) from APP.TERM_ATTRIBUTE where ATTR_ID in ( " + attrIds
				+ ") and TERM_ATTR_ID in ( " + taIds + " )";

		// execute the query
		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query);
				ResultSet rs = stmt.executeQuery();) {

			// get all the terms ids
			while (rs.next())
				termIds.add(rs.getInt("TERM_ID"));

			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return termIds;
	}

	/**
	 * Get the searchable attributes ids in a comma separated way in order to use
	 * them as a filter in the main query
	 * 
	 * @return a string which contains the searchable attributes ids in a comma
	 *         separated way. If no searchable attribute is retrieved an empty
	 *         string is returned
	 */
	private String getAttributeFilter() {

		StringBuilder ids = new StringBuilder();

		// get all the searchable attributes
		Collection<Attribute> attrs = getSearchableAttributes();

		int attrCount = 0;

		// we take only the records which has as attribute one
		// of the searchable attributes
		for (Attribute attr : attrs) {

			// add the current attribute id to
			// the one which can be selected
			ids.append(attr.getId());

			attrCount++;

			// add the comma only if it is not
			// the last one
			if (attrCount < attrs.size())
				ids.append(",");
		}

		return ids.toString();
	}

	/**
	 * Select all the term attributes ids which match the keywords with the selected
	 * search metodology
	 * 
	 * @param keywords
	 * @param type
	 * @return a string which contains the ids of the term attributes which matched
	 *         the keywords with their value using the selected search methodology
	 */
	private String getTermAttrFilter(ArrayList<String> keywords, SearchType type) {

		// output
		StringBuilder taFilter = new StringBuilder();

		// query to get the term attributes
		StringBuilder query = new StringBuilder();

		query.append("select TERM_ATTR_ID from APP.TERM_ATTRIBUTE where ");

		// filter on term attributes values
		int count = 0;
		for (String key : keywords) {

			String keyLike = "'%" + key.toUpperCase() + "%'";

			// search on the term attributes values (we have already filtered
			// the non relevant term attributes in the join)
			query.append(" upper( ATTR_VALUE ) like " + keyLike);

			// go to the next keyword
			count++;

			// if it is not the last keyword add also
			// the conjunction operator
			if (count < keywords.size()) {
				query.append(" ");
				query.append(getLogicalOp(type));
				query.append(" ");
			}
		}

		// execute the query to retrieve the term attributes
		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query.toString());
				ResultSet rs = stmt.executeQuery();) {

			// add ids to array list
			ArrayList<Integer> ids = new ArrayList<>();

			while (rs.next())
				ids.add(rs.getInt("TERM_ATTR_ID"));

			// add ids to the string in a comma separated
			// way. We need the array list to have the
			// size
			count = 0;
			for (Integer id : ids) {

				taFilter.append(id);

				count++;

				// add the comma only if it is not
				// the last one
				if (count < ids.size())
					taFilter.append(",");
			}

			rs.close();
			stmt.close();
			con.close();

		} catch (SQLException e) {
			LOGGER.error("DB error", e);
			e.printStackTrace();
		}

		return taFilter.toString();
	}

	/**
	 * Get all the searchable term types (defined by the user preferences)
	 * 
	 * @return
	 */
	private ArrayList<TermType> getSearchableTermTypes() {

		ArrayList<TermType> types = new ArrayList<>();

		// get all the search options related to term types
		SearchOptionDAO optDao = new SearchOptionDAO(catalogue);
		Collection<SearchOption> opts = optDao.getEnabledByType(OptionType.TERM_TYPE);

		for (SearchOption opt : opts) {
			types.add(catalogue.getTermTypeById(opt.getId()));
		}

		return types;
	}

	/**
	 * Get all the searchable attributes (defined by the user preferences)
	 * 
	 * @return
	 */
	private ArrayList<Attribute> getSearchableAttributes() {

		ArrayList<Attribute> attrs = new ArrayList<>();

		// get all the search options related to term types
		SearchOptionDAO optDao = new SearchOptionDAO(catalogue);
		Collection<SearchOption> opts = optDao.getEnabledByType(OptionType.ATTRIBUTE);

		for (SearchOption opt : opts) {
			attrs.add(catalogue.getAttributeById(opt.getId()));
		}

		return attrs;
	}

	/**
	 * Get the logical operator which is used to combine the search results on
	 * different term fields. We can combine the searches with ORs or ANDs.
	 * 
	 * @param type ANY_WORD/ALL_WORDS
	 * @return
	 */
	private String getLogicalOp(SearchType type) {

		String op = null;
		switch (type) {
		case ANY_WORD:
			op = "or";
			break;
		case ALL_WORDS:
			op = "and";
			break;
		default:
			break;
		}

		return op;
	}
}
