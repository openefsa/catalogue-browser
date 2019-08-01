<<<<<<< HEAD
package user_preferences;

import java.util.ArrayList;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;


public class ContentProviderUserPref implements IStructuredContentProvider {

	public void dispose ( ) {
	}

	public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {
	}

	public Object[] getElements ( Object list ) {
		
		ArrayList< CataloguePreference > l = ( ArrayList< CataloguePreference > ) list;
		return l.toArray();
	}

}
=======
package user_preferences;

import java.util.ArrayList;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;


public class ContentProviderUserPref implements IStructuredContentProvider {

	public void dispose ( ) {
	}

	public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {
	}

	public Object[] getElements ( Object list ) {
		
		ArrayList< CataloguePreference > l = ( ArrayList< CataloguePreference > ) list;
		return l.toArray();
	}

}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
