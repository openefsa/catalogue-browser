package ui_term_applicability;
import java.util.ArrayList;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;

import catalogue_browser_dao.ParentTermDAO;
import catalogue_object.Applicability;
import catalogue_object.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;
import dcf_user.User;
import messages.Messages;
import term.LabelProviderTerm;
import ui_describe.FormSelectTerm;
import ui_search_bar.HierarchyChangedListener;
import ui_search_bar.HierarchyEvent;
import utilities.GlobalUtil;

/**
 * Class which allows to create an applicability table (i.e. it shows the applicabilities of a term)
 * and its contextual menu
 * @author avonva
 *
 */

public class TableApplicability {

	private Catalogue catalogue;
	
	private Composite parent;                // where to create the table
	private TableViewer applicabilityTable;  // table of applicabilities
	private Term term;                       // input term
	
	// listener for the menu items of the applicability menu of the table
	private Listener addListener;
	private HierarchyChangedListener openListener;
	private Listener removeListener;
	private HierarchyChangedListener usageListener;
	
	
	/**
	 * Get the table viewer of the class
	 * @return
	 */
	public TableViewer getTable() {
		return applicabilityTable;
	}
	
	
	/**
	 * Set the listener which is called when the open menu item is pressed
	 * @param listener
	 */
	public void addOpenListener ( HierarchyChangedListener listener ) {
		openListener = listener;
	}
	
	
	/**
	 * Set the listener which is called when the remove menu item is pressed
	 * @param listener
	 */
	public void addRemoveListener ( Listener listener ) {
		removeListener = listener;
	}
	
	
	/**
	 * Set the listener which is called when the add menu item is pressed
	 * @param listener
	 */
	public void addAddListener ( Listener listener ) {
		addListener = listener;
	}
	
	
	/**
	 * Set the listener which is called when the usage menu item is pressed
	 * @param listener
	 */
	public void addUsageListener ( HierarchyChangedListener listener ) {
		usageListener = listener;
	}
	
	/**
	 * Set the term which has to be displayed
	 * @param term
	 */
	public void setTerm ( Term term ) {
		
		if ( applicabilityTable.getTable().isDisposed() )
			return;
		
		// if term null remove input and return
		if ( term == null ) {
			applicabilityTable.setInput( null );
			applicabilityTable.getTable().setMenu( null );
			return;
		}
		
		// set the current term 
		this.term = term;
		
		// set the input for the table
		applicabilityTable.setInput( term.getApplicabilities() );
		
		// refresh the table
		applicabilityTable.refresh();
		
		// create the related menu for the term
		createApplicabilityMenu(); 
	}
	
	
	/**
	 * Constructor, given the parent composite create on it an applicability table
	 * @param parent
	 * @throws Exception 
	 */
	public TableApplicability ( Composite parent, Catalogue catalogue ) {
		
		// set parameters
		this.parent = parent;
		this.catalogue = catalogue;
		
		// create a group for the applicability 
		Group groupTermApplicability = new Group( parent , SWT.NONE );

		groupTermApplicability.setText( Messages.getString("TableApplicability.TableLabel") ); //$NON-NLS-1$
		groupTermApplicability.setLayout( new FillLayout() );

		// layout data for the applicability table group
		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.minimumHeight = 200;
		gridData.heightHint = 200;
		gridData.minimumWidth = 100;
		gridData.widthHint = 100;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = false;
		groupTermApplicability.setLayoutData( gridData );

		// create an applicability table
		applicabilityTable = new TableViewer( groupTermApplicability , SWT.BORDER | SWT.SINGLE
				| SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.NONE );

		applicabilityTable.getTable().setHeaderVisible( true );
		applicabilityTable.getTable().setLayoutData( gridData );
		applicabilityTable.setContentProvider( new ContentProviderApplicability() );

		// Add the "Term" column
		GlobalUtil.addStandardColumn( applicabilityTable, new TermColumnLabelProvider( catalogue ), 
				Messages.getString("TableApplicability.TermColumn") );

		// Add the "Hierarchy/Facet" column
		GlobalUtil.addStandardColumn( applicabilityTable, new HierarchyColumnLabelProvider(), 
				Messages.getString("TableApplicability.HierarchyColumn") ); //$NON-NLS-1$

		// add the "Usage" column and its label provider
		GlobalUtil.addStandardColumn( applicabilityTable, new UsageColumnLabelProvider(), 
				Messages.getString("TableApplicability.UsageColumn"), 80, SWT.CENTER ); //$NON-NLS-1$
	}
	
	
	
