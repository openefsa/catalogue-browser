package business_rules;
import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ContentProviderWarning implements IStructuredContentProvider {

	public void dispose ( ) {
	}

	public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {}

	@SuppressWarnings("unchecked")
	public Object[] getElements ( Object warnings ) {
		ArrayList< String > l = (ArrayList< String >) warnings;
		return l.toArray();
	}
}
