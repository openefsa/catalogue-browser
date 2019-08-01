package term_type;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

public class LabelProviderTermType implements ILabelProvider {

	@Override
	public String getText(Object attr) {

		TermType termType = (TermType) attr;

		// get the name of the term attribute from the attribute
		return termType.getLabel();
	}

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
	public Image getImage(Object attr) {
		return null;
	}
}
