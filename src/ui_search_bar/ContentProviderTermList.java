package ui_search_bar;
import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import catalogue_object.Term;

/**
 * This class is the Content provider for the result of search query.
 * 
 * @author thomm
 * 
 */
public class ContentProviderTermList implements IStructuredContentProvider {

	public void dispose ( ) {
	}

	public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {
		// System.out.println("Input changed: old=" + arg1 + ", new=" + arg2);
	}

	public Object[] getElements ( Object searchResults ) {
		@SuppressWarnings("unchecked")
		ArrayList< Term > l = (ArrayList< Term >) searchResults;
		return l.toArray();
	}

}
