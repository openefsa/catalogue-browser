package catalogue_object;

/**
 * Global term which indicates that all the hierarchies
 * in which the {@code term} is NOT present should
 * be shown. This is used to add the term into new
 * hierarchies.
 * @author avonva
 *
 */
public class AvailableHierarchiesTerm extends GlobalTerm {

	Term term;
	
	/**
	 * 
	 * @param name
	 * @param term
	 */
	public AvailableHierarchiesTerm( String name, Term term ) {
		super( name );
		this.term = term;
	}

	public AvailableHierarchiesTerm( Term term ) {
		this( "", term );
	}
	
	public Term getTerm() {
		return term;
	}
}
