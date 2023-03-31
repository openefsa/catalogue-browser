package catalogue_object;

import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import data_transformation.BooleanConverter;
import data_transformation.DateTrimmer;
import global_manager.GlobalManager;
import naming_convention.SpecialValues;
import term.WrongKeyException;

public class Attribute extends SortableCatalogueObject implements Mappable {

	private static final Logger LOGGER = LogManager.getLogger(Attribute.class);

	// Set the fixed values of attributes
	public static final String cardinalitySingle = "single";
	public static final String cardinalityRepeatable = "repeatable";

	public static final String booleanTrue = "true";
	public static final String booleanFalse = "false";

	public static final String applicabilityBase = "base";
	public static final String applicabilityAttribute = "attribute";
	public static final String applicabilityBoth = "both";

	public static final String inheritanceValue = "V";
	public static final String inheritanceRestriction = "R";
	public static final String inheritanceDisabled = "D";

	public static final String reportableMandatory = "M";
	public static final String reportableOptional = "O";
	public static final String reportableDisabled = "D";

	public static final String stringTypeName = "xs:string";
	public static final String integerTypeName = "xs:integer";
	public static final String decimalTypeName = "xs:decimal";
	public static final String doubleTypeName = "xs:double";
	public static final String booleanTypeName = "xs:boolean";
	public static final String catalogueTypeName = "catalogue";

	private int maxLength, precision, scale;
	private String reportable, type;
	private String catalogueCode, singleOrRepeatable, inheritance;
	private boolean visible, searchable, uniqueness, termCodeAlias;

	/**
	 * Create a default attribute (for the attribute editor)
	 * 
	 * @return
	 */
	public static Attribute getDefaultAttribute() {

		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();

		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();

		int newOrder = 0;
		for (Attribute attr : currentCat.getAttributes()) {
			// if an attribute with an order greater or equal to
			// the new attribute order => update the new attribute order
			if (attr.getOrder() >= newOrder)
				newOrder = attr.getOrder() + 1;
		}

		return new Attribute(currentCat, -1, "Change", "Change", "", "", reportableDisabled, true, false, newOrder,
				stringTypeName, -1, -1, -1, "", cardinalitySingle, inheritanceDisabled, false, false, "", "", null,
				null, null, false);
	}

	public Attribute(Catalogue catalogue, int id, String code, String name, String label, String scopenotes,
			String reportable, boolean visible, boolean searchable, int order, String type, int maxLength,
			int precision, int scale, String catalogueCode, String singleOrRepeatable, String inheritance,
			boolean uniqueness, boolean termCodeAlias, String status, String version, Timestamp lastUpdate,
			Timestamp validFrom, Timestamp validTo, boolean deprecated) {

		super(catalogue, id, code, name, label, scopenotes, order, status, version, lastUpdate, validFrom, validTo,
				deprecated);

		this.reportable = reportable;
		this.visible = visible;
		this.searchable = searchable;
		this.type = type;
		this.maxLength = maxLength;
		this.precision = precision;
		this.scale = scale;
		this.catalogueCode = catalogueCode;
		this.singleOrRepeatable = singleOrRepeatable;
		this.inheritance = inheritance;
		this.uniqueness = uniqueness;
		this.termCodeAlias = termCodeAlias;

		// default value
		if (reportable == null || reportable.isEmpty())
			reportable = reportableDisabled;
	}

	public String getReportable() {

		// default value
		if (reportable == null || reportable.isEmpty())
			reportable = reportableDisabled;

		return reportable;
	}

