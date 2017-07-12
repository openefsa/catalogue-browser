package catalogue_object;

import java.sql.Timestamp;
import java.util.ArrayList;

import catalogue.Catalogue;
import catalogue_browser_dao.ParentTermDAO;
import data_transformation.BooleanConverter;
import data_transformation.DateTrimmer;
import global_manager.GlobalManager;
import term.WrongKeyException;

/**
 * Model a record of the Hierarchy DB table
 * @author avonva
 *
 */
public class Hierarchy extends SortableCatalogueObject implements Mappable {

	// the code of the master hierarchy code when we read/write a term sheet
	// used for import/export excel
	public static final String MASTER_HIERARCHY_CODE = "master";
	
	private String applicability;
	private boolean master;
	private String groups;
	
	/**
	 * Get a default hierarchy object with all the fields set to their default value
	 * we use this method to add new hierachies with the hierachy editor
	 * @return
	 */
	public static Hierarchy getDefaultHierarchy () {
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		int newOrder = 0;
		for ( Hierarchy hierarchy : currentCat.getHierarchies() ) {
			// if a hierarchy with an order greater or equal to
			// the new hierarchy order => update the new hierarchy order
			if ( hierarchy.getOrder() >= newOrder )
				newOrder = hierarchy.getOrder() + 1;
		}
		
		return new Hierarchy ( currentCat, -1, "Change", "Change", "", "", 
				"base", newOrder, "", false, "", null, null, null, false, "" );
	}
	
	public Hierarchy( Catalogue catalogue ) {
		super( catalogue );
	}
	
	public Hierarchy( Catalogue catalogue, int id, String code, String name, String label, String scopenotes,
			String applicability, int order, String status, boolean master, String version, Timestamp lastUpdate,
			Timestamp validFrom, Timestamp validTo, boolean deprecated, String groups ) {

		super ( catalogue, id, code, name, label, scopenotes, order, status, 
				version, lastUpdate, validFrom, validTo, deprecated );

		this.applicability = applicability;
		this.master = master;
		this.groups = groups;
	}
	
	public void setApplicability(String applicability) {
		this.applicability = applicability;
	}
	public void setGroups(String groups) {
		this.groups = groups;
	}
	
	public String getApplicability() {
		return applicability;
	}
	
	public String getGroups() {
		return groups;
	}
	
	
	/**
	 * Retrieve a hierarchy field using the db column name
	 * @param key
	 * @return
	 */
	public String getValueByKey( String key ) {
		
		String value = "";
		
		switch ( key ) {
		case "HIERARCHY_CODE":
			value = getCode(); break;
		case "HIERARCHY_NAME":
			value = getName(); break;
		case "HIERARCHY_LABEL":
			value = getLabel(); break;
		case "HIERARCHY_SCOPENOTE":
			value = getScopenotes(); break;
		case "HIERARCHY_APPLICABILITY":
			value = getApplicability(); break;
		case "HIERARCHY_ORDER":
			value = String.valueOf( getOrder() ); break;
		case "HIERARCHY_VERSION":
			value = getVersion(); break;
		case "HIERARCHY_GROUPS":
			value = getGroups(); break;
		case "HIERARCHY_LAST_UPDATE":
			if ( getLastUpdate() != null )
				value = DateTrimmer.dateToString( getLastUpdate() ); 
			break;
		case "HIERARCHY_VALID_FROM":
			if ( getValidFrom() != null )
				value = DateTrimmer.dateToString( getValidFrom() ); 
			break;
		case "HIERARCHY_VALID_TO":
			if ( getValidTo() != null )
				value = DateTrimmer.dateToString( getValidTo() ); 
			break;
		case "HIERARCHY_STATUS":
			value = getStatus(); break;
		case "HIERARCHY_DEPRECATED":
			value = BooleanConverter.toNumericBoolean( String.valueOf( isDeprecated() ) ); break;
		default:
			break;
		}
		
		return value;
	}
	
	
	@Override
	public void setVariableByKey(String key, String value) throws WrongKeyException {
		
		boolean found = true;

		key = key.toLowerCase();
		
		// search in the property field, otherwise if nothing is found then search in the others
		try {
			super.setVariableByKey(key, value);
		} catch ( WrongKeyException e ) {
			
			switch ( key ) {
			case "applicability":
				setApplicability( value ); break;
			case "status":
				setStatus( value ); break;
			default:
				found = false; break;
			}
		}
		
		if ( !found )
			throw new WrongKeyException();
		
	}
	
	@Override
	public String getVariableByKey(String key) throws WrongKeyException {
		
		boolean found = true;
		
		key = key.toLowerCase();
		
		String value = null;
		
		// try to retrieve a field from the dcf property fields
		// if no field is retrieved, search on the hierarchy fields
		try {
			
			value = super.getVariableByKey(key);
			
		} catch ( WrongKeyException e ) {
			
			switch ( key ) {
			case "applicability":
				value = getApplicability(); break;
			case "status":
				value = getStatus(); break;
			default:
				found = false; break;
			}
		}
		
		if ( !found )
			throw new WrongKeyException();
		
		return value;
	}
	
	
	/**
	 * Is the hierarchy a facet hierarchy?
	 * @return
	 */
	public boolean isFacet () {
		return applicability.equals( "attribute" );
	}
	
	/**
	 * is the hierarchy both base and facet ?
	 * @return
	 */
	public boolean isBoth () {
		return applicability.equals( "both" );
	}
	
	/**
	 * Is the hierarchy a base hierarchy?
	 * @return
	 */
	public boolean isHierarchy () {
		return applicability.equals( "base" ) || applicability.equals( "both" );
	}
	
	/**
	 * Is the hierarchy the master hierarchy?
	 * @return
	 */
	public boolean isMaster () {
		return master;
	}

	
	/**
	 * Get the first level terms, the ones which do not have a parent in this hierarchy
	 * @return
	 */
	public ArrayList<Term> getFirstLevelNodes( boolean hideDeprecated, boolean hideNotReportable ) {
		
		ParentTermDAO parentDao = new ParentTermDAO( catalogue );
		
		return parentDao.getFirstLevelNodes( this, hideDeprecated, hideNotReportable );
	}
	
	/**
	 * Normalize order of term in hierarchy
	 * @param hierarchy
	 * @return
	 */
	public void normalizeLevel() {
		Term.normalizeLevel ( getFirstLevelNodes( false, false ), this );
	}
	

	@Override
	public boolean equals(Object obj) {
		
		if ( obj instanceof Hierarchy )
			return getId() == ( (Hierarchy) obj ).getId();
		
		return super.equals(obj);
	}
	
	@Override
	public String toString() {
		return "HIERARCHY id " + getId() + " code " + getCode() + " label " + getLabel();
	}
	

}
