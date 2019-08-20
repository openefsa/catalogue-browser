package term;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import catalogue_object.AvailableHierarchiesTerm;
import catalogue_object.CatalogueObject;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import ui_main_panel.TermFilter;

/**
 * Class used for fill the tree view, thats implements ITreeContentProvider who
 * is an interface to content providers for tree structure oriented viewers
 * 
 * @author
 */
public class ContentProviderTerm implements ITreeContentProvider {

	// current hierarchy
	private Hierarchy hierarchy;

	// root of the tree
	private Object root;

	// applicability flags of the tree, which terms should be visualized?
	private boolean hideDeprecated = false;
	private boolean hideNotUse = false;

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object arg1, Object arg2) {
	}

	/**
	 * Method that return children element of parent(arg0) selected from user. This
	 * method is called when it the contentProvider needs to create or display the
	 * child elements of the domain(arg0). Should answer an array of domain objects
	 * that represent the unfiltered children of parent.
	 * 
	 * @param arg0 is the parent element
	 */
	public Object[] getChildren(Object arg0) {

		ArrayList<CatalogueObject> elem = new ArrayList<>();

		if (arg0 == null)
			return elem.toArray();

		// if we have a term we simply get its children
		if (arg0 instanceof Term) {

			// get all the children of the term
			ArrayList<Term> children = ((Term) arg0).getChildren(hierarchy, hideDeprecated, hideNotUse);

			elem.addAll(children);
		}

		else if ((arg0 instanceof Collection<?>)) {
			Collection<Term> searchTerms = (Collection<Term>) arg0;

			searchTerms = TermFilter.filterByFlag(hideDeprecated, hideNotUse, searchTerms, hierarchy);

			// filter terms and add them
			elem.addAll(searchTerms);
		}

		// if we have a hierarchy we simply get all its terms
		// in the first level
		// do not show hierarchies children if we are showing available hierarchies term
		else if (arg0 instanceof Hierarchy && !(root instanceof AvailableHierarchiesTerm)) {

			// get the terms which are at the first level in the hierarchy
			ArrayList<Term> children = ((Hierarchy) arg0).getFirstLevelNodes(hideDeprecated, hideNotUse);

			elem.addAll(children);
		}

		else if (arg0 instanceof AvailableHierarchiesTerm) {

			// show only the hierarchies in which the term is not present
			Term term = ((AvailableHierarchiesTerm) arg0).getTerm();

			ArrayList<Hierarchy> hiers = term.getNewHierarchies();

			// sort by order
			Collections.sort(hiers);

			elem.addAll(hiers);
		}

		return elem.toArray();
	}

	/**
	 * This method is used to obtain the root elements for the tree viewer. This
	 * method is invoked by calling the setInput method on tree viewer, should
	 * answer with the appropriate domain objects of the inputElement. In our case
	 * we retrieving first nodes domain object with foodexDAO.getFirstLevelNodes()
	 * method.
	 */
	public Object[] getElements(Object arg0) {

		this.root = arg0;

		return getChildren(arg0);
	}

	/**
	 * The getParent method is used to obtain the parent of the given element(arg0).
	 * The tree viewer calls its content provider's getParent method when it needs
	 * to reveal collapsed domain objects programmatically and to set the expanded
	 * state of domain objects. This method should answer the parent of the domain
	 * object element.
	 */
	public Object getParent(Object arg0) {

		Object parent = null;

		if (arg0 instanceof Term) {
			parent = ((Term) arg0).getParent(hierarchy);
		}

		return parent;
	}

	/**
	 * The hasChildren method is invoked by the tree viewer when it needs to know
	 * whether a given domain object has children. The tree viewer asks its content
	 * provider if the domain object represented by element has any children. This
	 * method is used by the tree viewer to determine whether or not a plus or minus
	 * should appear on the tree widget.
	 */
	public boolean hasChildren(Object arg0) {

		boolean hasChildren = false;

		if (arg0 instanceof Hierarchy && !(root instanceof AvailableHierarchiesTerm)) {
			hasChildren = !((Hierarchy) arg0).getFirstLevelNodes(hasChildren, hideNotUse).isEmpty();
		} else if (arg0 instanceof Term) {
			hasChildren = ((Term) arg0).hasChildren(hierarchy, hideDeprecated, hideNotUse);
		}

		return hasChildren;
	}

	/**
	 * Set the current hierarchy for the tree viewer
	 * 
	 * @param hierarchy
	 */
	public void setCurrentHierarchy(Hierarchy hierarchy) {
		this.hierarchy = hierarchy;
	}

	/**
	 * Hide deprecated terms from the visualization
	 * 
	 * @param hideDeprecated
	 */
	public void setHideDeprecated(boolean hideDeprecated) {
		this.hideDeprecated = hideDeprecated;
	}

	/**
	 * Hide not reportable terms from the visualisation
	 * 
	 * @param hideNotUse
	 */
	public void setHideNotUse(boolean hideNotUse) {
		this.hideNotUse = hideNotUse;
	}
}
