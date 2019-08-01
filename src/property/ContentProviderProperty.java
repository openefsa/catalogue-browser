<<<<<<< HEAD
package property;
import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import catalogue_object.SortableCatalogueObject;

public class ContentProviderProperty implements IStructuredContentProvider {

	public void dispose ( ) {
	}

	public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {}

	public Object[] getElements ( Object list ) {
		ArrayList< SortableCatalogueObject > l = (ArrayList< SortableCatalogueObject >) list;
		return l.toArray();
	}

}
=======
package property;
import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import catalogue_object.SortableCatalogueObject;

public class ContentProviderProperty implements IStructuredContentProvider {

	public void dispose ( ) {
	}

	public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {}

	public Object[] getElements ( Object list ) {
		ArrayList< SortableCatalogueObject > l = (ArrayList< SortableCatalogueObject >) list;
		return l.toArray();
	}

}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
