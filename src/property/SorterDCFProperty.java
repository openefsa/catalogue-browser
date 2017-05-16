package property;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import catalogue_object.SortableCatalogueObject;

/**
 * Class used to sort DCFProperties using their order
 * 
 * @author
 * 
 */
public class SorterDCFProperty extends ViewerSorter {

	@Override
	public int compare ( Viewer viewer , Object e1 , Object e2 ) {
		int cmps = 0;
		if ( ( e1 instanceof SortableCatalogueObject ) && ( e2 instanceof SortableCatalogueObject ) ) {
			SortableCatalogueObject t1 = (SortableCatalogueObject) e1;
			SortableCatalogueObject t2 = (SortableCatalogueObject) e2;

			if ( t1.getOrder() < t2.getOrder() )
				cmps = -1;
			else if ( t1.getOrder() == t2.getOrder() )
				cmps = 0;
			else if ( t1.getOrder() > t2.getOrder() )
				cmps = 1;
		}
		return cmps;
	}
}
