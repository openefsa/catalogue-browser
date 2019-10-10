package ui_implicit_facet;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import catalogue_object.Attribute;
import catalogue_object.Term;

public class SorterImplicitFacets extends ViewerSorter {

	@Override
	public int compare ( Viewer viewer , Object e1 , Object e2 ) {
		
		int cmps = 0;
		
		// we order facet categories by their order code
		if ( ( e1 instanceof Attribute ) && ( e2 instanceof Attribute ) ) {
			
			Attribute t1 = (Attribute) e1;
			Attribute t2 = (Attribute) e2;

			if ( t1.getOrder() < t2.getOrder() )
				cmps = -1;
			else if ( t1.getOrder() == t2.getOrder() )
				cmps = 0;
			else if ( t1.getOrder() > t2.getOrder() )
				cmps = 1;
			
			return cmps;

		}
		
		// we order terms by their codes
		if ( ( e1 instanceof Term ) && ( e2 instanceof Term ) ) {
			
			Term t1 = (Term) e1;
			Term t2 = (Term) e2;

			return t1.getCode().compareTo( t2.getCode() );

		} else
			cmps = 0;
		return cmps;

	}

}
