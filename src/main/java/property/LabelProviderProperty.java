package property;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;

public class LabelProviderProperty implements ILabelProvider {

	Image image1;

	public LabelProviderProperty() {
		/*
		 * image1= new
		 * Image(Display.getCurrent(),"bin/toolbarButtonGraphics/general/New24.gif" );
		 */
		image1 = null;
	}

	public void addListener(ILabelProviderListener arg0) {
	}

	public void dispose() {
		image1 = null;
	}

	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	public void removeListener(ILabelProviderListener arg0) {
	}

	/**
	 * Method that obtain the cached image corresponding to the descriptor.
	 */
	public Image getImage(Object term) {
		// obtain the cached image corresponding to the descriptor
		return image1;
	}

	public String getText(Object arg0) {

		if (arg0 instanceof Hierarchy) {

			Hierarchy t = (Hierarchy) arg0;

			// TODO bring it back as was before after inserting the group id directly in the
			// catalogue
			String label = "";
			// if the attribute is found show the facet category group
			Attribute facetCategory = t.getCatalogue().getAttributeByName(t.getCode());
			if (facetCategory != null && t.getCatalogue().isMTXCatalogue())
				label = "[" + facetCategory.getCode() + "] " + facetCategory.getLabel();
			else
				label = t.getLabel();

			if (t.isDeprecated()) {
				label = label + " [DEPRECATED]";
			}
			return label;
		} else {
			Nameable t = (Nameable) arg0;
			return t.getLabel();
		}
	}
}
