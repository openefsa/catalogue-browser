package ui_implicit_facet;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import catalogue.Catalogue;
import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import global_manager.GlobalManager;
import i18n_messages.CBMessages;
import ui_describe.FormSelectTerm;
import utilities.GlobalUtil;

/**
 * The second tab of the main page of the foodex browser. It includes the
 * implicit facets tree. This tab is also used into the describe window to add
 * new facets
 * 
 * @author avonva
 * @author shahaal
 */
public class FrameTermImplicitFacets implements Observer {

	private Composite parent;
	private Term term;
	private TreeImplicitFacets implicitFacets;
	private boolean visible;

	// the type of facets (implicit/explicit) which are added with an Add operation,
	// we use this to create explicit facets into the describe and implicit facets
	// in the editing mode instead
	private FacetType newFacetType;
	// called each time a facet is added (if multiple, it is called multiple times)
	private Listener addDescriptorListener;
	// called each time that a facet is removed
	private Listener removeDescriptorListener;
	// called at the end of an adding operation, useful if we have to do something
	private Listener updateListener;

	private Attribute facetCategory;

	public void addAddDescriptorListener(Listener listener) {
		this.addDescriptorListener = listener;
	}

	public void addRemoveDescriptorListener(Listener listener) {
		this.removeDescriptorListener = listener;
	}

