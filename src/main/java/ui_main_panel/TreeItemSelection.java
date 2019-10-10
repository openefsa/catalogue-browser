package ui_main_panel;

import catalogue_object.Hierarchy;
import catalogue_object.Term;

public class TreeItemSelection {

	private Term term;
	private Hierarchy hierarchy;
	
	public TreeItemSelection(Term term, Hierarchy hierarchy) {
		this.term = term;
		this.hierarchy = hierarchy;
	}
	
	public Term getTerm() {
		return term;
	}
	
	public Hierarchy getHierarchy() {
		return hierarchy;
	}
	
	@Override
	public String toString() {
		return term + " " + hierarchy;
	}
}
