package user_preferences;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

public class LabelProviderSearchOption implements ILabelProvider {

	@Override
	public void addListener(ILabelProviderListener arg0) {}

	@Override
	public void dispose() {}

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener arg0) {}

	@Override
	public Image getImage(Object arg0) {
		return null;
	}

	@Override
	public String getText(Object arg0) {
		
		return ( (SearchOption) arg0 ).getName();
	}
}