	/**
	 * called at the end of an adding operation, useful if we have to do something
	 * only once in multiple selection cases
	 * 
	 * @param listener
	 */
	public void addUpdateListener(Listener listener) {
		this.updateListener = listener;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
	 * Set the current term for the tree
	 * 
	 * @param term
	 */
	public void setTerm(Term term) {
		this.term = term;
		getTreeViewer().setInput(term);
		refresh();
	}

	/**
	 * Set the hierarchy for the tree (used in label provider)
	 * 
	 * @param hierarchy
	 */
	public void setHierarchy(Hierarchy hierarchy) {
		implicitFacets.setHierarchy(hierarchy);
	}

	/**
	 * Add the contextual menu to the tree
	 */
	public void addMenu() {
		addFacetCategoryMenu(parent);
	}

	/**
	 * Remove the contextual menu from the tree viewer
	 */
	public void removeMenu() {
		// get the tree
		Tree tree = getTree();
		// if menu is not null than dispose
		if (tree.getMenu() != null)
			tree.getMenu().dispose();
		// reset menu
		tree.setMenu(null);
	}

	/**
	 * Refresh the tree
	 * 
	 * @author shahaal
	 */
	public void refresh() {
		// get the tree
		TreeViewer tree = getTreeViewer();
		// refresh tree
		tree.refresh();
		// expand all nodes
		tree.expandAll();
	}

	/**
	 * Get the tree viewer
	 * 
	 * @return
	 */
	public TreeViewer getTreeViewer() {
		return implicitFacets.getTreeViewer();
	}

	/**
	 * Get the tree
	 * 
	 * @return
	 */
	public Tree getTree() {
		return getTreeViewer().getTree();
	}

	/**
	 * Remove (into the facet category) the ancestor of the new facet added; note
	 * that the method is applied to all the facets (implicit and explicit)
	 * 
	 * @author shahaal
	 * @return
	 */
	private void checkAncestors(Term descriptor) {

		for (FacetDescriptor fd : term.getDescriptorsByCategory(facetCategory, true)) {
			if (descriptor.hasAncestor(fd.getDescriptor(), facetCategory.getHierarchy()))
				term.removeImplicitFacet(fd);
		}

	}

	/**
	 * Create the implicit facet tree. newFacetType is used to define the type of
	 * the facets descriptors which are added to the already present facets
	 * descriptors. Are they implicit facets (editing mode, we modify the implicit
	 * facets of a term) or explicit facets (describe, we are adding facet to make a
	 * code)
	 * 
	 * @param parent
	 * @param newFacetType
	 */
	public FrameTermImplicitFacets(Composite parent, FacetType newFacetType, Catalogue catalogue) {

		this.parent = parent;
		this.newFacetType = newFacetType;
		// add the implicit facets list in the tab
		implicitFacets = new TreeImplicitFacets(parent, catalogue);

	}

	/**
	 * Add the contextual menu to the implicit facets
	 * 
	 * @author shahaal
	 * @param parent
	 * @param implicitFacets
	 * @return
	 */
	private Menu addFacetCategoryMenu(final Composite parent) {

		// create the category menu
		Menu categoryMenu = new Menu(parent.getShell(), SWT.POP_UP);
		// initialize the add inner item
		final MenuItem addItem = new MenuItem(categoryMenu, SWT.PUSH);
		addItem.setImage(new Image(parent.getDisplay(), ClassLoader.getSystemResourceAsStream("add-icon.png")));
		addItem.setText(CBMessages.getString("TreeImplicitFacets.AddCommand"));
		// initialize the remove inner item
		final MenuItem removeItem = new MenuItem(categoryMenu, SWT.PUSH);
		removeItem.setImage(new Image(parent.getDisplay(), ClassLoader.getSystemResourceAsStream("remove-icon.png")));
		removeItem.setText(CBMessages.getString("TreeImplicitFacets.RemoveCommand"));

		// get the tree viewer
		TreeViewer tree = getTreeViewer();
		// set the menu for the tree
		tree.getTree().setMenu(categoryMenu);

		// set the listener
		categoryMenu.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				// get the flags for the selected term
				boolean[] flags = checkSelection(tree.getSelection());
				// set the menu items based on flags
				addItem.setEnabled(flags[0]);
				removeItem.setEnabled(flags[1]);
			}
		});

		// listener called when click on add menu item
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// set selected term to the new window
				addToSelectedCategory(parent.getShell(), tree.getSelection());
			}
		});

		// add the double click listener on the tree treated as add button
		implicitFacets.addDoubleClickListener(new Listener() {
			@Override
			public void handleEvent(Event e) {
				// if in edit mode enable double click
				if (categoryMenu != null)
					addToSelectedCategory(parent.getShell(), tree.getSelection());
			}
		});

		// add the remove option
		removeItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeFromSelectedCategory(parent.getShell(), tree.getSelection());
			}
		});

		return categoryMenu;
	}

	/**
	 * method used for checking if it is possible to add or remove term to the
	 * selection
	 * 
	 * @param sel
	 * @return
	 */
	protected boolean[] checkSelection(ISelection sel) {
		// return if no term selected or empty selection
		if (term == null || sel.isEmpty())
			return null;
		// get the array with flags (add, remove, multiple)
		return isAddableRemovable(term, ((IStructuredSelection) sel).getFirstElement());
	}

	/**
	 * Check if we can add or remove a term from the describe list
	 * 
	 * @param baseTerm
	 * @param facetCategory
	 * @return
	 */
	public boolean[] isAddableRemovable(Term baseTerm, Object facetCategory) {

		// define the flag variables
		boolean isAddable, isMultipleAddable, isRemovable = false;

		// facet category cardinality
		String cardinality = null;

		// number of facets under the facet category
		int descriptorsCount = -1;

		/**
		 * Count the number of descriptors related to the category. If facet category is
		 * selected count the real number of facets. If facet selected than there is at
		 * least one item in facet category (sufficient for add/remove check).
		 */

		// if facet category is selected
		if (facetCategory instanceof Attribute) {
			// cast the facet category
			Attribute fc = (Attribute) facetCategory;
			// get the cardinality
			cardinality = fc.getSingleOrRepeatable();
			// count the descriptors
			descriptorsCount = baseTerm.getDescriptorsByCategory(fc, true).size();
		} else {
			// if facet selected get the cardinality from its facet category
			DescriptorTreeItem fc = (DescriptorTreeItem) facetCategory;
			// get the cardinality
			cardinality = fc.getDescriptor().getFacetCategory().getSingleOrRepeatable();
			// we have at least one facet (since facet has been selected)
			descriptorsCount = 1;

			/**
			 * Allow removing only if a facet is selected (not facet category). In addition
			 * only non inherited facets can be removed (to remove an inherited facet we
			 * have to go back to the original parent which owns that facet and remove it).
			 */
			isRemovable = !fc.isInherited();
		}

		// if single cardinality (0 or 1 descriptor allowed) or repeatable
		isAddable = ((cardinality.equals(Attribute.cardinalitySingle) && descriptorsCount == 0)
				|| cardinality.equals(Attribute.cardinalityRepeatable));
		// allow adding descriptors without limitations
		isMultipleAddable = cardinality.equals(Attribute.cardinalityRepeatable);

		// Return embedded results
		return new boolean[] { isAddable, isRemovable, isMultipleAddable };
	}

	/**
	 * allow user to add facets to the selected category
	 * 
	 * @param parent
	 * @param sel
	 */
	public void addToSelectedCategory(Shell parent, final ISelection sel) {

		// return if empty selection
		if (sel.isEmpty())
			return;

		// get the selected element ( facet category or descriptor )
		Object selectedElem = ((IStructuredSelection) (sel)).getFirstElement();

		if (selectedElem instanceof Attribute) 
			// if we have selected the facet category simply cast it
			facetCategory = (Attribute) selectedElem;
		else // if we have selected a descriptor get the category from it
			facetCategory = ((DescriptorTreeItem) selectedElem).getDescriptor().getFacetCategory();
		
		// return if non MTX catalogue or non editable cat
		Catalogue cat = facetCategory.getCatalogue();
		if (!cat.isMTXCatalogue()) {
			GlobalUtil.showDialog(parent, CBMessages.getString("TableFacetApplicability.AddFacetWarningTitle"),
					CBMessages.getString("TableFacetApplicability.AddFacetWarningMessage"), SWT.ICON_WARNING);
			return;
		}
		
		/**
		 * open a form to select descriptors. In particular we enable the multiple
		 * selection only if the facet category is repeatable (cardinality 0 or + )
		 */
		FormSelectTerm sf = new FormSelectTerm(parent, CBMessages.getString("Browser.SelectTermWindowTitle"),
				term.getCatalogue(), facetCategory.isRepeatable());

		// set the root term for the form in order to show only
		// the facet related to the facet category
		sf.setRootTerm(facetCategory);

		// if cardinality is single
		if (!facetCategory.isRepeatable()) {

			// if we have the inherited implicit facet
			// (it can be only one, cardinality is single)
			ArrayList<DescriptorTreeItem> inh = term.getInheritedImplicitFacets(facetCategory);
			
			if (!inh.isEmpty()) {

				// then we can only specify better the inherited
				// facet, we set as root term the inherited descriptor!
				sf.setRootTerm(inh.get(0).getTerm(), facetCategory.getHierarchy());
			}
		}

		// display the form
		sf.display();

		// here we have closed the form and the descriptors have been selected

		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();

		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();

		// for each selected descriptor we add it
		for (Term descriptor : sf.getSelectedTerms()) {

			Attribute facetAttr = currentCat.findImplicitFacetsAttribute();

			// check if the selected term has ancestors into the facets group folder
			checkAncestors(descriptor);

			// create the term attribute for the facet descriptor
			TermAttribute ta = new TermAttribute(term, facetAttr,
					FacetDescriptor.getFullFacetCode(descriptor, facetCategory));

			// create the new facet descriptor using the facet category and the term full
			// code we set the facet type with the newFacetType
			FacetDescriptor attr = new FacetDescriptor(descriptor, ta, newFacetType);

			// add the descriptor to the implicit facets
			term.addImplicitFacet(attr);

			// update the table input
			setTerm(term);

			// call the add listener if it was set
			if (addDescriptorListener != null) {
				// set as event data the new attribute
				Event event = new Event();
				event.data = attr;
				addDescriptorListener.handleEvent(event);
			}
		}

		// call the update listener if add process if finished
		if (updateListener != null)
			updateListener.handleEvent(new Event());

	}

	/**
	 * allow user to remove explicit facets to the selected category
	 * 
	 * @param parent
	 * @param sel
	 */
	protected void removeFromSelectedCategory(Shell parent2, ISelection selectedElem) {

		// return if empty selection
		if (selectedElem.isEmpty())
			return;
		// get the selected tree item from the implicit facet tree
		// (we can have only remove a descriptor tree item not a facet categories )
		DescriptorTreeItem treeItem = (DescriptorTreeItem) ((IStructuredSelection) selectedElem).getFirstElement();
		// remove the descriptor in the selected tree node
		term.removeImplicitFacet(treeItem.getDescriptor());
		// update table input
		setTerm(term);

		// call the remove listener if it was set
		if (removeDescriptorListener != null) {
			// set as event data the removed attribute
			Event event = new Event();
			event.data = treeItem.getDescriptor();
			removeDescriptorListener.handleEvent(event);
		}

		// call the update listener if remove process if finished
		if (updateListener != null) {
			updateListener.handleEvent(new Event());
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		// forward update to the table
		implicitFacets.update(o, arg);
	}
}
