package property;
import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import catalogue_object.SortableCatalogueObject;

public class ContentProviderProperty implements IStructuredContentProvider {

	public void dispose ( ) {
	}

	public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {
		//System.out.println("Input changed: old=" + arg1 + ", new=" + arg2);
	}

	public Object[] getElements ( Object list ) {
		@SuppressWarnings("unchecked")
		ArrayList< SortableCatalogueObject > l = (ArrayList< SortableCatalogueObject >) list;
		return l.toArray();
	}

}
