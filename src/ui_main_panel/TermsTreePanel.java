package ui_main_panel;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import already_described_terms.DescribedTerm;
import already_described_terms.Picklist;
import already_described_terms.PicklistDAO;
import already_described_terms.RecentTermDAO;
import catalogue.Catalogue;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;
import dcf_user.User;
import dcf_webservice.ReserveLevel;
import global_manager.GlobalManager;
import messages.Messages;
import term_clipboard.TermClipboard;
import term_clipboard.TermOrderChanger;
import ui_describe.FormDescribedTerms;
import ui_describe.FormTermCoder;
import ui_general_graphics.DialogSingleText;
import ui_search_bar.HierarchyChangedListener;
import ui_search_bar.HierarchyEvent;
import user_preferences.CataloguePreferenceDAO;
import utilities.GlobalUtil;


/**
 * Class to manage the main terms tree viewer (the one you can find in the main page of the tool,
 * at the center of the screen) and its contextual menu.
 * @author avonva
 *
 */
public class TermsTreePanel extends Observable implements Observer {

	private Catalogue catalogue;
	
	private MultiTermsTreeViewer tree;
	private Shell shell;
	private Hierarchy selectedHierarchy;  // current hierarchy, retrieved from observable
	
	private MenuItem otherHierarchies, deprecateTerm, termMoveDown, termMoveUp, termLevelUp, describe, recentTerms,
	addTerm, cutTerm, copyNode, copyBranch, copyCode, copyTerm, fullCopyTerm, pasteTerm, 
	prefSearchTerm, favouritePicklist;
	
	private Listener updateListener;
	private HierarchyChangedListener changeHierarchyListener;
	private ISelectionChangedListener selectionListener;
	
	// term clipboard to manage all the cut copy paste operations on terms
	TermClipboard termClip;
	
	// order changer to manage move up/move down/move level up/drag&drop operations on terms
	TermOrderChanger termOrderChanger;

	/**
	 * Constructor
	 * @param shell
	 * @param tree
	 */
	public TermsTreePanel( Composite parent, Catalogue catalogue ) {
		
		this.shell = parent.getShell();
		this.catalogue = catalogue;
		
		// initialize the term clipboard object
		termClip = new TermClipboard();
		
		// initialize the term order changer object
		termOrderChanger = new TermOrderChanger();
		
		// add the main tree viewer
		tree = createTreeViewer( parent );
	}
	
	
	/**
	 * Called when something needs to be refreshed
	 * @param listener
	 */
	public void addUpdateListener ( Listener listener ) {
		this.updateListener = listener;
	}
	
	/**
	 * Called when something needs to be refreshed
	 * @param listener
	 */
	public void addDropListener ( Listener listener ) {
		tree.addDropFinishedListener( listener );
	}
	
	/**
	 * Called when the see in other hierarchies button is pressed
	 * in the event data there is the selected hierarchy
	 * @param listener
	 */
	public void addChangeHierarchyListener ( HierarchyChangedListener listener ) {
		changeHierarchyListener = listener;
	}
	
	
	/**
	 * Called when the selection of the tree viewer changed
	 * @param listener
	 */
	public void addSelectionChangedListener ( ISelectionChangedListener selectionListener ) {
		this.selectionListener = selectionListener;
	}
	
	/**
	 * Add the contextual menu to the tree
	 */
	public void addContextualMenu( boolean forceCreation ) {

		// set the menu if no empty selection
		if ( tree.isSelectionEmpty() )
			tree.removeMenu();
		
		// if the menu is not set yet, create it
		else if ( !tree.hasMenu() || forceCreation ) {

			// create the tree contextual menu
			tree.setMenu( createTreeMenu() );
		}
	}

	/**
	 * Get the selected terms from the tree viewer
	 * @return
	 */
	public ArrayList<Term> getSelectedTerms() {
		return tree.getSelectedTerms();
	}
	
