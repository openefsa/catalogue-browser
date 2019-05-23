package ui_implicit_facet;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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
import messages.Messages;
import ui_describe.FormSelectTerm;

/**
 * The third tab of the main page of the foodex browser. It includes the
 * implicit facets tree. This tab is also used into the describe window to add
 * new facets
 * 
 * @author avonva
 * @author shahaal
 */
public class FrameTermImplicitFacets implements Observer {

	private Composite parent;
	private Term term; // the considered term
	private TreeImplicitFacets implicitFacets; // tree which shows the facet of a term
	private boolean visible; // the tab is shown to the user?

	// the type of facets (implicit/explicit) which are added with an Add operation,
	// we use this to create explicit facets into the describe and implicit facets
	// in the editing mode instead
	private FacetType newFacetType;

	private Listener addDescriptorListener; // called each time that a facet is added (if multiple, it is called
											// multiple times)

	private Listener removeDescriptorListener; // called each time that a facet is removed
	private Listener updateListener; // called at the end of an adding operation, useful if we have to do something
										// only once in multiple selection cases

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
		implicitFacets.getTreeViewer().setInput(term);
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
		addImplicitFacetsMenu(parent.getShell(), implicitFacets.getTreeViewer());
	}

	/**
	 * Remove the contextual menu from the tree viewer
	 */
	public void removeMenu() {

		if (implicitFacets.getTreeViewer().getTree().getMenu() != null)
			implicitFacets.getTreeViewer().getTree().getMenu().dispose();

		implicitFacets.getTreeViewer().getTree().setMenu(null);
	}

	/**
	 * Refresh the tree
	 * 
	 * @author shahaal
	 */
	public void refresh() {
		
		try {
			// set redraw to false
			implicitFacets.getTreeViewer().getControl().setRedraw(false);
			// make ui changes
			implicitFacets.getTreeViewer().refresh();
			implicitFacets.getTreeViewer().expandAll();
		} finally {
			// flush changes at once
			implicitFacets.getTreeViewer().getControl().setRedraw(true);
		}
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
	private Menu addImplicitFacetsMenu(final Shell parent, final TreeViewer implicitFacets) {

		// create the menu
		Menu implicitFacetOperationMenu = new Menu(parent, SWT.POP_UP);

		final MenuItem addImplicitFacet = new MenuItem(implicitFacetOperationMenu, SWT.PUSH);

		addImplicitFacet
				.setImage(new Image(Display.getCurrent(), ClassLoader.getSystemResourceAsStream("add-icon.png")));
		addImplicitFacet.setText(Messages.getString("TreeImplicitFacets.AddCommand"));

		// remove menu item
		final MenuItem removeImplicitFacet = new MenuItem(implicitFacetOperationMenu, SWT.PUSH);

		removeImplicitFacet
				.setImage(new Image(Display.getCurrent(), ClassLoader.getSystemResourceAsStream("remove-icon.png")));
		removeImplicitFacet.setText(Messages.getString("TreeImplicitFacets.RemoveCommand"));

		// set the menu for the tree
		implicitFacets.getTree().setMenu(implicitFacetOperationMenu);

		// set the listener
		implicitFacetOperationMenu.addListener(SWT.Show, new Listener() {

			public void handleEvent(Event event) {
				// return if no term selected or empty selection
				if (term == null || implicitFacets.getSelection().isEmpty())
					return;

				Object selectedElem = ((IStructuredSelection) implicitFacets.getSelection()).getFirstElement();

				boolean[] enables = isAddableRemovable(term, selectedElem);

				addImplicitFacet.setEnabled(enables[0]);
				removeImplicitFacet.setEnabled(enables[1]);
			}
		});

		// shahaal: double click on the term for directly opening add term window
		implicitFacets.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent arg0) {

				Object selectedElem = ((IStructuredSelection) implicitFacets.getSelection()).getFirstElement();

				boolean[] enables = isAddableRemovable(term, selectedElem);

				// if it is possible to add terms
				if (enables[0])
					addTermsToSelection(parent, implicitFacets);
			}
		});

		// shahaal: listener called when click on add menu item
		addImplicitFacet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addTermsToSelection(parent, implicitFacets);
			}

		});

		// add the remove option
		removeImplicitFacet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				// return if empty selection
				if (implicitFacets.getSelection().isEmpty())
					return;

				// get the selected tree item from the implicit facet tree
				// ( we can have only selected a descriptor tree item, since remove is disabled
				// for
				// facet categories )
				DescriptorTreeItem treeItem = (DescriptorTreeItem) ((IStructuredSelection) (implicitFacets
						.getSelection())).getFirstElement();

				// remove the descriptor contained in the selected tree node from the implicit
				// facets
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
		});

		return implicitFacetOperationMenu;
	}

	/**
	 * Check if we can add or remove a term from the describe list.
	 * 
	 * @return boolean array, first => can we add one term?, second => can we remove
	 *         one term?, third => can we add more than one term?
	 */
	public boolean[] isAddableRemovable(Term baseTerm, Object facetCategory) {

		boolean isAddable = false;
		boolean isRemovable = false;
		boolean isMultipleAddable = false;

		String cardinality = null;

		// count the number of descriptors which are under the same facet category
		int descriptorsCount = -1;

		// Here we count the number of descriptors related to the chosen category.
		// If we actually have selected a facet category we can compute the real number
		// of descriptors under that folder. If instead we have selected a facet
		// descriptor we can only say that there is at least one descriptor in the
		// selected facet category! (it is sufficent for the addable/removable check)

		// if we selected a facet category ( i.e. yellow folder )
		if (facetCategory instanceof Attribute) {
			// get the cardinality
			cardinality = ((Attribute) facetCategory).getSingleOrRepeatable();

			// count the descriptors
			descriptorsCount = baseTerm.getDescriptorsByCategory((Attribute) facetCategory, true).size();

			// shahaal: add the impl facets
			descriptorsCount += baseTerm.getInheritedImplicitFacets((Attribute) facetCategory).size();

		} else {
			// Author: shahaal
			// if we selected a facet descriptor we get the cardinality from the attribute
			// contained into the
			// descriptor, which is the facet category
			cardinality = ((DescriptorTreeItem) facetCategory).getDescriptor().getFacetCategory()
					.getSingleOrRepeatable();

			// cardinality = "single";
			// we have at least one descriptor
			descriptorsCount = 1;

			// allow removing descriptors only if we selected a descriptor
			// We cannot remove a facet category!
			// Moreover we can remove a facet only if it is not inherited!
			// To remove an inherited facet we have to go back to the original parent which
			// owns that facet and remove it
			isRemovable = !((DescriptorTreeItem) facetCategory).isInherited();
			//
		}

		// get the attribute cardinality and evaluate the addability/removability
		switch (cardinality) {

		// if single cardinality ( zero or one descriptor allowed )
		case Attribute.cardinalitySingle:
			// allow adding descriptors only if others are not already present
			if (descriptorsCount == 0)
				isAddable = true;
			break;

		// if repeatable cardinality ( zero or more descriptors )
		case Attribute.cardinalityRepeatable:
			// allow adding descriptors without limitations
			isAddable = true;
			isMultipleAddable = true;
			break;

		default:
			isAddable = false;
			isRemovable = false;
			isMultipleAddable = false;
			break;
		}

		// Return embedded results
		boolean results[] = { isAddable, isRemovable, isMultipleAddable };

		return (results);
	}

	@Override
	public void update(Observable o, Object arg) {

		// forward update to the table
		implicitFacets.update(o, arg);
	}

	/**
	 * method used to create the window which show all the possible terms which
	 * could be added to the group selected
	 * 
	 * @author shahaal
	 * @param parent
	 * @param implicitFacets
	 */
	public void addTermsToSelection(final Shell parent, final TreeViewer implicitFacets) {

		// return if empty selection
		if (implicitFacets.getSelection().isEmpty())
			return;

		// get the selected element ( facet category or descriptor )
		Object selectedElem = ((IStructuredSelection) (implicitFacets.getSelection())).getFirstElement();

		if (selectedElem instanceof Attribute) // if we have selected the facet category simply cast it
			facetCategory = (Attribute) selectedElem;
		else // if we have selected a descriptor get the category from it
			facetCategory = ((DescriptorTreeItem) selectedElem).getDescriptor().getFacetCategory();

		// open a form to select descriptors. In particular we enable the multiple
		// selection only if the facet category is repeatable (cardinality 0 or + )

		// shahaal
		parent.setEnabled(false);

		FormSelectTerm sf = new FormSelectTerm(parent, Messages.getString("Browser.SelectTermWindowTitle"),
				term.getCatalogue(), facetCategory.isRepeatable(), false);

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
		if (updateListener != null) {
			updateListener.handleEvent(new Event());
		}
		// shahaal
		if (!parent.isDisposed())
			parent.setEnabled(true);

	}
}
