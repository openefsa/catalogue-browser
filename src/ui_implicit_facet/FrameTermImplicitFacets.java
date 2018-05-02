package ui_implicit_facet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

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
 *
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
	
	private Object selElement; //get the selected facet folder
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
	 */
	public void refresh() {
		implicitFacets.getTreeViewer().refresh();
		implicitFacets.getTreeViewer().expandAll();
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
	 * Remove the ancestor (if exists in the selected folder)
	 * when a child term of it is added
	 * 
	 * @return
	 */
	public void checkAncestor(TreeViewer implicitFacets ) {
		
		/*
		 * Author: AlbyDev
		 * Date: 30/04/2018
		*/
		
		//get the content
		ContentProviderImplicitFacets provider = (ContentProviderImplicitFacets) implicitFacets.getContentProvider();
		//get the child of the content
		DescriptorTreeItem[] children =Arrays.stream(provider.getChildren(facetCategory)).toArray(DescriptorTreeItem[]::new);
		//list of facet parent group
		ArrayList<DescriptorTreeItem> parents = new ArrayList<>();
		
		for (DescriptorTreeItem o : children) {
			// find the parent
			if ((o.getTerm().getParent(facetCategory.getHierarchy()) == null)) {
				//System.out.println(o.getTerm().getName() + " is a master group;");
				parents.add(o);
			}
		}
		
		//it is supposed to have more children then parents so we loop through the children removing the parents
		for (DescriptorTreeItem c : children) {
			if(parents.isEmpty())
				break;
			
			for (DescriptorTreeItem p : parents)
				//remove the parent if it includes the children and if it is the parent of the children
				if (!parents.contains(c) & c.getTerm().hasAncestor(p.getTerm(), facetCategory.getHierarchy()))
					implicitFacets.remove(p);
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
		
		
		addImplicitFacet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				// return if empty selection
				if (implicitFacets.getSelection().isEmpty())
					return;
				
				// get the selected element ( facet category or descriptor )
				Object selectedElem = ((IStructuredSelection) (implicitFacets.getSelection())).getFirstElement();
				
				//get the selected facet folder
				selElement = ((IStructuredSelection) implicitFacets.getSelection()).getFirstElement();
				
				// get the facet category from the selection
				if (selectedElem instanceof Attribute) // if we have selected the facet category simply cast it
					facetCategory = (Attribute) selectedElem;
				else // if we have selected a descriptor get the category from it
					facetCategory = ((DescriptorTreeItem) selectedElem).getDescriptor().getFacetCategory();
				
				
				// open a form to select descriptors. In particular we enable the multiple
				// selection
				// only if the facet category is repeatable, that is, with cardinality zero or
				// more
				
				//Author: AlbyDev
				parent.setEnabled(false);
				//
				FormSelectTerm sf = new FormSelectTerm(parent, Messages.getString("Browser.SelectTermWindowTitle"),
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
				for (int i = 0; i < sf.getSelectedTerms().size(); i++) {

					// get the current descriptor
					Term descriptor = (Term) sf.getSelectedTerms().get(i);
					
					// find the implicit facet attribute
					// TODO move this code under Catalogue
					Attribute facetAttr = null;
					for (Attribute attr : currentCat.getAttributes()) {
						if (attr.isImplicitFacet()) {
							facetAttr = attr;
							break;
						}
					}

					// create the term attribute for the facet descriptor
					TermAttribute ta = new TermAttribute(term, facetAttr,
							FacetDescriptor.getFullFacetCode(descriptor, facetCategory));
					
					// create the new facet descriptor using the facet category and the term full
					// code
					// we set the facet type with the newFacetType
					FacetDescriptor attr = new FacetDescriptor(descriptor, ta, newFacetType);

					// add the descriptor to the implicit facets
					term.addImplicitFacet(attr);
					
					// update the table input
					setTerm(term);
					
					//Author: AlbyDev
					checkAncestor(implicitFacets) ;
					//
					
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
				//Author: AlbyDev
				if (!parent.isDisposed())
					parent.setEnabled(true);
				//
			}

		});

		// remove menu item
		final MenuItem removeImplicitFacet = new MenuItem(implicitFacetOperationMenu, SWT.PUSH);

		removeImplicitFacet
				.setImage(new Image(Display.getCurrent(), ClassLoader.getSystemResourceAsStream("remove-icon.png")));
		removeImplicitFacet.setText(Messages.getString("TreeImplicitFacets.RemoveCommand"));

		removeImplicitFacet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				// return if empty selection
				if (implicitFacets.getSelection().isEmpty())
					return;
				
				//Author: AlbyDev
				//get the selected facet folder
				selElement = ((IStructuredSelection) implicitFacets.getSelection()).getFirstElement();
				
				 // if we have selected a descriptor get the category from it
				facetCategory = ((DescriptorTreeItem) selElement).getDescriptor().getFacetCategory();
				//
				
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

				//Author: AlbyDev
				checkAncestor(implicitFacets) ;
				//
				
				// call the remove listener if it was set
				if (removeDescriptorListener != null) {

					// set as event data the removed attribute
					Event event = new Event();
					event.data = treeItem.getDescriptor();

					removeDescriptorListener.handleEvent(event);
				}

			}
		});

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
		}else {
			//Author: AlbyDev
			// if we selected a facet descriptor we get the cardinality from the attribute
			// contained into the
			// descriptor, which is the facet category
			//cardinality = ((DescriptorTreeItem) facetCategory).getDescriptor().getFacetCategory()
			//		.getSingleOrRepeatable();
			cardinality = "single";
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
			
		//Author: AlbyDev
		default:
			isAddable = false;
			isRemovable=false;
			isMultipleAddable = false;
			break;
		//
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
}
