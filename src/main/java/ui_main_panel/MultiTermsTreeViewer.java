package ui_main_panel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;
import catalogue_object.Term.Position;
import global_manager.GlobalManager;
import term.ContentProviderTerm;
import term.LabelProviderTerm;
import term_clipboard.TermClipboard;

/**
 * class that implement the tree viewer
 * 
 * @author shahaal
 *
 */
public class MultiTermsTreeViewer extends Observable implements Observer {

	private static final Logger LOGGER = LogManager.getLogger(MultiTermsTreeViewer.class);

	// providers of the tree
	private ContentProviderTerm contentProvider;
	private LabelProviderTerm labelProvider;
	private SorterTermViewer sorter;
	private TreeViewer tree;
	private TermTreeDragSourceListener drag;
	private TermTreeDropTargetListener drop;

	/**
	 * Create the tree viewer
	 * 
	 * @param parent
	 * @param multi     true to use multiple selection
	 * @param catalogue the catalogue we are working with
	 */
	public MultiTermsTreeViewer(Composite parent, boolean multi, int style, Catalogue catalogue) {

		if (multi)
			// Multiple selection with checkboxes
			tree = new CheckboxTreeViewer(parent, SWT.CHECK | SWT.BORDER);
		else
			// single selection
			tree = new TreeViewer(parent, style);

		tree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		contentProvider = new ContentProviderTerm();

		labelProvider = new LabelProviderTerm();

		sorter = new SorterTermViewer();

		// initialise drag n drop listeners
		drag = new TermTreeDragSourceListener();
		drop = new TermTreeDropTargetListener(tree);

		// set providers and sorter
		tree.setContentProvider(contentProvider);
		tree.setLabelProvider(labelProvider);
		tree.setSorter(sorter);

		// internal hash table to speeds up the mapping between elements and SWT items
		tree.setUseHashlookup(true);

		// default hierarchy is the master
		// if catalogue is opened
		if (catalogue != null) {
			setHierarchy(catalogue.getDefaultHierarchy());
		}

	}

	/**
	 * Add the drag n drop support to the tree
	 */
	public void addDragAndDrop() {

		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] { TextTransfer.getInstance() };

