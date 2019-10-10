package ui_search_bar;

import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
/**
 * Event to support the selection of a hierarchy
 * starting from the applicable hierarchies of a term.
 * @author avonva
 *
 */
public class HierarchyEvent {

	private Hierarchy hierarchy;
	private Nameable term;
	
	/**
	 * Set the hierarchy which is changed
	 * @param hierarchy
	 */
	public void setHierarchy(Hierarchy hierarchy) {
		this.hierarchy = hierarchy;
	}
	
	/**
	 * Get the selected hierarchy
	 * @return
	 */
	public Hierarchy getHierarchy() {
		return hierarchy;
	}
	
	/**
	 * Set the selected term
	 * @param term
	 */
	public void setTerm(Nameable term) {
		this.term = term;
	}
	
	/**
	 * Get the selected term (if there is one)
	 * @return
	 */
	public Nameable getTerm() {
		return term;
	}
}
