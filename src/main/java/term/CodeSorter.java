package term;

import java.util.Comparator;

import catalogue_object.Term;

/**
 * Alphabetical sorter for terms
 * @author avonva
 *
 */
public class CodeSorter implements Comparator<Term> {

	@Override
	public int compare(Term arg0, Term arg1) {
		// sort by code
		return arg0.getCode().compareTo( arg1.getCode() );
	}
}
