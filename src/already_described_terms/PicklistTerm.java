package already_described_terms;

import catalogue.Catalogue;

/**
 * This class is used to model a term which is contained in a picklist (excel file with already described terms)
 * @author avonva
 *
 */
public class PicklistTerm extends DescribedTerm {

	// the picklist add also the level information to the described term
	// this information is used to indent terms in the window
	private int id, level;
	
	/**
	 * Constructor
	 * @param level, level of indentation (visualization purposes)
	 * @param code
	 * @param label
	 */
	public PicklistTerm( Catalogue catalogue, int level, String code, String label ) {
		
		super(catalogue, code, label);
		
		this.level = level;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	/**
	 * Get the term level of indentation
	 * @return
	 */
	public int getLevel() {
		return level;
	}
	
	/**
	 * Get the picklist term name indented using its level
	 * @return
	 */
	public String getIndentedLabel() {
		
		StringBuilder label = new StringBuilder();

		// make the indentation using the picklist level
		for ( int i = 1; i < getLevel(); i++ )
			label.append( "\t" );
		
		label.append( getLabel() );
		
		return label.toString();
	}
}
