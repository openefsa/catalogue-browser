package ui_term_properties;

import java.util.ArrayList;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue_browser_dao.AttributeDAO;
import catalogue_object.Attribute;
import catalogue_object.Catalogue;
import catalogue_object.TermAttribute;
import global_manager.GlobalManager;

public class EditingSupportImplicitAttribute extends EditingSupport {

	private Composite parent;
	private Listener updateListener;
	
	public enum Column {
		KEY,
		VALUE
	};
	
	Column column;
	
	public EditingSupportImplicitAttribute( Composite parent, TableViewerColumn viewer, Column column ) {
		super( viewer.getViewer() );
		this.column = column;
		this.parent = parent;
	}

	@Override
	protected boolean canEdit(Object arg0) {
		return true;
	}
	
	/**
	 * Add an update listener which is called when something needs to be updated
	 * @param listener
	 */
	public void addUpdateListener ( Listener listener ) {
		updateListener = listener;
	}

	@Override
	protected CellEditor getCellEditor(Object arg0) {

		TermAttribute ta = (TermAttribute) arg0;
		String attrType = ta.getAttribute().getType();
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		AttributeDAO attrDao = new AttributeDAO( currentCat );
		
		// if we are modifying the key object
		if ( column == Column.KEY ) {
			return new ComboBoxCellEditor( parent, attrDao.getApplicableAttributesNames( ta.getTerm() ) );
		}
		
		// if we are modifying the value object
		// if boolean type
		if ( attrType.equals( Attribute.booleanTypeName ) )
			return new ComboBoxCellEditor( parent, 
					new String[] { Attribute.booleanTrue, Attribute.booleanFalse} );
		
		// default, text cell editor
		return new TextCellEditor( parent );
	}

	@Override
	protected Object getValue(Object arg0) {

		TermAttribute ta = (TermAttribute) arg0;
		
		// get the information from the user properties file
		Object value = ta.getValue();
		
		CellEditor e = getCellEditor( arg0 );
		if ( e instanceof  ComboBoxCellEditor ) {
			
			// get the index of the selected item
			int index = 0;
			for ( String item : ( (ComboBoxCellEditor) e).getItems() ) {

				// if we have found the right value, stop and save the index as value
				if ( item.equals( ta.getValue() ) )
					break;

				index++;
			}
			
			value = index;
		}
		
		return value;

	}

	@Override
	protected void setValue(Object termAttribute, Object value) {
		
		String newValue = null;
		
		CellEditor e = getCellEditor( termAttribute );
		if ( e instanceof ComboBoxCellEditor ) {
			
			String [] items = ( (ComboBoxCellEditor) e).getItems();
			
			// avoid out of bounds exceptions (sometimes a -1 is returned if no element is clicked)
			int intval = (int) value;
			if ( intval >= 0 && intval < items.length )
				newValue = items[ intval ];
			else  // otherwise return and do nothing
				return;
		}
		else
			newValue = (String) value;
		
		// handle the event
		if ( updateListener != null ) {
			
			// hack to pass 2 different argument
			ArrayList<Object> data = new ArrayList<>();
			data.add( termAttribute );
			data.add( newValue );

			Event event = new Event();
			event.data = data;
			updateListener.handleEvent( event );
		}
	}

}
