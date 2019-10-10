package ui_term_applicability;

import java.util.ArrayList;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import catalogue_object.Attribute;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import i18n_messages.CBMessages;
import utilities.GlobalUtil;

/**
 * Create a facet applicability table (i.e. it shows the applicability of the
 * facets, the attribute the
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class TableFacetApplicability {

	private Composite parent; // the parent composite which hosts the table
	private TableViewer tableViewer; // table of facet applicabilities

	/**
	 * Get the table viewer of the class
	 * 
	 * @return
	 */
	public TableViewer getTable() {
		return tableViewer;
	}

	/**
	 * Set the term that has to be displayed
	 * 
	 * @param term
	 */
	public void setTerm(Term term) {

		// return if term == null
		if (term == null) {
			tableViewer.setInput(null);
			tableViewer.getTable().setMenu(null);
			return;
		}

		// update the menu according to the selected term
		// addApplicabilityFacetMenu( term );

		// update the table content
		// tableViewer.setInput( term.getAllTermAttributes() ); TODO
		tableViewer.refresh();
	}

	/**
	 * Constructor, create a facet applicability table into the parent composite
	 * 
	 * @param parent
	 */
	public TableFacetApplicability(Composite parent) {

		this.parent = parent;

		// create the table
		tableViewer = new TableViewer(parent,
				SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

		// set its content provider for the table
		tableViewer.setContentProvider(new ContentProviderTermAttribute());

		// set the layout data for the table viewer
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.minimumHeight = 150;
		gridData.heightHint = 150;

		tableViewer.getTable().setLayoutData(gridData);
		tableViewer.getTable().setHeaderVisible(true);
		// tableViewer.setSorter( new SorterTermAttribute() );

		// ### Add the "Attribute" column and its label provider ###
		GlobalUtil.addStandardColumn(tableViewer, new AttributeColumnLabelProvider(),
				CBMessages.getString("TableFacetApplicability.AttributeColumn")); //$NON-NLS-1$

		// ### Add the cardinality column and its label provider ###
		// TableViewerColumn cardinalityCol = GlobalUtil.addStandardColumn (tableViewer,
		// new CardinalityColumnLabelProvider(),
		// Messages.getString("TableFacetApplicability.CardinalityColumn") );
		// //$NON-NLS-1$

		// add the editing support
		// cardinalityCol.setEditingSupport( new EditingSupportTermAttribute(
		// tableViewer , "Cardinality" ) ); //$NON-NLS-1$

		// ### Add the Inherited column and its label provider ###
		GlobalUtil.addStandardColumn(tableViewer, new InheritedColumnLabelProvider(),
				CBMessages.getString("TableFacetApplicability.InheritedColumn")); //$NON-NLS-1$
	}

	/**
	 * Label provider for the "Attribute" column
	 * 
	 * @author avonva
	 *
	 */
	private class AttributeColumnLabelProvider extends ColumnLabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;

		}

		@Override
		public String getText(Object element) {
			// TermAttributeBETA att = (TermAttributeBETA) element;
			// return "[" + att.getAttribute().getCode() + "]" +
			// att.getAttribute().getExtendedName(); //$NON-NLS-1$ //$NON-NLS-2$
			return "";
		}
	}

	/**
	 * Label provider for the column "Cardinality"
	 * 
	 * @author avonva
	 *
	 */
	@SuppressWarnings("unused")
	private class CardinalityColumnLabelProvider extends ColumnLabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			String cardText = "";
			return cardText;
		}
	}

	/**
	 * Label provider for the "Inherited" column
	 * 
	 * @author avonva
	 *
	 */
	private class InheritedColumnLabelProvider extends ColumnLabelProvider {

		@Override
		public Image getImage(Object element) {

			return null;
		}

		@Override
		public String getText(Object element) {

			String definedText = ""; //$NON-NLS-1$

			return definedText;

		}
	}

	/**
	 * Content provider for the table viewer
	 * 
	 * @author avonva
	 *
	 */
	private class ContentProviderTermAttribute implements IStructuredContentProvider {

		public void dispose() {
		}

		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}

		public Object[] getElements(Object arg0) {
			ArrayList<TermAttribute> att = new ArrayList<>();
			return att.toArray();
		}
	}

	/**
	 * Add an attribute menu item to the facet menu
	 * 
	 * @param menu
	 * @param term
	 * @param attr
	 */
	@SuppressWarnings("unused")
	private MenuItem addAttributeMenuItem(Menu menu, final Term term, Attribute attr) {

		// create a new menu item
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		// set the text of the menu item with the attribute name
		menuItem.setText(attr.getName());

		// add a listener to the menu item
		menuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {

			}
		});

		return menuItem;
	}

	/**
	 * Add the Add applicability menu item to the menu for the selected term
	 * 
	 * @param menu
	 * @param term
	 * @return
	 */
	private MenuItem addAddMenuItem(Menu menu, final Term term) {

		// add the "add" menu item to the menu
		final MenuItem addFacet = new MenuItem(menu, SWT.CASCADE);
		addFacet.setText(CBMessages.getString("TableFacetApplicability.AddCmd")); //$NON-NLS-1$

		// only if a term is selected I will create the subMenu
		/*
		 * if ( term != null ) {
		 * 
		 * // the following is the sub-menu for the add operation final Menu
		 * addFacetsMenu = new Menu( parent.getShell() , SWT.DROP_DOWN );
		 * 
		 * // crash here //final MenuItem[] addFacetsMenuItems = new
		 * MenuItem[foodexDAO.Attributes.size()]; int c = 0; for ( Iterator< Attribute >
		 * i = foodexDAO.Attributes.iterator(); i.hasNext() ; ) {
		 * 
		 * // get the current attribute Attribute attr = i.next();
		 * 
		 * // add the menu item related to the current available attribute // if the
		 * attribute is not already present in the term attribute if (
		 * !term.hasAttribute( attr ) ) addAttributeMenuItem ( addFacetsMenu, term, attr
		 * );
		 * 
		 * } // If there are no attribute to use the function add is disabled if ( c > 0
		 * ) { addFacet.setMenu( addFacetsMenu ); addFacet.setEnabled( true ); } else
		 * addFacet.setEnabled( false ); }
		 * 
		 * // if the term is null disable the add item if ( term == null )
		 * addFacet.setEnabled( false );
		 */
		return addFacet;

	}

	/**
	 * Add the remove menu item for the applicabilities
	 * 
	 * @param menu
	 * @param term
	 * @return
	 */
	private MenuItem addRemoveMenuItem(Menu menu, final Term term) {

		// create the menu item
		final MenuItem menuItem = new MenuItem(menu, SWT.PUSH);

		// set the menu item text
		menuItem.setText(CBMessages.getString("TableFacetApplicability.RemoveCmd")); //$NON-NLS-1$

		// add the selection listener
		menuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

			}

		});

		if (term == null)
			menuItem.setEnabled(false);

		return menuItem;
	}

	/**
	 * Add a contextual menu to the applicability facet table
	 * 
	 * @param parent
	 * @param applicabilityFacetTable
	 * @return
	 */
	@SuppressWarnings("unused")
	private Menu addApplicabilityFacetMenu(Term term) {

		// create the menu
		final Menu menu = new Menu(parent.getShell(), SWT.POP_UP);

		// set the menu for the table
		tableViewer.getTable().setMenu(menu);

		// refresh the menu items to refresh the attribute which are not used
		// for the selected term
		GlobalUtil.disposeMenuItems(menu);

		// ### add menu item ###
		addAddMenuItem(menu, term);

		// ### remove menu item ###
		addRemoveMenuItem(menu, term);

		return menu;
	}
}
