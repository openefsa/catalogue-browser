package ui_pending_request_list;

import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class RequestContentProvider implements IStructuredContentProvider {

	public void dispose() {}

	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

	public Object[] getElements(Object warnings) {
		@SuppressWarnings("unchecked")
		ArrayList<String> l = (ArrayList<String>) warnings;
		return l.toArray();
	}
}
