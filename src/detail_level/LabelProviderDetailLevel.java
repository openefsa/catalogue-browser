package detail_level;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class LabelProviderDetailLevel implements ILabelProvider {

	@Override
	public String getText(Object attr) {

		DetailLevelGraphics detailLevel = (DetailLevelGraphics) attr;

		// get the name of the detail level from the attribute
		return detailLevel.getLabel();
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
		
		DetailLevelGraphics detailLevel = (DetailLevelGraphics) attr;

		// try to get the image from the main folder
		Image image = null;
		try {
			image = new Image( Display.getCurrent() , this.getClass().getClassLoader().getResourceAsStream(
					detailLevel.getImageName() ) );
		} catch ( Exception e ) {
			System.err.println( "Cannot find icons" );
		}
		
		return image;
	}
}
