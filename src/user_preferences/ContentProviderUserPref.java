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
		
		@SuppressWarnings("unchecked")
		ArrayList< CataloguePreference > l = ( ArrayList< CataloguePreference > ) list;
		return l.toArray();
	}

}
