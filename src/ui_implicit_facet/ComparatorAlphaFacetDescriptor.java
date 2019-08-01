package ui_implicit_facet;

import java.util.Comparator;

/**
 * Decide the order of facet descriptors to make the full code string
 * @author avonva
 *
 */
public class ComparatorAlphaFacetDescriptor implements Comparator<FacetDescriptor> {
	
	@Override
	public int compare(FacetDescriptor o1, FacetDescriptor o2) {
		
		int comp = 0;
		
		// check order of attributes
		int orderComp = o1.getFacetHeader().compareTo( o2.getFacetHeader() );
		
		// if same order, order by code
		if ( orderComp == 0 )
			comp = o1.getTerm().getCode().compareTo( o2.getTerm().getCode() );
		else
			comp = orderComp;
		
		return comp;
	}
}
