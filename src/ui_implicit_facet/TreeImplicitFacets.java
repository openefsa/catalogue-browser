package ui_implicit_facet;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import catalogue.Catalogue;
import catalogue_object.Hierarchy;
import ui_main_panel.HierarchySelector;

/**
 * Create a tree with the implicit facets of the selected term in it
 * 
 * @author avonva
 * @author shahaal
 * 
 */
public class TreeImplicitFacets implements Observer {

	TreeViewer tree; // tree which contains the implicit facets

	private LabelProviderImplicitFacets labelProvider;
	private ContentProviderImplicitFacets contentProvider;

	/**
	 * Get the implicit facets tree viewer
	 * 
	 * @return
	 */
	public TreeViewer getTreeViewer() {
		return tree;
	}

	/**
	 * Set the current hierarchy
	 * 
	 * @param hierarchy
	 */
	public void setHierarchy(Hierarchy hierarchy) {
		labelProvider.setCurrentHierarchy(hierarchy);
	}

	/**
	 * Constructor, it creates the implicit facet tree viewer in the parent
	 * composite
	 * 
	 * @param parent
	 */
	public TreeImplicitFacets(Composite parent, Catalogue catalogue) {

		// instantiate the tree
		tree = new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.NONE);

		// set the layout of the tree
		tree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// set the label provider for the facets groups
		labelProvider = new LabelProviderImplicitFacets(catalogue);

		// set the label provider
		tree.setLabelProvider(labelProvider);

		contentProvider = new ContentProviderImplicitFacets();

		// set the content provider
		tree.setContentProvider(contentProvider);

		// set how the terms are sorted
		tree.setSorter(new SorterImplicitFacets());

		// set the initial input
		tree.setInput(null);
	}

	@Override
	public void update(Observable arg0, Object arg1) {

		if (arg0 instanceof HierarchySelector) {

			Hierarchy selectedHierarchy = ((HierarchySelector) arg0).getSelectedHierarchy();
			labelProvider.setCurrentHierarchy(selectedHierarchy);
		}
	}

	/**
	 * Add double click listener
	 * 
	 * @param listener
	 */
	public void addDoubleClickListener(IDoubleClickListener listener) {
		tree.addDoubleClickListener(listener);
	}
}
