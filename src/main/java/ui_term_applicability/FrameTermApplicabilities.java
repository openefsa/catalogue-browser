package ui_term_applicability;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_object.Term;
import i18n_messages.CBMessages;

public class FrameTermApplicabilities {

	private TableApplicability applTable;
	private TableFacetApplicability facetApplTable;
	
	public TableApplicability getApplTable() {
		return applTable;
	}
	
	public TableFacetApplicability getFacetApplTable() {
		return facetApplTable;
	}

	/**
	 * Set the selected term and update the graphics
	 * @param term
	 */
	public void setTerm ( Term term ) {
		applTable.setTerm( term );
		/*facetApplTable.setTerm( term );
		
		if ( term == null )
			restrictionTree.setInput( null );
		else {
			addRestrictionsMenu(parent.getShell(), restrictionTree.getTreeViewer() );
		}*/
	}
	
	/**
	 * Refresh all the contents
	 */
	public void refresh() {
		applTable.getTableViewer().refresh();
		/*facetApplTable.getTable().refresh();
		restrictionTree.getTreeViewer().refresh();*/
	}
	
	/**
	 * Construct the tab into the composite parent
	 * @param parent
	 */
	public FrameTermApplicabilities( Composite parent, Catalogue catalogue ) {
		
		// create applicability table
		applTable = new TableApplicability ( parent, catalogue );
		
		// group for facets applicabilities
		/*Group groupFacet = new Group( parent , SWT.NONE );
		groupFacet.setText( Messages.getString("TableFacetApplicability.Title") );
		groupFacet.setLayout( new GridLayout( 1 , true ) );*/
		
		// layout data
		/*GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;*/
		//parent.setLayoutData( gridData );

		
		// create facet applicability table
		/*facetApplTable = new TableFacetApplicability ( groupFacet );
		facetApplTable.getTable().addSelectionChangedListener( new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
			}
		});
		
		// create restriction facet tree
		restrictionTree = new TreeFacetRestrictions ( groupFacet, catalogue );*/
		
		// add the menu
		//addRestrictionsMenu ( parent.getShell(), restrictionTree.getTreeViewer() );
	}
	
	

	/**
	 * Add the restrictions contextual menu ( we define it here and not in the treefacetrestrictions object
	 * since there are some interactions with the other tables... )
	 * @param parent
	 * @param termAttributeApplicability
	 * @return
	 */
	@SuppressWarnings("unused")
	private Menu addRestrictionsMenu ( final Shell parent, final TreeViewer termAttributeApplicability ) {
		
		Menu menu = new Menu( parent , SWT.POP_UP );

		final MenuItem addTermApplicability = new MenuItem( menu , SWT.PUSH );
		addTermApplicability.setText( CBMessages.getString("TreeFacetRestrictions.AddCommand") );
		
		addTermApplicability.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {
				
			}
		} );
		
		
		final MenuItem removeTermApplicability = new MenuItem( menu , SWT.PUSH );
		removeTermApplicability.setText( CBMessages.getString("TreeFacetRestrictions.RemoveCommand") );

		removeTermApplicability.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {
				

			}
		} );

		addTermApplicability.setEnabled( false );
		removeTermApplicability.setEnabled( false );

		menu.addListener( SWT.Show, new Listener() {
				
			public void handleEvent ( Event event ) {
				
			}
		} );

		termAttributeApplicability.getTree().setMenu( menu );
		
		return menu;
	}
	
	
	
}
