package ui_implicit_facet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import i18n_messages.CBMessages;
import term.LabelProviderTerm;

/**
 * Class which provide the term's label shown in the various ui tabs
 * 
 * @author Alban.Shahaj
 *
 */
public class LabelProviderImplicitFacets implements ILabelProvider {

	private static final Logger LOGGER = LogManager.getLogger(LabelProviderImplicitFacets.class);

	LabelProviderTerm termLabelProvider;
	Image facetCategoryImage = null;

	/**
	 * Set the current hierarchy
	 * 
	 * @param hierarchy
	 */
	public void setCurrentHierarchy(Hierarchy hierarchy) {
		termLabelProvider.setCurrentHierarchy(hierarchy);
	}

	public LabelProviderImplicitFacets() {

		termLabelProvider = new LabelProviderTerm();

		try {
			facetCategoryImage = new Image(Display.getCurrent(),
					LabelProviderImplicitFacets.class.getClassLoader().getResourceAsStream("FacetFolder.ico"));
		} catch (Exception e) {
			LOGGER.error("Cannot get image", e);
			e.printStackTrace();
		}

	}

	public void addListener(ILabelProviderListener arg0) {

	}

	public void dispose() {

	}

	public boolean isLabelProperty(Object arg0, String arg1) {

		return false;
	}

	public void removeListener(ILabelProviderListener arg0) {

	}

	public Image getImage(Object arg0) {

		if (arg0 != null) {

			// for terms we get the corex flag image
			if (arg0 instanceof DescriptorTreeItem) {
				DescriptorTreeItem np = (DescriptorTreeItem) arg0;
				Nameable t = np.getTerm();
				return termLabelProvider.getImage(t);
			}

			// for facet category we use the facet folder image
			if (arg0 instanceof Attribute) {
				return facetCategoryImage;
			}
		}

		return null;
	}

	// get the tree item names
	public String getText(Object arg0) {

		if (arg0 != null) {
			if (arg0 instanceof DescriptorTreeItem) {

				DescriptorTreeItem item = (DescriptorTreeItem) arg0;

				// get the term from the tree item
				Nameable term = item.getTerm();
				if (term != null) {
					// set first the hierarchy which the terms belongs to
					termLabelProvider.setCurrentHierarchy(item.getDescriptor().getFacetCategory().getHierarchy());
					// return the term label
					return termLabelProvider.getText(term);
				} else
					return CBMessages.getString("LabelProviderImplicitFacet.ErrorName1");

			} else {

				// get the facet category label and code
				if (arg0 instanceof Attribute) {
					Attribute facetCategory = (Attribute) arg0;
					String text = "[" + facetCategory.getCode() + "] " + facetCategory.getLabel();
					return text;
				}
			}
		}

		return CBMessages.getString("LabelProviderImplicitFacet.ErrorName2"); //$NON-NLS-1$
	}

}
