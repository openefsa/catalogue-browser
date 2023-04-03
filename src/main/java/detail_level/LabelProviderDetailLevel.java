package detail_level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class LabelProviderDetailLevel implements ILabelProvider {

	private static final Logger LOGGER = LogManager.getLogger(LabelProviderDetailLevel.class);

	@Override
	public String getText(Object attr) {

		DetailLevelGraphics detailLevel = (DetailLevelGraphics) attr;

		// get the name of the detail level from the attribute
		return detailLevel.getLabel();
	}

	@Override
	public void addListener(ILabelProviderListener arg0) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener arg0) {
	}

	@Override
	public Image getImage(Object attr) {

		DetailLevelGraphics detailLevel = (DetailLevelGraphics) attr;

		// try to get the image from the main folder
		Image image = null;
		try {
			image = new Image(Display.getCurrent(),
					LabelProviderDetailLevel.class.getClassLoader().getResourceAsStream(detailLevel.getImageName()));
		} catch (Exception e) {
			LOGGER.error("Cannot find icons", e);
			e.printStackTrace();
		}

		return image;
	}
}