	public String getType() {
		return type;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public int getPrecision() {
		return precision;
	}

	public int getScale() {
		return scale;
	}

	public String getCatalogueCode() {
		return catalogueCode;
	}

	public String getSingleOrRepeatable() {
		return singleOrRepeatable;
	}

	public String getInheritance() {
		return inheritance;
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean isSearchable() {
		return searchable;
	}

	public boolean isUniqueness() {
		return uniqueness;
	}

	public boolean isTermCodeAlias() {
		return termCodeAlias;
	}

	/**
	 * Check if the attribute is a generic attribute
	 * 
	 * @return
	 */
	public boolean isGeneric() {
		return !isTermType() && !isDetailLevel() && !isCatalogue() && !isImplicitFacet() && !isAllFacet();
	}

	/**
	 * Check if the attribute is the corex flag (or level of detail)
	 * 
	 * @return
	 */
	public boolean isDetailLevel() {
		return getName().equals(SpecialValues.DETAIL_LEVEL_NAME);
	}

	/**
	 * Check if the attribute is the state flag (or type of term)
	 * 
	 * @return
	 */
	public boolean isTermType() {
		return getName().equals(SpecialValues.TERM_TYPE_NAME);
	}

	/**
	 * Check if the attribute is the implicit facet attribute, the one which
	 * represents the facetheader.facetfode$...
	 * 
	 * @return
	 */
	public boolean isImplicitFacet() {
		return getName().equals(SpecialValues.IMPLICIT_FACETS_NAME);
	}

	/**
	 * Check if the attribute is the all facet attribute, the one which represents
	 * the facetheader.facetfode$... with all the inherited facets!
	 * 
	 * @return
	 */
	public boolean isAllFacet() {
		return getName().equals(SpecialValues.ALL_FACETS_NAME);
	}

	/**
	 * check if the attribute is repeatable (i.e. it contains several values
	 * separated by $)
	 * 
	 * @return
	 */
	public boolean isRepeatable() {
		return getSingleOrRepeatable().equals(cardinalityRepeatable);
	}

	/**
	 * Check if the attribute type is catalogue. If this is the case then we have an
	 * implicit facet attribtue
	 * 
	 * @return
	 */
	public boolean isCatalogue() {
		return type.equals(catalogueTypeName);
	}

	public void setReportable(String reportable) {
		this.reportable = reportable;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public void setCatalogueCode(String catalogueCode) {
		this.catalogueCode = catalogueCode;
	}

	public void setSingleOrRepeatable(String singleOrRepeatable) {
		this.singleOrRepeatable = singleOrRepeatable;
	}

	public void setInheritance(String inheritance) {
		this.inheritance = inheritance;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}

	public void setUniqueness(boolean uniqueness) {
		this.uniqueness = uniqueness;
	}

	public void setTermCodeAlias(boolean termCodeAlias) {
		this.termCodeAlias = termCodeAlias;
	}

	@Override
	public void setVariableByKey(String key, String value) throws WrongKeyException {

		boolean found = true;
		key = key.toLowerCase();

		// search before in the property fields,then in the attribute ones
		try {
			super.setVariableByKey(key, value);
		} catch (WrongKeyException e) {
			LOGGER.error("Error ", e);
			e.printStackTrace();
			
			switch (key) {
			case "type":
				setType(value);
				break;
			case "reportable":
				setReportable(value);
				break;
			case "visible":
				setVisible(Boolean.parseBoolean(value));
				break;
			case "searchable":
				setSearchable(Boolean.parseBoolean(value));
				break;
			case "maxlength":
				setMaxLength(Integer.parseInt(value));
				break;
			case "precision":
				setPrecision(Integer.parseInt(value));
				break;
			case "scale":
				setScale(Integer.parseInt(value));
				break;
			case "catcode":
				setCatalogueCode(value);
				break;
			case "single_repeatable":
				setSingleOrRepeatable(value);
				break;
			case "inheritance":
				setInheritance(value);
				break;
			case "uniqueness":
				setUniqueness(Boolean.parseBoolean(value));
				break;
			case "termcodealias":
				setTermCodeAlias(Boolean.parseBoolean(value));
				break;
			default:
				found = false;
				break;
			}
		}

		// if nothing is found exception
		if (!found)
			throw new WrongKeyException();

	}

	@Override
	public String getVariableByKey(String key) throws WrongKeyException {

		boolean found = true;
		String value = null;

		key = key.toLowerCase();

		// try to retrieve a field from the dcf property fields
		// if no field is retrieved, search on the attribute fields
		try {

			value = super.getVariableByKey(key);

		} catch (WrongKeyException e) {
			LOGGER.error("Error ", e);
			e.printStackTrace();
			
			switch (key) {
			case "type":
				value = getType();
				break;
			case "reportable":
				value = getReportable();
				break;
			case "visible":
				value = String.valueOf(isVisible());
				break;
			case "searchable":
				value = String.valueOf(isSearchable());
				break;
			case "maxlength":
				if (getMaxLength() == -1)
					value = "";
				else
					value = String.valueOf(getMaxLength());
				break;
			case "precision":
				if (getPrecision() == -1)
					value = "";
				else
					value = String.valueOf(getPrecision());
				break;
			case "scale":
				if (getScale() == -1)
					value = "";
				else
					value = String.valueOf(getScale());
				break;
			case "catcode":
				value = getCatalogueCode();
				break;
			case "single_repeatable":
				value = getSingleOrRepeatable();
				break;
			case "inheritance":
				value = getInheritance();
				break;
			case "uniqueness":
				value = String.valueOf(isUniqueness());
				break;
			case "termcodealias":
				value = String.valueOf(isTermCodeAlias());
				break;
			default:
				found = false;
				break;
			}
		}

		if (!found)
			throw new WrongKeyException();

		return value;
	}

	@Override
	public String getValueByKey(String key) {

		String value = "";

		switch (key) {
		case "ATTR_CODE":
			value = getCode();
			break;
		case "ATTR_NAME":
			value = getName();
			break;
		case "ATTR_LABEL":
			value = getLabel();
			break;
		case "ATTR_SCOPENOTE":
			value = getScopenotes();
			break;
		case "ATTR_ORDER":
			value = String.valueOf(getOrder());
			break;
		case "ATTR_TYPE":
			value = getType();
			break;
		case "ATTR_REPORTABLE":
			value = getReportable();
			break;
		case "ATTR_VISIBLE":
			value = String.valueOf(isVisible());
			break;
		case "ATTR_SEARCHABLE":
			value = String.valueOf(isSearchable());
			break;
		case "ATTR_MAX_LENGTH":

			int maxLength = getMaxLength();

			if (maxLength != -1)
				value = String.valueOf(maxLength);
			else
				value = "";
			break;

		case "ATTR_PRECISION":

			int precision = getPrecision();

			if (precision != -1)
				value = String.valueOf(precision);
			else
				value = "";
			break;

		case "ATTR_SCALE":

			int scale = getScale();

			if (scale != -1)
				value = String.valueOf(scale);
			else
				value = "";
			break;

		case "ATTR_CAT_CODE":
			value = getCatalogueCode();
			break;
		case "ATTR_SINGLE_REPEATABLE":
			value = getSingleOrRepeatable();
			break;
		case "ATTR_INHERITANCE":
			value = getInheritance();
			break;
		case "ATTR_UNIQUENESS":
			value = String.valueOf(isUniqueness());
			break;
		case "ATTR_TERM_CODE_ALIAS":
			value = String.valueOf(isTermCodeAlias());
			break;
		case "ATTR_VERSION":
			value = getVersion();
			break;
		case "ATTR_LAST_UPDATE":
			if (getLastUpdate() != null)
				value = DateTrimmer.dateToString(getLastUpdate());
			break;
		case "ATTR_VALID_FROM":
			if (getValidFrom() != null)
				value = DateTrimmer.dateToString(getValidFrom());
			break;
		case "ATTR_VALID_TO":
			if (getValidTo() != null)
				value = DateTrimmer.dateToString(getValidTo());
			break;
		case "ATTR_STATUS":
			value = getStatus();
			break;
		case "ATTR_DEPRECATED":
			value = BooleanConverter.toNumericBoolean(String.valueOf(isDeprecated()));
			break;

		default:
			break;
		}

		return value;
	}

	/*
	 * private String[] splitCatalogueCode() {
	 * 
	 * // Return if the attribute is not a catalogue attribute if ( !isCatalogue() )
	 * { System.err.println (
	 * "Cannot get the catalogue code of a non-catalogue attribute" ); return null;
	 * }
	 * 
	 * String[] codes = this.getCatalogueCode().split( "\\." );
	 * 
	 * // Skip if wrong format if ( codes.length != 2 ) { System.err.println (
	 * "Wrong attributeCatalogueCode format! Found " + this.getCatalogueCode() +
	 * " The code format should be: catalogueCode.HierarchyCode" ); return null; }
	 * 
	 * return codes; }
	 */

	/*
	 * public String getSplittedCatalogueCode() { String[] codes =
	 * splitCatalogueCode();
	 * 
	 * if ( codes == null ) return null;
	 * 
	 * 
	 * }
	 */

	/**
	 * Check if the attribute is a facet category or not.
	 * 
	 * @return
	 */
	public boolean isFacetCategory() {
		return getHierarchy() != null;
	}

	/**
	 * Retrieve the hierarchy of a catalogue attribute
	 * 
	 * @return Hierarchy if it is found, otherwise null.
	 */
	public Hierarchy getHierarchy() {

		// Return if the attribute is not a catalogue attribute
		if (!isCatalogue()) {
			return null;
		}

		String catalogueCode = catalogue.getCode();

		// search the hierarchy
		for (Hierarchy h : catalogue.getHierarchies()) {

			// get the catalogue code and the hierarchy code from the composite field of the
			// attribute
			// these information are separated by a dot
			// codes[0] => the code of the catalogue which contains the hierarchy
			// codes[1] => the hierarchy code
			String[] codes = this.getCatalogueCode().split("\\.");

			// Skip if wrong format
			if (codes.length != 2) {
				LOGGER.error("Wrong attributeCatalogueCode format! Found " + this.getCatalogueCode()
						+ " The code format should be: catalogueCode.HierarchyCode");
				continue;
			}

			// skip check if local catalogue, since its code is not relevant
			// skip if the catalogue code is not the same as the current catalogue
			// (because we cannot retrieve the information)
			if (!catalogue.isLocal() && !codes[0].equals(catalogueCode))
				continue;

			// if the hierarchy code of the catalogue attribute is the same
			// of the current considered hierarchy we have finished
			if (codes[1].equals(h.getCode()))
				return h;
		}

		return null;
	}

	@Override
	public String toString() {
		return "ATTRIBUTE " + getLabel() + ";code " + getCode() + ";id " + getId();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Attribute) {
			Attribute attr = (Attribute) obj;
			return getId() == attr.getId();
		}

		return super.equals(obj);
	}
}
