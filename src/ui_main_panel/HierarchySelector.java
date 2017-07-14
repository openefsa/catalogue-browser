package ui_main_panel;

import java.util.Collection;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;
import global_manager.GlobalManager;
import messages.Messages;
import property.ContentProviderProperty;
import property.LabelProviderProperty;
import property.SorterCatalogueObject;

/**
 * Graphics which allows selecting which hierarchy/facet lists to display
 * Extends observable in order to update all the classes (i.e. the observers) which needs the current 
 * selected hierarchy for making their calculation.
 * @author avonva
 *
 */
public class HierarchySelector extends Observable implements Observer {

	private Catalogue catalogue;
	
	// parent composite
	private Composite parent;
	
	// The combo box for selecting Hierarchies
	private ComboViewer hierarchyCombo;

	// radio button for hierarchies and facets
	private Button hierarchyBtn, facetBtn;

	// listener called when a hierarchy is selected
	private Listener selectionChangedListener;

	private Hierarchy currentHierarchy;
	
	/**
	 * Constructor, create all the graphics under the parent composite
	 * @param parent
	 */
	public HierarchySelector( Composite parent ) {
		this.parent = parent;
	}
	
	
	/**
	 * Listener which is called when a hierarchy is selected
	 * @param selectionChangedListener
	 */
	public void addSelectionChangedListener ( Listener selectionChangedListener ) {
		this.selectionChangedListener = selectionChangedListener;
	}
	
	/**
	 * Display the hierarchy filter
	 */
	public void display() {
		
		Label selectionLabel = new Label( parent , SWT.NONE );
		selectionLabel.setText( Messages.getString("HierarchySelector.Title") );

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = false;
		selectionLabel.setLayoutData( gridData );

		// radio button for visualizing hierarchies in the combo box
		hierarchyBtn = new Button( parent , SWT.RADIO );
		hierarchyBtn.setText( Messages.getString("HierarchySelector.Hierarchies") );
		hierarchyBtn.setSelection( true );
		hierarchyBtn.setEnabled( false );

		gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = false;
		hierarchyBtn.setLayoutData( gridData );

		// radio button for visualizing facets lists in the combo box
		facetBtn = new Button( parent , SWT.RADIO );
		facetBtn.setText( Messages.getString("HierarchySelector.Facets") );
		facetBtn.setEnabled( false );

		gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = false;
		facetBtn.setLayoutData( gridData );
		
		hierarchyCombo = new ComboViewer( parent , SWT.READ_ONLY );
		hierarchyCombo.setLabelProvider( new LabelProviderProperty() );
		hierarchyCombo.setContentProvider( new ContentProviderProperty() );
		hierarchyCombo.setSorter( new SorterCatalogueObject() );
		hierarchyCombo.getCombo().setEnabled( false );
		
		gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumWidth = 150;
		gridData.widthHint = 180;
		
		hierarchyCombo.getCombo().setLayoutData( gridData );
		
		
		// if a hierarchy is selected from the combo box
		hierarchyCombo.addSelectionChangedListener( new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {

				// get the selection of the combo box and notify observers
				updateCurrentHierarchy();
			}
		});
		
		// if hierarchy radio button is pressed
		hierarchyBtn.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				// return if the button is in false state
				if ( !hierarchyBtn.getSelection() )
					return;
				
