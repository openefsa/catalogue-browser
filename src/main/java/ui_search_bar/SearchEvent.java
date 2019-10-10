package ui_search_bar;

import java.util.ArrayList;

import org.eclipse.swt.widgets.Event;

import catalogue_object.Term;

/**
 * Search event to host data of arrayList of term type
 * @author avonva
 *
 */
public class SearchEvent extends Event {
	
	private ArrayList<Term> data;
	
	public void setResults(ArrayList<Term> data) {
		this.data = data;
	}
	public ArrayList<Term> getResults() {
		return data;
	}
}
