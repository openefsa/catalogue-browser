package ui_describe;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_object.Attribute;
import catalogue_object.GlobalTerm;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;
import messages.Messages;
import session_manager.RestoreableWindow;
import session_manager.WindowPreference;
import ui_main_panel.MultiTermsTreeViewer;
import ui_main_panel.TermFilter;
import ui_search_bar.SearchBar;
import ui_search_bar.SearchEvent;
import ui_search_bar.SearchListener;
import ui_term_properties.FrameTermFields;
import user_preferences.UIPreference;

/**
 * This class is used to display a list of nameable 
 * (i.e terms, hierarchies, global terms) with a tree
 * visualization panel. It is possible to select the nameable
 * objects and give as output them.
 * Note that both single selection and multiple selection 
 * with checkboxes are supported.
 * @author avonva
 *
 */
public class FormSelectTerm implements Observer, RestoreableWindow {

	private static final String WINDOW_CODE = "FormSelectTerm";
	
	// output list
	private ArrayList<Nameable> selectedTerms;
	
	private boolean multi;              // multiple selection is enabled?
	private Catalogue catalogue;        // the catalogue which contains the objects to show
	private Nameable rootTerm;          // root of the tree
	private Hierarchy rootHierarchy;    // the hierarchy in which the root term is contained (if applicable)
	
	private Shell shell;
	private Shell dialog;
	private String title;
	private SearchBar searchBar;
	private TermFilter termFilter;
	private MultiTermsTreeViewer tree;
	private TableSelectedDescriptors selectedDescriptors;
	private FrameTermFields termPropTab = null;
	
	private boolean searchEnabled = true;
	
	/**
	 * Initialize the form with the shell and the title string. The boolean enableMultipleSelection is used
	 * to initialize the treeviewer as multiple selector or not.
	 * @param shell
	 * @param title
	 * @param multiSel true to enable multiple selection of objects
	 */
	
	public FormSelectTerm( Shell shell, String title, Catalogue catalogue, boolean enableMultipleSelection ) {
		
		this.shell = shell;
		this.catalogue = catalogue;
		this.multi = enableMultipleSelection;
		
		// default title
		this.title = Messages.getString( "FormSelectTerm.DialogTitle" );
		
		if ( title == null || title.length() > 0 ) {
			this.title = title;
		}
		
		selectedTerms = new ArrayList<>();
	}
	
	// The following four method are externally used, that is they are called
	// outside this form to make other operations (they are like output arrays)
	
	/**
	 * Get all the selected terms (used in both single and multiple selection)
	 * Used to make code compatible with previous version
	 * A fix could remove this variable
	 * @return
	 */
	public ArrayList<Nameable> getSelectedTerms ( ) {
		return selectedTerms;
	}
	
	
	/**
	 * Set the root term of the tree viewer for a global term.
	 * A global term is just a fake term to group different
	 * types of hierarchies and terms. For example, we can
	 * use the global term AllHierarchies to show all the
	 * hierarchies (and then the terms under the hierarchies).
	 * @param rootTerm
	 */
	public void setRootTerm ( GlobalTerm rootTerm ) {
		this.rootTerm = rootTerm;
		this.searchEnabled = false;
	}
	
	/**
	 * Set the root term of the tree viewer for
	 * a term. We need to specify which hierarchy to
	 * consider related to the term because otherwise
	 * we don't know which are the term children.
	 * @param rootTerm
	 */
	public void setRootTerm ( Term rootTerm, Hierarchy hierarchy ) {
		
		this.rootTerm = rootTerm;
		this.rootHierarchy = hierarchy;
		this.catalogue = hierarchy.getCatalogue();
	}
	
	/**
	 * Set the root term of the tree with a facet category
	 * @param rootTerm
	 */
	public void setRootTerm ( Attribute facetCategory ) {
		
		this.rootHierarchy = facetCategory.getHierarchy();
		this.rootTerm = rootHierarchy;
		this.catalogue = rootHierarchy.getCatalogue();
	}
	
	/**
	 * Set the root term of the tree with a facet category
	 * @param rootTerm
	 */
	public void setRootTerm ( Hierarchy hierarchy ) {
		
		this.rootHierarchy = hierarchy;
		this.rootTerm = rootHierarchy;
		this.catalogue = hierarchy.getCatalogue();
	}
	
	@Override
	public String getWindowCode() {
		return WINDOW_CODE;
	}
	
	@Override
	public Shell getWindowShell() {
		return dialog;
	}

