package property;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;

import catalogue_object.Attribute;
import catalogue_object.SortableCatalogueObject;
import term.WrongKeyException;

public class EditingSupportSimpleProperty extends EditingSupport {

	private final TableViewer			_viewer;

	private final String				_property;

	public EditingSupportSimpleProperty( TableViewer viewer, String property ) {
		super( viewer );
		_viewer = viewer;
		_property = property.toLowerCase();
	}

	@Override
	protected boolean canEdit ( Object element ) {

		SortableCatalogueObject attribute = (SortableCatalogueObject) element;

		// can edit code only for new hierarchies
		// convention, id = -1 => new hierarchy/attribute
		// if new we can edit it, otherwise no
		if ( _property.equals( "code" ) )
			return attribute.getId() == -1;  

		// all other fields can be edited
		return true;
	}

	@Override
	protected CellEditor getCellEditor ( Object arg0 ) {
		CellEditor e = null;

		switch ( _property ) {
		
		// strings and numerics
		case "code":
		case "name":
		case "label":
		case "scopenotes":
		case "order":
		case "status":
		case "maxlength":
		case "precision":
		case "scale":
		case "catcode":
			e = new TextCellEditor( _viewer.getTable() );
			break;
	
		// multi value fields
		case "type":
			e = new ComboBoxCellEditor( _viewer.getTable() , new String[] { 
					Attribute.stringTypeName, Attribute.booleanTypeName,
					Attribute.decimalTypeName, Attribute.integerTypeName, 
					Attribute.doubleTypeName, Attribute.catalogueTypeName } );
			break;
			
		case "applicability":
			e = new ComboBoxCellEditor( _viewer.getTable() , new String[] { Attribute.applicabilityBase, 
					Attribute.applicabilityAttribute, Attribute.applicabilityBoth } );
			break;
			
		case "single_repeatable":
			e = new ComboBoxCellEditor( _viewer.getTable() , new String[] { Attribute.cardinalitySingle, 
					Attribute.cardinalityRepeatable } );
			break;
			
		case "inheritance":
			e = new ComboBoxCellEditor( _viewer.getTable() , new String[] { 
					Attribute.inheritanceValue, Attribute.inheritanceRestriction, 
					Attribute.inheritanceDisabled } );
			break;
			
		case "reportable":
			e = new ComboBoxCellEditor( _viewer.getTable() , new String[] { 
					Attribute.reportableMandatory, Attribute.reportableOptional, 
					Attribute.reportableDisabled } );
			break;
			
		// booleans
		case "uniqueness":
		case "termcodealias":
		case "visible":
		case "searchable":
			e = new ComboBoxCellEditor( _viewer.getTable() , new String[] { Attribute.booleanTrue, 
					Attribute.booleanFalse } );
			break;
		}
		
		return e;

	}

	
	@Override
	protected Object getValue ( Object element ) {

		Object result = null;

		SortableCatalogueObject sp = (SortableCatalogueObject) element;
		
		try {
			
			result = sp.getVariableByKey( _property );

			// if we have a combobox cell editor, we take the string value
			// instead of the selection index
			CellEditor e = getCellEditor( _property );
			if ( e instanceof ComboBoxCellEditor ) {

				// get the index of the selected item
				int index = 0;
				for ( String item : ((ComboBoxCellEditor) e).getItems() ) {
					
					// if we have found the right value, stop and save the index as value
					if ( item.equals( sp.getVariableByKey( _property ) ) )
						break;
					
					index++;
				}

				// return the result
				result = index;
			}
		} catch ( WrongKeyException exception ) {
			exception.printStackTrace();
		}

		if ( _property.equals( "facet" ) ) {
			//Attribute att = (Attribute) sp;
			//result = new Integer( _facets.indexOf( att.getFacet() ) );
		}

		return result;

	}
	


	@Override
	protected void setValue ( Object element , Object value ) {

		SortableCatalogueObject sp = (SortableCatalogueObject) element;
		
		// if we have a combobox cell editor, we set the value as
		// the string we selected instead of the selection index
		CellEditor e = getCellEditor( _property );
		if ( e instanceof ComboBoxCellEditor ) {
			String [] items = ((ComboBoxCellEditor) e).getItems();
			
			// avoid out of bounds exceptions (sometimes a -1 is returned if no element is clicked)
			int intval = (int) value;
			if ( intval >= 0 && intval < items.length )
				value = items[ intval ];
			else  // otherwise return and do nothing
				return;
		}
		
		// try to set the current variable with the new value
		try {
			sp.setVariableByKey( _property, value.toString() );
		} catch (WrongKeyException e1) {
			
			e1.printStackTrace();
		}
		
		/* TODO
		if ( _property.equals( "facet" ) ) { 
			//Attribute att = (Attribute) sp;
			try {
				//att.setFacet( _facets.get( (Integer) value ) );
			} catch ( Exception exception ) {
				exception.printStackTrace();
			}
		}*/

		_viewer.refresh();
	}

}
