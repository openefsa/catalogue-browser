package user_preferences;

import catalogue.Catalogue;

/**
 * Class to model the record of the tables : type_search_opt and attr_search_opt
 * We use search options to filter the search results according to
 * the term types and the term implicit attributes
 * @author avonva
 *
 */
public class SearchOption {

	private Catalogue catalogue;
	
	// the id of the term type or the implicit attribute
	private int id;
	
	// is this option enabled?
	private boolean enabled;
	
	// we have a term attribute or an implicit attribute?
	private OptionType type;
	
	/**
	 * Create a search option object
	 * @param id the id of either the term type or the implicit attribute
	 * we are considering
	 * @param enabled if the search option is enabled (if so the search will
	 * search also the keywords in this field)
	 */
	public SearchOption( Catalogue catalogue, int id, boolean enabled, OptionType type ) {
		this.catalogue = catalogue;
		this.id = id;
		this.enabled = enabled;
		this.type = type;
	}
	
	/**
	 * Get the term type or attribute id
	 * @return
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Get if the option is enabled or not
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Get the type of the search option
	 * @return
	 */
	public OptionType getType() {
		return type;
	}
	
	/**
	 * Check if the option regards a term type
	 * @return
	 */
	public boolean isTermType() {
		return type == OptionType.TERM_TYPE;
	}
	
	/**
	 * Check if the option regards an attribute
	 * @return
	 */
	public boolean isAttribute() {
		return type == OptionType.ATTRIBUTE;
	}
	
	/**
	 * Get the catalogue related to this search option
	 * @return
	 */
	public Catalogue getCatalogue() {
		return catalogue;
	}
	
	/**
	 * Get the search option name
	 * Note that we assume that consistency is respected.
	 * @return
	 */
	public String getName () {
		
		String name = null;
		
		switch ( type ) {
		case TERM_TYPE:
			name = catalogue.getTermTypeById( id ).getLabel();
			break;
		case ATTRIBUTE:
			name = catalogue.getAttributeById( id ).getLabel();
			break;
		}
		
		return name;
	}
	
	/**
	 * Enable or disable the option
	 * @param enabled
	 */
	public void setEnabled( boolean enabled ) {
		this.enabled = enabled;
	}
}
