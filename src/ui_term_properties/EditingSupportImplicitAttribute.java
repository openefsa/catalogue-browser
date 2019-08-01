package ui_term_properties;

import java.util.ArrayList;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.TermAttributeDAO;
import catalogue_object.Attribute;
import catalogue_object.TermAttribute;

public class EditingSupportImplicitAttribute extends EditingSupport {

	private Catalogue catalogue;
	private Composite parent;
	
	private TermAttribute ta;
	private ArrayList<Attribute> attrs;
	private ArrayList<String> attrsLabels;
	
	private TableViewer table;
	
	public enum Column {
		KEY,
		VALUE
	};
	
	Column column;
	
	public EditingSupportImplicitAttribute( Composite parent, final TableViewer table,
			final Catalogue catalogue, TableViewerColumn viewer, Column column ) {
		super( viewer.getViewer() );
		this.catalogue = catalogue;
		this.column = column;
		this.parent = parent;
		this.table = table;
	}

	@Override
	protected boolean canEdit(Object arg0) {
		return true;
	}

	@Override
	protected CellEditor getCellEditor(Object arg0) {

		// get the selected attribute
		ta = (TermAttribute) ( (IStructuredSelection) table.
				getSelection() ).getFirstElement();
		
		AttributeDAO attrDao = new AttributeDAO( catalogue );
		attrs = attrDao.getApplicableAttributes( ta.getTerm() );
		
		// get the remaining attributes labels
		attrsLabels = new ArrayList<>();
		
		for ( int i = 0; i < attrs.size(); i++ ) {
			attrsLabels.add( attrs.get(i).getLabel() );
		}
		
		// convert into array
		String[] items = new String[ attrsLabels.size() ];
		for ( int i = 0; i < attrsLabels.size(); ++i )
			items[i] = attrsLabels.get(i);
		
		TermAttribute ta = (TermAttribute) arg0;
		String attrType = ta.getAttribute().getType();

		// if we are modifying the key object
		if ( column == Column.KEY ) {
			return new ComboBoxCellEditor( parent, items );
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
		
		Object value = ta.getValue();
		
		CellEditor e = getCellEditor( arg0 );
		if ( e instanceof  ComboBoxCellEditor ) {
			value = attrs.indexOf( ta.getAttribute() );
		}
		
		return value;
	}

	@Override
	protected void setValue(Object termAttribute, Object value) {
		
		TermAttribute ta = (TermAttribute) termAttribute;
		
		CellEditor e = getCellEditor( termAttribute );
		if ( e instanceof ComboBoxCellEditor ) {
			
			// update the attribute of the term attribute
			int intval = (int) value;
			if ( intval >= 0 && intval < attrs.size() )
				ta.setAttribute( attrs.get( intval ) );
		}
		else  // update the term attribute value
			ta.setValue( (String) value );
		
		// initialize term attribute dao
		TermAttributeDAO taDao = new TermAttributeDAO( ta.getTerm().getCatalogue() );
		
		// update the term attributes permanently in the db
		taDao.updateByA1( ta.getTerm() );
		
		// refresh the table
		table.refresh();
	}
}