	/**
	 * Label provider for the column "Hierarchy/Facet"
	 * @author avonva
	 *
	 */
	private class HierarchyColumnLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText ( Object element ) {
			Applicability p = (Applicability) element;
			return p.getHierarchy().getLabel();
		}
	}
	
	
	
	/**
	 * Label provider for the column "Usage"
	 * @author avonva
	 *
	 */
	private class UsageColumnLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText ( Object element ) {
			Applicability p = (Applicability) element;
			return String.valueOf( p.isReportable() );
		}
	}
	
	
	
	/**
	 * Label provider for the column "Term"
	 * @author avonva
	 *
	 */
	private class TermColumnLabelProvider extends ColumnLabelProvider {

		LabelProviderTerm termLabelProvider;
		
		// Constructor
		public TermColumnLabelProvider( Catalogue catalogue ) {	
			termLabelProvider = new LabelProviderTerm();
		}

		@Override
		public Image getImage ( Object element ) {
			Applicability p = (Applicability) element;
			Nameable t = p.getParentTerm();
			
			// it there is no parent we have to report the hierarchy image
			if ( t == null || t.getLabel() == null || t.getLabel().isEmpty() )
				return termLabelProvider.getImage( p.getHierarchy() );
			
			return termLabelProvider.getImage( t );

		}

		@Override
		public String getText ( Object element ) {
			
			Applicability p = (Applicability) element;
			
			// get the parent term (i.e. the applicability for the current hierarchy)
			Nameable t = p.getParentTerm();
			
			// it there is no parent we have to report the hierarchy name
			if ( t == null || t.getLabel() == null || t.getLabel().isEmpty() )
				return p.getHierarchy().getLabel();
			
			termLabelProvider.setCurrentHierarchy( p.getHierarchy() );
			
			return termLabelProvider.getText( t );
		}
	}
	
	
	/**
	 * Content provider of the table
	 * @author avonva
	 *
	 */
	private class ContentProviderApplicability implements IStructuredContentProvider {

		public void dispose ( ) {}

		public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {}

		public Object[] getElements ( Object applicabilities ) {
			@SuppressWarnings("unchecked")
			ArrayList< Applicability > apps = (ArrayList< Applicability >) applicabilities;
			if ( apps != null )
				return apps.toArray();
			else
				return null;
		}
	}
	
	
	
	/**
	 * Create the applicability menu into a table
	 * @param parent
	 * @param applicabilityTable
	 * @return
	 */
	private Menu createApplicabilityMenu () {
		
		// create menu
		Menu applicabilityOperationMenu = new Menu( parent.getShell() , SWT.POP_UP );

		// add open item
		final MenuItem openMI = addOpenApplicabilityMI ( applicabilityOperationMenu );

		new MenuItem( applicabilityOperationMenu , SWT.SEPARATOR );

		// add add item
		final MenuItem addMI = addAddApplicabilityMI ( applicabilityOperationMenu );

		// add edit item
		final MenuItem editApplicability = addEditApplicabilityMI ( applicabilityOperationMenu );

		final Menu applicabilityUsageMenu = createApplicabilityUsageMenu();
		editApplicability.setMenu( applicabilityUsageMenu );
		
		// add remove item
		final MenuItem removeMI = addRemoveApplicabilityMI ( applicabilityOperationMenu );
		
		applicabilityOperationMenu.addListener( SWT.Show, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				
				User user = User.getInstance();
				
				// can modify only if editing and if we have selected some applicabilities
				boolean enabled = !applicabilityTable.getSelection().isEmpty() 
						&& user.canEdit( term.getCatalogue() );
				
				openMI.setEnabled( enabled );
				addMI.setEnabled( enabled );
				editApplicability.setEnabled( enabled );
				removeMI.setEnabled( enabled );
				
				if ( applicabilityTable.getSelection().isEmpty() )
					return;
				
				// get the selected applicability
				Applicability appl = (Applicability) ( (IStructuredSelection) applicabilityTable
						.getSelection() ).getFirstElement();
				
				// enable removing only if the hierarchy is not the master and if the term has not children in the
				// selected hierarchy
				boolean canRemove = !appl.getHierarchy().isMaster() &&
						term.hasChildren( appl.getHierarchy(), false, false );

				removeMI.setEnabled( enabled && canRemove );
				
			}
		});
		

		applicabilityTable.getTable().setMenu( applicabilityOperationMenu );
		
		return applicabilityOperationMenu;
	}
	
	/**
	 * Add a open applicability menu item into the table menu
	 * @param menu
	 * @param table
	 * @return
	 */
	private MenuItem addOpenApplicabilityMI ( Menu menu ) {

		// create open menu item
		MenuItem openApplicability = new MenuItem( menu, SWT.PUSH );
		openApplicability.setText( Messages.getString("TableApplicability.OpenCmd") ); //$NON-NLS-1$
		openApplicability.addSelectionListener( new SelectionAdapter() {

			public void widgetSelected ( SelectionEvent e ) {

				if ( applicabilityTable.getSelection().isEmpty() || 
						! ( applicabilityTable.getSelection() instanceof IStructuredSelection ) )
					return;

				// get the selected applicability
				Applicability appl = (Applicability) ( (IStructuredSelection) applicabilityTable
						.getSelection() ).getFirstElement();

				// get the selected hierarchy and call the listener with it
				final Hierarchy selectedHierarchy = appl.getHierarchy();
				final Nameable parent = appl.getParentTerm();

				ArrayList<Object> data = new ArrayList<>();
				data.add( selectedHierarchy );
				data.add( parent );
				
				// call the open listener passing as data the selected hierarchy
				// for the open applicability 
				HierarchyEvent openEvent = new HierarchyEvent();
				openEvent.setHierarchy( selectedHierarchy );
				openEvent.setTerm( parent );

				// call the listener if it was set
				if ( openListener != null )
					openListener.hierarchyChanged( openEvent );
			}
		} );
		
		return openApplicability;
	}
	
	/**
	 * Add a remove applicability menu item into the menu
	 * @param menu
	 * @param applicabilityTable
	 * @return
	 */
	private MenuItem addRemoveApplicabilityMI ( Menu menu ) {
		
		MenuItem removeApplicability = new MenuItem( menu , SWT.PUSH );
		removeApplicability.setText( Messages.getString("TableApplicability.RemoveCmd") );

		removeApplicability.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {
				
				// get the selected applicability
				Applicability appl =(Applicability) ( (IStructuredSelection) ( applicabilityTable
						.getSelection() ) ).getFirstElement();
				
				// remove applicability? you choose
				MessageBox mb = new MessageBox( parent.getShell() , SWT.YES | SWT.NO | SWT.ICON_WARNING );
				mb.setText( Messages.getString("TableApplicability.RemoveWarningTitle") );
				mb.setMessage( Messages.getString("TableApplicability.RemoveWarningMessage") );
				
				int val = mb.open();

				if ( val == SWT.YES ) {

					// remove the applicability from the term permanently
					term.removeApplicability( appl, true );
				}

				applicabilityTable.refresh();

				// call the remove listener if it was set
				if ( removeListener != null )
					removeListener.handleEvent( new Event() );

			}
		} );
		
		return removeApplicability;
	}
	
	/*
	 * Add a add applicability menu item into the table
	 */
	private MenuItem addAddApplicabilityMI ( Menu menu ) {
		
		MenuItem addApplicability = new MenuItem( menu , SWT.PUSH );
		addApplicability.setText( Messages.getString("TableApplicability.AddCmd") );

		// if we add an applicability open the select term form to choose the parent
		addApplicability.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {

				FormSelectTerm f = new FormSelectTerm( parent.getShell(), 
						Messages.getString("TableApplicability.SelectTermWindowTitle"),
						term.getCatalogue(), false );
				
				// use the master hierarchy to choose the parent term
				// then you can select all the other hierarchies with the
				// guided approach
				f.setRootTerm( term.getCatalogue().getMasterHierarchy() );
				f.display();
				
				// return if nothing was selected
				if ( f.getSelectedTerms() == null || f.getSelectedTerms().isEmpty() )
					return;
				
				final Nameable parentTerm = (Nameable) f.getSelectedTerms().get(0);
				
				// if we have selected a term => get all the hierarchies where the parent is
				// presnet but the child not and add the applicabilities
				if ( parentTerm instanceof Term ) {
					
					if ( ( (Term) parentTerm ).getNewHierarchies( term ).isEmpty() ) {
					
						GlobalUtil.showDialog( parent.getShell(),
								Messages.getString("TableApplicability.EmptyApplicableHierarchiesTitle"),
								Messages.getString("TableApplicability.EmptyApplicableHierarchiesMessage"), 
								SWT.ICON_INFORMATION);
						
						return;
					}
					
					
					FormSelectApplicableHierarchies form = new FormSelectApplicableHierarchies(
							(Term) parentTerm, term, parent.getShell() );
					
					// if the ok button is pressed add all the applicabilities to the selected hierarchies
					form.addDoneListener( new Listener() {
						
						@Override
						public void handleEvent(Event event) {
							
							// get all the selected hierarchies
							@SuppressWarnings("unchecked")
							ArrayList<Hierarchy> hierarchies = ( ArrayList<Hierarchy> ) event.data;
							
							// for each hierarchy add the applicability
							for ( Hierarchy hierarchy : hierarchies )
								addApplicability ( parentTerm, hierarchy );
							
							applicabilityTable.refresh();
						}
					});
					
					form.display();
					
				}  // if we have selected a hierarchy, then we add the applicability between the term
				   // and the hierarchy itself (which is the parent)
				else if ( parentTerm instanceof Hierarchy ) {
					addApplicability ( parentTerm, (Hierarchy) parentTerm );
				}
				
				
				applicabilityTable.refresh();

				// call the add listener if it is defined
				if ( addListener != null )
					addListener.handleEvent( new Event() );
			}

		} );
		
		return addApplicability;
	}
	
	

	
	/**
	 * Set the applicability between the selected term and the parent in the selected hierarchy
	 * @param parent
	 * @param hierarchy
	 */
	private void addApplicability ( Nameable parentTerm, Hierarchy hierarchy ) {

		ParentTermDAO parentDao = new ParentTermDAO( term.getCatalogue() );
		
		// create the applicability for the term (note that we create it since the 
		// term in the ram is not updated. We update in this way the term in order
		// to save time
		Applicability appl = new Applicability( term, parentTerm, hierarchy, 
				parentDao.getNextAvailableOrder( term, hierarchy ), true );

		// add the applicability
		term.addApplicability( appl, true );
	}
	
	/**
	 * Create the menu which allows to make a term reportable or not
	 * @param parent
	 * @return
	 */
	private Menu createApplicabilityUsageMenu () {

		Menu menu = new Menu( parent.getShell() , SWT.DROP_DOWN );
		
		final MenuItem reportable = new MenuItem( menu, SWT.RADIO );
		reportable.setText( Messages.getString( "TableApplicability.ReportableCmd" ) );
		
		final MenuItem notReportable = new MenuItem( menu, SWT.RADIO );
		notReportable.setText( Messages.getString( "TableApplicability.NonReportableCmd" ) );
		
		
		reportable.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				changeTermReportability ( term, true );
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		
		notReportable.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				changeTermReportability ( term, false );
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		// When the menu is shown
		menu.addListener( SWT.Show, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				
				if ( applicabilityTable.getSelection().isEmpty() )
					return;
				
				Applicability appl = (Applicability) ( (IStructuredSelection) applicabilityTable.
						getSelection() ).getFirstElement();
				
				// highlight the correct button
				reportable.setSelection( term.isReportable( appl.getHierarchy() ) );
				notReportable.setSelection( !term.isReportable( appl.getHierarchy() ) );
				
			}
		});
		
		
		return menu;
	}
	
	/**
	 * Update the reportability of the term
	 * @param term
	 * @param reportable
	 */
	private void changeTermReportability ( Term term, boolean reportable ) {
		
		if ( applicabilityTable.getSelection().isEmpty() )
			return;
		
		// get the selected applicability and update the reportability of the
		// term in the related hierarchy
		Applicability appl = (Applicability) ( (IStructuredSelection) applicabilityTable.
				getSelection() ).getFirstElement();
		
		term.setReportability ( appl.getHierarchy(), reportable );
		
		ParentTermDAO parentDao = new ParentTermDAO( term.getCatalogue() );
		
		parentDao.update ( appl.getHierarchy(), term, reportable );
		
		HierarchyEvent event = new HierarchyEvent();
		event.setHierarchy( appl.getHierarchy() );
		event.setTerm( term );
		
		usageListener.hierarchyChanged( event );
		
		applicabilityTable.refresh();
	}
	
	/**
	 * Add a edit applicability menu item into the table
	 * @param parent
	 * @param applicabilityTable
	 * @return
	 */
	private MenuItem addEditApplicabilityMI ( Menu menu ) {
		
		MenuItem editApplicability = new MenuItem( menu , SWT.CASCADE );
		editApplicability.setText( Messages.getString("TableApplicability.UsageCmd") );

		
		return editApplicability;
	}
	
}
