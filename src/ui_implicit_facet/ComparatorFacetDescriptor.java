package ui_implicit_facet;

import java.util.Comparator;

/**
 * Decide the order of facet descriptors to make the full code string
 * @author avonva
 *
 */
public class ComparatorFacetDescriptor implements Comparator<FacetDescriptor> {
	
	@Override
	public int compare(FacetDescriptor o1, FacetDescriptor o2) {
		
		return o1.getTerm().getCode().compareTo( o2.getTerm().getCode() );
	}
}