	/**
	 * This method is called when the form is displayed
	 */
	public void display () {

		dialog = new Shell( shell , SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );
		dialog.setImage( new Image( Display.getCurrent() , this.getClass().getClassLoader()
				.getResourceAsStream( "Choose.gif" ) ) );
		dialog.setMaximized( true );

		dialog.setText( title );
		dialog.setLayout( new GridLayout( 2 , false ) );
		

		// Add a search bar
		searchBar = new SearchBar( dialog, false );
		
		// set search root term
		if ( rootTerm instanceof Term ) {
			searchBar.setRootTerm( (Term) rootTerm );
		}
		
		searchBar.setCatalogue( catalogue );
		searchBar.setCurrentHierarchy( rootHierarchy );
		
		// set the search listener
		searchBar.setListener( new SearchListener() {
			
			@Override
			public void searchPerformed( SearchEvent e ) {
				
				// get the search results
				ArrayList< Term > searchResults = e.getResults();
				
				// if no results are retrieved, a message box is shown
				if ( searchResults.isEmpty() ) {
					
					MessageBox mb = new MessageBox( dialog , SWT.OK );
					mb.setText( Messages.getString("FormSelectTerm.SearchResultTitle") );
					mb.setMessage( Messages.getString("FormSelectTerm.SearchResultMessage") );
					mb.open();
					return;
				}
				
				// show results
				FormDescribeSearchResult resultsForm = new FormDescribeSearchResult(shell, 
						Messages.getString("FormSelectTerm.SearchResultWindowTitle"), rootHierarchy,
						searchResults );
				
				resultsForm.setHideDeprecated( termFilter.isHidingDeprecated() );
				resultsForm.setHideNotInUse( termFilter.isHidingNotReportable() );
				
				resultsForm.display( catalogue );
				
				// get selected term
				final Term selectedTerm = resultsForm.getSelectedTerm();

				if ( selectedTerm == null )
					return;
				
				// If multi selection, check the term and
				// add it to the selected descriptors table
				if ( multi ) {
					// add if not already present
					if ( !selectedDescriptors.contains( selectedTerm ) ) {
						
						selectedDescriptors.addTerm( selectedTerm );
						
						// check term in the tree
						tree.checkTerm( selectedTerm, true );
					}
				}
				else {
					
					// add term in output
					selectedTerms.add( selectedTerm );

					// close the form (we needed only one term => job done)
					dialog.close();	
				}
			}
		});

		// show the search bar
		searchBar.display();
		
		searchBar.setEnabled( searchEnabled );

		dialog.setDefaultButton( searchBar.getButton() );
		
		// add the filters to the tree viewer
		Composite filterComp = new Composite( dialog, SWT.NONE );
		filterComp.setLayout( new GridLayout( 4, false ) );
		
		termFilter = new TermFilter( filterComp );
		termFilter.display( UIPreference.hideDeprDescribe, 
				UIPreference.hideNotReprDescribe,
				UIPreference.hideTermCodeDescribe );
		termFilter.setEnabled( true );
		
		// open tree viewer with multi selection enabled if required
		tree = new MultiTermsTreeViewer( dialog, multi, 
				SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL, catalogue );
		
		tree.setHierarchy( rootHierarchy );
		
		// set the input of the tree
		tree.setInput( rootTerm );
		
		// add the tree as observer of the term filter
		// and restore the term filter status to the
		// previous one
		termFilter.addObserver( tree );
		termFilter.addObserver( searchBar );
		termFilter.restoreStatus();
		
		// Layout settings for term properties
		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.minimumHeight = 120;
		gridData.minimumWidth = 250;
		gridData.widthHint = 250;
		gridData.heightHint = 250;
		
		// Right panel
		Composite rightPanelComposite = new Composite( dialog, SWT.NONE );
		rightPanelComposite.setLayout( new GridLayout( 1, false ) );
		rightPanelComposite.setLayoutData( gridData );
		
		// Add the selected descriptors table 
		// only if multiple selection allowed
		if ( multi ) {
			selectedDescriptors = addTableSelectedDescriptors( rightPanelComposite );
			selectedDescriptors.setCurrentHierarchy( rootHierarchy );
		}
		
		// add only scopenotes and implicit attributes 
		// of the frame term properties
		ArrayList<String> properties = new ArrayList<>();
		properties.add( "scopenotes" );
		properties.add( "attributes" );
		termPropTab = new FrameTermFields( rightPanelComposite, properties );

		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.minimumWidth = 500;
		gridData.widthHint = 300;
		
		tree.setLayoutData( gridData );

		
		Composite c2 = new Composite( dialog , SWT.NONE );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		c2.setLayoutData( gridData );

		c2.setLayout( new GridLayout( 2 , true ) );
		
		Button ok = new Button( c2 , SWT.PUSH );
		ok.setText( Messages.getString("FormSelectTerm.OkButton") );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.RIGHT;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		ok.setLayoutData( gridData );
		
		Button cancel = new Button( c2 , SWT.PUSH );
		cancel.setText( Messages.getString("FormSelectTerm.CancelButton") );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.LEFT;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		cancel.setLayoutData( gridData );
        
        // if ok button is pressed
		ok.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected ( SelectionEvent event ) {
				
				// set the output list
				setOutput();
				
				dialog.close();
			}
		} );

		cancel.addSelectionListener( new SelectionAdapter() {

			public void widgetSelected ( SelectionEvent event ) {
				dialog.close();
			}
		} );
		
		tree.addDoubleClickListener( new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent arg0) {
				
				IStructuredSelection sel = (IStructuredSelection) arg0.getSelection();
				
				if ( sel.isEmpty() )
					return;
				
				if ( multi ) {
					tree.invertTermCheck( (Nameable) sel.getFirstElement() );
				}
				else {
					
					// set the selected term as output
					setOutput();
					
					// close the dialog
					dialog.close();
				}
			}
		});
		
		// when we check/uncheck terms from the tree
		tree.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent arg0) {

				// get the checked term
				Term term = (Term) arg0.getElement();

				if ( arg0.getChecked() ) {
					
					// if the term is not already present add it
					if ( !selectedDescriptors.contains( term ) )
						selectedDescriptors.addTerm( term );
				}
				else
					selectedDescriptors.removeTerm( term );
			}
		});
		
		// When the user selects an item from the tree viewer we call this method
		tree.addSelectionChangedListener( new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {

				// if the selection is empty clear the label
				if ( tree.isSelectionEmpty() )
					return;

				// set the term for the term properties tab
				termPropTab.setTerm( tree.getFirstSelectedTerm() );
			}
		});
		
		// when enter is pressed
		dialog.addTraverseListener(new TraverseListener() {

			@Override
			public void keyTraversed(TraverseEvent e) {

				// if RETURN is pressed and the user does not use it for SEARCH purposes
				if ( e.detail == SWT.TRAVERSE_RETURN && !searchBar.getText().isFocusControl() ) {

					// return if empty selection or wrong class type
					if ( tree.isSelectionEmpty() )
						return;

					// invert the check state of the selected term
					if ( multi ) {
						tree.invertTermCheck( tree.getFirstSelectedTerm() );
					}
				}
			}
		});
		
		dialog.setMaximized( false );
		dialog.pack();

		// restore previous window dimensions
		WindowPreference.restore( this );
		WindowPreference.saveOnClosure( this );

		dialog.open();

		while ( !dialog.isDisposed() ) {
			if ( !dialog.getDisplay().readAndDispatch() )
				dialog.getDisplay().sleep();
		}
		dialog.dispose();
	}
	
	/**
	 * Add the table which contains the checked descriptors 
	 * (only multiple selection)
	 * @param parent
	 * @return
	 */
	private TableSelectedDescriptors addTableSelectedDescriptors ( Composite parent ) {
		
		TableSelectedDescriptors selectedDescriptors = new TableSelectedDescriptors( parent, catalogue );

		// called when a term is removed from the table
		selectedDescriptors.addRemoveListener( new Listener() {

			@Override
			public void handleEvent(Event event) {

				// get the selected term which has to be removed
				final Term selectedTerm = (Term) event.data;

				// remove check from tree
				tree.checkTerm( selectedTerm, false );
			}
		});
		
		selectedDescriptors.addOpenListener( new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				
				// get the selected term which has to be removed
				final Term selectedTerm = (Term) event.data;

				// remove check from tree
				tree.selectTerm( selectedTerm );
				
			}
		});
		
		// update term tab if terms of selected descriptors table are selected
		selectedDescriptors.addSelectionListener( new Listener() {

			@Override
			public void handleEvent(Event event) {

				// get the selected term
				Term selectedTerm = (Term) event.data;

				// update the term properties tab
				termPropTab.setTerm( selectedTerm );
			}
		});

		return selectedDescriptors;
	}

	/**
	 * Create the output list using the
	 * selected/checked objects of the tree
	 * We need to copy the elements since
	 * when we close the window the tree
	 * will be disposed.
	 */
	private void setOutput() {
		
		selectedTerms.clear();
		
		if ( multi ) {
			
			// add all the checked terms
			for ( Nameable obj : tree.getCheckedTerms() )
				selectedTerms.add( obj );
			
			// if no element was checked, then add the selected
			// term (if there is one)
			if ( selectedTerms.isEmpty() && !tree.isSelectionEmpty() ) {
				selectedTerms.add( tree.getFirstSelectedObj() );
			}
		}
		else {
			
			// add the selected term if it was set
			if ( !tree.isSelectionEmpty() )
				selectedTerms.add( tree.getFirstSelectedObj() );
		}
	}
	
	@Override
	public void update(Observable arg0, Object arg1) {
		tree.update(arg0, arg1);
	}
}

