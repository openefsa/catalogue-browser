package ui_implicit_facet;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

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
	// tree which contains the implicit facets
	TreeViewer tree; 

	private LabelProviderImplicitFacets labelProvider;
	private ContentProviderImplicitFacets contentProvider;

	private Listener addDoubleClickListener;

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
		labelProvider = new LabelProviderImplicitFacets();

		// set the label provider
		tree.setLabelProvider(labelProvider);

		contentProvider = new ContentProviderImplicitFacets();

		// set the content provider
		tree.setContentProvider(contentProvider);

		// set how the terms are sorted
		tree.setSorter(new SorterImplicitFacets());

		// set the initial input
		tree.setInput(null);

		// add double click listener to the implicit facets tree
		tree.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent arg0) {
				// call the add listener if it was set
				if (addDoubleClickListener != null) {
					addDoubleClickListener.handleEvent(new Event());
				}
				
			}
		});
	}

	/**
	 * add double click listener
	 * 
	 * @param listener
	 */
	public void addDoubleClickListener(Listener listener) {
		this.addDoubleClickListener = listener;
	}

	@Override
	public void update(Observable arg0, Object arg1) {

		if (arg0 instanceof HierarchySelector) {
			Hierarchy selectedHierarchy = ((HierarchySelector) arg0).getSelectedHierarchy();
			labelProvider.setCurrentHierarchy(selectedHierarchy);
		}
	}
}
