package ui_main_panel;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import catalogue.Catalogue;
import catalogue_browser_dao.TermAttributeDAO;
import catalogue_object.Term;
import dcf_user.User;
import global_manager.GlobalManager;
import messages.Messages;
import ui_implicit_facet.FacetDescriptor;
import ui_implicit_facet.FacetType;
import ui_implicit_facet.FrameTermImplicitFacets;
import ui_search_bar.HierarchyChangedListener;
import ui_search_bar.HierarchyEvent;
import ui_term_applicability.FrameTermApplicabilities;
import ui_term_properties.FrameTermFields;

/**
 * Panel which contains the three tabs, which shows term properties,
 * applicabilities and implicit facets. Implicit facets are permanently added
 * and removed from the db using this UI!
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class TermPropertiesPanel implements Observer {

	private Catalogue catalogue;
	private Term term;

	// tabs
	private FrameTermFields propTab;
	private FrameTermApplicabilities applTab;
	private FrameTermImplicitFacets facetTab;

	// applicability listeners
	private HierarchyChangedListener openListener;
	private Listener addListener;
	private Listener removeListener;
	private HierarchyChangedListener usageListener;

	/**
	 * Initialise the panel and display it
	 * 
	 * @param parent
	 */
	public TermPropertiesPanel(Composite parent, Catalogue catalogue) {

		this.catalogue = catalogue;

		// Create a tab folder into the form
		TabFolder folder = new TabFolder(parent, SWT.NONE);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		folder.setLayout(new GridLayout(1, false));

		// add tabs into the tab folder parent
		addTermPropertyTab(folder);
		addFacetTab(folder);
		addApplicabilityTab(folder);
	}

	/**
	 * Add the term properties tab to the parent tab folder
	 * 
	 * @param parent
	 */
	private void addTermPropertyTab(TabFolder parent) {

		TabItem tabNames = new TabItem(parent, SWT.NONE);
		tabNames.setText(Messages.getString("TermProperties.TabName"));

		Composite compNames = new Composite(parent, SWT.NONE);
		compNames.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		compNames.setLayout(new GridLayout(1, false));

		tabNames.setControl(compNames);

		// create the first tab with code, names, scopenotes, attributes...
		propTab = new FrameTermFields(compNames);

		// disable tab 1 at the beginning (no catalogue is open)
		propTab.setEnabled(false);
	}

	/**
	 * Add the applicability tab to the parent tab folder
	 * 
	 * @param parent
	 */
	private void addApplicabilityTab(TabFolder parent) {

		// create a new tab and set its name
		TabItem tabApplicability = new TabItem(parent, SWT.NONE);
		tabApplicability.setText(Messages.getString("TableApplicability.TabName"));

		Composite compApplicability = new Composite(parent, SWT.NONE);
		compApplicability.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		compApplicability.setLayout(new GridLayout(1, false));
		//compApplicability.setText(Messages.getString("TableApplicability.Title"));

		tabApplicability.setControl(compApplicability);

		// create the second tab
		applTab = new FrameTermApplicabilities(compApplicability, catalogue);

		applTab.getApplTable().addOpenListener(new HierarchyChangedListener() {

			@Override
			public void hierarchyChanged(HierarchyEvent arg0) {
				if (openListener != null)
					openListener.hierarchyChanged(arg0);
			}
		});

		applTab.getApplTable().addAddListener(new Listener() {

			@Override
			public void handleEvent(Event arg0) {

				facetTab.refresh();

				if (addListener != null)
					addListener.handleEvent(arg0);
			}
		});

		applTab.getApplTable().addRemoveListener(new Listener() {

			@Override
			public void handleEvent(Event arg0) {

				facetTab.refresh();

				if (removeListener != null)
					removeListener.handleEvent(arg0);
			}
		});

		applTab.getApplTable().addUsageListener(new HierarchyChangedListener() {

			@Override
			public void hierarchyChanged(HierarchyEvent arg0) {

				if (usageListener != null)
					usageListener.hierarchyChanged(arg0);
			}
		});
	}

	/**
	 * Add the implicit facet tab to the tab folder parent
	 * 
	 * @param parent
	 */
	private void addFacetTab(final TabFolder parent) {

		// add a new tab for implicit facets
		TabItem tabFacets = new TabItem(parent, SWT.NONE);
		tabFacets.setText(Messages.getString("TreeImplicitFacets.TabName"));
		tabFacets.setData("implicitFacets");

		Composite compImplicitFacets = new Composite(parent, SWT.NONE);
		compImplicitFacets.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		compImplicitFacets.setLayout(new GridLayout(1, false));

		tabFacets.setControl(compImplicitFacets);

		// create the tab 3 with implicit facets
		// we set that new facets which will be added are considered as implicit facets
		// and not as explicit (as in the describe)
		facetTab = new FrameTermImplicitFacets(compImplicitFacets, FacetType.IMPLICIT, catalogue);

		// if a facet is added in the implicit facet tab
		facetTab.addAddDescriptorListener(new Listener() {

			@Override
			public void handleEvent(Event event) {

				if (catalogue == null)
					return;

				// get the facet descriptor term attribute
				FacetDescriptor descriptor = (FacetDescriptor) event.data;

				// add the term attribute descriptor in the db
				TermAttributeDAO taDao = new TermAttributeDAO(catalogue);

				// update the term attributes of the term
				taDao.updateByA1(descriptor.getTerm());

				// refresh the content of the tab
				facetTab.setTerm(descriptor.getTerm());
			}
		});

		// if a facet is removed
		facetTab.addRemoveDescriptorListener(new Listener() {

			@Override
			public void handleEvent(Event event) {

				if (catalogue == null)
					return;

				// get the new descriptor
				FacetDescriptor descriptor = (FacetDescriptor) event.data;

				// update the implicit facets of the base term
				// related to the descriptor
				TermAttributeDAO taDao = new TermAttributeDAO(catalogue);

				taDao.updateByA1(descriptor.getTerm());

				// refresh the content of the implicit facet tab
				facetTab.setTerm(descriptor.getTerm());
			}
		});

		// update implicit facets tab only when it is shown (avoid slowdowns)
		parent.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				Object data = parent.getSelection()[0].getData();

				if (data != null && data.equals("implicitFacets")) {

					// we have selected the implicit facet tab
					facetTab.setTerm(term);
					facetTab.setVisible(true);

				} else
					facetTab.setVisible(false);
			}
		});
	}

	/**
	 * Set the current catalogue
	 * 
	 * @param catalogue
	 */
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	/**
	 * Set the current term
	 * 
	 * @param term
	 */
	public void setTerm(Term term) {
		this.term = term;
		refresh();
	}

	/**
	 * Listener called when the properties of the term properties tab changed.
	 * 
	 * @param updateListener
	 */
	public void addUpdateListener(Listener updateListener) {
		propTab.addUpdateListener(updateListener);
	}

	/**
	 * Listener called when an applicability is opened through the applicability tab
	 * 
	 * @param openListener
	 */
	public void addOpenListener(HierarchyChangedListener openListener) {
		this.openListener = openListener;
	}

	/**
	 * Listener called when an applicability is added through the applicability tab
	 * 
	 * @param addListener
	 */
	public void addAddListener(Listener addListener) {
		this.addListener = addListener;
	}

	/**
	 * Listener called when an applicability is removed through the applicability
	 * tab
	 * 
	 * @param removeListener
	 */
	public void addRemoveListener(Listener removeListener) {
		this.removeListener = removeListener;
	}

	/**
	 * Listener called when an applicability usability is changed through the
	 * applicability tab
	 * 
	 * @param usageListener
	 */
	public void addUsageListener(HierarchyChangedListener usageListener) {
		this.usageListener = usageListener;
	}

	/**
	 * Enable/disable tabs
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {

		if (!enabled) {

			resetInput();
		}

		propTab.setEnabled(enabled);
	}

	/**
	 * Reset the input of the tab
	 */
	public void resetInput() {

		propTab.reset();
		applTab.setTerm(null);
		facetTab.setTerm(null);
	}

	/**
	 * Redraw the detail level and term type of the first tab in order to adapt
	 * combobox or textbox based on edit mode or not
	 */
	public void redraw() {
		propTab.redraw();
		propTab.setTerm(term);
	}

	/**
	 * Refresh tabs
	 */
	public void refresh() {

		User user = User.getInstance();
		
		boolean editMode = catalogue != null && user.canEdit(catalogue);

		// make editable only if editing mode
		propTab.setTerm(term);
		propTab.setEditable(editMode);
		propTab.refresh();

		// update the input of the applicability table
		applTab.setTerm(term);
		applTab.refresh();
		
		// allow facet editing only in editing mode
		if (editMode)
			facetTab.addMenu();
		else
			facetTab.removeMenu();

		// update implicit facets only if necessary since
		// the operation is quite expensive
		if (facetTab.isVisible()) {
			facetTab.setTerm(term);
			facetTab.refresh();
		}

	}

	@Override
	public void update(Observable arg0, Object arg1) {

		// get updates on the selected catalogue
		if (arg0 instanceof GlobalManager && arg1 instanceof Catalogue) {

			this.catalogue = (Catalogue) arg1;

			resetInput();
		}

		// get updates on the selected term
		if (arg0 instanceof TermsTreePanel) {

			Term term = ((TermsTreePanel) arg0).getFirstSelectedTerm();
			setTerm(term);
		}

		// pass the update to the facet tab
		facetTab.update(arg0, arg1);
	}

}
