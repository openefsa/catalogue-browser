package catalogue_object;

import java.util.ArrayList;
import java.util.StringTokenizer;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import detail_level.DetailLevelGraphics;
import global_manager.GlobalManager;
import naming_convention.SpecialValues;
import term_type.TermType;

/**
 * Class that models the table term attributes (relationship between attributes and terms)
 * @author avonva
 *
 */
public class TermAttribute {

	// id which is incrementally used for assigning NEW term attributes to NEW terms
	private static int lastId = Integer.MIN_VALUE;
	
	private int id;
	private Term term;
	private Attribute attribute;
	private String value;
	
	public TermAttribute( int id, Term term, Attribute attribute, String value ) {
		this.id = id;
		this.term = term;
		this.attribute = attribute;
		this.value = value;
	}
	
	public TermAttribute( Term term, Attribute attribute, String value ) {
		this(lastId, term, attribute, value);
		lastId++;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}

	/**
	 * Get the default term attribute object for a specific term ( we cannot create a default term,
	 * so we need it)
	 * @param term
	 * @return
	 */
	public static TermAttribute getDefaultTermAttribute( Term term ) {
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		AttributeDAO attrDao = new AttributeDAO( currentCat );
		
		// if there are not available attributes => return we cannot add anything
		ArrayList<Attribute> availableAttr = attrDao.getApplicableAttributes( term );
		if ( availableAttr.isEmpty() )
			return null;
		
		// otherwise set as default key the first available attribute label
		Attribute defaultAttribute = availableAttr.get(0);
		
		String defaultValue;
		switch ( defaultAttribute.getType() ) {
		case Attribute.booleanTypeName:
			defaultValue = "true"; break;
		case Attribute.integerTypeName:
		case Attribute.decimalTypeName:
			defaultValue = "0"; break;
		case Attribute.stringTypeName:
			defaultValue = "!CHANGE!"; break;
		default:
			defaultValue = "";
		}
		
		TermAttribute ta = new TermAttribute( term, defaultAttribute, defaultValue );
		
		return ta;
	}
	
	
	/**
	 * Get the detail level "hierarchy" for the term t, that is, the default (used to create a new term)
	 * @return
	 */
	public static TermAttribute getDefaultDetailLevel( Term t ) {
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		AttributeDAO attrDao = new AttributeDAO( currentCat );
		
		// get the attribute related to the detail levels
		Attribute attribute = attrDao.getByName( SpecialValues.DETAIL_LEVEL_NAME );
		
		// create the term attribute with "H" as detail level (which is the hierarchy)
		TermAttribute ta = new TermAttribute( t, attribute, "H" );
		
		return ta;
	}
	
	/**
	 * Get the default term type
	 * @param t
	 * @return
	 */
	public static TermAttribute getDefaultTermType( Term t ) {

		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		AttributeDAO attrDao = new AttributeDAO( currentCat );
		
		// get the attribute related to term types
		Attribute attribute = attrDao.getByName( SpecialValues.TERM_TYPE_NAME );

		// create the term attribute with the first available term type code
		TermAttribute ta = new TermAttribute( t, attribute, 
				currentCat.getTermTypes().get(0).getCode() );

		return ta;
	}
	
	
	public Term getTerm() {
		return term;
	}
	public Attribute getAttribute() {
		return attribute;
	}
	public String getValue() {
		return value;
	}
	
	/**
	 * Parse the dollar separated repeatable attribute value to get all
	 * the values in a list.
	 * @param value
	 * @return
	 */
	public ArrayList<String> getRepeatableValues () {
		
		ArrayList<String> values = new ArrayList<>();
		
		StringTokenizer st = new StringTokenizer( value, "$" );
		
		while ( st.hasMoreTokens() ) {
			values.add( st.nextToken() );
		}
		
		return values;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}
	public void setTerm(Term term) {
		this.term = term;
	}
	
	/**
	 * Check if the term attribute is a detail level and it is a hierarchy
	 * @return
	 */
	public boolean isHierarchyDetailLevel () {
		
		if ( !attribute.isDetailLevel() )
			return false;
		
		return( value.equals( "H" ) );
	}
	
	@Override
	public String toString() {
		return "TERMATTRIBUTE: id=" + id + " term=" + term + " attr=" + attribute + " value=" + value;
	}
	
	/**
	 * Two term attributes are equal if their terms and attributes are equal
	 */
	@Override
	public boolean equals(Object obj) {
		
		// if we have a term type we check that the code of the term type
		// is the term attribute value to recognize it
		if ( obj instanceof TermType ) {
			return getValue().equals( ( (TermType) obj).getCode() );
		}
		
		// if we have a detail level we check that the code is = term attribute value
		if ( obj instanceof DetailLevelGraphics ) {
			return getValue().equals( ( (DetailLevelGraphics) obj).getCode() );
		}
		
		// if we instead have a term attribute
		TermAttribute ta = (TermAttribute) obj;
		return ta.getId() == id;
	}
}
