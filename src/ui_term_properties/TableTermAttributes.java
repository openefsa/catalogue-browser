package ui_term_properties;


import java.util.ArrayList;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.TermAttributeDAO;
import catalogue_object.Attribute;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import dcf_user.User;
import messages.Messages;
import ui_term_properties.EditingSupportImplicitAttribute.Column;
import utilities.GlobalUtil;

/**
 * Table used to show all the generic attributes of a term with a key-value methods
 * We can also edit the attributes (add-remove) using a contextual menu
 * @author avonva
 *
 */
public class TableTermAttributes {

	private Composite parent;
	
	private TableViewer table;
	private TableViewerColumn keyCol;
	private TableViewerColumn valueCol;
	private Term term;
	
	public TableTermAttributes( Composite parent ) {

		this.parent = parent;
		
		// layout data for the attributes table group
		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.minimumHeight = 150;
		gridData.heightHint = 200;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;

		// create a group for the attributes 
		Group group = new Group( parent , SWT.NONE );
		group.setText( Messages.getString( "TermProperties.ImplicitAttributes" ) );
		group.setLayout( new FillLayout() );
		group.setLayoutData( gridData );

		// create the attribute table
		table = new TableViewer( group , SWT.BORDER | SWT.SINGLE
				| SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.NONE );
		
		// make headers visible
		table.getTable().setHeaderVisible( true );
		
		table.setContentProvider( new ContentProviderAttributes() );
		
		// Add the "Key" column
		keyCol = GlobalUtil.addStandardColumn( table, new KeyColumnLabelProvider(), 
				Messages.getString( "TermProperties.AttributesKeyColumn" ), 150 );
		
		// Add the "Value" column
		valueCol = GlobalUtil.addStandardColumn( table, new ValueColumnLabelProvider(), 
				Messages.getString("TermProperties.AttributesValueColumn"), 300 );
	}
	
	/**
	 * Set the table enabled or not
	 * @param enabled
	 */
	public void setEnabled ( boolean enabled ) {
		table.getTable().setEnabled(enabled);
	}
	
	
	/**
	 * Set the term which has to be displayed
	 * @param term
	 */
	public void setTerm ( Term term ) {
		
		this.term = term;
		
		// if term null return
		if ( term == null ) {
			table.setInput( null );
			removeEdit();
			return;
		}

		User user = User.getInstance();
		
		// add editing support only if possible
		if ( user.canEdit( term.getCatalogue() ) )
			addEdit();
		else
			removeEdit();
		
		// set the input for the table
		table.setInput( term.getGenericAttributes() );
		
		// refresh the table
		table.refresh();
	}
	
	/**
	 * Enable editing feature
	 */
	private void addEdit () {

		// set editing support for both columns
		keyCol.setEditingSupport( getEditingSupport( keyCol, EditingSupportImplicitAttribute.Column.KEY ) );

		valueCol.setEditingSupport( getEditingSupport( valueCol, EditingSupportImplicitAttribute.Column.VALUE ) );

		// set the contextual menu for editing purposes
		Menu menu = createMenu ( parent.getShell(), table );
		table.getTable().setMenu( menu );
	}
	
	/**
	 * Disable the editing feature
	 */
	private void removeEdit () {
		keyCol.setEditingSupport( null );
		valueCol.setEditingSupport( null );
		table.getTable().setMenu( null );
	}
	
