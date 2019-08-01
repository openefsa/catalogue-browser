package import_catalogue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.CatalogueRelationDAO;
import catalogue_browser_dao.TermAttributeDAO;
import catalogue_object.Attribute;
import catalogue_object.RepeatableParser;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import naming_convention.Headers;
import open_xml_reader.ResultDataSet;
import term_code_generator.CodeGenerator;

/**
 * Importer of the term attributes
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class TermAttributeImporter extends SheetImporter<TermAttribute> {

	private CatalogueRelationDAO<TermAttribute, Term, Attribute> dao;
	private Catalogue catalogue;
	private HashMap<String, Integer> termIds;
	private ArrayList<Attribute> attributes;

	// things for append
	private HashMap<String, String> newCodes;

	public TermAttributeImporter(CatalogueRelationDAO<TermAttribute, Term, Attribute> dao, Catalogue catalogue)
			throws SQLException {

		this.dao = dao;
		this.catalogue = catalogue;

		// get all the term ids
		termIds = createIdHashMap(catalogue, "TERM_ID", "TERM_CODE", "APP.TERM");

		AttributeDAO attrDao = new AttributeDAO(catalogue);
		attributes = attrDao.getAll();
	}

	public TermAttributeImporter(Catalogue catalogue) throws SQLException {
		this(new TermAttributeDAO(catalogue), catalogue);
	}

	/**
	 * Activate this method to manage also new terms (i.e. appended terms) into the
	 * term attribute importer class. In particular, if a term with code containing
	 * {link CodeGenerator#TEMP_TERM_CODE} is encountered, its real code is taken
	 * from the {@code newCodes} hashmap (which was created before in the
	 * {@link TermSheetImporter#importSheet()} if new terms were encountered!) in
	 * order to get its reference to the term contained into the db
	 * 
	 * @param newCodes
	 */
	public void manageNewTerms(HashMap<String, String> newCodes) {
		this.newCodes = newCodes;
	}

	@Override
	public TermAttribute getByResultSet(ResultDataSet rs) {
		return null;
	}

	@Override
	public void insert(Collection<TermAttribute> data) {
		dao.insert(data);
	}

	@Override
	public Collection<TermAttribute> getAllByResultSet(ResultDataSet rs) {

		Collection<TermAttribute> tas = new ArrayList<>();

		// initialise the code generator
		CodeGenerator generator = new CodeGenerator();

		// get the term code
		String termCode = rs.getString(Headers.TERM_CODE);

		if (generator.isTempCode(termCode))
			termCode = newCodes.get(termCode);

		// skip if no term code was found
		if (termCode == null || termCode.isEmpty())
			return null;

		// get the term id using the term code from the hashmap (global var)
		int termId = termIds.get(termCode);

		// for each attribute we create a record for the term attributes table
		for (Attribute attr : attributes) {

			// get the attribute value in the term sheet using the attribute name
			String attrValue = rs.getString(attr.getName());

			// continue only if there is indeed a value
			if (attrValue == null || attrValue.isEmpty())
				continue;

			// if repeatable we insert a record for each single value
			if (attr.isRepeatable()) {

				// add all the term attributes to the collection
				for (String singleValue : RepeatableParser.getRepeatableValues(attrValue))
					tas.add(createTermAttribute(termId, attr, singleValue));
			} else { // if single attribute add a single term attribute
				tas.add(createTermAttribute(termId, attr, attrValue));
			}
		}

		return tas;
	}

	/**
	 * Create a single term attribute using the available information
	 * 
	 * @param termId
	 * @param attr
	 * @param value
	 * @return
	 */
	private TermAttribute createTermAttribute(int termId, Attribute attr, String value) {

		// create an empty term with its id
		// we do not need other information
		// to insert a term attribute into the db
		Term term = new Term(catalogue);
		term.setId(termId);

		TermAttribute ta = new TermAttribute(term, attr, value);

		return ta;
	}

	@Override
	public void end() {

	}
}
