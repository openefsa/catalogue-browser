<<<<<<< HEAD
package user_preferences;

import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ContentProviderSearchOption implements IStructuredContentProvider {

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

	@Override
	public Object[] getElements(Object arg0) {
		
		ArrayList< SearchOption > opts = (ArrayList< SearchOption >) arg0;
		return opts.toArray();
	}

}
=======
package user_preferences;

import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ContentProviderSearchOption implements IStructuredContentProvider {

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

	@Override
	public Object[] getElements(Object arg0) {
		
		ArrayList< SearchOption > opts = (ArrayList< SearchOption >) arg0;
		return opts.toArray();
	}

}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
