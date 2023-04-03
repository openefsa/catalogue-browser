package term;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import catalogue_object.GlobalTerm;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;
import detail_level.DetailLevelDAO;
import detail_level.DetailLevelGraphics;
import i18n_messages.CBMessages;

/**
 * This class load all the visualization Label in GUI TableView to view the
 * result of search query.
 * 
 * @author thomm
 * 
 */
public class LabelProviderTerm extends LabelProvider implements IFontProvider {

	private static final Logger LOGGER = LogManager.getLogger(LabelProviderTerm.class);

	private ArrayList<DetailLevelGraphics> detailLevels;

	private final Image facetFolder;
	private final Image hierarchyFolder;
	private final Image folder;
	private ArrayList<ImageCache> images = new ArrayList<>();

	// currently selected hierarchy
	private Hierarchy currentHierarchy;
	private boolean hideCode = false;

	/**
	 * Set if we want to hide the term code in the visualization
	 * 
	 * @param hide
	 */
	public void setHideCode(boolean hideCode) {
		this.hideCode = hideCode;
	}

	/**
	 * Initialize the label provider and make it aware of the catalogue we are
	 * working with
	 * 
	 * @param catalogue
	 */
	public LabelProviderTerm() {

		DetailLevelDAO detailDao = new DetailLevelDAO();

		// fetch all the detail levels
		detailLevels = detailDao.getAll();

		// load images
		hierarchyFolder = getImage("HierarchyFolder.ico", null);
		facetFolder = getImage("FacetFolder.ico", null);
		folder = getImage("Folder.gif", null);
	}

	/**
	 * Load a image from a file Pass as input also a cache object to store a cache
	 * 
	 * @param imageName
	 * @param cache
	 * @return
	 */
	private Image getImage(String imageName, Image cache) {

		// if empty cache load image
		if (cache == null) {

			try {
				cache = new Image(Display.getCurrent(),
						LabelProviderTerm.class.getClassLoader().getResourceAsStream(imageName));
			} catch (Exception e) {
				LOGGER.error("Cannot get image", e);
				e.printStackTrace();
			}
		}

		return cache;
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

	/**
	 * Get the image related to a term
	 */
	@SuppressWarnings("unlikely-arg-type")
	public Image getImage(Object term) {

		if (term instanceof Term) {

			Term t = (Term) term;

			// if level of detail not set return void image
			if (t.getDetailLevel() == null)
				return null;

			int index = detailLevels.indexOf(t.getDetailLevel());

			// if detail level not found => return
			// this is the case when we have a "None"
			// detail level
			if (index == -1)
				return null;

			// get the current detail level
			DetailLevelGraphics dlg = detailLevels.get(index);

			// try to get the image from the main folder
			Image image = null;
			try {

				// check the cache, if found return the image already instantiated
				for (ImageCache cache : images) {
					if (cache.getKey().equals(dlg.getImageName()))
						return cache.getImage();
				}

				// otherwise create a new image and add it to the cache
				image = new Image(Display.getCurrent(),
						LabelProviderTerm.class.getClassLoader().getResourceAsStream(dlg.getImageName()));

				images.add(new ImageCache(dlg.getImageName(), image));

			} catch (Exception e) {
				LOGGER.error("Cannot find icons", e);
				e.printStackTrace();
			}

			return image;

		} else {

			if (term instanceof Hierarchy) {

				// if we have a base hierarchy return the hierarchy folder
				// otherwise return the facet folder
				Hierarchy h = (Hierarchy) term;
				if ((hierarchyFolder != null) && (h.isHierarchy()))
					return hierarchyFolder;
				else
					return facetFolder;

			} else if (term instanceof GlobalTerm)
				return folder;
		}
		return null;
	}

	/**
	 * Get the detail levels
	 * 
	 * @return
	 */
	public ArrayList<DetailLevelGraphics> getDetailLevels() {
		return detailLevels;
	}

	/**
	 * Get the text of a term which needs to be displayed
	 */
	public String getText(Object term) {

		String text = null;

		if (term instanceof Term) {

			Term t = (Term) term;

			// flag for dismissed and not reportable terms
			String flag = "";

			// if not hiding code, add it to flag
			if (!hideCode)
				flag = flag + "[" + t.getCode() + "]";

			// if deprecated add the deprecated flag
			if (t.isDeprecated()) {
				flag = flag + CBMessages.getString("LabelProviderTerm.DeprecatedFlag");
			} else if (t.isDismissed(currentHierarchy)) {
				flag = flag + CBMessages.getString("LabelProviderTerm.DismissedFlag");
			}
			// if non reportable add the non reportable flag
			else if (!t.isReportable(currentHierarchy))
				flag = flag + CBMessages.getString("LabelProviderTerm.NotReportableFlag");

			// term name + term flag
			text = t.getShortName(true) + " " + flag;

		} else if (term instanceof Nameable) {

			Nameable t = (Nameable) term;
			text = t.getLabel();
		} else {
			text = CBMessages.getString("LabelProviderTerm.NameNotAvailable");
		}
		LOGGER.info("The text of a term which needs to be displayed : " + text);
		return text;
	}

	@Override
	public Font getFont(Object term) {

		if (term instanceof Term) {

			Term t = (Term) term;

			return t.getApplicabilityFont(currentHierarchy);
		}

		return null;
	}

	/**
	 * Show all the terms related to this hierarchy
	 * 
	 * @param hierarchy
	 */
	public void setCurrentHierarchy(Hierarchy hierarchy) {
		currentHierarchy = hierarchy;
	}

	/**
	 * Get the current hierarchy of the label provider
	 * 
	 * @return
	 */
	public Hierarchy getCurrentHierarchy() {
		return currentHierarchy;
	}

	/**
	 * Class to save data of images in a key, data object
	 * 
	 * @author avonva
	 *
	 */
	private class ImageCache {
		private Image image;
		private String key;

		public ImageCache(String key, Image image) {
			this.key = key;
			this.image = image;
		}

		public String getKey() {
			return key;
		}

		public Image getImage() {
			return image;
		}
	}
}