	/**
	 * Get the selected terms from the tree viewer
	 * @return
	 */
	public ArrayList<Nameable> getSelectedObjs() {
		return tree.getSelectedObjs();
	}
	
	/**
	 * Get the first selected term of the tree if it is present
	 * @return
	 */
	public Term getFirstSelectedTerm() {
		return tree.getFirstSelectedTerm();
	}
	
	/**
	 * Check if the tree has an empty selection or not
	 * @return
	 */
	public boolean isSelectionEmpty() {
		return tree.isSelectionEmpty();
	}

	
	/**
	 * Refresh the tree viewer
	 */
	public void refresh( boolean label ) {
		tree.refresh( label );
	}
	
	/**
	 * Refresh the tree viewer
	 */
	public void refresh() {
		tree.refresh();
	}
	
	/**
	 * Refresh a specific object of the tree
	 * @param term
	 */
	public void refresh( Nameable term ) {
		tree.refresh( term );
	}
	
	/**
	 * Select a term of the tree viewer
	 * @param term
	 */
	public void selectTerm ( Nameable term ) {
		tree.selectTerm( term, 0 );
	}
	
	/**
	 * Select a term of the tree viewer, expand tree to the selected level
	 * @param term
	 */
	public void selectTerm ( Nameable term, int level ) {
		tree.selectTerm( term, level );
	}
	
	/**
	 * Expand all the selected terms
	 * @param level
	 */
	public void expandSelectedTerms ( int level ) { 
		
		for ( Nameable term : getSelectedTerms() )
			selectTerm( term, level );
	}
	
	/**
	 * Collapse all the selected terms
	 * @param level
	 */
	public void collapseSelectedTerms ( int level ) { 
		
		for ( Nameable term : getSelectedTerms() )
			collapseToLevel( term, level );
	}
	
	/**
	 * Collapse the tree to the selected object
	 * @param term
	 * @param level
	 */
	public void collapseToLevel ( Nameable term, int level ) {
		tree.collapseToLevel( term, level );
	}
	
	/**
	 * Collapse the entire tree
	 */
	public void collapseAll () {
		tree.collapseAll();
	}
	
	/**
	 * Set the tree input
	 * @param input
	 */
	public void setInput ( Object input ) {
		tree.setInput( input );
	}
	