		// add the possibility to drag terms to copy their codes
		tree.addDragSupport(ops, transfers, drag);
		tree.addDropSupport(ops, transfers, drop);
	}

	/**
	 * Get the selected nameable from the tree viewer
	 * 
	 * @return
	 */
	public ArrayList<Nameable> getSelectedObjs() {

		ArrayList<Nameable> terms = new ArrayList<>();

		// get the tree selection
		IStructuredSelection selection = (IStructuredSelection) tree.getSelection();

		// return if empty selection
		if (selection.isEmpty())
			return terms;

		// for each of the selected terms
		Iterator<?> iter = selection.iterator();

		// get all the selected terms
		while (iter.hasNext())
			terms.add((Nameable) iter.next());

		return terms;
	}

	/**
	 * Get the selected terms from the tree viewer
	 * 
	 * @return
	 */
	public ArrayList<Term> getSelectedTerms() {

		ArrayList<Term> terms = new ArrayList<>();

		for (Nameable n : getSelectedObjs()) {
			if (n instanceof Term)
				terms.add((Term) n);
		}

		return terms;
	}

	/**
	 * Get the checked nameable of the check tree viewer
	 * 
	 * @return
	 */
	public ArrayList<Nameable> getCheckedObjs() {

		ArrayList<Nameable> terms = new ArrayList<>();

		// go on only if we have indeed a checkbox tree viewer
		if (!(tree instanceof CheckboxTreeViewer))
			return terms;

		CheckboxTreeViewer checkTree = (CheckboxTreeViewer) tree;

		// get the checked elements and add them to the output list
		Object[] checked = checkTree.getCheckedElements();

		for (int i = 0; i < checked.length; i++) {
			terms.add((Nameable) checked[i]);
		}

		return terms;
	}

	/**
	 * Get the checked terms of the check tree viewer
	 * 
	 * @return
	 */
	public ArrayList<Term> getCheckedTerms() {

		ArrayList<Term> terms = new ArrayList<>();

		for (Nameable n : getCheckedObjs()) {
			if (n instanceof Term)
				terms.add((Term) n);
		}

		return terms;
	}

	/**
	 * Get the first selected term of the tree if it is present
	 * 
	 * @return
	 */
	public Term getFirstSelectedTerm() {

		ArrayList<Term> terms = getSelectedTerms();

		if (terms.isEmpty())
			return null;

		return terms.get(0);
	}

	/**
	 * Get the first selected term of the tree if it is present
	 * 
	 * @return
	 */
	public Nameable getFirstSelectedObj() {

		ArrayList<Nameable> objs = getSelectedObjs();

		if (objs.isEmpty())
			return null;

		return objs.get(0);
	}

	/**
	 * Get the inner tree viewer
	 * 
	 * @return
	 */
	public TreeViewer getTreeViewer() {
		return tree;
	}

	/**
	 * Check if the tree has an empty selection or not
	 * 
	 * @return
	 */
	public boolean isSelectionEmpty() {
		return tree.getSelection().isEmpty();
	}

	/**
	 * Add a selection listener to the tree
	 * 
	 * @param listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		tree.addSelectionChangedListener(listener);
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		tree.removeSelectionChangedListener(listener);
	}

	/**
	 * Add a check listener to the tree (if multiple selection enabled)
	 * 
	 * @param listener
	 */
	public void addCheckStateListener(ICheckStateListener listener) {

		if (!(tree instanceof CheckboxTreeViewer))
			return;

		((CheckboxTreeViewer) tree).addCheckStateListener(listener);
	}

	/**
	 * Add double click listener
	 * 
	 * @param listener
	 */
	public void addDoubleClickListener(IDoubleClickListener listener) {
		tree.addDoubleClickListener(listener);
	}

	/**
	 * Add a listener called when a drop action finished
	 * 
	 * @param listener
	 */
	public void addDropFinishedListener(Listener listener) {
		drop.addDropFinishedListener(listener);
	}

	/**
	 * Refresh the tree viewer
	 */
	public void refresh(boolean label) {
		tree.refresh(label);
	}

	/**
	 * Refresh the tree viewer
	 */
	public void refresh() {
		tree.refresh();
	}

	/**
	 * Refresh a specific object of the tree
	 * 
	 * @param term
	 */
	public void refresh(Nameable term) {
		tree.refresh(term);
	}

	/**
	 * Select a term of the tree viewer
	 * 
	 * @param term
	 */
	public void selectTerm(Nameable term) {
		selectTerm(term, 0);
	}

	/**
	 * Select a term of the tree viewer, expand tree to the selected level
	 * 
	 * @author shahaal
	 * @param term
	 */
	public void selectTerm(Nameable term, int level) {

		if (term == null)
			return;

		// get the focus
		// if(flag)
		// tree.getControl().setFocus();

		try {
			// don't refresh while expanding (better performance)
			tree.getControl().setRedraw(false);

			// expand the tree until the term level and load ancestors
			tree.expandToLevel(term, level);
			
			// select the term
			tree.setSelection(new StructuredSelection(term));

		} finally {
			// refresh after expanding
			tree.getControl().setRedraw(true);
		}

	}

	/**
	 * Expand all the selected terms
	 * 
	 * @param level
	 */
	public void expandSelectedTerms(int level) {
		for (Nameable term : getSelectedTerms())
			selectTerm(term, level);
	}

	/**
	 * Collapse all the selected terms
	 * 
	 * @param level
	 */
	public void collapseSelectedTerms(int level) {

		for (Nameable term : getSelectedTerms())
			collapseToLevel(term, level);
	}

	/**
	 * Collapse the tree to the selected object
	 * 
	 * @param term
	 * @param level
	 */
	public void collapseToLevel(Nameable term, int level) {

		try {
			// dont refresh while expanding for better memory performance
			tree.getControl().setRedraw(false);

			// collapse the tree until the term level
			tree.collapseToLevel(term, level);

		} finally {// Flush all changes at once
			tree.getControl().setRedraw(true);
		}

	}

	/**
	 * Collapse the entire tree
	 */
	public void collapseAll() {
		tree.collapseAll();
	}

	/**
	 * Set the tree input
	 * 
	 * @param input
	 */
	public void setInput(Object input) {
		tree.setInput(input);
	}

	/**
	 * Check/uncheck a term of the tree
	 * 
	 * @param term
	 * @param state
	 */
	public boolean checkTerm(Nameable term, boolean state) {

		// go on only if we have indeed a checkbox tree viewer
		if (!(tree instanceof CheckboxTreeViewer))
			return false;

		CheckboxTreeViewer checkTree = (CheckboxTreeViewer) tree;

		return checkTree.setChecked(term, state);
	}

	/**
	 * Invert the check state of an object in the tree
	 * 
	 * @param term
	 * @param state
	 */
	public boolean invertTermCheck(Nameable term) {

		// go on only if we have indeed a checkbox tree viewer
		if (!(tree instanceof CheckboxTreeViewer))
			return false;

		CheckboxTreeViewer checkTree = (CheckboxTreeViewer) tree;

		boolean checked = checkTree.getChecked(term);

		return checkTerm(term, !checked);
	}

	/**
	 * Set the tree menu
	 * 
	 * @param menu
	 */
	public void setMenu(Menu menu) {
		tree.getTree().setMenu(menu);
	}

	/**
	 * Check if a menu was already set
	 * 
	 * @return
	 */
	public boolean hasMenu() {
		return tree.getTree().getMenu() != null;
	}

	/**
	 * Remove the tree menu
	 */
	public void removeMenu() {

		if (tree.getTree().getMenu() != null)
			tree.getTree().getMenu().dispose();

		tree.getTree().setMenu(null);
	}

	/**
	 * Add a filter to the tree viewer
	 * 
	 * @param filter
	 */
	public void addFilter(ViewerFilter filter) {
		tree.addFilter(filter);
	}

	/**
	 * Set the layout data for the tree
	 * 
	 * @param data
	 */
	public void setLayoutData(Object data) {
		tree.getTree().setLayoutData(data);
	}

	/**
	 * Set the hierarchy on which all the assumptions are taken
	 * 
	 * @param hierarchy
	 */
	public void setHierarchy(Hierarchy hierarchy) {

		// update the hierarchy for the label provider
		labelProvider.setCurrentHierarchy(hierarchy);

		// update the hierarchy for the content provider
		contentProvider.setCurrentHierarchy(hierarchy);

		// update the hierarchy for the sorter
		sorter.setCurrentHierarchy(hierarchy);

		// update the hierarchy in the drop listener
		drop.setHierarchy(hierarchy);

		// update the input of the tree
		tree.setInput(hierarchy);
	}

	@Override
	public void update(Observable arg0, Object arg1) {

		// if the check boxes for visualizing terms are changed
		if (arg0 instanceof TermFilter) {

			boolean hideDeprecated = ((TermFilter) arg0).isHidingDeprecated();
			boolean hideNotInUse = ((TermFilter) arg0).isHidingNotReportable();
			boolean hideTermCode = ((TermFilter) arg0).isHidingTermCode();

			// update content provider settings
			contentProvider.setHideDeprecated(hideDeprecated);
			contentProvider.setHideNotUse(hideNotInUse);
			
			// update label provider settings
			labelProvider.setHideCode(hideTermCode);

			// refresh contents
			tree.refresh();

		}
	}

	/**
	 * Drag listener to copy the terms codes and names using the drag and drop
	 * functionality
	 * 
	 * @author avonva
	 *
	 */
	private class TermTreeDragSourceListener implements DragSourceListener {

		/**
		 * Start the drag operation
		 */
		public void dragStart(DragSourceEvent event) {
		}

		public void dragSetData(DragSourceEvent event) {

			if (isSelectionEmpty())
				return;

			// if we are copying text dragging terms we set the data as their code and name
			// tab separated
			if (TextTransfer.getInstance().isSupportedType(event.dataType)) {

				// get the terms code and names using the term clipboard
				TermClipboard clip = new TermClipboard();
				event.data = clip.copyCodeName(getSelectedTerms());
			}
		}

		public void dragFinished(DragSourceEvent event) {
		}
	}

	/**
	 * Class to manage the DROP action of a drag and drop operation
	 * 
	 * @author avonva
	 *
	 */
	public class TermTreeDropTargetListener extends ViewerDropAdapter {

		// viewer from which we get the terms
		private TreeViewer viewer;
		private int location;
		private Term target;
		private Hierarchy hierarchy;

		private Listener dropFinishedListener;

		/**
		 * Initialize the listener
		 * 
		 * @param viewer
		 */
		public TermTreeDropTargetListener(Viewer viewer) {
			super(viewer);
			this.viewer = (TreeViewer) viewer;
		}

		/**
		 * Set the hierarchy to consider in the drag n drop actions
		 * 
		 * @param hierarchy
		 */
		public void setHierarchy(Hierarchy hierarchy) {
			this.hierarchy = hierarchy;
		}

		/**
		 * When a drop action is finished
		 * 
		 * @param dropFinishedListener
		 */
		public void addDropFinishedListener(Listener dropFinishedListener) {
			this.dropFinishedListener = dropFinishedListener;
		}

		/**
		 * Get the selected term of the tree viewer
		 * 
		 * @return
		 */
		public ArrayList<Term> getSelectedTerms() {

			ArrayList<Term> selectedTerms = new ArrayList<>();
			Iterator<?> termIter = ((IStructuredSelection) viewer.getSelection()).iterator();
			while (termIter.hasNext()) {
				Nameable t = (Nameable) termIter.next();
				if (t instanceof Term)
					selectedTerms.add((Term) t);
			}

			return selectedTerms;
		}

		@Override
		public void drop(DropTargetEvent event) {

			if (viewer.getSelection().isEmpty())
				return;

			location = this.determineLocation(event);
			target = (Term) determineTarget(event);

			switch (location) {

			// dropped before the target
			case ViewerDropAdapter.LOCATION_BEFORE:

				LOGGER.info("Drop before " + target);

				for (Term source : getSelectedTerms()) {
					source.moveAsSibling(target, hierarchy, Position.BEFORE);
				}

				break;

			// dropped after the target
			case ViewerDropAdapter.LOCATION_AFTER:

				LOGGER.info("Drop after " + target);

				ArrayList<Term> selectedTerms = getSelectedTerms();

				// move the terms after the target (invert selection to preserve
				// the order of terms)
				for (int i = selectedTerms.size() - 1; i >= 0; i--) {

					Term source = selectedTerms.get(i);
					source.moveAsSibling(target, hierarchy, Position.AFTER);
				}

				break;

			// dropped on the target
			case ViewerDropAdapter.LOCATION_ON:
				// we use the term clipboard since this action is the same as
				// a cut paste branch action

				// create an instance of the term clipboard
				TermClipboard termClip = new TermClipboard();

				// cut and paste the selected terms as children of the target term
				termClip.cutBranch(getSelectedTerms(), hierarchy);
				termClip.paste(target, hierarchy);

				break;

			// dropped into nothing
			case ViewerDropAdapter.LOCATION_NONE:
				break;
			}

			// refresh applicability table from the main ui thread
			if (dropFinishedListener != null)
				dropFinishedListener.handleEvent(new Event());

			super.drop(event);
		}

		@Override
		public boolean performDrop(Object arg0) {

			Term target = (Term) getCurrentTarget();

			if (target == null || viewer.getSelection().isEmpty())
				return false;

			viewer.refresh();
			viewer.refresh(target);

			return true;
		}

		@Override
		public boolean validateDrop(Object arg0, int operation, TransferData transferType) {

			// get an instance of the global manager
			GlobalManager manager = GlobalManager.getInstance();

			// do not allow in read only mode
			if (manager.isReadOnly())
				return false;

			Term target = (Term) getCurrentTarget();

			if (target == null)
				return false;

			// cannot drop on terms which are dragged (i.e. selected)
			// cannot move parent as child of the parent children (otherwise stackoverflow)
			for (Term source : getSelectedTerms()) {
				if (source.equals(target) || target.hasAncestor(source, hierarchy))
					return false;
			}

			return true;
		}
	}

	/**
	 * Sorter to sort the tree terms
	 * 
	 * @author avonva
	 *
	 */
	private class SorterTermViewer extends ViewerSorter {

		private Hierarchy hierarchy;

		/**
		 * Set the current hierarchy
		 * 
		 * @param hierarchy
		 */
		public void setCurrentHierarchy(Hierarchy hierarchy) {
			this.hierarchy = hierarchy;
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {

			if (!(e1 instanceof Term) || !(e2 instanceof Term))
				return 0;

			Term t1 = (Term) e1;
			Term t2 = (Term) e2;
			int i1 = 0;
			String s1 = "";
			try {
				i1 = t1.getOrder(hierarchy);
			} catch (Exception e) {
				try {
					s1 = t1.getName();
				} finally {
					i1 = 0;
				}
			}
			int i2 = 0;
			String s2 = "";
			try {
				i2 = t2.getOrder(hierarchy);
			} catch (Exception e) {
				try {
					s2 = t2.getName();
				} finally {
					s2 = "0";
				}
			}
			int cmpi;
			int cmps;
			try {
				cmpi = (i1 < i2) ? -1 : (i1 > i2) ? 1 : 0;
				return cmpi;
			} catch (NullPointerException e) {
				LOGGER.error("Null pointer", e);
				e.printStackTrace();
			
				cmps = s1.compareTo(s2);
				cmpi = 0;
				cmps = 0;
			}
			return cmps;
		}
	}
}
