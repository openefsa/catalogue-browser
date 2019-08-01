<<<<<<< HEAD
package term_type;

import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ContentProviderTermType implements IStructuredContentProvider {

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

	@Override
	public Object[] getElements(Object list) {
		ArrayList< TermType > l = (ArrayList< TermType >) list;
		return l.toArray();
	}

}
=======
package term_type;

import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ContentProviderTermType implements IStructuredContentProvider {

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

	@Override
	public Object[] getElements(Object list) {
		ArrayList< TermType > l = (ArrayList< TermType >) list;
		return l.toArray();
	}

}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