	/**
	 * Create a menu for editing the table
	 * @param parent
	 * @return
	 */
	private Menu createMenu( Shell parent, final TableViewer table ) {
		
		Menu menu = new Menu ( parent, SWT.NONE );
		
		final MenuItem addMI = new MenuItem ( menu, SWT.PUSH );
		final MenuItem removeMI = new MenuItem ( menu, SWT.PUSH );
		
		
		// add an attribute
		addMI.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				
				TermAttribute ta = TermAttribute.getDefaultTermAttribute( term );
				
				// if no term attribute was created => no attribute is available so we return
				if ( ta == null )
					return;

				// add the element in the table
				table.add( ta );
				
				// add the attribute to the term in ram
				term.addAttribute( ta );
				
				// initialize term attribute dao
				TermAttributeDAO taDao = new TermAttributeDAO( term.getCatalogue() );
				
				// update the term attributes into the db
				taDao.updateByA1( term );
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		
		// remove an attribute
		removeMI.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				
				if ( table.getSelection().isEmpty() )
					return;
				
				// get the selected attribute
				TermAttribute ta = (TermAttribute) ( (IStructuredSelection) table.
						getSelection() ).getFirstElement();
				
				// remove the attribute from the term
				term.removeAttribute( ta );
				
				// initialize term attribute dao
				TermAttributeDAO taDao = new TermAttributeDAO( term.getCatalogue() );
				
				// remove the attribute from the database
				taDao.updateByA1( term );
				
				// refresh the table
				setTerm( term );
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		
		// Enable disable buttons
		menu.addListener( SWT.Show, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				
				addMI.setEnabled( false );
				removeMI.setEnabled( false );
				
				// no term => no action allowed
				if ( term == null )
					return;
				
				AttributeDAO attrDao = new AttributeDAO( term.getCatalogue() );
				
				// can add only if there exist generic attributes
				if ( !attrDao.fetchGeneric().isEmpty() ) {
					addMI.setEnabled( true );
					addMI.setText( Messages.getString( "TermProperties.AddCommand" ) );
				}
				else
					addMI.setText( Messages.getString( "TermProperties.NoAttributeCommand" ) );
				
				// can remove only if the term has generic attributes
				if ( !term.getGenericAttributes().isEmpty() )
					removeMI.setEnabled( true );
				
				removeMI.setText(  Messages.getString( "TermProperties.RemoveCommand" ) );
			}
		});
		
		return menu;
	}
	
	/**
	 * Get the editing support implicit attribute related to the chosen column type
	 * @param column
	 * @return
	 */
	private EditingSupportImplicitAttribute getEditingSupport ( TableViewerColumn col, 
			final EditingSupportImplicitAttribute.Column column ) {
		
		EditingSupportImplicitAttribute edit = new EditingSupportImplicitAttribute( table.getTable(),
				col, column);
		
		edit.addUpdateListener( new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				
				@SuppressWarnings("unchecked")
				ArrayList<Object> data = (ArrayList<Object>) event.data;
				
				// get the term attribute
				TermAttribute ta = (TermAttribute) data.get(0);
				
				// get the new value (key or value)
				String newValue = (String) data.get(1);
				
				if ( column == Column.KEY ) {
					
					// initialize dao for attributes
					AttributeDAO attrDao = new AttributeDAO( ta.getTerm().getCatalogue() );
					
					// update the term attribute attribute: we search for the correct attribute
					// which matches the name = newValue (since we are modifying the key)
					for ( Attribute attr : attrDao.getAll() ) {

						// if we have found the correct attribute
						if ( attr.getName().equals( newValue ) ) {
							
							// update the attribute
							ta.setAttribute( attr );
							break;
						}
					}
				}
				
				// if instead we are updating the term attribute value
				if ( column == Column.VALUE )
					ta.setValue( newValue );
				
				// initialize term attribute dao
				TermAttributeDAO taDao = new TermAttributeDAO( ta.getTerm().getCatalogue() );
				
				// update the term attributes permanently in the db
				taDao.updateByA1( term );
				
				// refresh the table with the term (the term attribute was taken from
				// the term, therefore the term is already updated)
				setTerm ( term );
			}
		});
		
		return edit;
	}
	
	/**
	 * Label provider for the column "Key"
	 * @author avonva
	 *
	 */
	private class KeyColumnLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText ( Object element ) {
			TermAttribute attr = (TermAttribute) element;
			return attr.getAttribute().getLabel();
		}
	}
	
	/**
	 * Label provider for the column "Value"
	 * @author avonva
	 *
	 */
	private class ValueColumnLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText ( Object element ) {
			TermAttribute attr = (TermAttribute) element;
			return attr.getValue();
		}
	}
	
	/**
	 * Content provider of the table
	 * @author avonva
	 *
	 */
	private class ContentProviderAttributes implements IStructuredContentProvider {

		public void dispose ( ) {}

		public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {}

		/**
		 * Get the attributes
		 */
		public Object[] getElements ( Object attributes ) {
			
			@SuppressWarnings("unchecked")
			ArrayList< Attribute > attrs = (ArrayList< Attribute >) attributes;
			
			if ( attrs != null )
				return attrs.toArray();
			else
				return null;
		}
	}
}
