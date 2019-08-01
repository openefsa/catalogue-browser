package import_catalogue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueEntityDAO;
import naming_convention.SpecialValues;
import term_type.TermType;
import term_type.TermTypeDAO;

/**
 * Class which is used to import the term types of a catalogue. The term types
 * are stored inside the scopenote of the attribute named "termType" with the
 * following syntax:
 * 
 * {@code This is the type of the term$r=raw commodity$f=facets$...}
 * 
 * the first element is the scopenote itself, while the other elements in a $
 * separated format are the term types codes and labels. In particular the
 * element before the equal sign is the term type code (which is the one used in
 * the catalogue workbook). The second element instead is the text which is
 * displayed for the corresponding term type code.
 * 
 * @author avonva
 *
 */
public class TermTypeImporter {

	private static final Logger LOGGER = LogManager.getLogger(TermTypeImporter.class);

	private CatalogueEntityDAO<TermType> dao;
	private Catalogue catalogue;

	public TermTypeImporter(CatalogueEntityDAO<TermType> dao, Catalogue catalogue) {
		this.dao = dao;
		this.catalogue = catalogue;
	}

	public TermTypeImporter(Catalogue catalogue) {
		this(new TermTypeDAO(catalogue), catalogue);
	}

	/**
	 * Import the term types. Note that you should import the attribute sheet first
	 * with {@link AttributeSheetImporter}
	 */
	public void importSheet() {
		dao.insert(getTermTypeValues());
	}

	/**
	 * Create the table related to the term types retrieved from the term type
	 * scopenote
	 * 
	 * @author shahaal
	 * @author avonva
	 */
	private ArrayList<TermType> getTermTypeValues() {

		// output array
		ArrayList<TermType> termTypes = new ArrayList<>();

		String scopenoteQuery = "select ATTR_SCOPENOTE from APP.ATTRIBUTE where ATTR_NAME = ?";

		// solve memory leak
		try (Connection con = catalogue.getConnection();
				PreparedStatement getScopenoteQuery = con.prepareStatement(scopenoteQuery)) {

			getScopenoteQuery.clearParameters();

			// get only the term type attribute
			getScopenoteQuery.setString(1, SpecialValues.TERM_TYPE_NAME);

			ResultSet rs = getScopenoteQuery.executeQuery();

			// get the scopenote
			if (rs.next()) {

				// parse the attribute scopenote using the $ separator
				StringTokenizer st = new StringTokenizer(rs.getString("ATTR_SCOPENOTE"), "$");

				int tokenCount = 0;

				// for each term type code-description
				while (st.hasMoreTokens()) {

					tokenCount++;

					// get the pair code=description
					String token = st.nextToken();

					// skip if we are reading the scopenote!
					if (tokenCount == 1) {
						continue;
					}

					// get the code and description separately
					String[] values = token.split("=");

					// if wrong number of elements return
					if (values.length != 2) {

						LOGGER.error("Wrong term type syntax in scopenotes, found : " + token + " expected: "
								+ "code=description. Check also the white spaces.");

						termTypes = null;
						break;
					}

					// add the term type code, description to the hashmap
					// the id is not important here, we give 1 as default
					termTypes.add(new TermType(1, values[0], values[1]));
				}
			}
			
			rs.close();
			getScopenoteQuery.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return termTypes;
	}
}