				setSelection ( catalogue.getDefaultHierarchy() );
			}
		} );

		
		// if facet radio button is pressed
		facetBtn.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				// return if the button is in false state
				if ( !facetBtn.getSelection() )
					return;
				
				if ( catalogue.getFacetHierarchies().isEmpty() ) {
					System.err.println( "Cannot select facet radio button, "
							+ "no facets hierarchies were found for " + catalogue );
					return;
				}

				// get the first facet category
				Hierarchy hierarchy = catalogue.getFacetHierarchies().get(0);
				
				// select it
				setSelection ( hierarchy );
			}
		} );
	}

	/**
	 * Refresh hierarchy selector and its input
	 */
	public void refresh() {

		if ( catalogue == null )
			return;

		setInput ( catalogue.getInUseHierarchies() );

		// try to select the previous hierarchy
		if ( currentHierarchy != null ) {
			if ( !setSelection( currentHierarchy ) ) {
				// set the master or the default hierarchy
				// as selection (default choice)
				setSelection ( catalogue.getDefaultHierarchy() );
			}
		}

		hierarchyCombo.refresh();
		refreshRadioButtons();
	}

	/**
	 * Refresh the hierarchy selector filter
	 */
	public void refreshFilter () {
		setHierarchyFilter( getHierarchyFilter( facetBtn.getSelection() ) );
	}
	
	/**
	 * Set the combo box input
	 * @param hierarchies
	 */
	public void setInput ( Collection<Hierarchy> hierarchies ) {
		hierarchyCombo.setInput( hierarchies );
		hierarchyCombo.refresh();
	}
	
	/**
	 * Enable disable the entire panel
	 * @param enabled
	 */
	public void setEnabled ( boolean enabled ) {
		
		hierarchyCombo.getCombo().setEnabled( enabled );
		
		if ( enabled )
			refreshRadioButtons();
		else {
			hierarchyBtn.setEnabled( false );
			facetBtn.setEnabled( false );
		}
	}
	
	/**
	 * Refresh the radio buttons
	 */
	private void refreshRadioButtons () {
		
		// enable only if we have facets 
		// (it is useless to have radio buttons if
		// we have only base hierarchies)
		boolean radioEnabled = catalogue != null 
				&& catalogue.hasAttributeHierarchies();

		hierarchyBtn.setEnabled( radioEnabled );
		facetBtn.setEnabled( radioEnabled );
	}
	
	/**
	 * Select the chosen hierarchy
	 * @param hierarchy
	 * @return false if the hierarchy was not changed true otherwise
	 */
	@SuppressWarnings("unchecked")
	public boolean setSelection ( Hierarchy hierarchy ) {

		Collection<Hierarchy> input = ( (Collection<Hierarchy>) hierarchyCombo.getInput() );
		
		// the hierarchy is not in the hierarchies list
		if ( !input.isEmpty() && !input.contains ( hierarchy ) ) {
			System.err.println( "Cannot change hierarchy selector selection with " 
				+ hierarchy + " since it is not contained in the available hierarchies");
			return false;
		}
		
		hierarchyBtn.setSelection( hierarchy.isHierarchy() );
		facetBtn.setSelection( hierarchy.isFacet() );
		
		// set the first filter
		setHierarchyFilter ( getHierarchyFilter( hierarchy.isFacet() ) );
		
		hierarchyCombo.setSelection( new StructuredSelection( hierarchy ) );
		
		updateCurrentHierarchy();
		
		return true;
	}
	
	/**
	 * Reset all the graphics to the default
	 */
	public void resetGraphics () {
		hierarchyCombo.resetFilters();
		hierarchyCombo.setInput( null );
		hierarchyBtn.setSelection( true );
		facetBtn.setSelection( false );
	}
	
	/**
	 * Get the current hierarchy
	 * @return
	 */
	public Hierarchy getSelectedHierarchy () {
		return currentHierarchy;
	}
	
	/**
	 * Change the current hierarchy
	 * @param showFacet
	 */
	private void updateCurrentHierarchy () {

		// get the selected facet list
		currentHierarchy = (Hierarchy) ( (IStructuredSelection) hierarchyCombo.
				getSelection() ).getFirstElement();

		// call selection listener if we have a hierarchy
		if ( currentHierarchy != null ) {

			// Notify the observers that the hierarchy is now changed
			setChanged();
			notifyObservers( currentHierarchy );

			callSelectionListener( currentHierarchy );
		}
	}
	
	/**
	 * Set a filter to the hierarchy combo box
	 * @param filter
	 */
	private void setHierarchyFilter ( ViewerFilter filter ) {
		
		// remove previous filters
		hierarchyCombo.resetFilters();
		
		// show facets
		hierarchyCombo.addFilter( filter );
	}
	
	/**
	 * Call the selection listener passing as data the selected hierarchy
	 * @param hierarchy
	 */
	private void callSelectionListener ( Hierarchy hierarchy ) {

		// call the selection listener if it was set
		if ( selectionChangedListener != null ) {

			// call the listener
			Event event = new Event();
			event.data = hierarchy;
			selectionChangedListener.handleEvent( event );
		}
	}
	
	
	/**
	 * Get the hierarchies filter to show only the desired hierarchies in the combo box
	 * We use this method to show only hierarchies or facets lists and to remove the master
	 * hierarchy in non editing mode
	 * @param showFacet, if true it show facets instead of hierarchies
	 * @return
	 */
	private ViewerFilter getHierarchyFilter ( final boolean showFacet ) {
		
		// Filter the item of the comboviewer (hierarchies), in order to block master hierarchy in read mode
		ViewerFilter filter = new ViewerFilter() {

			@Override
			public boolean select(Viewer arg0, Object arg1, Object element) {
				
				// if no catalogue is opened => hide all
				if ( catalogue == null )
					return false;

				Hierarchy hierarchy = (Hierarchy) element;
				
				// if not used hierarchy return false
				if ( catalogue.getNotUsedHierarchies().contains( hierarchy ) )
					return false;
				
				// if we want only facets and the current hierarchy is a facet then return true
				if ( showFacet && hierarchy.isFacet() )
					return true;
				
				// if we want only hierarchies
				if ( !showFacet ) {

					// first check if the hierarchy is the master hierarchy
					// if so hide master if non edit mode
					//if( hierarchy.isMaster() && catalogue.isMasterHierarchyHidden() )
					//	return false;
					
					// return true if we have a hierarchy
					if ( hierarchy.isHierarchy() )
						return true;
				}
				
				// default return false
				return false;
			}
		};
		return filter;
	}

	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		
		// if the current catalogue was changed
		// update it
		if ( arg0 instanceof GlobalManager && arg1 instanceof Catalogue ) {

			this.catalogue = (Catalogue) arg1;
			
			// update the selectable hierarchies
			if ( catalogue != null )
				refresh();
		}
	}
}
