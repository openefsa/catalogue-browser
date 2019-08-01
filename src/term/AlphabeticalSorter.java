package term;

import java.util.Comparator;

import catalogue_object.Term;

/**
 * Alphabetical sorter for terms
 * @author avonva
 *
 */
public class AlphabeticalSorter implements Comparator<Term> {

	@Override
	public int compare(Term arg0, Term arg1) {
		// sort by short name (or extended if short not present)
		return arg0.getShortName( true ).compareTo( arg1.getShortName( true ) );
	}
}