	/**
	 * Create the main tree viewer in the parent composite
	 * @param parent
	 * @param ReadOnly
	 * @return
	 */
	public MultiTermsTreeViewer createTreeViewer( Composite parent ) {

		MultiTermsTreeViewer tree = new MultiTermsTreeViewer( parent, false, 
				SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL, catalogue );
		
		// allow drag n drop
		tree.addDragAndDrop();
		
		tree.addSelectionChangedListener( new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				
				// if the selection is empty or bad instance return
				if ( arg0.getSelection().isEmpty() || 
						!( arg0.getSelection() instanceof IStructuredSelection ) )
					return;

				setChanged();
				notifyObservers();
				
				// add the menu to the tree if it was not set before
				addContextualMenu( false );
				
				if ( selectionListener != null ) {
					selectionListener.selectionChanged( arg0 );
				}
			}
		});
		
		return tree;
	}
	
	
	
	/*===========================================================
	 * 
	 *                 TREE CONTEXTUAL MENU 
	 *                 
	 *  See in other hierarchies
	 *  Move up
	 *  Move down
	 *  Move one level up
	 *  Add
	 *  Remove
	 *  Cut 
	 *  Copy
	 *  Copy code + name
	 *  Copy full code + name
	 *  Paste
	 *  Paste branch
	 *  Describe
	 *  Recently Described Terms
	 *  Picklist
	 *  Search term in picklist
	 * 
	 * 
	 *===========================================================*/
	
	/**
	 * Create a right click contextual menu for a tree which contains terms
	 */
	public Menu createTreeMenu () {

		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		/* Menu for the tree */

		Menu termMenu = new Menu( shell , SWT.POP_UP );

		/* Menu which helps browsing the hierarchies among terms */

		otherHierarchies = addChangeHierarchyMI ( termMenu );
		
		// Add edit buttons if we are in editing mode
		// and if we are not (un)reserving a catalogue
		if ( !manager.isReadOnly() ) {

			new MenuItem( termMenu, SWT.SEPARATOR );

			// term order operations
			termMoveUp = addTermMoveUpMI ( termMenu );

			termMoveDown = addTermMoveDownMI ( termMenu );

			termLevelUp = addTermLevelUpMI( termMenu );

			new MenuItem( termMenu , SWT.SEPARATOR );

			// cut copy paste operations
			cutTerm = addCutBranchMI( termMenu );
			
			copyNode = addCopyNodeMI( termMenu );
			
			copyBranch = addCopyBranchMI( termMenu );
			
			pasteTerm = addPasteMI ( termMenu );
			
			new MenuItem( termMenu , SWT.SEPARATOR );
			
			// new term operation
			addTerm = addNewTermMI( termMenu );

			deprecateTerm = addDeprecateTermMI( termMenu );

			new MenuItem( termMenu , SWT.SEPARATOR );
		}

		// add copy term code
		copyCode = addCopyCodeMI( termMenu );

		// add copy term
		copyTerm = addCopyCodeNameMI( termMenu );

		// add copy full code
		fullCopyTerm = addCopyTermFullcodeMI ( termMenu );

		// separator for cut paste elements and describe function
		new MenuItem( termMenu, SWT.SEPARATOR );

		// Describe term : describe function to add explicit facets to the terms
		describe = addDescribeMI ( termMenu );

		// add recently described terms
		recentTerms = addRecentlyDescribedTermsMI ( termMenu );

		// add favourite picklist
		favouritePicklist = addFavouritePicklistMI ( termMenu );

		// search term in picklist
		prefSearchTerm = addSearchTermInPicklistMI ( termMenu );

		
		// when the tools menu is showed update the menu items status
		termMenu.addListener( SWT.Show, new Listener() {

			public void handleEvent ( Event event ) {
				
				// get an instance of the global manager
				GlobalManager manager = GlobalManager.getInstance();
				
				// if editing mode update edit mode buttons
				if ( !manager.isReadOnly() ) {
					
					ReserveLevel level;
					
					// if the editing of the catalogue is forced
					// we use the forced editing level to refresh
					// the UI, otherwise we get the standard
					// reserve level we have on the catalogue
					String username = User.getInstance().getUsername();
					
					if ( catalogue.isForceEdit( username ) )
						level = catalogue.getForcedEditLevel( username );
					else
						level = catalogue.getReserveLevel();
					
					updateEditModeMI( level );
				}
				
				// update read only buttons
				updateReadOnlyMI();
			}
		} );
		
		return termMenu;
	}
	
	/**
	 * Update the menu item of the menu based on the selected term/hierarchy
	 * ONLY FOR EDITING MODE BUTTONS
	 */
	private void updateEditModeMI ( ReserveLevel reserveLevel ) {
		
		if ( selectedHierarchy == null )
			return;
		
		boolean canEdit = User.getInstance().canEdit( catalogue );
		boolean canEditMajor = catalogue.isLocal() || ( canEdit && reserveLevel.isMajor() );
		
		// enable add only in master hierarchy
		addTerm.setEnabled( canEdit && selectedHierarchy.isMaster() );
		
		if ( isSelectionEmpty() )
			return;
		
		// refresh deprecate term text
		if ( getFirstSelectedTerm().isDeprecated() ) {
			deprecateTerm.setText(  Messages.getString( "BrowserTreeMenu.RemoveDeprecation" ) );
			
			// allow only if the term has not deprecated parents
			// allow only for major releases
			deprecateTerm.setEnabled ( canEditMajor && 
					!getFirstSelectedTerm().hasDeprecatedParents() );
		}
		else {
			deprecateTerm.setText( Messages.getString( "BrowserTreeMenu.DeprecateTerm" ) );
			
			// allow only if the term has all the subtree deprecated, allow only for
			// major releases
			deprecateTerm.setEnabled ( canEditMajor && 
					getFirstSelectedTerm().hasAllChildrenDeprecated() );
		}
		
		// check if the selected terms can be moved in the current hierarchy
		termMoveUp.setEnabled( canEdit 
				&& termOrderChanger.canMoveUp( getSelectedTerms(), selectedHierarchy ) );
		
		termMoveDown.setEnabled( canEdit 
				&& termOrderChanger.canMoveDown( getSelectedTerms(), selectedHierarchy ) );
		
		termLevelUp.setEnabled( canEdit 
				&& termOrderChanger.canMoveLevelUp( getSelectedTerms(), selectedHierarchy ) );
		
		cutTerm.setEnabled( canEdit );

		copyNode.setEnabled( canEdit );
		
		copyBranch.setEnabled( canEdit );
		
		// can paste only if we are cutting/copying and we are pasting under a single term
		pasteTerm.setEnabled( canEdit 
				&& termClip.canPaste( 
						getFirstSelectedTerm(), selectedHierarchy ) );
	}
	
	
	/**
	 * Update the menu item of the menu based on the selected term/hierarchy
	 * ONLY FOR NON EDITING MODE BUTTONS
	 */
	private void updateReadOnlyMI () {

		if ( selectedHierarchy == null || tree.isSelectionEmpty() )
			return;
		
		otherHierarchies.setEnabled( true );
		
		boolean hasFacetCategories = catalogue.hasImplicitFacetCategories();
		
		// enable describe/recent terms and picklists only if we have facets
		describe.setEnabled ( hasFacetCategories );
		recentTerms.setEnabled( hasFacetCategories );
		
		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( catalogue );
		
		// Enable favourite picklist only if a favourite picklist was set
		favouritePicklist.setEnabled( hasFacetCategories && prefDao.hasFavouritePicklist() );
		
		copyCode.setEnabled( true );
		copyTerm.setEnabled ( true );
		fullCopyTerm.setEnabled( true );

		// search term in picklist, update text and enable
		prefSearchTerm.setEnabled( hasFacetCategories && prefDao.hasFavouritePicklist() );
		prefSearchTerm.setText( Messages.getString("BrowserTreeMenu.SearchTermInPicklistPt1") + 
				getFirstSelectedTerm().getTruncatedName( 10, true ) + 
				Messages.getString("BrowserTreeMenu.SearchTermInPicklistPt2"));
	}
	
	/*============================
	 * 
	 * TREE CONTEXTUAL MENU
	 * MENU ITEMS (SINGLE ELEMENTS)
	 * 
	 * 
	 *============================/
	
	/**
	 * Add a menu item which allows to browse the available hierarchies of the selected term
	 * @param menu
	 */
	private MenuItem addChangeHierarchyMI ( Menu menu ) {
		
		// Change hierarchy menu item of the menu when right clicking item in the main tree
		final MenuItem changeHierarchy = new MenuItem( menu , SWT.CASCADE );
		changeHierarchy.setText( Messages.getString("BrowserTreeMenu.SeeInOtherHierarchiesCmd") );

		// Initialize the menu"
		final Menu changeHierarchyMenu = new Menu( shell , SWT.DROP_DOWN );

		// Set the menu
		changeHierarchy.setMenu( changeHierarchyMenu );

		// Listener to the menu of hierarchies
		// when the menu is selected open the menu with the menuitem
		// the menuitem are the hierarchies that own the selected term
		changeHierarchyMenu.addListener(SWT.Show, new Listener() {

			@Override
			public void handleEvent(Event event) {

				// Return if no term is selected
				if ( isSelectionEmpty() )
					return;

				// reset the item of the menu, in order to update with the current term hierarchies
				for (MenuItem item : changeHierarchyMenu.getItems() )
					item.dispose();
				
				// get the hierarchies of the term
				ArrayList<Hierarchy> applHierarchies = getFirstSelectedTerm().getApplicableHierarchies();

				// if no hierarchy is found => return
				if ( applHierarchies == null )
					return;
				
				// insert the all the applicable hierarchies in the menu
				for ( int i = 0 ; i < applHierarchies.size() ; i++ ) {

					// get the current hierarchy
					final Hierarchy hierarchy = applHierarchies.get( i );

					// skip if master hierarchy, we do not show master hierarchy to normal users
					// if it is to be hidden
					if ( hierarchy.isMaster() && catalogue.isMasterHierarchyHidden() )
						continue;

					// create the menu item with the hierarchy label name
					MenuItem mi = new MenuItem( changeHierarchyMenu , SWT.PUSH );
					mi.setText( hierarchy.getLabel() );

					// if a hierarchy is selected => go to the selected hierarchy (selected term)
					mi.addSelectionListener( new SelectionAdapter() {

						public void widgetSelected ( SelectionEvent e ) {

							if ( changeHierarchyListener != null ) {

								HierarchyEvent event = new HierarchyEvent();
								event.setHierarchy( hierarchy );
								event.setTerm( getFirstSelectedTerm() );
								
								changeHierarchyListener.hierarchyChanged(event);
							}
						}
					} );
				}
			}
		});
		
		changeHierarchy.setEnabled( false );
		
		return changeHierarchy;
	}
	
	/**
	 * Add a menu item which allows moving a term up
	 * @param menu
	 */
	private MenuItem addTermMoveUpMI ( Menu menu ) {
		
		MenuItem termMoveUp = new MenuItem( menu , SWT.NONE );
		termMoveUp.setText( Messages.getString("BrowserTreeMenu.MoveUpCmd") ); //$NON-NLS-1$

		termMoveUp.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// move up the selected terms
				termOrderChanger.moveUp( getSelectedTerms(), selectedHierarchy );
				
				// refresh tree
				tree.refresh();
				
				if ( updateListener != null ) {
					updateListener.handleEvent( new Event() );
				}
			}
		} );
		
		termMoveUp.setEnabled( false );
		
		return termMoveUp;
	}
	
	/**
	 * Add a menu item which allows moving down a term
	 * @param menu
	 */
	private MenuItem addTermMoveDownMI ( Menu menu ) {
		
		MenuItem termMoveDown = new MenuItem( menu , SWT.NONE );
		termMoveDown.setText( Messages.getString("BrowserTreeMenu.MoveDownCmd") ); //$NON-NLS-1$

		termMoveDown.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// move down the selected terms
				termOrderChanger.moveDown( getSelectedTerms(), selectedHierarchy );
				
				// refresh tree
				tree.refresh();
				
				if ( updateListener != null ) {
					updateListener.handleEvent( new Event() );
				}
			}
		} );
		
		termMoveDown.setEnabled( false );
		
		return termMoveDown;
	}
	
	/**
	 * Add a menu item which allows moving a term one level up
	 * @param menu
	 */
	private MenuItem addTermLevelUpMI ( Menu menu ) {
		
		MenuItem termMoveLevelUp = new MenuItem( menu , SWT.NONE );
		termMoveLevelUp.setText( Messages.getString("BrowserTreeMenu.MoveLevelUpCmd") ); //$NON-NLS-1$

		termMoveLevelUp.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {

				if ( isSelectionEmpty() )
					return;
				
				// move one level up the selected terms
				termOrderChanger.moveLevelUp( getSelectedTerms(), selectedHierarchy );
				
				// refresh the tree
				tree.refresh();
			}
		} );
		
		termMoveLevelUp.setEnabled( false );
		
		return termMoveLevelUp;
	}
	
	
	/**
	 * Add a menu item which allows adding a new term as child of the selected term
	 * @param menu
	 */
	private MenuItem addNewTermMI ( Menu menu ) {
		
		MenuItem termAdd = new MenuItem( menu , SWT.NONE );
		termAdd.setText( Messages.getString("BrowserTreeMenu.AddNewTermCmd") );

		termAdd.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {
				
				Term child;

				TermDAO termDao = new TermDAO( catalogue );
				
				// if we do not have a term code mask we need to ask to the
				// user the term code
				if ( catalogue.getTermCodeMask() == null || 
						catalogue.getTermCodeMask().isEmpty() ) {

					DialogSingleText dialog = new DialogSingleText( shell, 1 );
					dialog.setTitle( Messages.getString( "NewTerm.Title" ) );
					dialog.setMessage( Messages.getString( "NewTerm.Message" ) );
					String code = dialog.open();

					if ( code == null )
						return;
					
					// check if the selected code is already present or not in the db
					if ( termDao.getByCode( code ) != null ) {
						
						GlobalUtil.showErrorDialog( shell, 
								Messages.getString( "NewTerm.DoubleCodeTitle" ), 
								Messages.getString( "NewTerm.DoubleCodeMessage" ) );
						return;
					}
					
					child = catalogue.addNewTerm( code, getFirstSelectedTerm(), selectedHierarchy );

				}
				else {
					
					// create a new default term with default attributes as child
					// of the selected term in the selected hierarchy
					child = catalogue.addNewTerm( getFirstSelectedTerm(), selectedHierarchy );
				}
				
				// refresh tree
				tree.refresh();
				
				// if the update listener was set call it
				if ( updateListener != null ) {
					
					// pass as data the new term
					Event event = new Event();
					event.data = child;
					
					updateListener.handleEvent( event );
				}
			}
		} );
		
		termAdd.setEnabled( false );
		
		return termAdd;
	}
	
	
	/**
	 * Add a menu item which allows to deprecate or to remove deprecation from the selected term
	 * @param menu
	 * @return
	 */
	private MenuItem addDeprecateTermMI ( Menu menu ) {
		
		MenuItem deprecateTerm = new MenuItem( menu , SWT.NONE );
		deprecateTerm.setText( Messages.getString( "BrowserTreeMenu.DeprecateTerm" ) );
		
		deprecateTerm.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				// invert the deprecated status
				getFirstSelectedTerm().setDeprecated( !getFirstSelectedTerm().isDeprecated() );
				
				TermDAO termDao = new TermDAO( catalogue );
				
				// update the term into the database
				termDao.update( getFirstSelectedTerm() );
				
				// refresh tree
				tree.refresh();
				
				// if the update listener was set call it
				if ( updateListener != null ) {
					
					// pass as data the new term
					Event event = new Event();
					event.data = getFirstSelectedTerm();
					
					updateListener.handleEvent( event );
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		deprecateTerm.setEnabled( false );
		
		return deprecateTerm;
	}
	
	/**
	 * Add a menu item which allows cutting a term
	 * @param menu
	 */
	private MenuItem addCutBranchMI ( Menu menu ) {
		
		MenuItem cutTerm = new MenuItem( menu , SWT.NONE );
		cutTerm.setText( Messages.getString("BrowserTreeMenu.CutCmd") );

		cutTerm.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {
				
				// cut branches for all the selected terms
				termClip.cutBranch( getSelectedTerms(), selectedHierarchy );
			} });
		
		cutTerm.setEnabled( false );
		
		return cutTerm;
	}

	/**
	 * Add a menu item which allows copying a term without its subtree
	 * in other hierarchies
	 * @param menu
	 */
	private MenuItem addCopyNodeMI ( Menu menu ) {
		
		MenuItem copyNode = new MenuItem( menu , SWT.NONE );
		copyNode.setText( Messages.getString("BrowserTreeMenu.CopyNodeCmd") );

		copyNode.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// copy only the selected terms without the subtree
				termClip.copyNode( getSelectedTerms(), selectedHierarchy );
			}
		} );
		
		copyNode.setEnabled( false );
		
		return copyNode;
	}
	
	/**
	 * Add a menu item which allows copying a term with all its subtree
	 * in other hierarchies
	 * @param menu
	 */
	private MenuItem addCopyBranchMI ( Menu menu ) {
		
		MenuItem copyBranch = new MenuItem( menu , SWT.NONE );
		copyBranch.setText( Messages.getString("BrowserTreeMenu.CopyBranchCmd") );

		copyBranch.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {
				
				// copy the entire branches under the selected terms
				termClip.copyBranch( getSelectedTerms(), selectedHierarchy );
			}
		} );
		
		copyBranch.setEnabled( false );
		
		return copyBranch;
	}

	/**
	 * Add a menu item which allows pasting a previously copied term
	 * @param menu
	 * @return 
	 */
	private MenuItem addPasteMI ( Menu menu ) {
		
		MenuItem pasteTerm = new MenuItem( menu , SWT.NONE );
		pasteTerm.setText( Messages.getString("BrowserTreeMenu.PasteCmd") ); //$NON-NLS-1$
		pasteTerm.setEnabled( false );

		pasteTerm.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {
				
				// paste the previous term under the new selected term under the new selected hierarchy
				termClip.paste( getFirstSelectedTerm(), selectedHierarchy );
				
				// refresh tree
				tree.refresh();
				
				// call the update listener if it was set
				if ( updateListener != null ) {
					updateListener.handleEvent( new Event() );
				}
			}
		} );
		
		pasteTerm.setEnabled( false );
		
		return pasteTerm;
	}
	
	/**
	 * Add a menu item which allows copying a term code
	 * @param menu
	 */
	private MenuItem addCopyCodeMI ( Menu menu ) {
		
		/* setting copy only code in menu item */
		MenuItem copycode = new MenuItem( menu , SWT.NONE );
		copycode.setText( Messages.getString("BrowserTreeMenu.CopyCmd") ); //$NON-NLS-1$

		copycode.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// copy the term code
				termClip.copyCode( getSelectedTerms() );
			}

		} );
		
		copycode.setEnabled( false );
		
		return copycode;
	}
	
	/**
	 * Add a menu item which allows copying a term
	 * @param menu
	 */
	private MenuItem addCopyCodeNameMI ( Menu menu ) {
		
		/* setting copy and name in menu item */
		MenuItem copyCodeName = new MenuItem( menu , SWT.NONE );
		copyCodeName.setText( Messages.getString("BrowserTreeMenu.CopyCodeNameCmd") );

		copyCodeName.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// copy the term code and name
				termClip.copyCodeName( getSelectedTerms() );
			}
		} );
		
		copyCodeName.setEnabled( false );
		
		return copyCodeName;
	}
	
	/**
	 * Add a menu item which allows copying the full code of a term
	 * @param menu
	 */
	private MenuItem addCopyTermFullcodeMI ( Menu menu ) {
		
		MenuItem fullCopyTerm = new MenuItem( menu , SWT.NONE );
		fullCopyTerm.setText( Messages.getString("BrowserTreeMenu.CopyFullCodeNameCmd") ); //$NON-NLS-1$

		fullCopyTerm.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// copy the term full code and name
				termClip.copyFullCodeName( getSelectedTerms() );
			}
		} );
		
		fullCopyTerm.setEnabled( false );
		
		return fullCopyTerm;
	}
	

	/**
	 * Add a menu item which allows opening the describe on the selected term
	 * @param menu
	 */
	private MenuItem addDescribeMI ( Menu menu ) {
		
		MenuItem describeTerm = new MenuItem( menu , SWT.NONE );
		describeTerm.setText( Messages.getString("BrowserTreeMenu.DescribeCmd") );

		// if describe is clicked
		describeTerm.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// open the describe form
				FormTermCoder tcf = new FormTermCoder( shell, 
						Messages.getString("Browser.DescribeWindowTitle"), catalogue );

				tcf.setBaseTerm( getFirstSelectedTerm() );

				tcf.display( catalogue );
			}
		} );
		
		describeTerm.setEnabled( false );
		
		return describeTerm;
	}
	
	
	/**
	 * Add a menu item which allows opening the recently described terms form
	 * @param menu
	 */
	private MenuItem addRecentlyDescribedTermsMI ( Menu menu ) {
		
		final MenuItem recentlyDescribeTerm = new MenuItem( menu , SWT.NONE );
		recentlyDescribeTerm.setText( Messages.getString("BrowserTreeMenu.RecentTermCmd") );

		recentlyDescribeTerm.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {
				
				RecentTermDAO recentDao = new RecentTermDAO( catalogue );
				
				// remove all the old terms from the recent terms
				// before showing them to the user
				recentDao.removeOldTerms();
				
				// load the list of terms: favourite terms or recently described terms (invertOrder is used to 
				// make the recent results in inverse order, that is, from the more recent to the less recent)
				ArrayList<DescribedTerm> describedTerms = recentDao.getAll();
				
				// show the window which allows to retrieve the last ten described terms
				FormDescribedTerms rdt = new FormDescribedTerms( shell, 
						Messages.getString("BrowserTreeMenu.RecentTermWindowTitle"), catalogue, describedTerms );

				rdt.display( catalogue );
			}
		} );
		
		return recentlyDescribeTerm;
	}
	
	/**
	 * Add a menu item which allows opening the favourite pick list form
	 * @param menu
	 */
	private MenuItem addFavouritePicklistMI ( Menu menu ) {
		
		MenuItem picklistMenuItem = new MenuItem( menu , SWT.NONE );
		picklistMenuItem.setText( Messages.getString("BrowserTreeMenu.PicklistCmd") ); //$NON-NLS-1$

		picklistMenuItem.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent e ) {
				
				CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( catalogue );
				
				// get the current picklist
				Picklist picklist = prefDao.getFavouritePicklist();
				
				// show the window which shows all the terms of a favourite pick list
				FormDescribedTerms rdt = new FormDescribedTerms( shell, 
						Messages.getString("BrowserTreeMenu.PicklistWindowTitle"), catalogue, picklist.getTerms() );
				
				rdt.display( catalogue );
			}
		} );
		
		picklistMenuItem.setEnabled( false );
		
		return picklistMenuItem;
	}
	
	
	/**
	 * Add a menu item which allows searching a term inside a picklist
	 * @param menu
	 */
	private MenuItem addSearchTermInPicklistMI ( Menu menu ) {
		
		// Tab to search the selected term into the favourite picklist
		// in particular, the search should find all the terms which contains
		// the selectedTerm as implicit or explicit facets

		MenuItem prefSearchTerm = new MenuItem( menu , SWT.NONE );
		prefSearchTerm.setText( Messages.getString("BrowserTreeMenu.SearchTermInPicklistCmd")); //$NON-NLS-1$

		prefSearchTerm.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( catalogue );
				
				// get the current picklist
				final Picklist picklist = prefDao.getFavouritePicklist();
				
				PicklistDAO pickDao = new PicklistDAO( catalogue );
				
				// show the window which shows all the terms of a favourite pick list
				FormDescribedTerms rdt = new FormDescribedTerms( shell, 
						Messages.getString("BrowserTreeMenu.PicklistWindowTitle"), catalogue,
						pickDao.searchTermInPicklist( picklist, getFirstSelectedTerm() ) );

				rdt.display( catalogue );
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		prefSearchTerm.setEnabled( false );
		
		return prefSearchTerm;
	}

	/**
	 * Called by the observable
	 */
	@Override
	public void update ( Observable o, Object data ) {

		tree.update( o, data );

		// update current hierarchy
		if ( o instanceof HierarchySelector ) {
			
			selectedHierarchy = ((HierarchySelector) o).getSelectedHierarchy();
			
			tree.setHierarchy( selectedHierarchy );
		}
		
		// update current catalogue
		if ( data instanceof Catalogue ) {
			
			catalogue = (Catalogue) data;
			
			if ( catalogue != null )
				tree.setHierarchy( catalogue.getDefaultHierarchy() );
		}
	}
}
